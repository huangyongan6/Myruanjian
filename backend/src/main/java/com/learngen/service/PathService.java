package com.learngen.service;

import com.learngen.model.LearningPath;

/**
 * 学习路径服务接口。
 */
public interface PathService {

    /**
     * 根据学生画像触发路径规划，生成 / 更新学习路径。
     */
    LearningPath generate(Long studentId);

    /** 获取某学生的最新学习路径。 */
    LearningPath getLatest(Long studentId);
}