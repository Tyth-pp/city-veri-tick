<template>
 <main :class="platform?'content':'management-content'">
 <div class="title"><div><div class="eyebrow">{{ platform?'平台运营 / 核销监管':'现场服务 / 核销数据' }}</div><h1>{{ platform?'核销监管':'核销数据' }}</h1><p>按活动、场次和时间核对门票与核销记录。</p></div></div>
 <section class="metrics"><div v-for="(label,key) in metricLabels" :key="key"><small>{{ label }}</small><strong>{{ loading||error?'—':displaySummary[key] }}</strong></div></section>
 <section class="panel toolbar">
  <label>活动<select v-model="activityId" aria-label="筛选活动" @change="sessionId=''"><option value="">全部活动</option><option v-for="a in activities" :key="a.id" :value="String(a.id)">{{ a.title }}</option></select></label>
  <label>场次<select v-model="sessionId" aria-label="筛选场次"><option value="">全部场次</option><option v-for="s in sessions" :key="s.id" :value="String(s.id)">{{ date(s.start) }} · {{ s.id }}</option></select></label>
  <label>状态<select v-model="status" aria-label="筛选门票状态"><option value="">全部状态</option><option value="UNUSED">未核销</option><option value="CHECKED_IN">已核销</option><option value="REFUNDED">已退款</option><option value="REFUNDING">退款暂停核销</option></select></label>
  <label>核销起始日期<ChineseDateTimeInput v-model="start" type="date" aria-label="核销起始日期" /></label><label>核销截止日期<ChineseDateTimeInput v-model="end" type="date" aria-label="核销截止日期" /></label>
  <label>订单<input v-model.trim="orderKeyword" placeholder="订单号或订单 ID" /></label><label>核销人员 / 票号<input v-model.trim="keyword" placeholder="姓名、账号、票号或核销码" /></label>
  <button @click="reset">重置筛选</button><button :disabled="loading" @click="load">{{ loading?'刷新中…':'刷新数据' }}</button>
  <small>上方统计与下方筛选结果一致；未核销不等同于可入场。日期按核销时间筛选。</small>
 </section>
 <section class="panel table-wrap"><p v-if="loading">记录加载中…</p><p v-else-if="error" class="error" role="alert">{{ error }} <button @click="load">重新加载</button></p><p v-else-if="invalidRange" class="error" role="alert">截止日期不能早于起始日期</p><p v-else-if="!filtered.length">暂无符合条件的门票记录</p><table v-else><thead><tr><th>活动 / 场次</th><th>票种</th><th>门票编号</th><th>订单</th><th>门票状态</th><th>核销人员</th><th>核销时间</th></tr></thead><tbody><tr v-for="r in filtered" :key="r.ticketId"><td><div class="activity"><div class="thumb"><ActivityCover :src="r.coverFileUrl || ''" :title="r.activityTitle" /></div><div><RouterLink :to="base+'/activities/'+r.activityId+(platform?'':'/edit')">{{ r.activityTitle || '活动资料不可用' }}</RouterLink><small>{{ date(r.sessionStartTime) }}</small></div></div></td><td>{{ r.ticketTypeName }}</td><td>{{ r.ticketNo || r.ticketId }}</td><td><RouterLink v-if="r.orderId" :to="base+'/orders/'+r.orderId">{{ r.orderNo || r.orderId }}</RouterLink><span v-else>—</span></td><td>{{ r.orderStatus==='REFUNDING'?'退款暂停核销':ticketStatus(r.ticketStatus) }}</td><td>{{ r.verifierName || r.verifierUsername || (r.verifierId?'账号 ID：'+r.verifierId:'—') }}<small v-if="r.verifierName">{{ r.verifierUsername }}</small></td><td>{{ date(r.checkedInAt) }}</td></tr></tbody></table><p v-if="!loading&&!error&&!invalidRange" class="hint">共 {{ filtered.length }} 条符合条件的记录</p></section>
 </main>
