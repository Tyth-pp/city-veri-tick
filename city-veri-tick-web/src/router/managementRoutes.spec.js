import { beforeEach, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { router } from './index'
import { useAuthStore } from '../stores/auth'
vi.mock('../api/auth', () => ({ authApi: { session: vi.fn(async () => ({ mustChangePassword: false })) } }))
beforeEach(() => { sessionStorage.clear(); setActivePinia(createPinia()) })
function login(role) { const auth = useAuthStore(); auth.accessToken = 'test-token'; auth.role = role }
it('canonicalizes every legacy organizer module without loading the old layout', async () => {
  login('ORGANIZER_ADMIN')
  for (const suffix of ['', '/activities', '/activities/new', '/activities/12/edit', '/ticket-inventory', '/orders', '/orders/42', '/verification']) {
    await router.push(`/management${suffix}?source=bookmark#content`)
    expect(router.currentRoute.value.path).toBe(`/management/organizer${suffix}`)
    expect(router.currentRoute.value.query.source).toBe('bookmark')
    expect(router.currentRoute.value.hash).toBe('#content')
    expect(router.currentRoute.value.matched[0].components.default.__file).toContain('OrganizerLayout.vue')
  }
})
it('routes platform bookmarks into platform modules rather than organizer business pages', async () => {
  login('PLATFORM_ADMIN')
  for (const [from, to] of [['/orders/42', '/orders/42'], ['/activities/12/edit', '/activities/12'], ['/activities/new', '/activities'], ['/ticket-inventory', '/activities'], ['/verification', '/verification']]) {
    await router.push('/management' + from)
    expect(router.currentRoute.value.path).toBe('/management/platform' + to)
    expect(router.currentRoute.value.matched[0].components.default.__file).toContain('PlatformAdminLayout.vue')
  }
})
it('requires authentication when a legacy bookmark is opened', async () => {
  await router.push('/management/activities/new')
  expect(router.currentRoute.value.path).toBe('/login')
  expect(router.currentRoute.value.query.redirect).toBe('/management/organizer/activities/new')
})
