package com.learngen.service.impl;

import com.learngen.exception.BusinessException;
import com.learngen.mapper.StudentMapper;
import com.learngen.model.Student;
import com.learngen.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 学生服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;

    @Override
    public Student create(Student student) {
        if (student.getName() == null || student.getName().isBlank()) {
            throw new BusinessException(400, "学生姓名不能为空");
        }
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.insert(student);
        log.info("学生创建 id={} name={}", student.getId(), student.getName());
        return student;
    }

    @Override
    public Student getById(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(404, "学生不存在：id=" + id);
        }
        return student;
    }
}