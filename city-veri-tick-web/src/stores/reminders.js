import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useFavoritesStore } from './favorites'
import { orderIdOf } from '../utils/id'
import { playReminderSound } from '../utils/reminderSound'

const empty = () => ({ settings: { issuedSound: false, openingSound: false }, history: [], seen: [], subscriptions: {}, orders: {}, refunds: {}, initialized: false })
export const useRemindersStore = defineStore('reminders', () => {
  const favorites = useFavoritesStore()
  const accountId = computed(() => favorites.accountId)
  const data = ref(empty()), toasts = ref([])
  function persist() { if (accountId.value) { try { localStorage.setItem(`venu-reminders:${accountId.value}`, JSON.stringify(data.value)) } catch { /* Keep in-memory reminders; payment and ticket issuance must not depend on browser storage. */ } } }
  watch(accountId, id => {
    toasts.value = []; data.value = empty()
    if (!id) return
    try {
      const saved = JSON.parse(localStorage.getItem(`venu-reminders:${id}`) || 'null')
      if (saved && Array.isArray(saved.history) && Array.isArray(saved.seen)) data.value = { ...empty(), ...saved, settings: { ...empty().settings, ...saved.settings } }
    } catch { /* Ignore invalid local records; real API results remain the source of reminders. */ }
  }, { immediate: true, flush: 'sync' })
  const settings = computed(() => data.value.settings), history = computed(() => data.value.history)
  const subscriptions = computed(() => data.value.subscriptions)
  function setSound(kind, enabled) { if (!['issuedSound', 'openingSound'].includes(kind)) return; data.value.settings[kind] = Boolean(enabled); persist() }
  function publish(notification) {
    if (!accountId.value || data.value.seen.includes(notification.key)) return
    const item = { ...notification, createdAt: Date.now() }
    data.value.seen.push(item.key)
    data.value.history = [item, ...data.value.history].slice(0, 100)
    toasts.value = [...toasts.value, { ...item, expiresAt: Date.now() + 8000 }].slice(-3)
    persist()
    if ((item.kind === 'issued' && settings.value.issuedSound) || (item.kind === 'opening' && settings.value.openingSound)) playReminderSound()
  }
  function dismiss(key) { toasts.value = toasts.value.filter(item => item.key !== key) }
  function confirmIssued(order) {
    const id = orderIdOf(order)
    if (!id || !['PAID', 'COMPLETED'].includes(order?.status) || !Array.isArray(order?.tickets) || !order.tickets.length || !order.tickets.every(ticket => ticket.ticketId || ticket.id)) return
    publish({ key: `issued:${id}`, kind: 'issued', title: '电子票已生成', message: `${order.activityTitle || '活动'} · ${order.tickets.length} 张电子票已放入票夹`, to: `/orders/${id}`, action: '查看电子票' })
  }
  function scanOrders(orders) {
    if (!Array.isArray(orders)) return
    for (const order of orders) {
      const id = orderIdOf(order)
      if (!id) continue
      const previous = data.value.orders[id]
      if (data.value.initialized && (!previous || previous === 'PENDING_PAYMENT')) confirmIssued(order)
      const refund=order.latestRefund
      if(refund?.id){
        const signature=`${refund.id}:${refund.status}`
        if(data.value.initialized && data.value.refunds[id]!==signature && ['REFUNDED','REJECTED'].includes(refund.status)){
          publish({key:`refund:${signature}`,kind:'refund',title:refund.status==='REFUNDED'?'模拟退款已完成':'退票申请未通过',message:refund.status==='REFUNDED'?`${order.activityTitle || '活动'} · 全部电子票已作废，请查看退款记录`:refund.reviewReason || '请查看订单中的审核结果',to:`/orders/${id}`,action:'查看售后进度'})
        }
        data.value.refunds[id]=signature
      }
      data.value.orders[id] = ['PAID', 'COMPLETED'].includes(order.status) && order.tickets?.length ? 'ISSUED' : order.status
    }
    data.value.initialized = true; persist()
  }
  function subscribe(activity) {
    if (!accountId.value || !activity?.id) return
    data.value.subscriptions[String(activity.id)] = { id: String(activity.id), title: activity.title || '活动', createdAt: Date.now(), delivered: false }
    persist()
  }
  function unsubscribe(id) { delete data.value.subscriptions[String(id)]; persist() }
  function applyPreview(id, preview) {
    const subscription = subscriptions.value[String(id)]
    if (!subscription || subscription.delivered || preview?.availability !== 'AVAILABLE') return
    publish({ key: `opening:${id}:${subscription.createdAt}`, kind: 'opening', title: '你关注的活动开售了', message: preview.activity?.title || subscription.title, to: `/activities/${id}`, action: '查看活动' })
    subscription.delivered = true; persist()
  }
  return { accountId, settings, history, subscriptions, toasts, setSound, dismiss, confirmIssued, scanOrders, subscribe, unsubscribe, applyPreview }
})
