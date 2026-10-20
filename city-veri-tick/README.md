# City Veri Tick 后端

当前业务验收基线：2026-10-18。Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、Redis；Maven 多模块为 `city-common`、`city-pojo`、`city-server`，只有 `city-server` 启动应用。

## 已实现业务

- 四角色登录、资料及密码管理、账号启停、退出和强制下线。
- 主办方活动/场次/票种库存配置，平台驳回、通过和下架，机构资源隔离。
- 用户下单防重、15分钟待支付、模拟支付出票、取消和超时库存释放。
- 实名信息快照、电子票与核销授权、实名核验、防重复核销、核销记录。
- 整单退票申请、主办方审核、模拟退款、票券作废、库存及优惠券联动。
- 普通/限量/秒杀优惠券、个人券包、订单优惠。
- 管理端统计、审核/退款业务消息；Redis 会话、验证码、活动缓存和领取预占。
- OSS 图片上传及 URL 适配代码已接入，实际使用依赖有效的存储配置。

支付、退款和 dev 短信属于模拟渠道。当前核销在线进行，票码为唯一固定码，不提供离线核销或动态防截图保证。

## 启动与验证

先准备 MySQL、Redis 和私有配置，再从本目录执行：

```powershell
mvn install -DskipTests
mvn -pl city-server spring-boot:run
```

默认 dev profile 使用本机 `application-dev.yml`，不得将其中凭据加入交付包。新环境配置参考 [无凭据配置样例](docs/application-local.example.yml) 和 [交付运行手册](../docs/delivery-guide.md)。

```powershell
mvn -pl city-server -am test
# 显式包含真实 MySQL、Redis 和四角色 HTTP 验收
mvn -pl city-server -am test "-Dtest=*Test,*IT" -Dsurefire.failIfNoSpecifiedTests=false
```

默认 `test` 不包含以 IT 结尾的集成测试。四角色验收使用本地开发数据库、随机临时账号，结束后清理本轮数据；不会发送真实短信或调用真实支付。

后端默认端口 `8082`；接口文档 `/doc.html`，OpenAPI `/v3/api-docs`。角色路径为 `/controller/admin/platform/**`、`/controller/admin/organizer/**`、`/controller/user/**` 和 `/controller/verify/**`。登录入口 `/controller/auth/login`；普通用户另有短信验证码登录入口。

## 交付资料

- [四角色验收报告](../docs/four-role-acceptance-2026-10-18.md)
- [交付运行手册与升级顺序](../docs/delivery-guide.md)
- [架构及命名约束](AGENTS.md)
- [Redis 计划及阶段记录](docs/superpowers/plans/2026-10-16-redis-rollout.md)

早期需求文档保留历史设计背景，当前状态以本基线与验收报告为准。Stream、榜单、UV、位置查询、容器化及生产压测没有包含在本次交付内。
