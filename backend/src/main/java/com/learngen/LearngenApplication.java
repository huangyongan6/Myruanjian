package com.learngen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学习多智能体系统后端启动类。
 *
 * <p>对应 CLAUDE.md §4.1 包结构 {@code com.learngen}。
 */
@SpringBootApplication
@MapperScan("com.learngen.mapper")
public class LearngenApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearngenApplication.class, args);
    }
}
