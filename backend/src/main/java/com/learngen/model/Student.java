package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生实体。
 *
 * <p>对应表 {@code student}。
 */
@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生姓名 */
    private String name;

    /** 头像 URL */
    private String avatar;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}