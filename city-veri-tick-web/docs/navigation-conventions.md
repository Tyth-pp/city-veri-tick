# 页面路由规范

主办方业务统一使用 `/management/organizer/...`，平台管理统一使用 `/management/platform/...`。页面链接、保存后返回地址和快捷入口均使用所属端的正式地址。

历史 `/management/...` 地址仅用于兼容旧书签，按当前登录角色转到对应正式模块，并保留查询参数及锚点。历史地址不挂载页面组件或旧版 `ManagementLayout`。平台管理员访问旧版活动创建、编辑入口时，进入活动审核模块，不进入主办方编辑功能。

用户端、主办方端、平台管理员端的布局内，业务页面通过 `route.path` 作为 RouterView 的 key。切换活动、订单、主办方或新建/编辑页面时，重新建立页面状态，避免沿用上一条记录；只调整 query 中的筛选条件时保留页面实例，由页面自身处理筛选变化。

布局和全局组件不以完整地址作为 key，用户端 `UserReminderHost` 保持挂载，确保切换业务页面不会打断提醒。核销端只有一个工作台入口，不存在旧版/新版重复路由；当前使用 `VerifyWorkbenchRefined.vue`。

回归覆盖：`src/router/managementRoutes.spec.js` 验证历史地址归一及角色隔离；`src/layouts/RouteReuse.spec.js` 验证三个端切换记录时更新页面、query 变化时保留实例、全局提醒不重复挂载。
