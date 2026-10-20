<template><main class="page"><header class="topbar"><h1>我的订单</h1><button class="icon-button" title="刷新订单" @click="load"><AppIcon name="refresh" /></button></header><div class="container orders-page"><div class="identity-order-note"><span><AppIcon name="user-check" size="17" /></span><p>{{ identityNotice }}</p></div><div class="status-tabs"><button v-for="tab in tabs" :key="tab.value" :class="{active:status===tab.value}" @click="selectTab(tab.value)">{{ tab.label }}</button></div><p v-if="loading" class="loading">订单加载中...</p><section v-else-if="error" class="state-card error card"><b>订单暂时加载失败</b><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></section><section v-else-if="!visibleOrders.length" class="state-card empty card"><span class="empty-icon"><AppIcon name="ticket" size="28" /></span><b>暂无{{ tabs.find(t=>t.value===status)?.label || '' }}订单</b><p>去发现一场喜欢的活动吧。</p><RouterLink class="primary-button empty-action" to="/home">去逛逛活动</RouterLink></section><div v-else class="order-list"><RouterLink v-for="order in visibleOrders" :key="orderIdOf(order)" class="card order-card" :to="`/orders/${orderIdOf(order)}`"><div class="order-card-head"><span>{{ format(order.createdAt) }}</span><b :class="statusClass(order.status)">{{ status==='after-sale' && order.latestRefund?.status==='REJECTED' ? '申请未通过' : label(order.status) }}</b></div><div class="order-card-body"><div class="order-poster"><ActivityCover :src="order.coverFileUrl || ''" :title="order.activityTitle" /></div><div class="order-info"><h2>{{ order.activityTitle || '活动订单' }}</h2><p>{{ order.ticketTypeName || '票种' }} × {{ order.quantity || 1 }}</p><p><AppIcon name="calendar" size="15" /> {{ format(order.sessionStartTime) }}</p><strong>¥{{ order.payAmount ?? order.amount ?? '0.00' }}</strong></div><span class="arrow">›</span></div><div v-if="order.status==='PENDING_PAYMENT'" class="pay-hint">订单待支付 · 库存将暂时为你保留</div></RouterLink></div></div></main></template>
<script setup>
import AppIcon from '../../components/AppIcon.vue'
import ActivityCover from '../../components/ActivityCover.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '../../api/order'
import { profileApi } from '../../api/profile'
import { isPendingUse } from '../../utils/orderTickets'
import { orderIdOf } from '../../utils/id'
const route=useRoute(),router=useRouter()
const tabs=[{label:'全部',value:'all'},{label:'待付款',value:'pending-payment'},{label:'待使用',value:'pending-use'},{label:'已完成',value:'completed'},{label:'退款/售后',value:'after-sale'}]
const status=ref(String(route.query.filter || 'all')),orders=ref([]),loading=ref(true),error=ref('')
const profile=ref(null),profileError=ref(false)
const identityNotice=computed(()=>{
  if(profileError.value) return '当前账号实名信息暂时无法获取'
  if(!profile.value) return '正在获取当前账号实名信息...'
  const realName=String(profile.value.realName||'').trim()
  return profile.value.realNameVerified && realName
    ? `仅显示当前 Venuo 账号实名姓名为「${realName}」的订单信息`
    : '当前账号尚未填写实名信息，仅显示该账号的订单信息'
})
const visibleOrders=computed(()=>orders.value.filter(matchesFilter))
function ticketsOf(order){return Array.isArray(order?.tickets)?order.tickets:[]}
function matchesFilter(order){
  const current=status.value, orderStatus=String(order.status||'').toUpperCase(), tickets=ticketsOf(order)
  if(current==='all') return true
  if(current==='pending-payment') return orderStatus==='PENDING_PAYMENT'
  if(current==='pending-use') return isPendingUse(order)
  if(current==='completed') return orderStatus==='COMPLETED' || (orderStatus==='PAID' && tickets.length>0 && tickets.every(ticket=>String(ticket.status||'').toUpperCase()==='CHECKED_IN'))
  if(current==='after-sale') return Boolean(order.latestRefund) || ['REFUNDING','REFUNDED','AFTER_SALE'].includes(orderStatus)
  return true
}
async function load(){loading.value=true;error.value='';try{const result=await orderApi.list()||[];orders.value=Array.isArray(result)?result:(result.content||result.records||[])}catch(e){error.value=e.message||'网络请求失败'}finally{loading.value=false}}
function selectTab(value){status.value=value;router.replace({query:{...route.query,filter:value==='all'?undefined:value}});load()}
const label=s=>({PENDING_PAYMENT:'待付款',PAID:'已支付',COMPLETED:'已完成',CANCELLED:'已取消',EXPIRED:'已过期',REFUNDED:'已退款',REFUNDING:'退款中',AFTER_SALE:'售后中'}[s]||s||'未知状态');const statusClass=s=>String(s||'').toLowerCase();const format=v=>v?String(v).replace('T',' ').replace(/([+-]\d\d:\d\d|\.\d+Z?)$/,''):'下单时间待定'
onMounted(()=>{
  load()
  profileApi.get().then((result)=>{profile.value=result}).catch(()=>{profileError.value=true})
})
</script>
<style scoped>
.order-poster{overflow:hidden}.orders-page{background:#f8fafc}.status-tabs{display:flex;gap:8px;overflow:auto;padding:13px 16px;background:#fff;box-shadow:0 1px 4px rgba(15,23,42,.04)}.status-tabs button{border:0;background:#e9edff;color:var(--body);height:38px;padding:0 17px;border-radius:20px;white-space:nowrap;font-size:13px}.status-tabs button.active{background:var(--primary);color:#fff;font-weight:700}.status-tabs button i{display:inline-grid;place-items:center;width:20px;height:20px;margin-left:5px;border-radius:50%;background:#f97316;color:#fff;font-style:normal;font-size:11px}.orders-page>.container{padding-top:16px}.state-card{padding:38px 20px;text-align:center}.state-card p{margin:9px 0 19px;color:var(--muted);font-size:13px}.empty-action{display:inline-flex;align-items:center;justify-content:center}.order-list{display:flex;flex-direction:column;gap:14px}.order-card{padding:14px;display:block}.order-card-head{display:flex;justify-content:space-between;color:var(--muted);font-size:12px;padding-bottom:11px;border-bottom:1px solid var(--line)}.order-card-head b{font-size:12px;color:var(--success);background:#d1fae5;padding:5px 8px;border-radius:13px}.pending_payment .order-card-head b{color:#9a3412;background:#ffdccb}.order-card-body{display:flex;align-items:center;gap:12px;padding:13px 0 7px}.order-poster{width:142px;min-width:100px;max-width:33%;aspect-ratio:1/1;background:#e8efff center/cover;border-radius:10px}.order-info{min-width:0;flex:1}.order-info h2{margin:0 0 8px;font-size:17px;line-height:1.3;color:#0f172a}.order-info p{margin:6px 0;color:var(--muted);font-size:13px}.order-total{display:flex;justify-content:space-between;align-items:center;background:#f0f3ff;border-radius:10px;padding:11px 12px;font-size:13px}.order-total strong{font-size:19px;color:#0f172a}.order-actions{display:flex;justify-content:flex-end;gap:9px;margin-top:11px}.order-actions button{min-height:42px}.order-actions .primary-button{padding:0 15px}.order-actions .ghost-button{background:#eef0ff;border:0}.payment-alert{display:flex;align-items:center;gap:8px;padding:13px 14px;margin-bottom:15px;background:#ffeadf;color:#9a3412;border-radius:15px;font-size:13px}.payment-alert button{margin-left:auto;border:0;background:#b45309;color:#fff;border-radius:10px;padding:9px 13px;font-weight:700}.order-info p{display:flex;align-items:center;gap:4px}.identity-order-note{display:flex;align-items:center;gap:8px;padding:10px 12px;margin:12px 0 10px;background:#eef5ff;border:1px solid #dbe8ff;border-radius:10px;color:#52688e}.identity-order-note>span{display:grid;place-items:center;width:28px;height:28px;flex:none;border-radius:8px;background:#dceaff;color:var(--primary)}.identity-order-note p{margin:0;font-size:12px;line-height:1.4}</style>



