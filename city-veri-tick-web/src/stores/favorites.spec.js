import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { useFavoritesStore } from './favorites'
vi.mock('../api/auth', () => ({ authApi: { logout: vi.fn().mockResolvedValue() } }))

function token(id) { return `header.${btoa(JSON.stringify({ sub: id }))}.signature` }
beforeEach(() => { localStorage.clear(); sessionStorage.clear(); setActivePinia(createPinia()) })
describe('account activity favorites', () => {
  it('persists favorites and isolates accounts including after a new login token', () => {
    const auth = useAuthStore(), favorites = useFavoritesStore()
    auth.accessToken = token('1')
    favorites.toggle({ id: '100', title: '演唱会' })
    expect(favorites.has('100')).toBe(true)
    auth.accessToken = token('2')
    expect(favorites.items).toEqual([])
    auth.accessToken = token('1')
    setActivePinia(createPinia())
    useAuthStore().accessToken = token('1')
    expect(useFavoritesStore().has('100')).toBe(true)
    useFavoritesStore().remove('100')
    expect(useFavoritesStore().items).toEqual([])
  })
  it('does not save guest favorites or expose another account after logout', async () => {
    const auth = useAuthStore(), favorites = useFavoritesStore()
    expect(() => favorites.toggle({ id: '100' })).toThrow()
    auth.accessToken = token('1')
    favorites.toggle({ id: '100' })
    await auth.logout()
    expect(favorites.items).toEqual([])
  })
  it('does not persist temporary cover links in new favorites', () => {
    useAuthStore().accessToken = token('1')
    useFavoritesStore().toggle({ id: '100', coverFileUrl: 'https://oss.test/cover?Expires=1', coverFileId: '20' })
    expect(JSON.parse(localStorage.getItem('venu-favorites:1'))[0].coverFileUrl).toBeUndefined()
    expect(useFavoritesStore().items[0].coverFileId).toBe('20')
  })
  it('recovers from malformed stored data without crashing', () => {
    useAuthStore().accessToken = token('1')
    localStorage.setItem('venu-favorites:1', 'invalid-json')
    expect(useFavoritesStore().items).toEqual([])
  })
})
