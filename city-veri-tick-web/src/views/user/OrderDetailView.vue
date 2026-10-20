<template>
  <main class="page order-detail-page">
    <header class="topbar"><button class="icon-button" aria-label="返回" @click="router.back()"><AppIcon name="arrow-left" /></button><h1>订单详情</h1><button class="icon-button" aria-label="刷新订单状态" :disabled="loading" @click="refresh"><AppIcon name="refresh" /></button></header>
    <p v-if="loading" class="loading">订单加载中…</p>
    <section v-else-if="error" class="container card state-card" role="alert"><AppIcon name="info" size="28" /><h2>订单暂时无法打开</h2><p>{{ error }}</p><button class="secondary-button" @click="refresh">重新加载</button></section>
    <div v-else-if="order" class="container order-detail">
      <section class="status-banner" :class="{ completed: isCompleted, subdued: !isPaid && !isPending }"><span class="status-icon"><AppIcon :name="isPaid ? 'check-circle' : isPending ? 'clock' : 'info'" size="24" /></span><div><h2>{{ statusTitle }}</h2><p>{{ statusMessage }}</p></div></section>
      <section class="card detail-event"><div class="event-overview"><ActivityCover class="event-cover" :src="order.coverFileUrl || ''" :title="order.activityTitle || ''" /><div class="event-copy"><h2>{{ order.activityTitle || '活动名称暂未提供' }}</h2><span class="ticket-type">{{ order.ticketTypeName || '票种暂未提供' }}</span><p><AppIcon name="calendar" size="15" /><span>{{ sessionRange(order) }}</span></p><p><AppIcon name="pin" size="15" /><span>{{ order.venueName || '场地暂未提供' }}</span></p></div></div><div class="amount-line"><span>共 {{ order.quantity ?? '—' }} 张 · {{ isPaid ? '实付金额' : '订单金额' }}</span><strong>¥{{ money(order.payAmount ?? order.amount) }}</strong></div></section>
      <section v-if="order.tickets?.length" class="ticket-section"><div class="section-heading"><h2>我的电子票</h2><span>共 {{ order.tickets.length }} 张</span></div><RouterLink v-for="ticket in order.tickets" :key="ticket.ticketId" class="card ticket-preview" :to="ticketLink(ticket)"><div class="ticket-summary"><span class="ticket-icon"><AppIcon name="ticket" size="21" /></span><div><b>{{ order.ticketTypeName || '电子票' }}</b><p>实名观众 {{ order.realName || '实名信息暂未提供' }}</p></div><span class="ticket-status" :class="ticketState(ticket, order).tone">{{ ticketState(ticket, order).label }}</span></div><div class="ticket-entry"><span>查看入场凭证</span><AppIcon name="chevron-right" size="16" /></div></RouterLink></section>
      <section class="card info-block"><h2>订单信息</h2><dl><div><dt>订单编号</dt><dd class="order-number">{{ order.orderNo || orderIdOf(order) }}</dd></div><div><dt>下单时间</dt><dd>{{ order.createdAt ? formatTicketTime(order.createdAt) : '—' }}</dd></div><div v-if="order.paidAt"><dt>支付时间</dt><dd>{{ formatTicketTime(order.paidAt) }}</dd></div><div><dt>支付方式</dt><dd>微信支付</dd></div><div><dt>实名姓名</dt><dd>{{ order.realName || '实名信息暂未提供' }}</dd></div><div><dt>身份证号</dt><dd>{{ order.idCardMasked || '—' }}</dd></div></dl></section>
      <section v-if="Number(order.discountAmount)>0" class="card info-block order-discount"><h2>费用明细</h2><dl><div><dt>票面总价</dt><dd>¥{{money(order.amount)}}</dd></div><div><dt>优惠券</dt><dd>{{order.couponName || '优惠券'}}</dd></div><div class="discount-value"><dt>实际抵扣</dt><dd>-¥{{money(order.discountAmount)}}</dd></div><div><dt>订单实付</dt><dd>¥{{money(order.payAmount)}}</dd></div></dl></section>
      <RefundSection :order="order" scope="user" @refresh="refresh" />
      <section class="rule-box"><AppIcon name="shield" size="18" /><p>退改及入场规则以活动公布的须知为准。</p></section><p v-if="message" class="inline-error" role="alert">{{ message }}</p>
      <div class="order-actions"><div><button v-if="isPending" class="ghost-button" :disabled="cancelling" @click="cancel">{{ cancelling ? '取消中…' : '取消订单' }}</button><RouterLink v-else-if="order.activityId" class="ghost-button" :to="`/activities/${order.activityId}`">查看活动</RouterLink><button v-if="isPending" class="primary-button" data-action="pay" :disabled="cancelling" @click="pay">立即支付 ¥{{ money(order.payAmount ?? order.amount) }}</button><RouterLink v-else-if="order.tickets?.length" class="primary-button" :to="ticketLink(preferredTicket)">查看电子票</RouterLink></div></div>
    </div>
  </main>
