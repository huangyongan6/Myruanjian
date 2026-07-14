package com.learngen.service;

import com.learngen.model.StudentProfile;

/**
 * 学生画像服务接口。
 *
 * <p>对应 CLAUDE.md §10.1：Service 层使用接口 + 实现类模式。
 */
public interface ProfileService {

    /**
     * 根据学生 ID 获取画像。若不存在则抛 {@code BusinessException}。
     */
    StudentProfile getByStudentId(Long studentId);

    /**
     * 创建或更新画像（基于 ProfileAgent 抽取结果）。
     *
     * @param studentId 学生 ID
     * @param profileJson ProfileAgent 输出的 JSON 字符串
     * @return 操作后的完整画像
     */
    StudentProfile upsertFromAgent(Long studentId, String profileJson);
}