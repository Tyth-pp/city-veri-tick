# City Event Ticketing · 城市票务与验票平台

面向城市活动的票务与验票系统，覆盖从活动发布、用户购票、支付到现场核销、售后退款、平台监管的完整业务闭环。系统包含四种角色：**用户（H5）、主办方、平台管理员、核销员**，各自拥有独立的工作台与会话。

## 功能特性

| 角色 | 入口 | 能力 |
|---|---|---|
| 用户（H5） | `/home` | 活动浏览、选票、实名下单、支付倒计时与模拟支付、取消订单、订单与电子票、整单退票、优惠券与个人券包、个人资料 |
| 主办方 | `/management/organizer` | 活动 / 场次 / 票种配置、库存管理、订单与退款审核、核销数据、核销员（员工）管理 |
| 平台管理员 | `/management/platform` | 活动审核、主办方机构账号、订单监管、优惠券管理、核销授权、用户管理、经营统计 |
| 核销员 | `/verify` | 核销码 / 身份证号核验、本人核销记录与统计 |

统一登录入口为 `/login`。支付与退款为模拟流程，现场核销需要联网；四角色在不同浏览器标签页保存独立会话。

## 技术栈

**后端**（`city-veri-tick`，Maven 多模块）

- Java 17 · Spring Boot 3.5 · Spring Security（JWT 无状态认证）
- MyBatis-Plus · MySQL · Redis（Lettuce）· Druid 连接池
- JJWT · Knife4j / springdoc（OpenAPI 文档）· 阿里云 OSS · Lombok

**前端**（`city-veri-tick-web`）

- Vue 3 · Vite · Vue Router · Pinia · Axios
- Vant（移动端 UI）· qrcode（电子票二维码）· Vitest

## 项目结构

```
city-veri-tick/              # 后端（Maven 多模块）
├── city-common/             # 公共模块：常量、异常、安全（JWT/会话）、工具、Redis Lua 脚本
├── city-pojo/               # DTO / 实体 / VO / 枚举
└── city-server/             # 主服务：Controller / Service / Mapper、配置、数据库脚本
    └── src/main/resources/
        ├── application.yml  # 配置模板（仅含 ${...} 占位符，可提交）
        ├── db/              # 建表与初始化 SQL
        └── redis/           # Lua 脚本（优惠券领取、短信验证码、活动缓存）
city-veri-tick-web/          # 前端（Vue 3 + Vite）
```

## 快速开始

### 环境要求

- JDK 17、Maven 3.6+
- MySQL 8.0、Redis
- Node.js 18+（建议 20+）

### 1. 启动后端

**准备数据库**：创建数据库并导入初始化脚本：

```sql
CREATE DATABASE city_veri_tick DEFAULT CHARACTER SET utf8mb4;
```

```bash
mysql -u root -p city_veri_tick < city-veri-tick/city-server/src/main/resources/db/city-veri-tick.sql
```

**配置凭证**：`application.yml` 通过 `${...}` 占位符引用环境配置，真实凭证放在本地配置文件 `application-dev.yml` 中（该文件含敏感信息，已被 `.gitignore` 忽略，需自行创建）。在 `city-server/src/main/resources/` 下新建 `application-dev.yml`：

```yaml
city:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: city_veri_tick
    username: <你的用户名>
    password: <你的密码>
  redis:
    host: localhost
    port: 6379
    password: <你的密码>
  oss:
    endpoint: oss-cn-beijing.aliyuncs.com
    bucket-name: <你的 Bucket>
    access-key-id: <你的 AccessKey ID>
    access-key-secret: <你的 AccessKey Secret>
    public-url: https://<你的 Bucket>.oss-cn-beijing.aliyuncs.com
  jwt:
    secret: <随机生成的密钥>
    ttl: 12h
    issuer: city-veri-tick
    blacklist-prefix: "jwt:blacklist:"
    user-tokens-prefix: "jwt:user-tokens:"
```

> JWT 密钥可用 `openssl rand -base64 32` 生成。

**启动**（默认端口 `8082`）：

```bash
cd city-veri-tick
mvn -pl city-server -am spring-boot:run
```

接口文档（Knife4j）：http://localhost:8082/doc.html

### 2. 启动前端

```bash
cd city-veri-tick-web
npm ci
npm run dev
```

开发服务器将 `/controller` 请求代理到 `http://localhost:8082`，可通过 `VITE_API_TARGET` 环境变量覆盖。请先启动后端、MySQL 和 Redis 后再登录。

## 配置说明

- `application.yml` 为模板（仅含占位符，随仓库提交）；`application-dev.yml` 等 profile 文件包含真实凭证，不入库。
- 前端 `.env.development` 等环境变量文件同样被忽略，团队共享配置请使用 `.env.example`。
- 敏感文件（凭证、密钥、证书）已在根目录 `.gitignore` 中统一排除。

## 测试

```bash
# 后端
cd city-veri-tick
mvn -pl city-server -am clean test

# 前端
cd city-veri-tick-web
npm run test:unit -- --run
npm run build
```

## 部署

前端构建产物为 `city-veri-tick-web/dist/`。部署时需服务器提供 SPA 路由回退，并将 `/controller` 转发到后端；开发代理不会随构建产物部署。

后端可打包为可执行 JAR：

```bash
cd city-veri-tick
mvn -pl city-server -am package
```

## 相关文档

- [后端架构与命名约定](city-veri-tick/AGENTS.md)
- [前端说明](city-veri-tick-web/README.md)
