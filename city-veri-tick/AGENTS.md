# 后端架构与命名约定

适用于本目录下的 Java 后端代码。修改已有业务时保留权限、事务、库存和状态校验，避免把结构整理扩展成业务行为修改。

## 包与依赖方向

- `com.city.result`：统一响应对象、响应码接口 `ResultCode` 和响应码枚举 `SystemResultCode`；固定提示文案常量放 `com.city.constant`。

- `com.city.service`：主要业务服务接口，名称以 `Service` 结尾。
- `com.city.service.impl`：实现上述接口的业务类，以 `ServiceImpl` 结尾。实现类可用明确渠道前缀，例如 `OssFileStorageServiceImpl` 实现 `FileStorageService`。
- Controller、业务服务的协作者优先依赖业务接口；不要从 Controller、异常处理器导入 `service.impl`。
- `com.city.cache`：缓存组件，以 `Cache` 等职责名称命名，不强制添加 Service/Impl 后缀。
- `com.city.service.support`：公共业务组装或规则支撑，使用 `Support`、`Policy` 等明确职责名称。这些类可有业务依赖，不要伪装成纯工具。
- `com.city.integration.<channel>`：外部渠道适配，例如开发短信发送器；接口保留能力名称，实现名称体现渠道或环境。
- `com.city.exception`：跨层传播的业务异常；不要让异常处理器依赖实现类内部异常类型。
- 事务注解保留在 Spring 托管实现的公开业务入口；不能因为增加接口而减少事务、MANDATORY、权限或并发校验。

## utils 提取规则

- 跨模块可复用的纯工具放 `city-common/src/main/java/com/city/utils`。只在单模块使用的辅助不必强行上移。
- 仅在发现实际重复且语义一致时提取；优先 JDK 或已有依赖，不预造工具。
- 工具不得依赖 Mapper、Redis、业务 Service、当前登录用户或事务。退款规则、库存扣减、验证码消费等留在业务组件。
- 工具类按职责命名，使用 final 类、私有构造器和静态方法；避免 CommonUtils、BusinessUtils、RedisUtils 等职责混杂的名称。
- 提取时保留 null、空白、大小写、Locale、边界值行为，并测试必要边界。

## 验证

- 重命名/迁包同步修改应用、测试、当前文档及依赖引用；不要只修改文件名。
- 迁包后至少一次 `mvn -pl city-server -am clean test`，避免旧 class 掩盖引用遗漏；具体测试范围按变更选择。
- 保留 `ServiceStructureTest` 的依赖与命名约束，并验证受影响的接口 Spring 装配及事务代理。
- 不输出 application-dev.yml 凭证。真实 Redis 测试隔离前缀并只清理自身 Key；MySQL 测试写入应回滚。


## 接口文档

新增或修改 Controller 接口时，同步维护 Swagger/Knife4j 文档。每个 Controller 使用中文 `@Tag`，每个映射方法使用中文 `@Operation(summary=...)`；重要业务规则写入 description，关键路径/查询参数补充 `@Parameter`，请求及响应字段按需使用 `@Schema`。当前登录身份参数不作为用户可填写参数展示。枚举、单位、金额比例、null/0等特殊值含义必须与实际代码一致。文档入口：http://localhost:8082/doc.html，生成源为 /v3/api-docs。校验编译和文档生成，不能只修改JavaDoc而漏掉OpenAPI注解。

## 响应提示文案

- 返回给用户的固定提示和显式 DTO 校验 message 统一引用 `city-common/src/main/java/com/city/constant` 中按业务划分的 `*MessageConstant`，不在过滤器、服务或异常处理器中直接写提示字符串。
- 按认证、账号、活动、门票、订单、退款、优惠券、管理通知和通用响应归类；相同含义及文案复用已有常量，避免新建一个混合所有业务的大类。
- `SystemResultCode` 继续管理现有错误码，message 引用对应业务文案常量；整理文案时保留错误码、HTTP 状态及原始文字。
- 带活动名称、原因或金额的动态提示使用常量模板及 `formatted(...)`；DTO 注解使用编译期字符串常量。API 文档描述、日志、SQL 和业务状态码不作为响应提示文案提取。
