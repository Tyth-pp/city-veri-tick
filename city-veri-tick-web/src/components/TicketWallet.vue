<template>
  <section class="ticket-wallet" aria-label="我的票夹" :aria-busy="loading">
    <div class="wallet-title">
      <h2><AppIcon name="ticket" size="20" /> 我的票夹</h2>
      <RouterLink v-if="!loading && !error" to="/orders?filter=pending-use">
        {{ ticketCount }} 张待使用 <AppIcon name="chevron-right" size="15" />
      </RouterLink>
    </div>
    <p v-if="loading" class="wallet-state" role="status">票夹加载中...</p>
    <div v-else-if="error" class="wallet-state wallet-error" role="alert">
      <p>票夹加载失败</p><button type="button" @click="$emit('retry')">点击重试 <AppIcon name="refresh" size="14" /></button>
    </div>
    <p v-else-if="!pendingOrders.length" class="wallet-state">暂无待使用电子票，支付成功后会显示在这里</p>
    <template v-else>
      <article v-for="order in previewOrders" :key="orderIdOf(order)" class="wallet-ticket">
        <div class="wallet-ticket-main">
          <div class="wallet-cover">
            <ActivityCover :src="order.coverFileUrl || ''" :title="order.activityTitle || '活动'" />
          </div>
          <div class="wallet-info">
            <h3 :title="order.activityTitle">{{ order.activityTitle || '活动电子票' }}</h3>
            <p><AppIcon name="calendar" size="14" /><span>{{ formatSessionTime(order.sessionStartTime) }}</span></p>
            <p><AppIcon name="pin" size="14" /><span>{{ order.venueName || '场馆信息暂未提供' }}</span></p>
          </div>
        </div>
        <div class="wallet-ticket-footer">
          <span>{{ order.ticketTypeName || '电子票' }} · 1 张待使用</span>
          <RouterLink :to="ticketLink(order)">查看电子票 <AppIcon name="qr" size="15" /></RouterLink>
        </div>
      </article>
    </template>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppIcon from './AppIcon.vue'
import ActivityCover from './ActivityCover.vue'
import { orderIdOf } from '../utils/id'
import { isPendingUse, unusedTickets } from '../utils/orderTickets'

const props = defineProps({ orders: { type: Array, default: () => [] }, loading: Boolean, error: Boolean })
defineEmits(['retry'])
const pendingOrders = computed(() => props.orders.filter(isPendingUse))
const ticketCount = computed(() => pendingOrders.value.reduce((total, order) => total + unusedTickets(order).length, 0))
const previewOrders = computed(() => {
  const purchaseTimeOf = order => {
    const value = String(order.createdAt || order.paidAt || '')
    // LocalDateTime values without an offset also represent Shanghai time.
    return Date.parse(/[zZ]|[+-]\d{2}:\d{2}$/.test(value) ? value : `${value}+08:00`)
  }
  return [...pendingOrders.value].sort((a, b) => {
    const at = purchaseTimeOf(a), bt = purchaseTimeOf(b)
    if (!Number.isFinite(at)) return Number.isFinite(bt) ? 1 : 0
    if (!Number.isFinite(bt)) return -1
    return bt - at
  }).slice(0, 1)
})
function formatSessionTime(value) {
  const match = String(value || '').match(/^\d{4}-(\d{2})-(\d{2})[T ](\d{2}:\d{2})/)
  return match ? `${Number(match[1])}月${Number(match[2])}日 ${match[3]}` : '场次时间暂未提供'
}
function ticketLink(order) {
  const tickets = unusedTickets(order)
  return tickets[0]?.ticketId
    ? `/tickets/${tickets[0].ticketId}`
    : `/orders/${orderIdOf(order)}`
}
</script>

<style scoped>
.ticket-wallet{margin-top:16px;padding:16px;border-radius:15px;background:linear-gradient(135deg,#174fd7,#1552d8);color:#fff}
.wallet-title{display:flex;align-items:center;justify-content:space-between;gap:8px}
.wallet-title h2{display:flex;align-items:center;gap:7px;margin:0;font-size:16px;line-height:1.4}
.wallet-title>a{display:flex;align-items:center;gap:2px;color:#e5edff;font-size:12px;white-space:nowrap}
.wallet-ticket{margin-top:12px;padding:12px;border:1px solid rgba(255,255,255,.13);border-radius:12px;background:rgba(255,255,255,.08)}
.wallet-ticket-main{display:flex;align-items:center;gap:12px}
.wallet-cover{display:grid;place-items:center;width:68px;height:82px;flex:none;overflow:hidden;border-radius:8px;background:rgba(255,255,255,.12);color:#d8e6ff}
.wallet-cover img{width:100%;height:100%;object-fit:cover}
.wallet-info{flex:1;min-width:0}
.wallet-info h3{margin:0 0 8px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:15px;line-height:1.4}
.wallet-info p{display:flex;align-items:flex-start;gap:5px;margin:6px 0 0;color:#e0eaff;font-size:12px;line-height:1.5}
.wallet-info p .app-icon{flex:none;margin-top:2px}
.wallet-info p span{min-width:0;overflow-wrap:anywhere}
.wallet-ticket-footer{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:12px;padding-top:10px;border-top:1px solid rgba(255,255,255,.14)}
.wallet-ticket-footer>span{min-width:0;color:#e0eaff;font-size:11px;line-height:1.5;overflow-wrap:anywhere}
.wallet-ticket-footer>a{display:flex;align-items:center;justify-content:center;gap:5px;flex:none;min-height:36px;padding:0 11px;border-radius:8px;background:#fff;color:#174fd7;font-size:12px;font-weight:600;white-space:nowrap}
.wallet-state{margin:0;padding:22px 0 6px;color:#e0eaff;font-size:12px;line-height:1.6}
.wallet-error{display:flex;align-items:center;justify-content:space-between;gap:12px}.wallet-error p{margin:0}
.wallet-error button{display:flex;align-items:center;gap:5px;padding:6px 9px;border:1px solid rgba(255,255,255,.4);border-radius:7px;background:transparent;color:#fff;font:inherit;cursor:pointer}
@media(max-width:350px){.ticket-wallet{padding:12px}.wallet-ticket{padding:10px}.wallet-cover{width:58px;height:74px}.wallet-ticket-main{gap:9px}.wallet-title h2{font-size:15px}.wallet-ticket-footer>a{padding:0 8px}}
</style>
