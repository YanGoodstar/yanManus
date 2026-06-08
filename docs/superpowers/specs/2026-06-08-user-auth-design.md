# 用户登录 + 多用户隔离 设计文档

## 1. 概述

为 yanManus 项目添加用户认证系统和多用户会话隔离能力。基于 Spring Security + JWT 实现邮箱密码登录，通过 Redis key 加 userId 前缀实现会话隔离，使用 Spring Data JPA + MySQL 8 持久化用户数据。

### 1.1 目标

- 用户通过邮箱 + 密码注册和登录
- 每个用户只能访问自己的会话数据
- 会话与会话之间完全隔离
- 保持现有 Agent 体系不变，仅改动会话管理的 key 结构

### 1.2 技术选型

| 组件 | 选型 | 版本 |
|------|------|------|
| ORM | Spring Data JPA | 3.4.x (继承 Boot) |
| 数据库 | MySQL 8 | 8.x |
| 安全框架 | Spring Security | 6.4.x (继承 Boot) |
| JWT | jjwt | 0.12.x |
| 会话存储 | Redis（现有，加 userId 前缀） | 不变 |

## 2. 数据模型

### 2.1 User 实体

表名：`app_user`（避免与 MySQL 保留字 `user` 冲突）

| 字段 | 数据库类型 | Java 类型 | 约束 | 说明 |
|------|-----------|----------|------|------|
| id | BIGINT | Long | PK, AUTO_INCREMENT | 主键 |
| email | VARCHAR(255) | String | UNIQUE, NOT NULL | 邮箱，登录凭证 |
| password | VARCHAR(255) | String | NOT NULL | BCrypt 加密密码 |
| created_at | DATETIME | LocalDateTime | NOT NULL, DEFAULT NOW() | 创建时间 |

索引：
- `uk_email` — email 唯一索引

### 2.2 DTO

- `RegisterRequest`：`{ email: String, password: String }`
- `LoginRequest`：`{ email: String, password: String }`
- `AuthResponse`：`{ token: String }`

## 3. 认证流程

### 3.1 注册

```
POST /api/auth/register
Content-Type: application/json

Request:  { "email": "user@example.com", "password": "123456" }
Response: 201 Created (body: { "message": "注册成功" })
```

流程：
1. 校验 email 格式（简单正则）
2. 校验 password 长度 >= 6
3. 查询 email 是否已存在，重复返回 409
4. BCrypt 加密密码，存入 MySQL
5. 返回 201

### 3.2 登录

```
POST /api/auth/login
Content-Type: application/json

Request:  { "email": "user@example.com", "password": "123456" }
Response: 200 OK { "token": "eyJhbG..." }
```

流程：
1. 根据 email 查找用户，不存在返回 401
2. BCrypt 验证密码，不匹配返回 401
3. 生成 JWT（payload: userId + email，有效期 24 小时）
4. 返回 token

### 3.3 JWT 规格

- 签名算法：HMAC-SHA256
- 过期时间：24 小时
- Payload：`{ sub: userId, email: email, iat: issuedAt, exp: expiresAt }`
- 密钥：配置在 `application.yaml` 的 `yanmanus.jwt.secret` 中
- 前端通过 `Authorization: Bearer <token>` 携带

## 4. Spring Security 配置

### 4.1 SecurityConfig

```
permitAll:
  - /api/auth/**
  - /api/health

需要认证:
  - /**（其余所有请求）
```

- CSRF：禁用（JWT 无状态）
- Session：STATELESS
- CORS：保持现有 CorsConfig，allowCredentials = true

### 4.2 JwtAuthenticationFilter

继承 `OncePerRequestFilter`，在 `UsernamePasswordAuthenticationFilter` 之前执行：

1. 从 `Authorization` header 提取 `Bearer <token>`
2. 用 jjwt 解析验证 token（签名、过期）
3. 提取 userId 和 email，构建 `UsernamePasswordAuthenticationToken`
4. 设置到 `SecurityContextHolder.getContext()`
5. token 无效或缺失则不设置，由 Spring Security 决定是否拒绝

### 4.3 PasswordEncoder

使用 `BCryptPasswordEncoder` 作为 Bean 注册。

## 5. 会话隔离（核心改动）

### 5.1 Redis Key 格式变更

```
旧：yanmanus:session:{sessionId}
新：yanmanus:session:{userId}:{sessionId}
```

### 5.2 ChatSessionManager 方法签名变更

所有公开方法增加 `Long userId` 作为第一个参数：

