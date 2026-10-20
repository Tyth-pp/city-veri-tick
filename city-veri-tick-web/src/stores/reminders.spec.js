import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { useRemindersStore } from './reminders'
const paid = { orderId: '1', status: 'PAID', activityTitle: '演唱会', tickets: [{ ticketId: '20' }] }
beforeEach(() => { localStorage.clear(); sessionStorage.clear(); setActivePinia(createPinia()); useAuthStore().accessToken = `h.${btoa(JSON.stringify({sub:'1'}))}.s` })
describe('real user reminders', () => {
  it('announces actual refund results once without issuing tickets again after rejection', () => {
    const store=useRemindersStore()
    store.scanOrders([paid])
    store.scanOrders([{...paid,status:'REFUNDING',latestRefund:{id:'30',status:'PENDING'}}])
    expect(store.toasts).toHaveLength(0)
    const rejected={...paid,latestRefund:{id:'30',status:'REJECTED',reviewReason:'不符合规则'}}
    store.scanOrders([rejected]);store.scanOrders([rejected])
    expect(store.toasts).toHaveLength(1);expect(store.toasts[0].kind).toBe('refund');expect(store.toasts[0].message).toContain('不符合规则')
  })
  it('only announces issued tickets and deduplicates across reloads', () => {
    const store = useRemindersStore()
    store.confirmIssued({ ...paid, tickets: [] })
    expect(store.toasts).toHaveLength(0)
    store.confirmIssued(paid)
    store.confirmIssued(paid)
    expect(store.toasts).toHaveLength(1)
    expect(store.history).toHaveLength(1)
    store.dismiss(store.toasts[0].key)
    expect(store.toasts).toHaveLength(0)
    expect(store.history).toHaveLength(1)
    setActivePinia(createPinia()); useAuthStore().accessToken = `h.${btoa(JSON.stringify({sub:'1'}))}.s`
    useRemindersStore().confirmIssued(paid)
    expect(useRemindersStore().toasts).toHaveLength(0)
  })
  it('suppresses existing paid orders on initial scan but detects pending orders becoming issued', () => {
    const store = useRemindersStore()
    store.scanOrders([paid, { orderId:'2', status:'PENDING_PAYMENT' }])
    expect(store.toasts).toHaveLength(0)
    store.scanOrders([paid, { ...paid, orderId:'2' }])
    expect(store.toasts).toHaveLength(1)
  })
  it('only announces subscribed activity opening and not repeated polling', () => {
    const store = useRemindersStore()
    store.applyPreview('10', { activity:{title:'展览'}, availability:'AVAILABLE' })
    expect(store.toasts).toHaveLength(0)
    store.subscribe({ id:'10', title:'展览' })
    store.applyPreview('10', { activity:{title:'展览'}, availability:'UPCOMING' })
    expect(store.toasts).toHaveLength(0)
    store.applyPreview('10', { activity:{title:'展览'}, availability:'AVAILABLE' })
    store.applyPreview('10', { activity:{title:'展览'}, availability:'AVAILABLE' })
    expect(store.toasts).toHaveLength(1)
  })
  it('clears visible reminders on account switch and keeps sound disabled by default', () => {
    const store = useRemindersStore()
    store.confirmIssued(paid)
    useAuthStore().accessToken = `h.${btoa(JSON.stringify({sub:'2'}))}.s`
    expect(store.toasts).toHaveLength(0)
    expect(store.history).toHaveLength(0)
    expect(store.settings.issuedSound).toBe(false)
  })
})
