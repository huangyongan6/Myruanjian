package com.learngen.controller;

import com.learngen.model.LearningPath;
import com.learngen.model.common.Result;
import com.learngen.service.PathService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习路径 REST API。
 *
 * <p>对应 CLAUDE.md §9.2：
 * <ul>
 *   <li>{@code POST /api/paths/generate} 触发路径规划</li>
 *   <li>{@code GET  /api/paths/{studentId}} 查询最新路径</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/paths")
@RequiredArgsConstructor
public class PathController {

    private final PathService pathService;

    @PostMapping("/generate")
    public Result<LearningPath> generate(@RequestBody GenerateRequest request) {
        log.info("触发路径规划 studentId={}", request.getStudentId());
        LearningPath path = pathService.generate(request.getStudentId());
        return Result.success(path);
    }

    @GetMapping("/{studentId}")
    public Result<LearningPath> get(@PathVariable Long studentId) {
        return Result.success(pathService.getLatest(studentId));
    }

    /**
     * 更新学习路径进度：将 currentStep 推进到指定位置。
     */
    @PutMapping("/{studentId}/current-step")
    public Result<LearningPath> updateCurrentStep(@PathVariable Long studentId,
                                                   @RequestBody UpdateCurrentStepRequest request) {
        log.info("更新学习进度 studentId={} currentStep={}", studentId, request.getCurrentStep());
        LearningPath path = pathService.updateCurrentStep(studentId, request.getCurrentStep());
        return Result.success(path);
    }

    @Data
    public static class GenerateRequest {
        private Long studentId;
    }

    @Data
    public static class UpdateCurrentStepRequest {
        private Integer currentStep;
    }
}