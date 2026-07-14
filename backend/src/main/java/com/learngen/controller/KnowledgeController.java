package com.learngen.controller;

import com.learngen.model.KnowledgePoint;
import com.learngen.model.common.Result;
import com.learngen.model.dto.KnowledgeDetailResponse;
import com.learngen.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库 REST API。
 *
 * <p>对应 CLAUDE.md §9.2：
 * <ul>
 *   <li>{@code GET /api/knowledge?module=2} 按模块列出知识点</li>
 *   <li>{@code GET /api/knowledge/search?name=线性} 按名称模糊检索</li>
 *   <li>{@code GET /api/knowledge/{id}} 知识点详情（含 Markdown 正文）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public Result<List<KnowledgePoint>> listByModule(@RequestParam(required = false) Integer module) {
        if (module == null) {
            // 不指定模块时返回空列表（避免一次性加载全部；后续可加分页）
            return Result.success(List.of());
        }
        return Result.success(knowledgeBaseService.listByModule(module));
    }

    @GetMapping("/search")
    public Result<List<KnowledgePoint>> searchByName(@RequestParam String name) {
        log.debug("按名称搜索知识点 name={}", name);
        return Result.success(knowledgeBaseService.searchByName(name));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDetailResponse> getById(@PathVariable Long id) {
        return knowledgeBaseService.findById(id)
                .map(point -> Result.success(new KnowledgeDetailResponse(
                        point,
                        knowledgeBaseService.loadMarkdown(point).orElse(null))))
                .orElse(Result.error(404, "知识点不存在：id=" + id));
    }

    /** 新增知识点（管理员用）。 */
    @PostMapping
    public Result<KnowledgePoint> create(@RequestBody KnowledgePoint point) {
        log.info("创建知识点 name={}", point.getName());
        return Result.success(knowledgeBaseService.create(point));
    }

    /** 更新知识点。 */
    @PutMapping("/{id}")
    public Result<KnowledgePoint> update(@PathVariable Long id, @RequestBody KnowledgePoint point) {
        return Result.success(knowledgeBaseService.update(id, point));
    }

    /** 删除知识点。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.success();
    }
}