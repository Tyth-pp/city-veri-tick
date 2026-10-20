import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { router } from './index'

beforeEach(() => { sessionStorage.clear(); setActivePinia(createPinia()) })

describe('guest route permissions', () => {
  it('hides the navigation bar when displaying an entry ticket', () => {
    expect(router.resolve('/tickets/123').meta.hideTabbar).toBe(true)
  })
  it('allows browsing and guest profile without authentication', async () => {
    for (const path of ['/home', '/activities/123', '/profile']) {
      await router.push(path)
      expect(router.currentRoute.value.path).toBe(path)
    }
  })

  it('requires login for purchases, tickets, orders, and management', async () => {
    for (const path of ['/orders/confirm', '/orders', '/tickets/123', '/management/platform', '/management/organizer', '/verify']) {
      await router.push(path)
      expect(router.currentRoute.value.path).toBe('/login')
      expect(router.currentRoute.value.query.redirect).toBe(path)
    }
  })

  it('redirects the legacy organizer verification URL to the role-specific module', async () => {
    sessionStorage.setItem('city-veri-tick-token', 'organizer-token')
    sessionStorage.setItem('city-veri-tick-role', 'ORGANIZER_ADMIN')
    await router.push('/management/verification')
    expect(router.currentRoute.value.path).toBe('/management/organizer/verification')
  })
})
