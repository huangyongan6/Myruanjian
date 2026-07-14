package com.learngen.service;

import com.learngen.model.KnowledgePoint;

import java.util.List;
import java.util.Optional;

/**
 * 机器学习知识库服务接口。
 *
 * <p>对应 CLAUDE.md §4.1 service/KnowledgeBaseService 与 doc/05-机器学习知识库规划.md。
 * 提供两类能力：
 * <ol>
 *   <li>元数据查询（按模块 / 名称匹配 {@link KnowledgePoint}）</li>
 *   <li>Markdown 正文读取（{@code resources/knowledge/} 目录下的 .md 文件）</li>
 * </ol>
 *
 * <p>检索能力：B1 改造后支持中文分词 + 同义词扩展 + 多字段加权打分（name 3 / desc 2 / path 1）。
 */
public interface KnowledgeBaseService {

    /**
     * 按模块编号（1~6）查询所有知识点元数据。
     */
    List<KnowledgePoint> listByModule(Integer module);

    /**
     * 按 ID 查询知识点元数据。
     */
    Optional<KnowledgePoint> findById(Long id);

    /**
     * 按名称模糊查询（单 keyword 兼容入口，内部委托 {@link #searchByName(String, List)}，
     * 不走分词，向后兼容老调用方）。
     */
    List<KnowledgePoint> searchByName(String keyword);

    /**
     * 增强检索：用分词+同义词扩展后的 term 列表做多字段加权打分（name 3 / desc 2 / path 1）。
     *
     * <p>实现要点：
     * <ul>
     *   <li>取出所有 {@link KnowledgePoint}（33 条，全表扫描可接受）</li>
     *   <li>对每个 term，遍历 3 个字段累加分数</li>
     *   <li>分数 &gt; 0 的按降序返回</li>
     * </ul>
     *
     * @param keyword        原始查询词（用于缓存 key 区分）
     * @param expandedTerms  分词+同义词扩展后的 term 列表（可含大小写混合）
     */
    List<KnowledgePoint> searchByName(String keyword, List<String> expandedTerms);

    /**
     * 读取知识点对应的 Markdown 正文（位于 classpath:knowledge/{contentPath}）。
     *
     * @param point 知识点
     * @return Markdown 文本；文件不存在时返回 {@link Optional#empty()}
     */
    Optional<String> loadMarkdown(KnowledgePoint point);

    /**
     * 便捷方法：按名称查找并加载 Markdown（取第一条匹配）。
     */
    Optional<String> loadMarkdownByName(String keyword);

    /**
     * 创建知识点（CLAUDE.md §17 修改数据库时需 DDL 变更，本接口对应元数据 CRUD）。
     */
    KnowledgePoint create(KnowledgePoint point);

    /**
     * 更新知识点（按 ID）。
     */
    KnowledgePoint update(Long id, KnowledgePoint point);

    /**
     * 删除知识点。
     */
    void delete(Long id);
}