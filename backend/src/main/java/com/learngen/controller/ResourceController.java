package com.learngen.controller;

import com.learngen.model.LearningResource;
import com.learngen.model.common.Result;
import com.learngen.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习资源 REST API。
 *
 * <p>对应 CLAUDE.md §9.2：
 * <ul>
 *   <li>{@code GET  /api/resources?type=doc} 查询资源列表</li>
 *   <li>{@code POST /api/resources/generate} 触发生成资源</li>
 *   <li>{@code GET  /api/resources/{id}} 资源详情</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "学习资源", description = "7 种资源类型生成与查询：doc / mindmap / quiz / reading / code / path / tutor")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "查询资源列表", description = "按学生 ID 与可选的资源类型过滤")
    public Result<List<LearningResource>> list(@RequestParam Long studentId,
                                               @RequestParam(required = false) String type) {
        log.debug("查询资源列表 studentId={} type={}", studentId, type);
        return Result.success(resourceService.listByStudent(studentId, type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询资源详情")
    public Result<LearningResource> getById(@PathVariable Long id) {
        return Result.success(resourceService.getById(id));
    }

    @PostMapping("/generate")
    @Operation(summary = "触发生成资源", description = "调对应 Agent，结合知识库摘要防幻觉")
    public Result<LearningResource> generate(@RequestBody GenerateRequest request) {
        log.info("触发生成资源 studentId={} type={} point={}",
                request.getStudentId(), request.getType(), request.getKnowledgePoint());
        LearningResource resource = resourceService.generate(
                request.getStudentId(), request.getType(), request.getKnowledgePoint());
        return Result.success(resource);
    }

    @Data
    public static class GenerateRequest {
        private Long studentId;
        private String type;
        private String knowledgePoint;
    }
}