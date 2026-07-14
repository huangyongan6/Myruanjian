package com.learngen.service;

import com.learngen.model.LearningResource;

import java.util.List;

/**
 * 学习资源服务接口。
 *
 * <p>对应 CLAUDE.md §4.1 ResourceService。
 */
public interface ResourceService {

    /**
     * 触发资源生成（调对应 Agent → 持久化 → 返回）。
     *
     * @param studentId       学生 ID
     * @param type            资源类型：doc / quiz / reading / code
     * @param knowledgePoint  知识点
     * @return 生成的资源（含完整 content JSON）
     */
    LearningResource generate(Long studentId, String type, String knowledgePoint);

    /**
     * 查询某学生的资源列表。
     */
    List<LearningResource> listByStudent(Long studentId, String type);

    /** 按 ID 查询。 */
    LearningResource getById(Long id);
}