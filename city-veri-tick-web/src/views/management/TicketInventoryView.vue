<template>
  <main class="management-content inventory-page">
    <div class="management-title">
      <div>
        <div class="eyebrow">资源中心 / 票种与库存</div>
        <h1>票种与库存</h1>
        <p>查看各活动票种销售状态、可售库存和锁定库存。</p>
      </div>
      <RouterLink class="management-button" to="/management/organizer/activities/new">＋ 新建活动</RouterLink>
    </div>

    <section class="inventory-summary">
      <div class="summary-card"><span class="summary-icon blue"><AppIcon name="ticket"/></span><div><small>票种总数</small><strong>{{ summary.ticketCount }}</strong></div></div>
      <div class="summary-card"><span class="summary-icon green"><AppIcon name="check-circle"/></span><div><small>可售库存</small><strong>{{ summary.stock }}</strong></div></div>
      <div class="summary-card"><span class="summary-icon orange"><AppIcon name="shield"/></span><div><small>锁定库存</small><strong>{{ summary.locked }}</strong></div></div>
      <div class="summary-card"><span class="summary-icon red"><AppIcon name="alert"/></span><div><small>低库存票种</small><strong>{{ summary.lowStock }}</strong></div></div>
    </section>

    <section class="management-card inventory-toolbar">
      <div class="filter-row">
        <label class="select-field">活动<select v-model="selectedActivityId" @change="loadSessions"><option value="">全部活动</option><option v-for="activity in activities" :key="activity.id" :value="String(activity.id)">{{ activity.title }}</option></select></label>
        <label class="search-field"><AppIcon name="search" size="18" /><input v-model.trim="keyword" placeholder="搜索活动或票种" /></label>
        <button class="management-button filter-button" type="button" @click="loadData">刷新</button>
      </div>
    </section>

    <section class="management-card table-wrap">
      <div v-if="loading" class="empty-panel">库存加载中...</div>
      <div v-else-if="error" class="empty-panel error-panel">{{ error }}<button class="management-button light" type="button" @click="loadData">重新加载</button></div>
      <div v-else-if="!filteredRows.length" class="empty-panel">暂无符合条件的票种</div>
      <table v-else class="management-table inventory-table">
        <thead><tr><th>活动 / 场次</th><th>票种</th><th>价格</th><th>可售库存</th><th>锁定库存</th><th>启用 / 可售状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="row in filteredRows" :key="row.ticket.id">
          <td><div class="activity-info"><div class="activity-thumb" :style="coverStyle(row.activity)"><span v-if="!row.activity.coverFileUrl">活动</span></div><div><b>{{ row.activity.title }}</b><small>{{ formatRange(row.session.startTime, row.session.endTime) }}</small></div></div></td>
          <td>{{ row.ticket.name }}</td><td class="price">¥{{ row.ticket.price }}</td>
          <td><strong :class="{ warning: row.inventory.stock <= 10 }">{{ row.inventory.stock }}</strong><small>总量 {{ row.inventory.totalStock }}</small></td>
          <td>{{ row.inventory.lockedStock }}</td>
          <td><span class="status-chip" :class="row.ticket.enabled ? 'status-PUBLISHED' : 'status-OFFLINE'">{{ saleStatus(row) }}</span><small>{{ row.ticket.enabled ? "票种已启用" : "票种已停用" }}</small></td>
          <td><RouterLink class="table-action" :to="`/management/organizer/activities/${row.activity.id}/edit`">管理票种</RouterLink></td>
        </tr></tbody>
      </table>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import {useRoute,useRouter} from 'vue-router'
const route=useRoute(),router=useRouter()
import { organizerActivityApi } from '../../api/organizerActivity'
import { organizerScheduleApi } from '../../api/organizerSchedule'