</template>
<script setup>
import ChineseDateTimeInput from './ChineseDateTimeInput.vue'
import {computed,onMounted,ref,watch} from 'vue'
import {useRoute} from 'vue-router'
import {platformOrderApi} from '../api/platformOrder'
import {organizerOrderApi} from '../api/organizerOrder'
import ActivityCover from './ActivityCover.vue'
const props=defineProps({platform:Boolean}),route=useRoute(),summary=ref(null),records=ref([]),loading=ref(false),error=ref('')
const activityId=ref(''),sessionId=ref(''),status=ref(''),start=ref(''),end=ref(''),orderKeyword=ref(''),keyword=ref('')
const base=computed(()=>props.platform?'/management/platform':'/management/organizer')
watch(()=>route.query,query=>{orderKeyword.value=String(query?.orderId||'');activityId.value=String(query?.activityId||'');sessionId.value=String(query?.sessionId||'');start.value=String(query?.start||'');end.value=String(query?.end||'');status.value=String(query?.status||'')},{immediate:true})
const metricLabels={totalTickets:'门票总数',usedTickets:'已核销',unusedTickets:'未核销',cancelledTickets:'已退款'}
const date=v=>v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'—'
const ticketStatus=v=>({UNUSED:'未核销',CHECKED_IN:'已核销',REFUNDED:'已退款',EXPIRED:'已过期',CANCELLED:'已作废'}[v]||v||'未知')
const activities=computed(()=>[...new Map(records.value.map(r=>[String(r.activityId),{id:r.activityId,title:r.activityTitle}])).values()])
const sessions=computed(()=>[...new Map(records.value.filter(r=>r.sessionId&&(!activityId.value||String(r.activityId)===activityId.value)).map(r=>[String(r.sessionId),{id:r.sessionId,start:r.sessionStartTime}])).values()])
const invalidRange=computed(()=>start.value&&end.value&&end.value<start.value)
const filtered=computed(()=>{
 const from=start.value?new Date(start.value+'T00:00:00').getTime():null,to=end.value?new Date(end.value+'T23:59:59.999').getTime():null,q=keyword.value.toLowerCase(),o=orderKeyword.value.toLowerCase()
 return records.value.filter(r=>(!activityId.value||String(r.activityId)===activityId.value)&&(!sessionId.value||String(r.sessionId)===sessionId.value)&&(!status.value||(status.value==='REFUNDING'?r.orderStatus==='REFUNDING':r.ticketStatus===status.value))&&(!o||String(r.orderId)===o||String(r.orderNo||'').toLowerCase().includes(o))&&(!q||[r.verifierName,r.verifierUsername,r.ticketNo,r.verificationCode].some(v=>String(v||'').toLowerCase().includes(q)))&&((from==null&&to==null)||(r.checkedInAt&&(from==null||Date.parse(r.checkedInAt)>=from)&&(to==null||Date.parse(r.checkedInAt)<=to))))
})
const displaySummary=computed(()=>({totalTickets:filtered.value.length,usedTickets:filtered.value.filter(r=>r.ticketStatus==='CHECKED_IN').length,unusedTickets:filtered.value.filter(r=>r.ticketStatus==='UNUSED').length,cancelledTickets:filtered.value.filter(r=>r.ticketStatus==='REFUNDED').length}))
function reset(){activityId.value='';sessionId.value='';status.value='';start.value='';end.value='';orderKeyword.value='';keyword.value=''}
let request=0
async function load(){const current=++request;loading.value=true;error.value='';const api=props.platform?platformOrderApi:organizerOrderApi;try{const r=await api.verificationRecords('');if(current===request){records.value=r||[]}}catch(e){if(current===request)error.value=e.message||'核销数据加载失败'}finally{if(current===request)loading.value=false}}
onMounted(load)
</script>
<style scoped>
.panel.table-wrap{overflow-x:auto}
main{padding:34px;max-width:1500px}.title{margin-bottom:24px}.eyebrow,.title p,.hint{font-size:13px;color:#74829a}h1{font-size:30px;margin:8px 0}.metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px;margin-bottom:20px}.metrics>div,.panel{padding:22px;background:#fff;border:1px solid #e0e7f1;border-radius:12px}.metrics small,.metrics strong{display:block}.metrics small{color:#74829a}.metrics strong{font-size:28px;margin-top:10px;color:#245cf5}.panel{margin-bottom:20px}.toolbar{display:flex;align-items:flex-end;flex-wrap:wrap;gap:14px}.toolbar label{display:flex;flex-direction:column;gap:7px;font-size:13px;color:#64748b}.toolbar input,.toolbar select,.toolbar button,.error button{padding:9px 12px;border:1px solid #d6e1f4;background:#f8faff;border-radius:7px;color:#24334d;min-height:38px;box-sizing:border-box}.toolbar small{width:100%;color:#74829a;font-size:12px}.table-wrap{overflow:auto}table{width:100%;min-width:1000px;border-collapse:collapse}th,td{padding:14px 10px;border-bottom:1px solid #e6ebf3;text-align:left;font-size:13px}th{background:#f7f9fd;color:#64748b}td small{display:block;color:#74829a;margin-top:6px}a{color:#245cf5;text-decoration:none}.activity{display:flex;align-items:center;gap:10px;min-width:250px}.thumb{width:72px;height:48px;flex:none;overflow:hidden;border-radius:7px}.activity .thumb :deep(.activity-cover){width:100%;height:100%}.error{color:#bd3245}@media(max-width:800px){main{padding:20px 14px}.metrics{grid-template-columns:1fr 1fr}}
</style>
