<template>
  <main class="management-content activity-page">
    <div class="management-title">
      <div>
        <div class="eyebrow">资源中心 / 活动</div>
        <h1>活动管理</h1>
        <p>查看、编辑和提交您的活动资源，统一管理活动状态。</p>
      </div>
      <RouterLink class="management-button" to="/management/organizer/activities/new">＋ 新建活动</RouterLink>
    </div>

    <section class="activity-summary">
      <div class="summary-card"><span class="summary-icon blue"><AppIcon name="calendar"/></span><div><small>全部活动</small><strong>{{ activities.length }}</strong></div></div>
      <div class="summary-card"><span class="summary-icon orange"><AppIcon name="clock"/></span><div><small>待审核</small><strong>{{ countByStatus('PENDING') }}</strong></div></div>
      <div class="summary-card"><span class="summary-icon green"><AppIcon name="check-circle"/></span><div><small>已发布</small><strong>{{ countByStatus('PUBLISHED') }}</strong></div></div>
      <div class="summary-card"><span class="summary-icon gray"><AppIcon name="edit"/></span><div><small>草稿</small><strong>{{ countByStatus('DRAFT') + countByStatus('REJECTED') }}</strong></div></div>
    </section>

    <section class="management-card activity-toolbar">
      <div class="toolbar-heading"><div><h2>活动列表</h2><span>共 {{ filteredActivities.length }} 项</span></div><button class="reset-button" type="button" @click="resetFilters">重置筛选</button></div>
      <div class="filter-row">
        <label class="search-field"><AppIcon name="search" size="17"/><input v-model.trim="keyword" placeholder="搜索活动名称或场地" /></label>
        <label class="select-field"><span>状态</span><select v-model="status"><option value="">全部状态</option><option value="DRAFT">草稿</option><option value="PENDING">待审核</option><option value="PUBLISHED">已发布</option><option value="REJECTED">已驳回</option><option value="OFFLINE">已下架</option></select></label>
        <button class="management-button filter-button" type="button" @click="load">查询</button>
      </div>
    </section>

    <section class="management-card table-wrap">
      <div v-if="loading" class="empty-panel">活动加载中...</div>
      <div v-else-if="error" class="empty-panel error-panel">{{ error }}<button class="management-button light" type="button" @click="load">重新加载</button></div>
      <div v-else-if="!filteredActivities.length" class="empty-panel"><div class="empty-icon"><AppIcon name="calendar"/></div><strong>暂无活动</strong><span>当前筛选条件下没有匹配的活动</span><RouterLink class="management-button light" to="/management/organizer/activities/new">创建第一个活动</RouterLink></div>
      <table v-else class="management-table">
        <thead><tr><th class="activity-column">活动信息</th><th>状态</th><th>活动场地</th><th>审核信息</th><th class="operation-column">操作</th></tr></thead>
        <tbody>
          <tr v-for="activity in filteredActivities" :key="activity.id">
            <td><div class="activity-title-cell"><div class="activity-thumb" :style="coverStyle(activity)"><span v-if="!activity.coverFileUrl">活动</span></div><div><b>{{ activity.title || '未命名活动' }}</b><small>ID：{{ activity.id }}</small></div></div></td>
            <td><span class="status-chip" :class="`status-${activity.status}`"><i></i>{{ statusLabel(activity.status) }}</span></td>
            <td><div class="venue-cell"><b>{{ activity.venueName || '未设置场地' }}</b><small>{{ activity.venueAddress || '暂无地址信息' }}</small></div></td>
            <td><span v-if="activity.rejectionReason" class="cell-danger">{{ activity.rejectionReason }}</span><span v-else class="cell-muted">{{ reviewLabel(activity.status) }}</span></td>
            <td><div class="action-row"><RouterLink class="table-action" :to="`/management/organizer/activities/${activity.id}/edit`">{{ isDraftLike(activity)?'继续编辑':'查看详情' }}</RouterLink><RouterLink class="table-action" :to="{path:'/management/organizer/orders',query:{activityId:activity.id}}">订单</RouterLink><RouterLink class="table-action" :to="{path:'/management/organizer/verification',query:{activityId:activity.id}}">核销</RouterLink><button v-if="isDraftLike(activity)" class="table-action primary" type="button" @click="submit(activity)">提交审核</button><button v-if="isDraftLike(activity)" class="table-action danger" type="button" @click="removeActivity(activity)">删除</button></div></td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</template>

