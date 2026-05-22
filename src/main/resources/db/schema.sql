-- AI Token 管理平台 - 全量表结构

CREATE TABLE IF NOT EXISTS ai_token_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    business_name VARCHAR(255) DEFAULT NULL COMMENT '公司名称',
    password    VARCHAR(255) NOT NULL COMMENT '密码',
    phone       VARCHAR(20)  COMMENT '手机号',
    email       VARCHAR(100) COMMENT '邮箱',
    role        VARCHAR(50)  DEFAULT 'USER' COMMENT '角色',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1正常 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_username (username)
) COMMENT '用户表';

CREATE TABLE IF NOT EXISTS ai_token_usage (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    VARCHAR(100) COMMENT '账户ID',
    user_id       VARCHAR(100) COMMENT '用户ID',
    apikey_id     BIGINT       COMMENT '关联API Key ID',
    tokens        BIGINT       COMMENT 'Token消耗量',
    request_count BIGINT       COMMENT '请求次数',
    record_date   DATE         COMMENT '记录日期',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_id (account_id),
    INDEX idx_record_date (record_date)
) COMMENT 'Token使用记录表';

CREATE TABLE IF NOT EXISTS ai_token_model (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name  VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_code  VARCHAR(100) NOT NULL COMMENT '模型编码',
    description VARCHAR(500) COMMENT '模型描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '模型信息表';

CREATE TABLE IF NOT EXISTS ai_token_apikey (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL COMMENT '关联用户ID',
    apikey      VARCHAR(255) NOT NULL COMMENT 'API Key 值',
    model_id    BIGINT NOT NULL COMMENT '关联模型ID',
    use_status  TINYINT DEFAULT 1 COMMENT '使用状态 1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_model_id (model_id)
) COMMENT '用户API Key表';

-- 初始化一些模型数据
INSERT INTO ai_token_model (model_name, model_code, description) VALUES
('Claude Opus 4.7', 'claude-opus-4-7', 'Anthropic Claude Opus 4.7 模型'),
('Claude Sonnet 4.6', 'claude-sonnet-4-6', 'Anthropic Claude Sonnet 4.6 模型'),
('Claude Haiku 4.5', 'claude-haiku-4-5', 'Anthropic Claude Haiku 4.5 模型'),
('DeepSeek V4', 'deepseek-v4', 'DeepSeek V4 模型'),
('GPT-5', 'gpt-5', 'OpenAI GPT-5 模型');
