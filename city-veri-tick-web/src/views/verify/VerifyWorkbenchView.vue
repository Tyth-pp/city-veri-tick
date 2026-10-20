<template>
  <main class="verify-shell">
    <header class="verify-header">
      <div><strong>Venuo</strong><span>核销工作台</span></div>
      <div class="verify-user">
        <StaffAccountMenu /
        ><button type="button" title="退出登录" @click="logout">退出</button>
      </div>
    </header>
    <section class="verify-content">
      <div class="verify-heading">
        <div>
          <div class="eyebrow">现场服务 / 门票核销</div>
          <h1>核销门票</h1>
          <p>请扫描或输入用户门票上的核销码，确认有效后完成入场核销。</p>
        </div>
        <div class="online-dot"><i></i>服务正常</div>
      </div>
      <section class="verify-card code-card">
        <div class="card-title">
          <div>
            <span class="card-icon">⌁</span>
            <div>
              <h2>输入核销码</h2>
              <p>支持扫码枪、二维码内容粘贴或手动输入</p>
            </div>
          </div>
        </div>
        <form @submit.prevent="submit">
          <label class="code-label" for="verification-code">核销码</label
          ><input
            id="verification-code"
            ref="codeInput"
            v-model.trim="code"
            autocomplete="off"
            autofocus
            placeholder="请扫描或输入核销码"
            @keyup.enter="submit"
          />
          <div class="form-actions">
            <button class="secondary-button" type="button" @click="clear">
              清空</button
            ><button
              class="primary-button"
              type="submit"
              :disabled="loading || !code"
            >
              {{ loading ? "核销中..." : "确认核销" }}
            </button>
          </div>
        </form>
      </section>
      <section v-if="ticket" class="ticket-preview verify-card">
        <div class="card-title">
          <h2>门票信息</h2>
          <span class="ticket-status">{{
            ticketStatusLabel(ticket.ticketStatus)
          }}</span>
        </div>
        <div class="ticket-grid">
          <div>
            <small>活动</small><b>{{ ticket.activityTitle || "—" }}</b>
          </div>
          <div>
            <small>票种</small><b>{{ ticket.ticketTypeName || "—" }}</b>
          </div>
          <div>
            <small>场次</small
            ><b>{{
              formatRange(ticket.sessionStartTime, ticket.sessionEndTime)
            }}</b>
          </div>
          <div>
            <small>门票编号</small><b>{{ ticket.ticketNo || "—" }}</b>
          </div>
        </div>
      </section>
      <section
        v-if="result"
        class="result-card"
        :class="result.ok ? 'success' : 'failure'"
      >
        <span class="result-icon">{{ result.ok ? "✓" : "!" }}</span>
        <div>
          <h2>{{ result.ok ? "核销成功" : "核销失败" }}</h2>
          <p>{{ result.message }}</p>
          <small v-if="result.ok">请让持票人凭有效门票入场。</small>
        </div>
      </section>
      <section class="stats-row">
        <div>
          <small>累计核销</small><strong>{{ stats.totalCheckedIn }}</strong>
        </div>
        <div>
          <small>今日核销</small><strong>{{ stats.todayCheckedIn }}</strong>
        </div>
        <button class="secondary-button" type="button" @click="loadData">
          刷新数据
        </button>
      </section>
      <section class="recent-card verify-card">
        <div class="card-title">
          <h2>最近核销</h2>
          <span>最近 50 条</span>
        </div>
        <div v-if="!records.length" class="recent-empty">暂无核销记录</div>
        <div
          v-for="record in records.slice(0, 8)"
          :key="`${record.ticketId}-${record.checkedInAt}`"
          class="recent-item"
        >
          <div>
            <b>{{ record.activityTitle || "活动" }}</b
            ><small>{{ record.ticketTypeName }} · {{ record.ticketNo }}</small>
          </div>
          <time>{{ formatDate(record.checkedInAt) }}</time>
        </div>
      </section>
      <section class="verify-tips">
        <h2>核销提示</h2>
        <ul>
          <li>每张门票只能核销一次，重复核销会被系统拦截。</li>
          <li>核销失败时请确认二维码完整，并检查门票状态。</li>
          <li>不要向无关人员泄露门票核销码。</li>
        </ul>
      </section>
    </section>
  </main>
