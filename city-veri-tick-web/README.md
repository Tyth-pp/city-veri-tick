# City Veri Tick 四角色前端

## 启动

```powershell
npm ci
npm run dev
```

开发服务器默认将 `/controller` 请求代理到 `http://localhost:8082`。请先启动后端、MySQL 和 Redis，再使用测试账号登录。

## 当前业务（2026-10-18）

- 用户 H5：游客活动浏览、选票、实名下单、支付倒计时、模拟支付、取消、订单及电子票、整单退票、优惠券及个人券包。
- 主办方 `/management/organizer`：活动场次票种配置、库存、订单退款审核、核销数据及核销员。
- 平台 `/management/platform`：活动审核、机构账号、订单监管、优惠券、核销授权、经营统计。
- 核销员 `/verify`：核销码及身份证号核验、本人核销记录和统计。

统一登录为 `/login`，游客入口 `/home`。电子票 `/tickets/:ticketId?orderId=:orderId` 从真实本人订单读取票券，二维码由实际核销码生成。支付与退款为模拟流程，核销需要联网。四角色在不同浏览器标签页保存独立会话。

开发代理可用 `VITE_API_TARGET` 环境变量覆盖。部署时需要服务器提供 SPA 路由回退，并把 `/controller` 转发到后端，开发代理不会随构建产物部署。

## 验证

```powershell
npm run test:unit -- --run
npm run build
```

构建产物为 `dist/`。真实浏览器脚本 `scripts/four-role-acceptance.cjs` 由后端 `FourRoleAcceptanceIT` 在临时账号存活期间启动，不能使用固定凭据单独运行。

- [交付运行手册](../docs/delivery-guide.md)
- [四角色验收报告](../docs/four-role-acceptance-2026-10-18.md)
- [浏览器截图与结果](../docs/acceptance-evidence/browser-result.json)
