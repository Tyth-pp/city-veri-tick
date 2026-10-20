import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

const PREFIX = 'venu-favorites:'
function readAccountId(token) {
  if (!token) return ''
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return String(decoded.sub || decoded.userId || '')
  } catch { return '' }
}
function read(key) {
  if (!key) return []
  try { const value = JSON.parse(localStorage.getItem(PREFIX + key) || '[]'); return Array.isArray(value) ? value : [] } catch { return [] }
}

export const useFavoritesStore = defineStore('favorites', () => {
  const auth = useAuthStore()
  const accountId = computed(() => readAccountId(auth.accessToken))
  const storedItems = ref([])
  watch(accountId, id => { storedItems.value = read(id) }, { immediate: true, flush: 'sync' })
  const items = computed(() => storedItems.value)
  function snapshotOf(activity) {
    const { coverFileUrl, sessions, ...snapshot } = activity
    const session = sessions?.[0]
    return { ...snapshot, startTime: session?.startTime || snapshot.startTime, endTime: session?.endTime || snapshot.endTime, minPrice: snapshot.minPrice ?? (session?.ticketTypes?.length ? Math.min(...session.ticketTypes.map(ticket => Number(ticket.price))) : null), favoritedAt: Date.now() }
  }
  function persist(next) {
    if (!accountId.value) throw new Error('请先登录后收藏活动')
    localStorage.setItem(PREFIX + accountId.value, JSON.stringify(next))
    storedItems.value = next
  }
  function has(activityId) { return items.value.some(item => String(item.id) === String(activityId)) }
  function toggle(activity) {
    if (!accountId.value) throw new Error('请先登录后收藏活动')
    persist(has(activity.id) ? items.value.filter(item => String(item.id) !== String(activity.id)) : [snapshotOf(activity), ...items.value])
  }
  function remove(activityId) { persist(items.value.filter(item => String(item.id) !== String(activityId))) }
  function clear() { persist([]) }
  return { accountId, items, has, toggle, remove, clear }
})
