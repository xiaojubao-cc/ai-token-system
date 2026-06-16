-- ================================================================
-- AI Token 管理平台 - 全量建表 + 示例数据
-- 可直接在 MySQL 中 source 执行
-- ================================================================

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS ai_token_user;
CREATE TABLE ai_token_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL COMMENT '用户名',
    business_name VARCHAR(255) DEFAULT NULL COMMENT '公司名称',
    password      VARCHAR(255) NOT NULL COMMENT '密码',
    phone         VARCHAR(20)  COMMENT '手机号',
    email         VARCHAR(100) COMMENT '邮箱',
    user_id       VARCHAR(255) COMMENT '天翼云用户ID',
    account_id    VARCHAR(255) COMMENT '天翼云账户ID',
    access_key    VARCHAR(255) COMMENT '天翼云AccessKey',
    security_key  VARCHAR(255) COMMENT '天翼云SecurityKey',
    role          VARCHAR(50)  DEFAULT 'USER' COMMENT '角色 ADMIN/USER',
    status        TINYINT      DEFAULT 1 COMMENT '状态 1正常 0禁用',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_username (username)
) COMMENT '用户表';

-- ----------------------------
-- 2. 用户 API Key 表
-- ----------------------------
DROP TABLE IF EXISTS ai_token_apikey;
CREATE TABLE ai_token_apikey (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '关联 ai_token_user.id',
    apikey      VARCHAR(500) COMMENT 'API Key 明文（用户自定义标识）',
    secret_key  VARCHAR(500) COMMENT 'API Key 密文（天翼云返回）',
    use_status  TINYINT      DEFAULT 1 COMMENT '使用状态 1正常 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '用户API Key表';

-- ----------------------------
-- 3. 模型信息表
-- ----------------------------
DROP TABLE IF EXISTS ai_token_model;
CREATE TABLE ai_token_model (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name  VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_code  VARCHAR(100) NOT NULL COMMENT '模型编码',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_model_code (model_code)
) COMMENT '模型信息表';

-- ----------------------------
-- 4. Token 用量汇总表（按 apikey + 日期 聚合）
-- ----------------------------
DROP TABLE IF EXISTS ai_token_usage;
CREATE TABLE ai_token_usage (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id     VARCHAR(100) COMMENT '账户ID',
    user_id        VARCHAR(100) COMMENT '用户ID',
    apikey_id      BIGINT       COMMENT '关联 ai_token_apikey.id',
    tokens         BIGINT       COMMENT 'Token消耗量',
    input_tokens   BIGINT       DEFAULT 0 COMMENT '输入Token量',
    output_tokens  BIGINT       DEFAULT 0 COMMENT '输出Token量',
    request_count  BIGINT       COMMENT '请求次数',
    total_duration BIGINT       DEFAULT 0 COMMENT '总时长(毫秒)',
    total_amount   BIGINT       DEFAULT 0 COMMENT '总张数',
    record_date    DATE         COMMENT '记录日期',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_id (account_id),
    INDEX idx_record_date (record_date),
    INDEX idx_apikey_date (apikey_id, record_date)
) COMMENT 'Token使用记录表';

-- ----------------------------
-- 5. Token 用量详情 - 模型维度
-- ----------------------------
DROP TABLE IF EXISTS ai_token_usage_detail;
CREATE TABLE ai_token_usage_detail (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usage_id       BIGINT NOT NULL COMMENT '关联 ai_token_usage.id',
    name           VARCHAR(100) COMMENT '模型名称',
    amount         BIGINT DEFAULT 0 COMMENT '张数',
    amount_request BIGINT DEFAULT 0 COMMENT '按张请求',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_detail_usage_id (usage_id)
) COMMENT 'Token使用详情-模型维度';

-- ----------------------------
-- 6. Token 用量详情 - 阶梯信息
-- ----------------------------
DROP TABLE IF EXISTS ai_token_usage_detail_stage;
CREATE TABLE ai_token_usage_detail_stage (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    detail_id    BIGINT NOT NULL COMMENT '关联 ai_token_usage_detail.id',
    input_tokens BIGINT DEFAULT 0 COMMENT '输入Token量',
    output_tokens BIGINT DEFAULT 0 COMMENT '输出Token量',
    min_context  BIGINT COMMENT '最小上下文',
    max_context  BIGINT COMMENT '最大上下文',
    INDEX idx_stage_detail_id (detail_id)
) COMMENT 'Token使用详情-阶梯信息';

-- ----------------------------
-- 7. Token 用量详情 - 分辨率时长
-- ----------------------------
DROP TABLE IF EXISTS ai_token_usage_detail_res_duration;
CREATE TABLE ai_token_usage_detail_res_duration (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    detail_id     BIGINT NOT NULL COMMENT '关联 ai_token_usage_detail.id',
    resolution    VARCHAR(50) COMMENT '分辨率',
    cnt           BIGINT DEFAULT 0 COMMENT '时长(毫秒)',
    request_count INT DEFAULT 0 COMMENT '请求次数',
    INDEX idx_res_dur_detail_id (detail_id)
) COMMENT 'Token使用详情-分辨率时长';

-- ----------------------------
-- 8. Token 用量详情 - 分辨率Token
-- ----------------------------
DROP TABLE IF EXISTS ai_token_usage_detail_res_token;
CREATE TABLE  ai_token_usage_detail_res_token (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    detail_id                   BIGINT NOT NULL COMMENT '关联 ai_token_usage_detail.id',
    resolution                  VARCHAR(50) COMMENT '分辨率',
    video_mode_output_token     BIGINT DEFAULT 0 COMMENT '有视频输出Token',
    video_less_mode_output_token BIGINT DEFAULT 0 COMMENT '无视频输出Token',
    request_count               INT DEFAULT 0 COMMENT '请求次数',
    INDEX idx_res_tok_detail_id (detail_id)
) COMMENT 'Token使用详情-分辨率Token';


-- ================================================================
-- 示例数据
-- ================================================================

-- ----------------------------
-- 用户：1 管理员 + 1 普通用户
-- 密码均为 123456（BCrypt 加密，实际使用时替换为加密值）
-- ----------------------------
INSERT INTO ai_token_user (id, username, business_name, password, phone, email, user_id, account_id, access_key, security_key, role, status) VALUES
(1, 'admin',         '管理平台',     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '13800000001', 'admin@test.com',   NULL, NULL, NULL, NULL, 'ADMIN', 1),
(2, 'demo_company',  '演示科技有限公司', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '13800000002', 'demo@test.com',    'ctyun_user_001', 'acc_001', 'ak_test_xxxxxxxxxx', 'sk_test_yyyyyyyyyy', 'USER', 1),
(3, 'test_company',  '测试企业有限公司', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '13900000003', 'test@company.com', 'ctyun_user_003', 'acc_003', 'ak_test_zzzzzzzzzzzzzz', 'sk_test_wwwwwwwwwwwww', 'USER', 1);

-- ----------------------------
-- API Key（归属于 demo_company 用户）
-- ----------------------------
INSERT INTO ai_token_apikey (id, user_id, apikey, use_status) VALUES
(1, 2, 'sk-apikey-prod-deepseek-xxxxxxxxxxxx', 1),
(2, 2, 'sk-apikey-test-claude-yyyyyyyyyyyyyy', 1);

-- ----------------------------
-- API Key（归属于 test_company 用户）
-- ----------------------------
INSERT INTO ai_token_apikey (id, user_id, apikey, use_status) VALUES
(3, 3, 'sk-apikey-test-deepseek-aaaaaaaaaa', 1),
(4, 3, 'sk-apikey-test-qwen-bbbbbbbbbb', 1);

-- ----------------------------
-- 模型数据
-- ----------------------------
INSERT INTO ai_token_model (model_name, model_code, description) VALUES
('Claude Opus 4.7',    'claude-opus-4-7',    'Anthropic Claude Opus 4.7 模型'),
('Claude Sonnet 4.6',  'claude-sonnet-4-6',  'Anthropic Claude Sonnet 4.6 模型'),
('Claude Haiku 4.5',   'claude-haiku-4-5',   'Anthropic Claude Haiku 4.5 模型'),
('DeepSeek V4',        'deepseek-v4',         'DeepSeek V4 模型'),
('GPT-5',              'gpt-5',               'OpenAI GPT-5 模型');

-- ----------------------------
-- Token 用量汇总（最近 7 天，apikey_id=1 和 apikey_id=2 各 6 条）
-- ----------------------------
INSERT INTO ai_token_usage (account_id, user_id, apikey_id, tokens, input_tokens, output_tokens, request_count, total_duration, total_amount, record_date) VALUES
-- apikey_id=1 (user=2, demo_company)
('acc_001', 'ctyun_user_001', 1, 125000,  75000,  50000, 320, 1800000, 12, '2026-05-30'),
('acc_001', 'ctyun_user_001', 1,  98000,  58000,  40000, 280, 1500000,  8, '2026-05-31'),
('acc_001', 'ctyun_user_001', 1, 156000,  90000,  66000, 410, 2200000, 15, '2026-06-01'),
('acc_001', 'ctyun_user_001', 1,  88000,  52000,  36000, 230, 1200000,  6, '2026-06-02'),
('acc_001', 'ctyun_user_001', 1, 210000, 130000,  80000, 520, 3100000, 20, '2026-06-03'),
('acc_001', 'ctyun_user_001', 1, 175000, 105000,  70000, 450, 2600000, 18, '2026-06-04'),
('acc_001', 'ctyun_user_001', 1, 142000,  88000,  54000, 380, 2100000, 14, '2026-06-05'),
-- apikey_id=2 (user=2, demo_company)
('acc_001', 'ctyun_user_001', 2,  65000,  40000,  25000, 180,  900000,  5, '2026-05-30'),
('acc_001', 'ctyun_user_001', 2,  72000,  43000,  29000, 200, 1050000,  7, '2026-05-31'),
('acc_001', 'ctyun_user_001', 2,  89000,  54000,  35000, 260, 1450000,  9, '2026-06-01'),
('acc_001', 'ctyun_user_001', 2,  56000,  34000,  22000, 150,  780000,  4, '2026-06-02'),
('acc_001', 'ctyun_user_001', 2, 134000,  80000,  54000, 380, 2100000, 14, '2026-06-03'),
('acc_001', 'ctyun_user_001', 2, 118000,  71000,  47000, 310, 1850000, 11, '2026-06-04'),
('acc_001', 'ctyun_user_001', 2,  96000,  58000,  38000, 250, 1400000,  9, '2026-06-05'),
-- apikey_id=3 (user=3, test_company)
('acc_003', 'ctyun_user_003', 3,  85000,  50000,  35000, 220, 1200000,  9, '2026-05-30'),
('acc_003', 'ctyun_user_003', 3,  72000,  42000,  30000, 200, 1050000,  7, '2026-05-31'),
('acc_003', 'ctyun_user_003', 3,  98000,  60000,  38000, 280, 1500000, 11, '2026-06-01'),
('acc_003', 'ctyun_user_003', 3,  65000,  38000,  27000, 180,  900000,  5, '2026-06-02'),
('acc_003', 'ctyun_user_003', 3, 110000,  68000,  42000, 320, 1700000, 13, '2026-06-03'),
('acc_003', 'ctyun_user_003', 3,  95000,  56000,  39000, 260, 1400000, 10, '2026-06-04'),
('acc_003', 'ctyun_user_003', 3,  78000,  46000,  32000, 210, 1150000,  8, '2026-06-05'),
-- apikey_id=4 (user=3, test_company)
('acc_003', 'ctyun_user_003', 4,  45000,  26000,  19000, 130,  650000,  4, '2026-05-30'),
('acc_003', 'ctyun_user_003', 4,  52000,  31000,  21000, 150,  780000,  5, '2026-05-31'),
('acc_003', 'ctyun_user_003', 4,  68000,  40000,  28000, 190, 1000000,  7, '2026-06-01'),
('acc_003', 'ctyun_user_003', 4,  41000,  24000,  17000, 120,  600000,  3, '2026-06-02'),
('acc_003', 'ctyun_user_003', 4,  75000,  45000,  30000, 210, 1100000,  8, '2026-06-03'),
('acc_003', 'ctyun_user_003', 4,  62000,  37000,  25000, 170,  900000,  6, '2026-06-04'),
('acc_003', 'ctyun_user_003', 4,  55000,  33000,  22000, 160,  800000,  5, '2026-06-05');

-- ----------------------------
-- Detail - 模型维度
-- ----------------------------
-- apikey_id=1, 2026-05-30: deepseek-chat + qwen-plus
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'deepseek-chat', 8,  280 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-05-30';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'qwen-plus',     4,  120 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-05-30';

-- apikey_id=1, 2026-05-31: deepseek-chat + qwen-plus
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'deepseek-chat', 5,  220 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-05-31';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'qwen-plus',     3,   90 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-05-31';

-- apikey_id=1, 2026-06-01: deepseek-chat + qwen-plus
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'deepseek-chat', 10, 350 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-06-01';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'qwen-plus',      5, 140 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-06-01';

-- apikey_id=2, 2026-05-30: claude-sonnet-4-6 + gpt-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'claude-sonnet-4-6', 3, 130 FROM ai_token_usage u WHERE u.apikey_id=2 AND u.record_date='2026-05-30';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'gpt-5',             2,  80 FROM ai_token_usage u WHERE u.apikey_id=2 AND u.record_date='2026-05-30';

-- apikey_id=2, 2026-06-01: claude-sonnet-4-6 + gpt-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'claude-sonnet-4-6', 5, 210 FROM ai_token_usage u WHERE u.apikey_id=2 AND u.record_date='2026-06-01';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'gpt-5',             3, 110 FROM ai_token_usage u WHERE u.apikey_id=2 AND u.record_date='2026-06-01';

-- apikey_id=1, 2026-06-05: deepseek-chat + qwen-plus
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'deepseek-chat', 9,  320 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-06-05';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'qwen-plus',     5,  140 FROM ai_token_usage u WHERE u.apikey_id=1 AND u.record_date='2026-06-05';

-- apikey_id=2, 2026-06-05: claude-sonnet-4-6 + gpt-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'claude-sonnet-4-6', 4, 190 FROM ai_token_usage u WHERE u.apikey_id=2 AND u.record_date='2026-06-05';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'gpt-5',             3, 100 FROM ai_token_usage u WHERE u.apikey_id=2 AND u.record_date='2026-06-05';

-- ----------------------------
-- Detail Stage - 阶梯信息
-- 使用 JOIN 子查询匹配对应的 detail 记录
-- ----------------------------
-- deepseek-chat, 2026-05-30: 3 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 35000, 22000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 28000, 18000, 32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 12000, 10000, 128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';

-- qwen-plus, 2026-05-30: 2 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 15000, 8000, 0, 32000   FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 10000, 6000, 32000, NULL FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';

-- deepseek-chat, 2026-06-01: 3 阶梯（数据更大）
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 45000, 30000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-01' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 35000, 25000, 32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-01' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 10000, 11000, 128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-01' AND d.name='deepseek-chat';

-- claude-sonnet-4-6, 2026-05-30: 3 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 20000, 12000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 15000, 8000,  32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id,  5000, 5000,  128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';

-- gpt-5, 2026-05-30: 2 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 10000, 5000, 0, 32000   FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='gpt-5';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id,  5000, 3000, 32000, NULL FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='gpt-5';

-- deepseek-chat, 2026-06-05: 3 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 38000, 24000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 32000, 20000, 32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 18000, 10000, 128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';

-- claude-sonnet-4-6, 2026-06-05: 3 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 22000, 13000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 18000, 10000, 32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 18000, 15000, 128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';

-- ----------------------------
-- Detail Resolution Duration - 分辨率时长
-- ----------------------------
-- deepseek-chat, 2026-05-30
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  450000, 160 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 850000, 80  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    500000, 40  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';

-- qwen-plus, 2026-05-30
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  200000, 70 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 350000, 35 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    150000, 15 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';

-- claude-sonnet-4-6, 2026-05-30
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  250000, 80 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 450000, 40 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    200000, 10 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';

-- deepseek-chat, 2026-06-05
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  520000, 200 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 980000, 100 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    600000, 50  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';

-- claude-sonnet-4-6, 2026-06-05
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  280000, 100 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 520000, 60  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    280000, 20  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';

-- ----------------------------
-- Detail Resolution Token - 分辨率Token
-- ----------------------------
-- deepseek-chat, 2026-05-30
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  12000, 6000,  160 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 15000, 8000,  80  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    18000, 10000, 40  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';

-- qwen-plus, 2026-05-30
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  4500, 2000, 70 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 6000, 3200, 35 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    7000, 3500, 15 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-05-30' AND d.name='qwen-plus';

-- claude-sonnet-4-6, 2026-05-30
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  6000,  3000, 80 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 8000,  4500, 40 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    10000, 5000, 10 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-05-30' AND d.name='claude-sonnet-4-6';

-- deepseek-chat, 2026-06-05
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  14000, 7000,  200 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 18000, 9000,  100 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    21000, 11000, 50  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=1 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';

-- claude-sonnet-4-6, 2026-06-05
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  7000,  3500, 100 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 9500,  5000, 60  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    12000, 6000, 20  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=2 AND u.record_date='2026-06-05' AND d.name='claude-sonnet-4-6';

-- ================================================================
-- user_id=3（test_company）完整测试数据
-- ================================================================

-- ----------------------------
-- Detail - 模型维度（apikey 3+4）
-- ----------------------------
-- apikey_id=3, 2026-05-30: deepseek-chat + gpt-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'deepseek-chat', 6, 160 FROM ai_token_usage u WHERE u.apikey_id=3 AND u.record_date='2026-05-30';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'gpt-5',         3,  90 FROM ai_token_usage u WHERE u.apikey_id=3 AND u.record_date='2026-05-30';

-- apikey_id=3, 2026-06-05: deepseek-chat + gpt-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'deepseek-chat', 5, 150 FROM ai_token_usage u WHERE u.apikey_id=3 AND u.record_date='2026-06-05';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'gpt-5',         3,  80 FROM ai_token_usage u WHERE u.apikey_id=3 AND u.record_date='2026-06-05';

-- apikey_id=4, 2026-05-30: qwen-plus + claude-haiku-4-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'qwen-plus',        2,  70 FROM ai_token_usage u WHERE u.apikey_id=4 AND u.record_date='2026-05-30';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'claude-haiku-4-5', 2,  60 FROM ai_token_usage u WHERE u.apikey_id=4 AND u.record_date='2026-05-30';

-- apikey_id=4, 2026-06-05: qwen-plus + claude-haiku-4-5
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'qwen-plus',        3, 100 FROM ai_token_usage u WHERE u.apikey_id=4 AND u.record_date='2026-06-05';
INSERT INTO ai_token_usage_detail (usage_id, name, amount, amount_request)
SELECT u.id, 'claude-haiku-4-5', 2,  70 FROM ai_token_usage u WHERE u.apikey_id=4 AND u.record_date='2026-06-05';

-- ----------------------------
-- Detail Stage - 阶梯信息（apikey 3+4）
-- ----------------------------
-- deepseek-chat, apikey 3, 2026-05-30: 3 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 22000, 14000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 18000, 12000, 32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 10000,  9000, 128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';

-- gpt-5, apikey 3, 2026-05-30: 2 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 13000, 7000, 0, 32000   FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='gpt-5';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id,  7000, 5000, 32000, NULL FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='gpt-5';

-- qwen-plus, apikey 4, 2026-05-30: 2 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 12000, 7000, 0, 32000   FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id,  8000, 5000, 32000, NULL FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';

-- claude-haiku-4-5, apikey 4, 2026-05-30: 2 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 10000, 5000, 0, 32000   FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='claude-haiku-4-5';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id,  6000, 4000, 32000, NULL FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='claude-haiku-4-5';

-- deepseek-chat, apikey 3, 2026-06-05: 3 阶梯
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 20000, 13000, 0,      32000  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 16000, 11000, 32000, 128000 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_stage (detail_id, input_tokens, output_tokens, min_context, max_context)
SELECT d.id, 10000,  8000, 128000, NULL  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';

-- ----------------------------
-- Detail Resolution Duration - 分辨率时长（apikey 3+4）
-- ----------------------------
-- deepseek-chat, apikey 3, 2026-05-30
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  320000, 110 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 580000, 60  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    300000, 30  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';

-- qwen-plus, apikey 4, 2026-05-30
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  180000, 50 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 280000, 30 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    120000, 15 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';

-- deepseek-chat, apikey 3, 2026-06-05
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '720p',  280000, 100 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '1080p', 520000, 55  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_duration (detail_id, resolution, cnt, request_count)
SELECT d.id, '4K',    350000, 25  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';

-- ----------------------------
-- Detail Resolution Token - 分辨率Token（apikey 3+4）
-- ----------------------------
-- deepseek-chat, apikey 3, 2026-05-30
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  9000,  4500, 110 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 12000, 6000, 60  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    14000, 7000, 30  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-05-30' AND d.name='deepseek-chat';

-- qwen-plus, apikey 4, 2026-05-30
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  3500, 1800, 50 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 4800, 2500, 30 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    5500, 2800, 15 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=4 AND u.record_date='2026-05-30' AND d.name='qwen-plus';

-- deepseek-chat, apikey 3, 2026-06-05
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '720p',  8000,  4000, 100 FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '1080p', 11000, 5500, 55  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
INSERT INTO ai_token_usage_detail_res_token (detail_id, resolution, video_mode_output_token, video_less_mode_output_token, request_count)
SELECT d.id, '4K',    13000, 6500, 25  FROM ai_token_usage_detail d JOIN ai_token_usage u ON d.usage_id=u.id WHERE u.apikey_id=3 AND u.record_date='2026-06-05' AND d.name='deepseek-chat';
