# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AI Token 管理系统，基于 Spring Boot 3.3.4 + Java 17，调用天翼云（TYY）API 管理 API Key 和 Token 用量查询。

## 构建与运行

```bash
# 构建项目
mvn clean package -DskipTests

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest="AiTokenSystemApplicationTest"

# 启动应用
mvn spring-boot:run
```

## 技术栈与架构

- **基础框架**: Spring Boot 3.3.4, Spring Security, Spring Validation
- **数据库**: 已配置 MyBatis Plus 3.5.8 + Druid 连接池 + dynamic-datasource 多数据源支持，但目前数据库功能尚未启用。数据库驱动为 MySQL 8.0.33
- **缓存**: Redisson 依赖已在 pom.xml 中注释掉，暂未启用
- **API 文档**: SpringDoc OpenAPI 2.6.0 + Knife4j 4.5.0
- **工具库**: Hutool 5.8.32, Lombok, MapStruct, Fastjson, Guava, EasyExcel
- **加密**: Jasypt 3.0.5 用于配置加密
- **日志**: Logback，支持控制台 + 滚动文件 + 异步输出，日志格式中包含 traceId

## 包结构与核心组件

```
com.ai.system
├── AiTokenSystemApplication    # 启动类，启用 @ConfigurationPropertiesScan
├── config
│   └── TyyProperties           # TYY API 配置（access-key, security-key, base-url 等）
└── util
    └── TyySignUtil             # TYY API 签名工具，使用 ai-api-sign-sdk (tyysdk)
```

## 待注意事项

1. **pom.xml 中 mainClass 配置有误**：当前指向 `com.ebupt.MainApp`，实际主类为 `com.ai.system.AiTokenSystemApplication`。如不使用 spring-boot-maven-plugin 打包可忽略，否则需要修正。
2. **logback-spring.xml 中 logger 引用了 `com.ebupt.*` 包名**，这是从前身项目复制过来的，后续业务代码应使用 `com.ai.system` 包名。
