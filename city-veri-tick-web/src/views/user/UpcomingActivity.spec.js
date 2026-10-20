import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import ActivityDetailView from './ActivityDetailView.vue'
import ActivityCard from '../../components/ActivityCard.vue'
import { activityApi } from '../../api/activity'
import { useAuthStore } from '../../stores/auth'
import { useRemindersStore } from '../../stores/reminders'
import { useFavoritesStore } from '../../stores/favorites'
const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { activityId: '1' }, fullPath: '/activities/1' }), useRouter: () => ({ push, back: vi.fn() }) }))
vi.mock('../../api/activity', () => ({ activityApi: { detail: vi.fn() } }))
const global = { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } }
const ticket = { id: '3', name: '普通票', enabled: true, availableStock: 10, price: 99, saleStartTime: '2030-01-01T10:00:00+08:00', saleEndTime: '2030-01-02T10:00:00+08:00' }
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear(); setActivePinia(createPinia()); vi.clearAllMocks()
  vi.useFakeTimers(); vi.setSystemTime(new Date('2030-01-01T09:00:00+08:00'))
  useAuthStore().accessToken = `h.${btoa(JSON.stringify({ sub: '1' }))}.s`
  activityApi.detail.mockResolvedValue({ id: '1', title: '城市音乐会', sessions: [{ id: '2', startTime: '2030-01-03T18:00:00+08:00', endTime: '2030-01-03T20:00:00+08:00', ticketTypes: [ticket] }] })
})
afterEach(() => vi.useRealTimers())
it('shows an upcoming card with opening time instead of an immediate purchase claim', () => {
  const w = mount(ActivityCard, { props: { activity: { id: '1', title: '城市音乐会', availability: 'UPCOMING', saleStartTime: ticket.saleStartTime } }, global })
  expect(w.text()).toContain('即将开售'); expect(w.text()).toContain('2030-01-01 10:00')
  expect(w.text()).not.toContain('立即购票'); w.unmount()
})
it('blocks early ticket selection, subscribes with a favorite and allows purchase only after opening', async () => {
  const w = mount(ActivityDetailView, { global }); await flushPromises()
  expect(w.get('.ticket-card').attributes('disabled')).toBeDefined()
  const remind = w.findAll('button').find(b => b.text() === '提醒我开售')
  expect(remind).toBeDefined(); await remind.trigger('click')
  expect(useFavoritesStore().has('1')).toBe(true)
  expect(useRemindersStore().subscriptions['1']).toBeDefined()
  expect(push).not.toHaveBeenCalled()
  vi.setSystemTime(new Date('2030-01-01T10:00:00+08:00')); await vi.advanceTimersByTimeAsync(1000)
  expect(w.get('.ticket-card').attributes('disabled')).toBeUndefined()
  await w.get('.ticket-card').trigger('click'); await w.get('.booking-dock .primary-button').trigger('click')
  expect(push).toHaveBeenCalledWith('/orders/confirm'); w.unmount()
})
it('disables purchase again when sales end while the detail page remains open', async () => {
  vi.setSystemTime(new Date('2030-01-01T11:00:00+08:00'))
  const w = mount(ActivityDetailView, { global }); await flushPromises()
  await w.get('.ticket-card').trigger('click')
  vi.setSystemTime(new Date('2030-01-02T10:00:01+08:00')); await vi.advanceTimersByTimeAsync(1000)
  expect(w.get('.booking-dock .primary-button').attributes('disabled')).toBeDefined()
  expect(push).not.toHaveBeenCalled(); w.unmount()
})
