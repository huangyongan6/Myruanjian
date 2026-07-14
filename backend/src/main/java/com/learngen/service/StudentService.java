package com.learngen.service;

import com.learngen.model.Student;

/**
 * 学生服务接口。
 */
public interface StudentService {

    /** 创建学生。 */
    Student create(Student student);

    /** 根据 ID 查询。 */
    Student getById(Long id);
}