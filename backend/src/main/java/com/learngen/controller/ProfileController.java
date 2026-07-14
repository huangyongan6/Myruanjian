package com.learngen.controller;

import com.learngen.model.StudentProfile;
import com.learngen.model.common.Result;
import com.learngen.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生画像 REST API。
 *
 * <p>对应 CLAUDE.md §9.1 / §9.2：
 * <ul>
 *   <li>{@code GET /api/profiles/{studentId}} 查询画像</li>
 *   <li>{@code POST /api/profiles/{studentId}/generate} 触发画像抽取</li>
 * </ul>
 *
 * <p>Controller 只做参数校验 + 调用 Service + 返回 Result（CLAUDE.md §9.3）。
 */
@Slf4j
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{studentId}")
    public Result<StudentProfile> getProfile(@PathVariable Long studentId) {
        log.debug("查询画像 studentId={}", studentId);
        return Result.success(profileService.getByStudentId(studentId));
    }

    /**
     * 触发画像抽取：基于已有对话上下文（body.content）调 ProfileAgent。
     * 通常由 ChatController 在对话流中调用，此入口保留手动触发能力。
     */
    @PostMapping("/{studentId}/generate")
    public Result<StudentProfile> generateProfile(@PathVariable Long studentId,
                                                  @RequestBody GenerateProfileRequest request) {
        log.info("触发画像抽取 studentId={}", studentId);
        StudentProfile profile = profileService.upsertFromAgent(studentId, request.getContent());
        return Result.success(profile);
    }

    /** 请求体：当前对话内容。 */
    @lombok.Data
    public static class GenerateProfileRequest {
        private String content;
    }
}