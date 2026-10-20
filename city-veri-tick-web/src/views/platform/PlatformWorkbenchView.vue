<template>
  <main class="content">
    <div class="title">
      <div>
        <div class="eyebrow">平台运营 / 工作台</div>
        <h1>平台工作台</h1>
        <p>统一查看平台活动审核和运营概况。</p>
      </div>
    </div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-else-if="loading" class="loading">看板数据加载中...</div>
    <section v-else class="metrics">
      <div><small>平台活动</small><strong>{{ stats.totalActivities }}</strong><span>待审核 {{ stats.pendingActivities }}</span></div>
      <div><small>已发布活动</small><strong>{{ stats.publishedActivities }}</strong><span>当前上线活动</span></div>
      <div><small>主办方</small><strong>{{ stats.totalOrganizers }}</strong><span>已通过 {{ stats.approvedOrganizers }}</span></div>
      <div><small>累计成交额</small><strong>{{ money(stats.paidRevenue) }}</strong><span>已支付订单 {{ stats.paidOrders }}</span></div>
    </section>
    <section class="panel">
      <div class="panel-title">
        <h2>审核队列</h2>
        <RouterLink to="/management/platform/activities">查看全部</RouterLink>
      </div>
      <p>
        当前有 <b>{{ stats.pendingActivities }}</b> 个活动等待平台管理员处理。
      </p>
    </section>
    <section v-if="!loading && !error" class="metrics secondary">
      <div><small>订单总量</small><strong>{{ stats.totalOrders }}</strong><span>全平台订单</span></div>
      <div><small>已出票数量</small><strong>{{ stats.totalTickets }}</strong><span>累计生成票券</span></div>
      <div><small>已核销票券</small><strong>{{ stats.checkedInTickets }}</strong><span>已完成入场核验</span></div>
      <div><small>核销率</small><strong>{{ stats.verificationRate }}%</strong><span>已核销 / 已出票</span></div>
    </section>
  </main>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { platformDashboardApi } from "../../api/platformDashboard";
const stats = ref({ totalActivities: 0, pendingActivities: 0, publishedActivities: 0, totalOrganizers: 0, approvedOrganizers: 0, totalOrders: 0, paidOrders: 0, paidRevenue: 0, totalTickets: 0, checkedInTickets: 0, verificationRate: 0 });
const loading = ref(true), error = ref("");
const money = (value) => `¥${Number(value || 0).toLocaleString("zh-CN", { minimumFractionDigits: 2 })}`;
onMounted(async () => {
  try { stats.value = await platformDashboardApi.summary(); }
  catch (e) { error.value = e?.message || "看板数据加载失败"; }
  finally { loading.value = false; }
});
</script>
<style scoped>
.content {
  padding: 34px;
  max-width: 1400px;
}
.title {
  margin-bottom: 28px;
}
.eyebrow {
  color: #6f7690;
  font-size: 13px;
  margin-bottom: 10px;
}
.title h1 {
  margin: 0 0 8px;
  font-size: 30px;
}
.title p {
  margin: 0;
  color: #6e7387;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.metrics div,
.panel {
  background: #fff;
  border: 1px solid #e1e5ef;
  border-radius: 10px;
  padding: 22px;
}
.metrics small,
.metrics strong {
  display: block;
}
.metrics span {
  display: block;
  color: #737b90;
  font-size: 12px;
  margin-top: 8px;
}
.secondary { margin-top: 16px; }
.loading, .error { padding: 22px 0; color: #737b90; }
.error { color: #c53a43; }
.metrics small {
  color: #737b90;
}
.metrics strong {
  font-size: 28px;
  margin-top: 12px;
  color: #124bd2;
}
.panel {
  margin-top: 22px;
}
.panel-title {
  display: flex;
  justify-content: space-between;
}
.panel-title h2 {
  margin: 0;
  font-size: 18px;
}
.panel-title a {
  color: #0d50dc;
}
.panel p {
  color: #6e7487;
}
@media (max-width: 700px) {
  .content {
    padding: 20px 14px;
  }
  .metrics {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
