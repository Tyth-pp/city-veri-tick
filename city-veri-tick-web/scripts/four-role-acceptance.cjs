// Invoked by FourRoleAcceptanceIT while its real HTTP server and fixtures are alive.
const path = require('node:path');
const fs = require('node:fs');
const moduleRoot = process.env.ACCEPTANCE_NODE_MODULES || 'C:/Users/27362/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const { chromium } = require(path.join(moduleRoot, 'playwright'));
const base = process.env.ACCEPTANCE_WEB_URL || 'http://127.0.0.1:5299';
const output = path.resolve(__dirname, '../../docs/acceptance-evidence');
const prefix = process.env.ACCEPTANCE_PREFIX;
if (!prefix || !process.env.ACCEPTANCE_PASSWORD) throw Error('Run through FourRoleAcceptanceIT; no fixed credentials.');
fs.mkdirSync(output, { recursive: true });
const errors = [], steps = [];
function assert(value, label) { if (!value) throw Error(label); }
async function main() {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.ACCEPTANCE_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' });
  try {
    const pages = {};
    const homes = { USER: '/home', ORGANIZER_ADMIN: '/management/organizer', PLATFORM_ADMIN: '/management/platform', VERIFIER: '/verify' };
    for (const [role, home] of Object.entries(homes)) {
      const context = await browser.newContext({ viewport: { width: role === 'USER' ? 390 : 1440, height: 1000 } });
      const page = await context.newPage(); pages[role] = page;
      page.on('pageerror', error => errors.push(`${role}: ${error.message}`));
      page.on('response', response => { if (response.url().includes('/controller/') && response.status() >= 500) errors.push(`${role}: HTTP ${response.status()} ${new URL(response.url()).pathname}`); });
      await page.goto(`${base}/login?mode=account`);
      await page.getByPlaceholder('请输入后台账号').fill(prefix + role);
      await page.getByPlaceholder('请输入密码', { exact: true }).fill(process.env.ACCEPTANCE_PASSWORD);
      await page.getByRole('button', { name: '登录', exact: true }).click();
      await page.waitForURL(base + home);
      await page.waitForLoadState('networkidle');
      await page.screenshot({ path: path.join(output, `${role}-home.png`), fullPage: true });
      steps.push(`${role}: browser form login and correct role landing`);
    }
    const user = pages.USER;
    await user.goto(`${base}/activities/${process.env.ACCEPTANCE_ACTIVITY}`);
    await user.getByRole('heading', { name: prefix + '城市活动验收', exact: true }).waitFor();
    await user.locator('.ticket-card').first().click();
    await user.getByRole('button', { name: '立即购票', exact: true }).click();
    await user.waitForURL('**/orders/confirm');
    await user.locator('.agreement input').check();
    await user.getByRole('button', { name: '提交订单并支付', exact: true }).click();
    await user.waitForURL('**/orders/pay/*');
    await user.getByRole('timer').waitFor();
    await user.screenshot({ path: path.join(output, 'USER-payment.png'), fullPage: true });
    const payment = user.waitForResponse(r => /\/orders\/\d+\/pay$/.test(r.url()) && r.request().method() === 'POST');
    await user.locator('.wechat-confirm').click();
    const paymentResponse = await payment;
    const paid = (await paymentResponse.json()).data;
    assert(paid?.status === 'PAID' && paid.tickets.length === 1, 'Browser payment must issue exactly one real ticket');
    await user.waitForURL('**/orders/' + paid.orderId);
    await user.goto(`${base}/tickets/${paid.tickets[0].ticketId || paid.tickets[0].id}?orderId=${paid.orderId}`);
    await user.waitForLoadState('networkidle');
    assert((await user.locator('body').innerText()).includes(prefix), 'Actual ticket page must display the purchased activity');
    await user.screenshot({ path: path.join(output, 'USER-ticket.png'), fullPage: true });
    steps.push('USER: activity selection → checkout → real pending order/countdown → simulated payment → electronic ticket');
    const verifier = pages.VERIFIER;
    await verifier.getByPlaceholder('扫描或输入核销码').fill(paid.tickets[0].verificationCode);
    await verifier.getByPlaceholder('输入观演人18位身份证号').fill('11010519491231002X');
    await verifier.getByRole('button', { name: '确认核验 ↵', exact: true }).click();
    await verifier.getByText('核销通过 · 允许通行', { exact: true }).waitFor();
    await verifier.locator('tbody tr').filter({ hasText: paid.tickets[0].verificationCode }).waitFor();
    await verifier.getByRole('button', { name: '确认核验 ↵', exact: true }).waitFor();
    await verifier.screenshot({ path: path.join(output, 'VERIFIER-check-in.png'), fullPage: true });
    steps.push('VERIFIER: real purchased ticket identity check-in and success feedback');
    await user.getByRole('button', { name: '刷新票券状态', exact: true }).click();
    await user.locator('.ticket-state').filter({ hasText: '已核销' }).waitFor();
    assert(await user.locator('.ticket-qr').count() === 0, 'Checked-in ticket must hide QR');
    steps.push('USER: ticket refresh after check-in shows used state and removes QR');
    await user.screenshot({ path: path.join(output, 'USER-ticket-checked-in.png'), fullPage: true });

    // A second, unused order exercises the actual refund form and organizer confirmation dialog.
    await user.goto(`${base}/activities/${process.env.ACCEPTANCE_ACTIVITY}`);
    await user.locator('.ticket-card').first().click();
    await user.getByRole('button', { name: '立即购票', exact: true }).click();
    await user.waitForURL('**/orders/confirm');
    await user.locator('.agreement input').check();
    await user.getByRole('button', { name: '提交订单并支付', exact: true }).click();
    await user.waitForURL('**/orders/pay/*'); await user.getByRole('timer').waitFor();
    const secondPayment = user.waitForResponse(r => /\/orders\/\d+\/pay$/.test(r.url()) && r.request().method() === 'POST');
    await user.locator('.wechat-confirm').click();
    const refundable = (await (await secondPayment).json()).data;
    await user.waitForURL('**/orders/' + refundable.orderId);
    await user.getByRole('button', { name: '申请整单退票', exact: true }).click();
    await user.getByPlaceholder('请说明退票原因，最多500字').fill('浏览器真实退款验收');
    await user.getByRole('button', { name: '确认申请', exact: true }).click();
    await user.getByText('等待主办方审核，电子票暂不可入场。', { exact: true }).waitFor();
    await user.screenshot({ path: path.join(output, 'USER-refund-pending.png'), fullPage: true });
    const organizer = pages.ORGANIZER_ADMIN;
    await organizer.goto(`${base}/management/organizer/orders/${refundable.orderId}`);
    await organizer.locator('[data-action="approve-refund"]').click();
    await organizer.getByRole('dialog').getByRole('button', { name: '同意并模拟退款', exact: true }).click();
    await organizer.getByText('模拟退款完成', { exact: true }).waitFor();
    await organizer.screenshot({ path: path.join(output, 'ORGANIZER-refund-complete.png'), fullPage: true });
    await user.reload();
    await user.getByText('模拟退款完成', { exact: true }).waitFor();
    await user.screenshot({ path: path.join(output, 'USER-refund-complete.png'), fullPage: true });
    steps.push('USER + ORGANIZER_ADMIN: real refund request form → review dialog → refunded result visible on both sides');
    const routes = {
      ORGANIZER_ADMIN: ['activities', 'ticket-inventory', 'orders', 'verification', 'verifiers'],
      PLATFORM_ADMIN: ['activities', 'organizers', 'orders', 'verification', 'users', 'coupons', 'verifiers', 'dashboard'],
      USER: ['orders', 'coupons', 'coupons/mine', 'profile', 'favorites']
    };
    for (const [role, paths] of Object.entries(routes)) {
      for (const suffix of paths) {
        const url = role === 'USER' ? `/${suffix}` : `${homes[role]}/${suffix}`;
        const page = pages[role];
        await page.goto(base + url); await page.waitForLoadState('networkidle');
        const text = await page.locator('body').innerText();
        assert(!/加载失败|数据加载失败|服务器异常|请求失败/.test(text), `${role} ${url}: load failure`);
        assert(new URL(page.url()).pathname === url, `${role} ${url}: unexpected redirect`);
        steps.push(`${role}: ${url} real API page loaded`);
      }
    }
    await pages.ORGANIZER_ADMIN.goto(`${base}/management/organizer/orders/${paid.orderId}`);
    await pages.ORGANIZER_ADMIN.waitForLoadState('networkidle');
    assert((await pages.ORGANIZER_ADMIN.locator('body').innerText()).includes(prefix), 'Organizer must see the real user order');
    await pages.ORGANIZER_ADMIN.screenshot({ path: path.join(output, 'ORGANIZER-order.png'), fullPage: true });
    await pages.PLATFORM_ADMIN.goto(`${base}/management/platform/orders/${paid.orderId}`);
    await pages.PLATFORM_ADMIN.waitForLoadState('networkidle');
    assert((await pages.PLATFORM_ADMIN.locator('body').innerText()).includes(prefix), 'Platform must see the real user order');
    await pages.PLATFORM_ADMIN.screenshot({ path: path.join(output, 'PLATFORM-order.png'), fullPage: true });
    await user.goto(`${base}/management/platform`); await user.waitForURL(base + '/home');
    steps.push('USER: management URL rejected by router');
    await user.goto(`${base}/about`); await user.waitForLoadState('networkidle');
    assert(!(await user.locator('body').innerText()).includes('离线验票能力'), 'About must describe implemented capabilities');
    await user.screenshot({ path: path.join(output, 'USER-about.png'), fullPage: true });
    assert(errors.length === 0, errors.join('\n'));
    fs.writeFileSync(path.join(output, 'browser-result.json'), JSON.stringify({ passed: true, steps, errors }, null, 2));
    console.log(`BROWSER ACCEPTANCE PASS: ${steps.length} checks, no mocked API responses.`);
  } catch (error) {
    fs.writeFileSync(path.join(output, 'browser-result.json'), JSON.stringify({ passed: false, steps, errors, failure: error.message }, null, 2));
    throw error;
  } finally { await browser.close(); }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
