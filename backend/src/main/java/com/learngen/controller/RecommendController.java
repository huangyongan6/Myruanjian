package com.learngen.controller;

import com.learngen.model.common.Result;
import com.learngen.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 推荐 REST API。
 *
 * <p>对应 CLAUDE.md §9.2：
 * <ul>
 *   <li>{@code GET /api/recommend/{studentId}?limit=5} 获取推荐资源</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping("/{studentId}")
    public Result<List<RecommendService.RecommendedResource>> recommend(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "5") int limit) {
        log.debug("生成推荐 studentId={} limit={}", studentId, limit);
        return Result.success(recommendService.recommend(studentId, limit));
    }
}