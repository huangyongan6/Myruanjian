package com.learngen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learngen.model.Student;

/**
 * 学生 Mapper。
 *
 * <p>对应表 {@code student}。继承 {@link BaseMapper} 获得内置 CRUD，
 * 复杂查询使用 {@code LambdaQueryWrapper}（CLAUDE.md §11）。
 */
public interface StudentMapper extends BaseMapper<Student> {
}