</template>
<script setup>
import StaffAccountMenu from '../../components/StaffAccountMenu.vue'
import { nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../../stores/auth";
import { verifierApi } from "../../api/verifier";
const router = useRouter(),
  auth = useAuthStore(),
  code = ref(""),
  loading = ref(false),
  result = ref(null),
  ticket = ref(null),
  records = ref([]),
  stats = ref({ totalCheckedIn: 0, todayCheckedIn: 0 }),
  codeInput = ref(null);
const ticketStatusLabel = (value) =>
  ({ UNUSED: "待核销", CHECKED_IN: "已核销", REFUNDED: "已退款" })[value] ||
  value ||
  "未知";
const formatDate = (value) =>
  value ? value.slice(0, 16).replace("T", " ") : "—";
const formatRange = (start, end) =>
  `${formatDate(start)} - ${end ? formatDate(end) : ""}`;
async function preview() {
  if (!code.value || loading.value) return;
  try {
    ticket.value = await verifierApi.detail(code.value);
  } catch (e) {
    ticket.value = null;
    result.value = { ok: false, message: e?.message || "无法查询该门票" };
  }
}
async function submit() {
  if (!code.value || loading.value) return;
  loading.value = true;
  result.value = null;
  try {
    ticket.value = await verifierApi.detail(code.value);
    if (ticket.value.ticketStatus !== "UNUSED") {
      result.value = {
        ok: false,
        message:
          "该门票当前不能核销：" + ticketStatusLabel(ticket.value.ticketStatus),
      };
      return;
    }
    ticket.value = await verifierApi.checkIn(code.value);
    result.value = { ok: true, message: "门票已成功核销。" };
    code.value = "";
    await loadData();
    await nextTick();
    codeInput.value?.focus();
  } catch (e) {
    result.value = {
      ok: false,
      message: e?.message || "核销失败，请稍后重试。",
    };
  } finally {
    loading.value = false;
  }
}
async function loadData() {
  try {
    stats.value = (await verifierApi.statistics()) || stats.value;
    records.value = (await verifierApi.records()) || [];
  } catch {}
}
function clear() {
  code.value = "";
  ticket.value = null;
  result.value = null;
  codeInput.value?.focus();
}
async function logout() {
  if (!await auth.logout()) return;
  router.replace("/login");
}
onMounted(loadData);
</script>
<style scoped>
.verify-shell {
  min-height: 100vh;
  background: #f6f8fc;
  color: #202334;
}
.verify-header {
  height: 72px;
  background: #fff;
  border-bottom: 1px solid #e1e5ef;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 52px;
}
.verify-header strong {
  display: block;
  color: #1148d1;
  font-size: 20px;
}
.verify-header span {
  display: block;
  color: #788096;
  font-size: 12px;
  margin-top: 4px;
}
.verify-user {
  display: flex;
  align-items: center;
  gap: 18px;
}
.verify-user span {
  display: inline;
  color: #565d72;
  font-size: 14px;
}
.verify-user button {
  border: 1px solid #d8deed;
  border-radius: 7px;
  background: #fff;
  color: #40506f;
  padding: 8px 15px;
  cursor: pointer;
}
.verify-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 54px 28px;
}
.verify-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}
.eyebrow {
  color: #6f7690;
  font-size: 13px;
  margin-bottom: 10px;
}
.verify-heading h1 {
  margin: 0 0 8px;
  font-size: 32px;
}
.verify-heading p {
  margin: 0;
  color: #6e7387;
}
.online-dot {
  background: #e3f8ed;
  color: #168155;
  padding: 8px 12px;
  border-radius: 18px;
  font-size: 12px;
}
.online-dot i {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #19a66c;
  margin-right: 6px;
}
.verify-card,
.verify-tips,
.result-card {
  background: #fff;
  border: 1px solid #e0e4ee;
  border-radius: 12px;
  box-shadow: 0 4px 16px #1f2a5508;
}
.code-card {
  padding: 28px;
}
.card-title > div {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 26px;
}
.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #e7edff;
  color: #1451d7;
  display: grid;
  place-items: center;
  font-size: 25px;
}
.card-title h2,
.verify-tips h2 {
  margin: 0 0 5px;
  font-size: 19px;
}
.card-title p {
  margin: 0;
  color: #7c8293;
  font-size: 13px;
}
.code-label {
  display: block;
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 8px;
}
.code-card input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cfd6e7;
  border-radius: 8px;
  height: 54px;
  padding: 0 16px;
  font-size: 17px;
  outline: none;
  letter-spacing: 0.4px;
}
.code-card input:focus {
  border-color: #1954db;
  box-shadow: 0 0 0 3px #1954db18;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.primary-button,
.secondary-button {
  border-radius: 8px;
  padding: 12px 22px;
  font-weight: 700;
  cursor: pointer;
}
.primary-button {
  border: 0;
  background: #0d50dc;
  color: #fff;
}
.secondary-button {
  border: 1px solid #d4dced;
  background: #fff;
  color: #4e5870;
}
.primary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.result-card {
  display: flex;
  gap: 15px;
  align-items: flex-start;
  margin-top: 18px;
  padding: 20px 24px;
}
.result-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 800;
}
.result-card h2 {
  margin: 0 0 5px;
  font-size: 17px;
}
.result-card p {
  margin: 0;
  font-size: 14px;
}
.result-card small {
  display: block;
  margin-top: 8px;
  color: #747b8e;
}
.result-card.success {
  border-color: #a9e6c8;
  background: #f3fff8;
}
.success .result-icon {
  background: #d9f7e7;
  color: #138755;
}
.success h2 {
  color: #138755;
}
.result-card.failure {
  border-color: #f0b8b8;
  background: #fff8f8;
}
.failure .result-icon {
  background: #ffe0e0;
  color: #c63e47;
}
.failure h2 {
  color: #c63e47;
}
.verify-tips {
  margin-top: 22px;
  padding: 22px 26px;
}
.verify-tips ul {
  margin: 12px 0 0;
  padding-left: 20px;
  color: #6b7285;
  line-height: 2;
  font-size: 13px;
}
@media (max-width: 640px) {
  .verify-header {
    padding: 0 18px;
  }
  .verify-content {
    padding: 28px 14px;
  }
  .verify-heading {
    gap: 14px;
    flex-direction: column;
  }
  .verify-heading h1 {
    font-size: 26px;
  }
  .online-dot {
    align-self: flex-start;
  }
  .code-card {
    padding: 20px;
  }
  .form-actions {
    flex-direction: column;
  }
  .primary-button,
  .secondary-button {
    width: 100%;
  }
}
.ticket-preview,
.recent-card {
  margin-top: 18px;
  padding: 22px 26px;
}
.ticket-preview .card-title,
.recent-card .card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.ticket-preview .card-title h2,
.recent-card .card-title h2 {
  margin: 0;
  font-size: 17px;
}
.ticket-status {
  color: #138350;
  background: #ddf8e9;
  border-radius: 14px;
  padding: 5px 10px;
  font-size: 12px;
}
.ticket-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.ticket-grid small,
.ticket-grid b {
  display: block;
}
.ticket-grid small,
.recent-item small {
  color: #858b9d;
  font-size: 12px;
}
.ticket-grid b {
  margin-top: 5px;
}
.stats-row {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 18px;
  padding: 16px 4px;
}
.stats-row div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stats-row small {
  color: #858b9d;
  font-size: 12px;
}
.stats-row strong {
  font-size: 22px;
  color: #0d50dc;
}
.stats-row .secondary-button {
  margin-left: auto;
  padding: 8px 14px;
}
.recent-card .card-title span {
  color: #858b9d;
  font-size: 12px;
}
.recent-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #edf0f5;
}
.recent-item b,
.recent-item small {
  display: block;
}
.recent-item small {
  margin-top: 4px;
}
.recent-item time {
  color: #737b8f;
  font-size: 12px;
  white-space: nowrap;
}
.recent-empty {
  color: #858b9d;
  font-size: 13px;
  padding: 10px 0;
}
@media (max-width: 640px) {
  .ticket-grid {
    grid-template-columns: 1fr;
  }
  .stats-row {
    gap: 18px;
  }
  .stats-row .secondary-button {
    margin-left: 0;
  }
}
</style>