import AppIcon from '../../components/AppIcon.vue'
import { inventorySaleStatus } from '../../utils/inventorySaleStatus'
const now=ref(Date.now());let clock
const saleStatus=row=>inventorySaleStatus(row,now.value)
onMounted(()=>{clock=setInterval(()=>now.value=Date.now(),1000)})
onUnmounted(()=>clearInterval(clock))
const activities = ref([]), rows = ref([]), loading = ref(false), error = ref(''), selectedActivityId = ref(''), keyword = ref('')
watch(()=>route.query,q=>{selectedActivityId.value=String(q.activityId||'');keyword.value=String(q.keyword||'');if(activities.value.length)loadSessions()},{immediate:true})
watch([selectedActivityId,keyword],()=>router.replace({query:{activityId:selectedActivityId.value||undefined,keyword:keyword.value||undefined}}))
const summary = computed(() => ({
  ticketCount: rows.value.length,
  stock: rows.value.reduce((sum, row) => sum + Number(row.inventory.stock || 0), 0),
  locked: rows.value.reduce((sum, row) => sum + Number(row.inventory.lockedStock || 0), 0),
  lowStock: rows.value.filter(row => Number(row.inventory.stock || 0) <= 10).length
}))
const filteredRows = computed(() => {
  const value = keyword.value.toLowerCase()
  return value ? rows.value.filter(row => `${row.activity.title}${row.ticket.name}`.toLowerCase().includes(value)) : rows.value
})
async function loadActivities() { activities.value = await organizerActivityApi.list() || [] }
async function loadSessions() {
  loading.value = true; error.value = ''
  try {
    const selected = selectedActivityId.value ? activities.value.filter(item => String(item.id) === String(selectedActivityId.value)) : activities.value
    const result = []
    for (const activity of selected) {
      const sessions = await organizerScheduleApi.listSessions(String(activity.id)) || []
      for (const session of sessions) {
        const tickets = await organizerScheduleApi.listTicketTypes(String(session.id)) || []
        for (const ticket of tickets) {
          const inventory = await organizerScheduleApi.inventory(String(ticket.id))
          if(!inventory) throw new Error("库存数据不可用，请重试")
          result.push({ activity, session, ticket, inventory })
        }
      }
    }
    rows.value = result
  } catch (e) { error.value = e?.message || '库存加载失败' } finally { loading.value = false }
}
async function loadData() { try { await loadActivities(); await loadSessions() } catch (e) { error.value = e?.message || '活动加载失败' } }
function formatRange(start, end) { return `${start?.slice(0, 16) || '时间待定'} - ${end?.slice(0, 16) || ''}` }
function coverStyle(activity) { return activity?.coverFileUrl ? { backgroundImage: `url(${activity.coverFileUrl})` } : {} }
onMounted(loadData)
</script>

<style scoped>
.inventory-page{max-width:1500px}.eyebrow{color:#6f7690;font-size:13px;margin-bottom:10px}.inventory-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:22px}.summary-card{display:flex;align-items:center;gap:14px;background:#fff;border:1px solid #e2e3ee;border-radius:10px;padding:18px 20px}.summary-card small,.summary-card strong{display:block}.summary-card small{color:#73798b;font-size:13px;margin-bottom:7px}.summary-card strong{font-size:25px}.summary-icon{width:38px;height:38px;border-radius:9px;display:grid;place-items:center;font-size:16px;font-weight:700}.summary-icon.blue{background:#e5edff;color:#1553dd}.summary-icon.green{background:#e0f8eb;color:#15945c}.summary-icon.orange{background:#fff2d9;color:#d68a00}.summary-icon.red{background:#ffe4e4;color:#c53a43}.inventory-toolbar{padding:18px 20px;margin-bottom:18px}.search-field,.select-field{display:flex;align-items:center;gap:8px;background:#fafaff;border:1px solid #dfe2ee;border-radius:8px;padding:0 12px;height:42px}.search-field{flex:1;min-width:240px;color:#7d8396}.search-field input{border:0;background:transparent;outline:0;min-width:0;width:100%;padding:0}.select-field select{border:0;background:transparent;outline:0;color:#292e40;padding:0 18px 0 4px}.filter-button{height:42px}.inventory-table td small{display:block;color:#858b9d;font-size:12px;margin-top:5px}.inventory-table td b{display:block}.activity-info{display:flex;align-items:center;gap:12px;min-width:300px}.activity-thumb{width:86px;height:54px;flex:none;display:grid;place-items:center;border-radius:8px;background:#e8efff center/cover no-repeat;color:#2b5fd8;font-size:11px;font-weight:700}.price{color:#0d50dc;font-weight:700}.warning{color:#c53a43}.error-panel{color:#c53a43}.error-panel .management-button{display:block;margin:12px auto 0}.table-action{white-space:nowrap}@media(max-width:1000px){.inventory-summary{grid-template-columns:repeat(2,1fr)}.management-table{min-width:900px}.table-wrap{overflow:auto}}@media(max-width:640px){.inventory-summary{grid-template-columns:1fr 1fr;gap:10px}.summary-card{padding:14px 10px}.summary-icon{width:32px;height:32px}.filter-row{align-items:stretch;flex-direction:column}.search-field{min-width:0}}
</style>
