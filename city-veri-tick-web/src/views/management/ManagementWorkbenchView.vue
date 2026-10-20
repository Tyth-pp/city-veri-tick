<template><main class="management-content"><div class="management-title"><div><h1>工作台</h1><p>欢迎回来，这里是您的活动业务概览。</p></div><span class="management-date">▣ {{ today }}</span></div><div class="metric-grid"><section v-for="item in metrics" :key="item.label" class="metric-card management-card"><span>{{ item.icon }}</span><small>{{ item.label }}</small><strong>{{ item.value }}</strong><em>{{ item.note }}</em></section></div><div class="dashboard-grid"><section class="management-card dashboard-panel"><h2>活动管理概览</h2><div class="dashboard-list"><div><span class="dashboard-dot blue"></span><b>全部活动</b><strong>{{ stats.total }}</strong></div><div><span class="dashboard-dot orange"></span><b>待审核</b><strong>{{ stats.pending }}</strong></div><div><span class="dashboard-dot green"></span><b>已发布</b><strong>{{ stats.published }}</strong></div><div><span class="dashboard-dot gray"></span><b>草稿</b><strong>{{ stats.draft }}</strong></div></div></section><section class="management-card dashboard-panel quick-panel"><h2>快捷操作</h2><div class="quick-grid"><RouterLink to="/management/activities/new">＋<span>创建活动</span></RouterLink><RouterLink to="/management/activities">▣<span>管理活动</span></RouterLink><a>▤<span>票种库存</span></a><a>♢<span>核销管理</span></a></div></section></div><section class="management-card recent-panel"><div class="panel-heading"><h2>最近活动</h2><RouterLink to="/management/activities">查看全部</RouterLink></div><div v-if="loading" class="empty-panel">活动加载中...</div><div v-else-if="error" class="empty-panel">{{ error }}</div><div v-else-if="!activities.length" class="empty-panel">暂无活动</div><div v-else class="recent-list"><div v-for="activity in activities.slice(0,5)" :key="activity.id"><span class="recent-icon">✓</span><div><b>{{ activity.title }}</b><small>{{ statusLabel(activity.status) }} · {{ activity.venueName || '场地待定' }}</small></div><RouterLink :to="`/management/activities/${activity.id}/edit`">查看</RouterLink></div></div></section></main></template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { organizerActivityApi } from '../../api/organizerActivity'

const activities = ref([])
const loading = ref(true)
const error = ref('')

const today = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const stats = computed(() => {
  const list = Array.isArray(activities.value) ? activities.value : []
  return {
    total: list.length,
    pending: list.filter((item) => item.status === 'PENDING').length,
    published: list.filter((item) => item.status === 'PUBLISHED').length,
    draft: list.filter((item) => item.status === 'DRAFT' || item.status === 'REJECTED').length
  }
})

const metrics = computed(() => [
  { icon: '▣', label: '全部活动', value: stats.value.total, note: '当前主办方' },
  { icon: '◷', label: '待审核', value: stats.value.pending, note: '等待平台处理' },
  { icon: '✓', label: '已发布', value: stats.value.published, note: '正在售票' },
  { icon: '!', label: '草稿', value: stats.value.draft, note: '需要继续编辑' }
])

const statusLabel = (status) => ({
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
  CANCELLED: '已取消'
}[status] || status)

onMounted(async () => {
  try {
    const result = await organizerActivityApi.list()
    activities.value = Array.isArray(result) ? result : []
  } catch (requestError) {
    error.value = requestError?.message || '活动加载失败'
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>.metric-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:18px}.metric-card{padding:20px;display:grid;grid-template-columns:auto 1fr;gap:5px 10px}.metric-card>span{grid-row:span 2;color:#0d50dc;font-size:24px}.metric-card small{color:#6f7486}.metric-card strong{font-size:25px}.metric-card em{grid-column:2;color:#7b8091;font-style:normal;font-size:12px}.dashboard-grid{display:grid;grid-template-columns:1.5fr 1fr;gap:22px;margin-top:22px}.dashboard-panel{padding:22px}.dashboard-panel h2,.recent-panel h2{font-size:17px;margin:0 0 20px}.dashboard-list{display:grid;grid-template-columns:1fr 1fr;gap:16px}.dashboard-list div{padding:18px;background:#fafaff;border-radius:8px;display:grid;grid-template-columns:auto 1fr auto;gap:9px;align-items:center}.dashboard-dot{width:9px;height:9px;border-radius:50%}.blue{background:#1553dd}.orange{background:#f0a62b}.green{background:#1ab676}.gray{background:#8d93a4}.quick-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.quick-grid a{min-height:84px;border-radius:8px;background:#eff1ff;color:#0d50dc;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;font-size:22px}.quick-grid span{font-size:13px;color:#32374a}.recent-panel{margin-top:22px;padding-bottom:8px}.panel-heading{padding:22px 22px 0;display:flex;justify-content:space-between}.panel-heading a{color:#0d50dc;font-weight:700}.recent-list>div{display:flex;align-items:center;gap:12px;padding:16px 22px;border-top:1px solid #ececf3}.recent-list small{display:block;color:#777d90;margin-top:5px}.recent-list a{margin-left:auto;color:#0d50dc}.recent-icon{width:30px;height:30px;border-radius:8px;background:#dff8eb;color:#11925a;display:grid;place-items:center}.management-date{color:#555b6d}@media(max-width:1100px){.metric-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:700px){.dashboard-grid{grid-template-columns:1fr}.dashboard-list{grid-template-columns:1fr}.metric-grid{gap:10px}.metric-card{padding:14px}}
</style>
