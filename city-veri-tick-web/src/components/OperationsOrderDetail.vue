<template>
 <main :class="platform?'content':'management-content'">
 <div class="title-row"><div><div class="eyebrow">{{ platform?'平台运营 / 订单监管 / 详情':'交易中心 / 订单详情' }}</div><h1>订单详情</h1><p>核对订单、购票实名信息和门票核销情况。</p></div><RouterLink :to="{path:base+'/orders',query:{...route.query}}">返回订单列表</RouterLink></div>
 <section v-if="loading" class="panel">订单加载中…</section><section v-else-if="error" class="panel error" role="alert">{{ error }} <button @click="load">重新加载</button></section>
 <template v-else-if="order">
 <section class="panel hero"><div><span class="status">{{ orderStatus(order.status) }}</span><h2>{{ order.activityTitle || '活动信息不可用' }}</h2><p>订单号：{{ order.orderNo }}</p></div><strong>¥{{ money(order.payAmount ?? order.amount) }}</strong></section>
 <section class="panel"><div class="section-heading"><h2>活动与场次</h2><RouterLink v-if="order.activityId" :to="base+'/activities/'+order.activityId+(platform?'':'/edit')">查看活动</RouterLink></div><dl><div><dt>所属主办方</dt><dd><RouterLink v-if="platform && order.organizerId" :to="base+'/organizers/'+order.organizerId">{{ order.organizerName || '机构资料不可用' }}</RouterLink><span v-else>{{ order.organizerName || '机构资料不可用' }}</span></dd></div><div><dt>活动场地</dt><dd>{{ order.venueName || '未记录' }}</dd></div><div><dt>场次时间</dt><dd>{{ date(order.sessionStartTime) }} 至 {{ date(order.sessionEndTime) }}</dd></div><div><dt>票种与数量</dt><dd>{{ order.ticketTypeName || '—' }} × {{ order.quantity }}</dd></div></dl></section>
 <section class="panel"><h2>购票实名信息</h2><p class="hint">以下为下单时保存的实名信息。</p><dl><div><dt>实名姓名</dt><dd>{{ order.realName || '历史订单未记录' }}</dd></div><div><dt>证件号码</dt><dd>{{ order.idCardMasked || '历史订单未记录' }}</dd></div><div v-if="platform"><dt>购票账号 ID</dt><dd>{{ order.userId || '—' }}</dd></div></dl></section>
 <section class="panel"><h2>金额与订单时间</h2><dl><div><dt>订单原价</dt><dd>¥{{ money(order.amount) }}</dd></div><div><dt>优惠金额</dt><dd>{{ order.discountAmount == null ? '未记录' : '¥'+money(order.discountAmount) }}</dd></div><div><dt>实付金额</dt><dd>¥{{ money(order.payAmount ?? order.amount) }}</dd></div><div><dt>下单时间</dt><dd>{{ date(order.createdAt) }}</dd></div><div><dt>支付时间</dt><dd>{{ date(order.paidAt) }}</dd></div><div v-if="order.status==='PENDING_PAYMENT'"><dt>支付截止时间</dt><dd>{{ date(order.expiredAt) }}</dd></div></dl></section>
 <section class="panel"><div class="section-heading"><h2>门票与核销记录</h2><RouterLink :to="{path:base+'/verification',query:{orderId:String(order.orderId || route.params.orderId)}}">查看核销记录</RouterLink></div><p v-if="!order.tickets?.length" class="hint">{{ order.status==='PENDING_PAYMENT'?'支付成功后生成电子票。':'当前订单暂无已生成门票。' }}</p><table v-else><thead><tr><th>门票编号</th><th>核销码</th><th>门票状态</th><th>核销人员</th><th>核销时间</th></tr></thead><tbody><tr v-for="ticket in order.tickets" :key="ticket.ticketId"><td>{{ ticket.ticketId }}</td><td>{{ ticket.verificationCode || '—' }}</td><td>{{ order.status==='REFUNDING'?'退款暂停核销':ticketStatus(ticket.status) }}</td><td>{{ ticket.verifierName || ticket.verifierUsername || (ticket.verifierId?'账号 ID：'+ticket.verifierId:'—') }}<small v-if="ticket.verifierName">{{ ticket.verifierUsername }}</small></td><td>{{ date(ticket.checkedInAt) }}</td></tr></tbody></table></section>
 <RefundSection :order="order" :scope="platform?'platform':'organizer'" @refresh="load" />
 </template></main>
</template>
<script setup>
import {computed,onMounted,ref} from 'vue'
import RefundSection from './RefundSection.vue'
import {useRoute} from 'vue-router'
import {platformOrderApi} from '../api/platformOrder'
import {organizerOrderApi} from '../api/organizerOrder'
const props=defineProps({platform:Boolean}),route=useRoute(),order=ref(null),loading=ref(true),error=ref('')
const base=computed(()=>props.platform?'/management/platform':'/management/organizer')
const orderStatus=v=>({PENDING_PAYMENT:'待付款',PAID:'已支付',CANCELLED:'已取消',EXPIRED:'已过期',REFUNDING:'退款审核中',REFUNDED:'已退款'}[v]||v||'未知')
const ticketStatus=v=>({UNUSED:'未核销',CHECKED_IN:'已核销',REFUNDING:'退款审核中',REFUNDED:'已退款',EXPIRED:'已过期',CANCELLED:'已作废'}[v]||v||'未知')
const money=v=>v==null?'—':Number(v).toFixed(2)
const date=v=>v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'—'
async function load(){loading.value=true;error.value='';try{order.value=await (props.platform?platformOrderApi:organizerOrderApi).detail(route.params.orderId)}catch(e){error.value=e.message||'订单详情加载失败'}finally{loading.value=false}}
onMounted(load)
</script>
<style scoped>
main{padding:34px;max-width:1400px}.title-row,.hero,.section-heading{display:flex;align-items:center;justify-content:space-between;gap:20px}.title-row{align-items:flex-start;margin-bottom:24px}.eyebrow,dt,.hint,.title-row p{color:#74829a;font-size:13px}h1{margin:8px 0;font-size:30px}.title-row p{margin:0}a{color:#245cf5;text-decoration:none;font-size:13px}.panel{padding:24px;background:#fff;border:1px solid #e0e7f1;border-radius:12px;margin-bottom:20px;overflow:auto}.panel h2{margin:0;font-size:18px}.hero h2{font-size:23px;margin:14px 0}.hero p{font-size:13px;color:#64748b}.hero>strong{font-size:28px;color:#245cf5}.status{display:inline-flex;background:#edf3ff;color:#245cf5;border-radius:16px;padding:5px 10px;font-size:12px}dl{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;margin:22px 0 0}dt{margin-bottom:8px}dd{margin:0;overflow-wrap:anywhere;font-size:14px}table{border-collapse:collapse;min-width:800px;width:100%;margin-top:18px}th,td{padding:14px 10px;text-align:left;border-bottom:1px solid #e6ebf3;font-size:13px}th{background:#f7f9fd;color:#64748b}td small{display:block;color:#74829a;margin-top:5px}.error{color:#bd3245}.error button{margin-left:12px}@media(max-width:800px){main{padding:20px 14px}dl{grid-template-columns:repeat(2,minmax(0,1fr))}.title-row{flex-wrap:wrap}}@media(max-width:480px){dl{grid-template-columns:1fr}}
</style>