</template>
<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '../../api/order'
import AppIcon from '../../components/AppIcon.vue'
import RefundSection from '../../components/RefundSection.vue'
import ActivityCover from '../../components/ActivityCover.vue'
import { orderIdOf } from '../../utils/id'
import { formatTicketTime, sessionRange, ticketState, orderLabel, money } from '../../utils/ticketPresentation'
const route = useRoute(), router = useRouter(), order = ref(null), loading = ref(true), error = ref(''), message = ref(''), cancelling = ref(false)
let request = 0, poll
const isPaid = computed(() => ['PAID','COMPLETED'].includes(order.value?.status))
const isPending = computed(() => order.value?.status === 'PENDING_PAYMENT')
const checkedCount = computed(() => order.value?.tickets?.filter(ticket => ticket.status === 'CHECKED_IN').length || 0)
const completeTicketRecords = computed(() => Number(order.value?.quantity) > 0 && order.value?.tickets?.length === Number(order.value.quantity))
// Payment stays PAID after admission; completion comes from the actual ticket records.
const isCompleted = computed(() => order.value?.status === 'COMPLETED' || (order.value?.status === 'PAID' && completeTicketRecords.value && checkedCount.value === Number(order.value.quantity)))
const isPartiallyUsed = computed(() => order.value?.status === 'PAID' && completeTicketRecords.value && checkedCount.value > 0 && order.value.tickets.some(ticket => ticket.status === 'UNUSED'))
const statusTitle = computed(() => isCompleted.value ? '已完成' : isPartiallyUsed.value ? '部分已使用' : orderLabel(order.value?.status))
const preferredTicket = computed(() => order.value?.tickets?.find(ticket => ticketState(ticket, order.value).active) || order.value?.tickets?.[0])
const statusMessage = computed(() => {
  if (isCompleted.value) return '全部电子票已核销，本次订单已完成'
  if (isPartiallyUsed.value) return `已核销 ${checkedCount.value} / ${order.value.quantity} 张，其余票券状态请查看下方电子票`
  if (isPending.value) return '请在支付时限内完成支付，逾期订单将自动取消'
  if (isPaid.value) return order.value?.tickets?.length ? '电子票已生成，请凭票及本人身份证入场' : '支付已完成，出票结果以实际票券记录为准'
  if (order.value?.status === 'REFUNDING') return '退票申请已提交，等待主办方审核，电子票暂停使用'
  if (order.value?.status === 'REFUNDED') return '整单模拟退款已完成，电子票已作废'
  return '订单及票券状态以当前记录为准'
})
function ticketLink(ticket) { return `/tickets/${ticket.ticketId}?orderId=${orderIdOf(order.value)}` }
async function refresh(silent = false) {
  silent = silent === true
  const current = ++request; if (!silent) { loading.value = true; error.value = ''; message.value = ''; order.value = null }
  try { const value = await orderApi.detail(String(route.params.orderId)); if (current === request) { if (!value) throw new Error('订单不存在'); order.value = value } }
  catch (e) { if (current === request && !silent) error.value = e.message || '网络请求失败' }
  finally { if (current === request) loading.value = false }
}
function pay() { router.push(`/orders/pay/${route.params.orderId}`) }
async function cancel() { if (cancelling.value) return; cancelling.value = true; const id = String(route.params.orderId); try { await orderApi.cancel(id); if (id === String(route.params.orderId)) await refresh() } catch (e) { message.value = e.message || '取消订单失败' } finally { cancelling.value = false } }
function onVisible() { if (document.visibilityState === 'visible') refresh() }
watch(() => route.params.orderId, refresh, { immediate: true })
onMounted(() => { document.addEventListener('visibilitychange', onVisible); poll = setInterval(() => { if (document.visibilityState !== 'hidden') refresh(true) }, 30000) })
onUnmounted(() => { request++; clearInterval(poll); document.removeEventListener('visibilitychange', onVisible) })
</script>
<style scoped>
.order-detail-page{background:#f5f7fb;padding-bottom:calc(146px + env(safe-area-inset-bottom))}.header-spacer{width:44px}.order-detail{padding-top:16px}.status-banner{display:flex;align-items:center;gap:13px;padding:18px 15px;border-radius:17px;background:#eaf3ff;margin-bottom:16px}.status-icon{display:grid;place-items:center;width:40px;height:40px;flex:none;border-radius:12px;background:#fff;color:#148a67}.status-banner h2{font-size:22px;margin:0 0 6px}.status-banner p{margin:0;color:#72839d;font-size:11px;line-height:1.6}.status-banner.subdued{background:#edf0f5}.subdued .status-icon{color:#7b899f}.detail-event,.info-block{padding:17px;border-radius:17px;box-shadow:0 2px 8px #1c3b6905}.event-overview{display:flex;align-items:flex-start;gap:13px}.event-cover{width:78px;height:110px;flex:none;border-radius:9px}.event-copy{flex:1;min-width:0}.event-copy h2{margin:0 0 9px;font-size:18px;line-height:1.45;overflow-wrap:anywhere}.ticket-type{display:inline-block;max-width:100%;padding:5px 9px;color:#2161e8;background:#eef3ff;font-size:11px;border-radius:7px;overflow-wrap:anywhere}.event-copy p{display:flex;align-items:flex-start;gap:5px;margin:10px 0 0;color:#74839b;font-size:11px;line-height:1.6}.event-copy .app-icon{flex:none;margin-top:2px}.amount-line{border-top:1px solid #e7edf5;margin-top:17px;padding-top:14px;display:flex;align-items:center;justify-content:space-between;gap:12px}.amount-line>span{color:#74839b;font-size:11px}.amount-line strong{font-size:25px;color:#2161e8;white-space:nowrap}.ticket-section{margin-top:20px}.section-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.section-heading h2,.info-block h2{margin:0;font-size:16px}.section-heading>span{font-size:11px;color:#74839b}.ticket-preview{display:block;padding:15px;margin-bottom:10px;border-radius:16px;box-shadow:0 2px 8px #1c3b6905}.ticket-summary{display:flex;align-items:center;gap:11px}.ticket-icon{display:grid;place-items:center;flex:none;width:39px;height:39px;border-radius:11px;background:#edf3ff;color:#2161e8}.ticket-summary>div{min-width:0;flex:1}.ticket-summary b{font-size:13px;overflow-wrap:anywhere}.ticket-summary p{margin:5px 0 0;color:#74839b;font-size:11px;line-height:1.5}.ticket-status{flex:none;border-radius:20px;padding:5px 9px;font-size:10px;background:#edf0f5;color:#74839b}.ticket-status.available{background:#e4f7ef;color:#148a67}.ticket-entry{display:flex;align-items:center;justify-content:space-between;border-top:1px solid #e7edf5;margin-top:13px;padding-top:10px;font-size:11px;color:#2161e8}.info-block{margin-top:16px}.info-block dl{margin:12px 0 0}.info-block dl>div{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:9px 0;font-size:12px;line-height:1.5}.info-block dt{color:#74839b;flex:none}.info-block dd{margin:0;text-align:right;overflow-wrap:anywhere;min-width:0}.order-number{font-size:11px}.rule-box{display:flex;align-items:center;gap:9px;padding:13px;background:#ecf2fc;border-radius:12px;margin:16px 0 0;color:#74839b}.rule-box .app-icon{flex:none}.rule-box p{margin:0;font-size:11px;line-height:1.6}.order-actions{position:fixed;bottom:64px;left:0;right:0;padding:10px 16px;background:#fff;border-top:1px solid #e7edf5;z-index:19}.order-actions>div{display:flex;gap:11px;max-width:780px;margin:auto}.order-actions .ghost-button{flex:1;color:#2161e8}.order-actions .primary-button{flex:2}.order-actions a,.order-actions button{display:flex;align-items:center;justify-content:center;min-width:0;font-size:13px;min-height:44px;padding:0 10px;text-align:center}.inline-error{color:#dc2626;font-size:12px}.state-card{text-align:center;padding:32px 20px;margin-top:18px}.state-card>.app-icon{color:#74839b}.state-card h2{font-size:18px}.state-card p{font-size:13px;color:#74839b}
.status-banner.completed{background:#e8f6f0}.status-banner.completed h2{color:#137b5c}.status-banner.completed p{color:#527c6d}
@media(max-width:350px){.event-cover{width:64px;height:94px}.event-overview{gap:10px}.detail-event{padding:13px}.ticket-summary{gap:8px}.ticket-summary b{font-size:12px}}
</style>

<style scoped>.discount-value dd{color:#e57d28}.order-discount dd{max-width:66%}</style>
