package com.learngen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.learngen.agent.AgentMessage;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.exception.AIServiceException;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.LearningResourceMapper;
import com.learngen.model.LearningResource;
import com.learngen.service.KnowledgeBaseService;
import com.learngen.service.ResourceService;
import com.learngen.agent.Orchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 资源服务实现。
 *
 * <p>对应 CLAUDE.md §10 / §11.3：构造器注入；调用 Agent 通过 Orchestrator；
 * 数据库操作使用 LambdaQueryWrapper。
 *
 * <p>对接 CLAUDE.md §19 防幻觉：触发资源生成时，先按知识点名称检索知识库，
 * 将 Markdown 正文（或摘要）作为上下文传入 Agent，避免模型捏造。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final Orchestrator orchestrator;
    private final LearningResourceMapper resourceMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RedisCacheSupport cache;

    private static final java.util.Set<String> SUPPORTED_TYPES =
            java.util.Set.of("doc", "quiz", "reading", "code", "path");

    /** 注入 Agent 上下文的知识库正文长度上限，避免 prompt 过长触发 10907 错误码。 */
    private static final int KNOWLEDGE_PREVIEW_MAX = 1500;

    @Override
    public LearningResource generate(Long studentId, String type, String knowledgePoint) {
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new BusinessException(400, "不支持的资源类型：" + type);
        }
        if (knowledgePoint == null || knowledgePoint.isBlank()) {
            throw new BusinessException(400, "知识点不能为空");
        }

        // 1. 检索知识库（CLAUDE.md §19 防幻觉）
        Map<String, Object> context = buildKnowledgeContext(knowledgePoint);

        // 2. 路由到对应 Agent
        AgentMessage result = orchestrator.dispatch(type, AgentMessage.builder()
                .studentId(studentId)
                .knowledgePoint(knowledgePoint)
                .role("user")
                .context(context)
                .build());

        String payload = result.getPayload() == null ? "" : result.getPayload();

        // 防御性校验：content 列是 JSON 类型,空串/空白会触发 "Invalid JSON text: \"The document is empty.\""
        // 若 Agent 实现了 process 但未走异常路径而返回空字符串，这里仍然兜底拦截
        if (payload.isBlank()) {
            log.warn("Agent 返回空 payload，跳过落库 studentId={} type={} point={}",
                    studentId, type, knowledgePoint);
            throw new AIServiceException(
                    "AI 未生成任何内容，请稍后再试（type=" + type + "）");
        }

        // 3. 持久化
        LearningResource resource = new LearningResource();
        resource.setStudentId(studentId);
        resource.setType(type);
        resource.setTitle(buildTitle(type, knowledgePoint));
        resource.setContent(payload);
        resource.setKnowledgePoint(knowledgePoint);
        resource.setDifficulty("medium");
        resource.setCreatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        // 失效该学生资源列表缓存；单条 byId:{newId} 不预热，下次 getById 自然填充
        cache.delete(cache.key("resource", "byStudent", studentId));
        log.info("资源生成 studentId={} type={} point={} id={} knowledgeHits={}",
                studentId, type, knowledgePoint, resource.getId(), context.size());
        return resource;
    }

    @Override
    public List<LearningResource> listByStudent(Long studentId, String type) {
        // 缓存里始终存「该学生全部资源，按 createdAt desc」；type 过滤在内存做
        List<LearningResource> cached = cache.get(
                cache.key("resource", "byStudent", studentId),
                new TypeReference<List<LearningResource>>() {});
        if (cached != null) {
            if (type == null || type.isBlank()) return cached;
            return cached.stream()
                    .filter(r -> type.equals(r.getType()))
                    .toList();
        }

        LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningResource::getStudentId, studentId)
                .orderByDesc(LearningResource::getCreatedAt);
        List<LearningResource> all = resourceMapper.selectList(wrapper);

        // 写缓存的版本不包含 type 过滤
        cache.set(cache.key("resource", "byStudent", studentId),
                all, RedisCacheSupport.TTL_RESOURCE_LIST);

        if (type == null || type.isBlank()) return all;
        return all.stream()
                .filter(r -> type.equals(r.getType()))
                .toList();
    }

    @Override
    public LearningResource getById(Long id) {
        LearningResource cached = cache.get(
                cache.key("resource", "byId", id), LearningResource.class);
        if (cached != null) return cached;

        LearningResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(404, "资源不存在：id=" + id);
        }
        cache.set(cache.key("resource", "byId", id),
                resource, RedisCacheSupport.TTL_RESOURCE_BY_ID);
        return resource;
    }

    /**
     * 检索知识库并组装 Agent 上下文。
     * <p>取按名称命中的第一条 Markdown 摘要（截断到 {@link #KNOWLEDGE_PREVIEW_MAX} 字），
     * 拼到 context.knowledge_preview，供 DocAgent / CodeCaseAgent 等消费。
     */
    private Map<String, Object> buildKnowledgeContext(String knowledgePoint) {
        Map<String, Object> context = new HashMap<>();
        context.put("knowledge_query", knowledgePoint);

        Optional<String> markdown = knowledgeBaseService.loadMarkdownByName(knowledgePoint);
        if (markdown.isPresent()) {
            String body = markdown.get();
            String preview = body.length() > KNOWLEDGE_PREVIEW_MAX
                    ? body.substring(0, KNOWLEDGE_PREVIEW_MAX) + "..."
                    : body;
            context.put("knowledge_preview", preview);
            log.debug("知识库命中 query={} previewLength={}", knowledgePoint, preview.length());
        } else {
            context.put("knowledge_preview", "");
            log.debug("知识库未命中 query={}", knowledgePoint);
        }
        return context;
    }

    private String buildTitle(String type, String knowledgePoint) {
        return switch (type) {
            case "doc" -> knowledgePoint + " 课程讲解";
            case "quiz" -> knowledgePoint + " 练习题";
            case "reading" -> knowledgePoint + " 拓展阅读";
            case "code" -> knowledgePoint + " 代码实操";
            case "path" -> knowledgePoint + " 学习路径";
            default -> knowledgePoint;
        };
    }
}