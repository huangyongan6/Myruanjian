-- =====================================================================
-- 学习多智能体系统 - 数据库初始化脚本
-- 对应 doc/04-数据库设计.md，遵循 CLAUDE.md §5 数据库规范
-- 数据库：Random（开发环境）/ 字符集 utf8mb4 / 排序规则 utf8mb4_unicode_ci
-- 存储引擎：InnoDB
-- =====================================================================

-- 使用数据库
USE Random;

-- ---------------------------------------------------------------------
-- 1. 学生表 student
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '学生姓名',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息表';

-- ---------------------------------------------------------------------
-- 2. 学习画像表 student_profile（6 维度画像，JSON 存储）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS student_profile;
CREATE TABLE student_profile (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id       BIGINT NOT NULL COMMENT '学生ID',
    knowledge_base   JSON COMMENT '知识基础：{math_level, programming_level, ml_familiarity}',
    cognitive_style  JSON COMMENT '认知风格：{visual, textual, hands_on}',
    learning_goal    JSON COMMENT '学习目标：{goal_type, target_direction}',
    weak_points      JSON COMMENT '易错点：{weak_topics[], mistake_types[]}',
    learning_pace    JSON COMMENT '学习节奏：{daily_hours, pace}',
    interest_area    JSON COMMENT '兴趣方向：{areas[], preferred_project_type}',
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生学习画像表（6维度）';

-- ---------------------------------------------------------------------
-- 3. 对话记录表 chat_message
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id  BIGINT NOT NULL COMMENT '学生ID',
    role        VARCHAR(20) NOT NULL COMMENT '角色：user / assistant',
    content     TEXT NOT NULL COMMENT '消息内容',
    agent_type  VARCHAR(50) DEFAULT NULL COMMENT '响应Agent类型',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话记录表';

-- ---------------------------------------------------------------------
-- 4. 学习资源表 learning_resource（5 种类型，content 为 JSON）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS learning_resource;
CREATE TABLE learning_resource (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id       BIGINT NOT NULL COMMENT '学生ID',
    type             VARCHAR(30) NOT NULL COMMENT '资源类型：doc / mindmap / quiz / reading / code',
    title            VARCHAR(200) NOT NULL COMMENT '资源标题',
    content          JSON NOT NULL COMMENT '资源内容（JSON存储）',
    knowledge_point  VARCHAR(100) DEFAULT NULL COMMENT '关联知识点',
    difficulty       VARCHAR(20) DEFAULT 'medium' COMMENT '难度：easy / medium / hard',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_type (type),
    INDEX idx_knowledge_point (knowledge_point),
    FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

-- ---------------------------------------------------------------------
-- 5. 学习路径表 learning_path
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS learning_path;
CREATE TABLE learning_path (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id   BIGINT NOT NULL COMMENT '学生ID',
    total_steps  INT DEFAULT 0 COMMENT '总步数',
    current_step INT DEFAULT 0 COMMENT '当前步数',
    path_data    JSON NOT NULL COMMENT '路径数据',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习路径表';

-- ---------------------------------------------------------------------
-- 6. 学习记录表 learning_record（效果评估用）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS learning_record;
CREATE TABLE learning_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id    BIGINT NOT NULL COMMENT '学生ID',
    resource_id   BIGINT DEFAULT NULL COMMENT '资源ID',
    action        VARCHAR(30) NOT NULL COMMENT '行为：view / complete / quiz',
    score         INT DEFAULT NULL COMMENT '分数（quiz时）',
    duration      INT DEFAULT NULL COMMENT '学习时长（秒）',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_resource_id (resource_id),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (resource_id) REFERENCES learning_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表（效果评估用）';

-- ---------------------------------------------------------------------
-- 7. 知识点表 knowledge_point（机器学习知识库元数据）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS knowledge_point;
CREATE TABLE knowledge_point (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    module        INT NOT NULL COMMENT '模块编号：1~6',
    name          VARCHAR(100) NOT NULL COMMENT '知识点名称',
    description   VARCHAR(500) DEFAULT NULL COMMENT '知识点简介',
    content_path  VARCHAR(255) DEFAULT NULL COMMENT 'Markdown 文件路径（相对 resources/knowledge）',
    difficulty    VARCHAR(20) DEFAULT 'medium' COMMENT '难度：easy / medium / hard',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点元数据表';