| 方法 | 变更前 | 变更后 |
|------|--------|--------|
| getMessages | `getMessages(sessionId)` | `getMessages(userId, sessionId)` |
| saveMessages | `saveMessages(sessionId, messages)` | `saveMessages(userId, sessionId, messages)` |
| clearSession | `clearSession(sessionId)` | `clearSession(userId, sessionId)` |
| sessionExists | `sessionExists(sessionId)` | `sessionExists(userId, sessionId)` |

本地缓存 key 格式同步变更：`{userId}:{sessionId}`

### 5.3 ChatController 改动

每个接口方法开头通过以下方式获取当前用户 ID：

```java
Long userId = SecurityUtils.getCurrentUserId();
```

`SecurityUtils` 是一个工具类，从 `SecurityContextHolder` 中提取已认证用户的 userId。

所有 `sessionManager.*` 调用传入 userId。

### 5.4 BaseAgent 改动

`BaseAgent.run()` 和 `BaseAgent.runWithSse()` 方法签名增加 `Long userId` 参数，传递给 `sessionManager` 的调用。

## 6. API 汇总

| Method | Path | 说明 | 认证 |
|--------|------|------|------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |
| GET | /api/health | 健康检查 | 否 |
| POST | /api/chat/send | 发送消息（快速响应） | 是 |
| GET | /api/chat/send/stream | 流式输出（快速响应） | 是 |
| POST | /api/chat/send/planning | 发送消息（深度思考） | 是 |
| GET | /api/chat/send/planning/stream | 流式输出（深度思考） | 是 |
| GET | /api/chat/session/{sessionId}/history | 查看会话历史 | 是 |
| DELETE | /api/chat/session/{sessionId} | 删除会话 | 是 |

## 7. 配置变更

### 7.1 application.yaml 新增

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yanmanus?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

yanmanus:
  jwt:
    secret: ${JWT_SECRET:yanManusDefaultSecretKeyForDevelopmentOnly2024!@#$}
    expiration: 86400000  # 24小时（毫秒）
```

## 8. 文件变更清单

### 8.1 新增文件

| 文件路径 | 说明 |
|----------|------|
| `entity/User.java` | 用户实体 |
| `repository/UserRepository.java` | 用户数据访问 |
| `dto/RegisterRequest.java` | 注册请求 DTO |
| `dto/LoginRequest.java` | 登录请求 DTO |
| `dto/AuthResponse.java` | 认证响应 DTO |
| `controller/AuthController.java` | 注册/登录接口 |
| `service/UserService.java` | 用户业务逻辑 |
| `config/SecurityConfig.java` | Spring Security 配置 |
| `security/JwtAuthenticationFilter.java` | JWT 认证过滤器 |
| `security/JwtUtil.java` | JWT 工具类（生成/解析） |
| `security/SecurityUtils.java` | 安全上下文工具类 |

### 8.2 修改文件

| 文件路径 | 变更内容 |
|----------|----------|
| `pom.xml` | 新增 JPA、MySQL、Security、jjwt 依赖 |
| `application.yaml` | 新增 datasource、jpa、jwt 配置 |
| `memory/ChatSessionManager.java` | 所有方法增加 userId 参数，key 加前缀 |
| `controller/ChatController.java` | 所有接口获取当前用户，传入 userId |
| `agent/BaseAgent.java` | run/runWithSse 方法增加 userId 参数 |
| `agent/YanManus.java` | 调用父类方法传入 userId |
| `agent/PlanningAgent.java` | 调用父类方法传入 userId |

## 9. 错误处理

| 场景 | HTTP 状态码 | 响应 |
|------|------------|------|
| 邮箱已注册 | 409 | `{ "error": "邮箱已被注册" }` |
| 邮箱未注册（登录） | 401 | `{ "error": "邮箱或密码错误" }` |
| 密码错误 | 401 | `{ "error": "邮箱或密码错误" }`（不区分具体原因） |
| token 无效/过期 | 401 | `{ "error": "未授权" }` |
| 未携带 token 访问受保护接口 | 401 | `{ "error": "未授权" }` |
| 邮箱格式不合法 | 400 | `{ "error": "邮箱格式不正确" }` |
| 密码长度不足 | 400 | `{ "error": "密码长度不能少于6位" }` |

## 10. 不变部分

- Agent 体系（BaseAgent、ReActAgent、ToolCallAgent、YanManus、PlanningAgent）的核心逻辑不变
- 所有 Tools 不变
- LLM 调用方式不变
- SSE 流式输出机制不变
- API 文档（Knife4j）自动扫描新 Controller
