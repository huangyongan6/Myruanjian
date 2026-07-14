package com.learngen.model.dto;

import com.learngen.model.KnowledgePoint;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 知识点详情响应（point 元数据 + Markdown 正文）。
 *
 * <p>对应 CLAUDE.md §9.4：禁止 Controller 直接返回 Map，统一返回 DTO。
 * KnowledgeController.getById 返回该对象以同时携带知识点元数据与 Markdown 正文。
 */
@Data
@AllArgsConstructor
public class KnowledgeDetailResponse {

    /** 知识点元数据。 */
    private KnowledgePoint point;

    /** Markdown 正文（若 classpath:knowledge/{contentPath} 不存在则为 null）。 */
    private String markdown;
}
