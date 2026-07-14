package com.learngen.model.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应体。
 *
 * <p>对应 CLAUDE.md §8.3。
 *
 * @param <T> 记录类型
 */
@Data
@NoArgsConstructor
public class PageResult<T> {

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页（从 1 开始） */
    private int page;

    /** 每页大小 */
    private int pageSize;

    public PageResult(List<T> records, long total, int page, int pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }
}