import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../stores/auth'
import { useRemindersStore } from '../stores/reminders'
import UserReminderHost from './UserReminderHost.vue'
vi.mock('../api/order', () => ({ orderApi:{list:vi.fn().mockResolvedValue([])} }))
vi.mock('../api/activity', () => ({ activityApi:{preview:vi.fn()} }))
afterEach(() => vi.useRealTimers())
describe('global reminder panel', () => {
  it('shows a progress bar, expires automatically and retains history', async () => {
    vi.useFakeTimers(); localStorage.clear(); setActivePinia(createPinia()); useAuthStore().accessToken = `h.${btoa(JSON.stringify({sub:'1'}))}.s`
    const store = useRemindersStore()
    const wrapper = mount(UserReminderHost, { global:{stubs:{RouterLink:{template:'<a><slot /></a>'}}} })
    store.confirmIssued({ orderId:'1',status:'PAID', tickets:[{ticketId:'2'}] })
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('电子票已生成')
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(true)
    await vi.advanceTimersByTimeAsync(4000)
    expect(Number(wrapper.find('[role="progressbar"]').attributes('aria-valuenow'))).toBe(4000)
    await vi.advanceTimersByTimeAsync(4100)
    expect(wrapper.find('.reminder-toast').exists()).toBe(false)
    expect(store.history).toHaveLength(1)
    wrapper.unmount()
  })
  it('lets the user close the reminder immediately without deleting its history', async () => {
    localStorage.clear(); setActivePinia(createPinia()); useAuthStore().accessToken = `h.${btoa(JSON.stringify({sub:'1'}))}.s`
    const store = useRemindersStore()
    const wrapper = mount(UserReminderHost, { global:{stubs:{RouterLink:{template:'<a><slot /></a>'}}} })
    store.confirmIssued({ orderId:'3',status:'PAID', tickets:[{ticketId:'4'}] })
    await wrapper.vm.$nextTick()
    await wrapper.get('[aria-label="关闭提醒"]').trigger('click')
    expect(wrapper.find('.reminder-toast').exists()).toBe(false)
    expect(store.history).toHaveLength(1)
    wrapper.unmount()
  })
})
