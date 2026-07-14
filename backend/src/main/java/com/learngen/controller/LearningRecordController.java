package com.learngen.controller;

import com.learngen.model.LearningRecord;
import com.learngen.model.common.Result;
import com.learngen.service.LearningRecordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 学习记录 REST API（效果评估用）。
 *
 * <p>对应 CLAUDE.md §9.2：
 * <ul>
 *   <li>{@code POST /api/records} 记录学习行为</li>
 *   <li>{@code GET  /api/records/{studentId}} 查询记录列表</li>
 *   <li>{@code GET  /api/records/{studentId}/evaluate} 学习效果评估报告</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class LearningRecordController {

    private final LearningRecordService recordService;

    @PostMapping
    public Result<LearningRecord> record(@RequestBody RecordRequest request) {
        log.debug("记录学习行为 studentId={} action={}", request.getStudentId(), request.getAction());
        return Result.success(recordService.record(
                request.getStudentId(),
                request.getResourceId(),
                request.getAction(),
                request.getScore(),
                request.getDuration()));
    }

    @GetMapping("/{studentId}")
    public Result<List<LearningRecord>> list(@PathVariable Long studentId) {
        return Result.success(recordService.listByStudent(studentId));
    }

    @GetMapping("/{studentId}/evaluate")
    public Result<Map<String, Object>> evaluate(@PathVariable Long studentId) {
        log.info("生成学习评估报告 studentId={}", studentId);
        return Result.success(recordService.evaluate(studentId));
    }

    @Data
    public static class RecordRequest {
        private Long studentId;
        private Long resourceId;
        /** view / complete / quiz */
        private String action;
        private Integer score;
        private Integer duration;
    }
}