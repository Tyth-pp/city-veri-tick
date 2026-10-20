import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter, useRoute } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, expect, it } from 'vitest'
import { defineComponent, h, onMounted } from 'vue'
import OrganizerLayout from './OrganizerLayout.vue'
import PlatformAdminLayout from './PlatformAdminLayout.vue'
import UserLayout from './UserLayout.vue'
beforeEach(() => { sessionStorage.clear(); setActivePinia(createPinia()) })
for (const [name, Layout] of Object.entries({ organizer: OrganizerLayout, platform: PlatformAdminLayout, user: UserLayout })) {
  it(`${name}: clears previous record state when the path changes but preserves it on query changes`, async () => {
    let mounts = 0, reminderMounts = 0
    const Probe = defineComponent({ setup() { const id = useRoute().params.id || 'new'; onMounted(() => mounts++); return () => h('p', { class: 'record-probe' }, id) } })
    const Reminder = defineComponent({ setup() { onMounted(() => reminderMounts++); return () => h('aside') } })
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: Layout, children: [{ path: 'records/:id', component: Probe }, { path: 'new', component: Probe }] }] })
    await router.push('/records/old')
    const w = mount({ template: '<RouterView />' }, { global: { plugins: [router], stubs: { StaffAccountMenu: true, UserReminderHost: Reminder, UserTabbar: true } } })
    await flushPromises(); expect(w.get('.record-probe').text()).toBe('old')
    await router.push('/records/latest'); await flushPromises()
    expect(w.get('.record-probe').text()).toBe('latest')
    await router.push('/new'); await flushPromises()
    expect(w.get('.record-probe').text()).toBe('new'); expect(mounts).toBe(3)
    await router.push('/new?tab=details'); await flushPromises(); expect(mounts).toBe(3)
    if (name === 'user') expect(reminderMounts).toBe(1)
    w.unmount()
  })
}