<script setup>
import {askAction} from "../../utils/actionDialog"
import { computed, onMounted, ref, watch } from 'vue'
import {useRoute,useRouter} from 'vue-router'
import AppIcon from '../../components/AppIcon.vue'
const route=useRoute(),router=useRouter()
import { organizerActivityApi } from '../../api/organizerActivity'

const activities = ref([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const status = ref('')
watch(()=>route.query,q=>{keyword.value=String(q.keyword||'');status.value=String(q.status||'')},{immediate:true})
watch([keyword,status],()=>router.replace({query:{...route.query,keyword:keyword.value||undefined,status:status.value||undefined}}))

const filteredActivities = computed(() => activities.value.filter((activity) => {
  const text = `${activity.id || ''} ${activity.title || ''} ${activity.venueName || ''}`
  return (!status.value || activity.status === status.value) && (!keyword.value || text.includes(keyword.value))
}))
const countByStatus = (value) => activities.value.filter((activity) => activity.status === value).length
const statusLabel = (value) => ({ DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下架', CANCELLED: '已取消' }[value] || value || '未知')
const reviewLabel = (value) => ({ PENDING: '等待平台审核', PUBLISHED: '审核通过', DRAFT: '尚未提交审核', REJECTED: '请修改后重新提交', OFFLINE: '活动已下架' }[value] || '—')
const isDraftLike = (activity) => activity.status === 'DRAFT' || activity.status === 'REJECTED'
const coverStyle = (activity) => activity.coverFileUrl ? { backgroundImage: `url(${activity.coverFileUrl})` } : {}
function resetFilters() { keyword.value = ''; status.value = '' }
async function load() { loading.value = true; error.value = ''; try { const result = await organizerActivityApi.list(); activities.value = Array.isArray(result) ? result : [] } catch (e) { error.value = e?.message || '活动加载失败' } finally { loading.value = false } }
async function submit(activity) { try { await organizerActivityApi.submit(activity.id); await load() } catch (e) { error.value = e?.message || '提交审核失败' } }
async function removeActivity(activity) {
  if (!await askAction({title:"删除活动草稿",message:`确定删除活动“${activity.title || '未命名活动'}”吗？删除后无法恢复。`,confirmText:"确认删除"})) return
  try { await organizerActivityApi.remove(activity.id); await load() } catch (e) { error.value = e?.message || '删除活动失败' }
}
onMounted(load)
</script>

<style scoped>
.activity-page{max-width:1500px}.eyebrow{color:#6f7690;font-size:13px;margin-bottom:10px}.management-title{margin-bottom:24px}.management-title h1{font-size:30px;letter-spacing:0}.management-title p{font-size:14px}.activity-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:22px}.summary-card{display:flex;align-items:center;gap:14px;background:#fff;border:1px solid #e2e3ee;border-radius:10px;padding:18px 20px;box-shadow:0 3px 12px #1f2a5508}.summary-card small,.summary-card strong{display:block}.summary-card small{color:#73798b;font-size:13px;margin-bottom:7px}.summary-card strong{font-size:25px;color:#202334}.summary-icon{width:38px;height:38px;border-radius:9px;display:grid;place-items:center;font-size:19px;font-weight:700}.summary-icon.blue{background:#e5edff;color:#1553dd}.summary-icon.orange{background:#fff2d9;color:#d68a00}.summary-icon.green{background:#e0f8eb;color:#15945c}.summary-icon.gray{background:#ececf4;color:#70768a}.activity-toolbar{padding:20px 22px;margin-bottom:18px}.toolbar-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}.toolbar-heading h2{margin:0 0 5px;font-size:17px}.toolbar-heading span{font-size:12px;color:#858b9d}.reset-button{border:0;background:none;color:#0d50dc;font-size:13px;cursor:pointer}.filter-row{align-items:center}.search-field,.select-field{display:flex;align-items:center;gap:8px;background:#fafaff;border:1px solid #dfe2ee;border-radius:8px;padding:0 12px;height:42px}.search-field{flex:1;min-width:240px;color:#7d8396}.search-field input{border:0;background:transparent;outline:0;min-width:0;width:100%;padding:0}.select-field{color:#6d7386}.select-field select{border:0;background:transparent;outline:0;color:#292e40;padding-right:18px}.filter-button{height:42px;padding:0 24px}.activity-page .table-wrap{overflow-x:auto}.management-table th{height:54px}.management-table td{height:86px}.management-table .activity-column{min-width:330px}.management-table{min-width:1120px}.operation-column{width:300px;min-width:300px}.activity-thumb{width:68px;height:50px;display:grid;place-items:center;border-radius:7px;background:#e8edff center/cover no-repeat;color:#7481a8;font-size:12px}.activity-title-cell{min-width:300px}.activity-title-cell b{font-size:14px}.activity-title-cell small{font-size:12px;color:#858b9d}.status-chip{gap:6px}.status-chip i{width:6px;height:6px;border-radius:50%;background:currentColor}.venue-cell b,.venue-cell small{display:block}.venue-cell b{font-size:13px;font-weight:600}.venue-cell small{color:#858b9d;font-size:12px;margin-top:5px;max-width:210px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.cell-muted{font-size:12px}.activity-page .action-row{display:flex;flex-wrap:wrap;align-items:center;justify-content:flex-start;gap:8px;width:264px}.activity-page .action-row .table-action{display:inline-flex;align-items:center;justify-content:center;flex:0 0 auto;min-height:34px;padding:0 12px;line-height:1.4;white-space:nowrap;text-decoration:none;box-sizing:border-box}.activity-page .action-row .table-action:hover{border-color:#9dbaf4;background:#edf3ff}.activity-page .action-row .table-action.primary:hover{background:#164fce;color:#fff}.activity-page .action-row .table-action.danger:hover{background:#fff0f1;border-color:#e7a5ae}.table-action{border:1px solid #d4defc;border-radius:6px;padding:8px 12px;color:#0d50dc;background:#f5f7ff;font-size:12px;font-weight:700}.table-action.primary{border-color:#0d50dc;background:#0d50dc;color:#fff}.table-action.danger{border-color:#f0c8cc;background:#fff5f5;color:#c53a43}.more-action{border:0;background:transparent;color:#747a8d;font-size:20px;padding:3px 7px}.empty-panel{display:flex;flex-direction:column;align-items:center;gap:10px;padding:78px 20px}.empty-panel strong{font-size:16px;color:#34394b}.empty-panel span{color:#83899a;font-size:13px}.empty-icon{width:44px;height:44px;border-radius:50%;display:grid;place-items:center;background:#eef1ff;color:#6c78a3;font-size:24px}.empty-panel .management-button{margin-top:8px}.error-panel{color:#c53a43}.error-panel .management-button{color:#0d50dc}
@media(max-width:1000px){.activity-summary{grid-template-columns:repeat(2,1fr)}.management-table{min-width:1120px}.table-wrap{overflow:auto}}
@media(max-width:640px){.activity-summary{grid-template-columns:1fr 1fr;gap:10px}.summary-card{padding:14px}.summary-icon{width:32px;height:32px}.management-title{align-items:flex-start;flex-direction:column}.filter-row{align-items:stretch;flex-direction:column}.filter-button{width:100%}}
</style>
