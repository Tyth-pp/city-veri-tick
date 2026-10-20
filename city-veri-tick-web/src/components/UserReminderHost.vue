<template>
  <aside class="reminder-host" aria-live="polite" aria-label="活动、出票与退款提醒">
    <TransitionGroup name="reminder">
      <section v-for="item in store.toasts" :key="item.key" class="reminder-toast" role="status">
        <span class="reminder-icon"><AppIcon :name="item.kind === 'issued' ? 'ticket' : 'bell'" size="20" /></span>
        <div class="reminder-copy"><b>{{ item.title }}</b><p>{{ item.message }}</p><RouterLink :to="item.to" @click="store.dismiss(item.key)">{{ item.action }} <AppIcon name="chevron-right" size="13" /></RouterLink><div class="reminder-progress" role="progressbar" aria-label="提醒自动关闭倒计时" :aria-valuenow="remaining(item)" aria-valuemin="0" aria-valuemax="8000"><i :style="{ width: `${remaining(item) / 80}%` }"></i></div></div>
        <button class="reminder-close" type="button" aria-label="关闭提醒" @click="store.dismiss(item.key)"><AppIcon name="plus" size="16" /></button>
      </section>
    </TransitionGroup>
  </aside>
</template>
<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { useRemindersStore } from '../stores/reminders'
import { useFavoritesStore } from '../stores/favorites'
import { orderApi } from '../api/order'
import { activityApi } from '../api/activity'
import { unlockReminderSound } from '../utils/reminderSound'
const store = useRemindersStore(), favorites = useFavoritesStore(), now = ref(Date.now())
let countdown, poll, busy = false, generation = 0
function remaining(item) { return Math.max(0, Math.min(8000, item.expiresAt - now.value)) }
async function check() {
  if (!store.accountId || busy || document.visibilityState === 'hidden') return
  const account = store.accountId, request = generation
  busy = true
  try {
    const results = await Promise.allSettled([
      orderApi.list(),
      ...Object.values(store.subscriptions).filter(subscription => !subscription.delivered).map(async subscription => {
        if (!favorites.has(subscription.id)) return { id: subscription.id, removed: true }
        return { id: subscription.id, preview: await activityApi.preview(subscription.id) }
      })
    ])
    if (request !== generation || account !== store.accountId) return
    const orders = results[0]
    if (orders.status === 'fulfilled') { const value = orders.value; store.scanOrders(Array.isArray(value) ? value : value?.records || value?.content || []) }
    for (const result of results.slice(1)) if (result.status === 'fulfilled') { const value = result.value; if (value.removed) store.unsubscribe(value.id); else store.applyPreview(value.id, value.preview) }
  } finally { busy = false }
}
watch(() => store.accountId, () => { generation++; check() }, { immediate: true })
watch(() => favorites.items.map(item => String(item.id)), (ids, oldIds) => { for (const id of oldIds || []) if (!ids.includes(id)) store.unsubscribe(id) })
function enableAudio() { if (store.settings.issuedSound || store.settings.openingSound) unlockReminderSound() }
onMounted(() => {
  countdown = setInterval(() => { now.value = Date.now(); for (const item of store.toasts) if (remaining(item) === 0) store.dismiss(item.key) }, 100)
  poll = setInterval(check, 30000)
  document.addEventListener('visibilitychange', check)
  document.addEventListener('pointerdown', enableAudio)
})
onUnmounted(() => { generation++; clearInterval(countdown); clearInterval(poll); document.removeEventListener('visibilitychange', check); document.removeEventListener('pointerdown', enableAudio) })
</script>
<style scoped>
.reminder-host{position:fixed;top:calc(12px + env(safe-area-inset-top));right:max(12px,calc((100vw - 780px)/2 + 12px));width:min(350px,calc(100vw - 24px));z-index:1000;display:grid;gap:10px;pointer-events:none}
.reminder-toast{position:relative;display:flex;align-items:flex-start;gap:11px;padding:15px 14px 12px;border:1px solid #d8e4ff;border-radius:15px;background:rgba(255,255,255,.98);box-shadow:0 10px 32px rgba(25,60,120,.18);pointer-events:auto;overflow:hidden;color:#13223d}
.reminder-icon{display:grid;place-items:center;width:36px;height:36px;flex:none;background:#edf3ff;border-radius:11px;color:#2161e8}.reminder-copy{flex:1;min-width:0;padding-right:16px}.reminder-copy b{display:block;font-size:14px;line-height:1.5}.reminder-copy p{margin:5px 0 7px;font-size:12px;line-height:1.5;color:#697b96;overflow-wrap:anywhere}.reminder-copy a{display:inline-flex;align-items:center;gap:3px;font-size:12px;font-weight:600;color:#2161e8}.reminder-close{position:absolute;right:7px;top:7px;display:grid;place-items:center;width:27px;height:27px;border:0;border-radius:8px;background:transparent;color:#7c8ca6}.reminder-close:hover{background:#edf3ff}.reminder-close .app-icon{transform:rotate(45deg)}.reminder-progress{height:3px;margin-top:12px;border-radius:6px;background:#e9effa;overflow:hidden}.reminder-progress i{display:block;height:100%;background:#3471ef;border-radius:inherit;transition:width .1s linear}.reminder-enter-active,.reminder-leave-active{transition:opacity .2s,transform .2s}.reminder-enter-from,.reminder-leave-to{opacity:0;transform:translateX(20px)}@media(prefers-reduced-motion:reduce){.reminder-enter-active,.reminder-leave-active,.reminder-progress i{transition:none}}
</style>
