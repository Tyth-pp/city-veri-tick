import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { authApi } from '../api/auth'

vi.mock('../api/auth', () => ({ authApi: { login: vi.fn(), loginBySms: vi.fn(), logout: vi.fn().mockResolvedValue() } }))

beforeEach(() => { sessionStorage.clear(); setActivePinia(createPinia()); vi.clearAllMocks() })

describe('guest identity', () => {
  it('retains its nickname across reloads without granting a login token', () => {
    const auth = useAuthStore()
    auth.enterGuest()
    const name = auth.guestName
    expect(name).toMatch(/^.+的.+$/)
    auth.enterGuest()
    expect(auth.guestName).toBe(name)
    setActivePinia(createPinia())
    const reloaded = useAuthStore()
    expect(reloaded.guestName).toBe(name)
    expect(reloaded.isGuest).toBe(true)
    expect(reloaded.isLoggedIn).toBe(false)
    expect(reloaded.accessToken).toBe('')
    expect(reloaded.role).toBe('')
  })

  it('replaces guest identity only after successful registration or login', async () => {
    const auth = useAuthStore()
    auth.enterGuest()
    authApi.loginBySms.mockRejectedValueOnce(new Error('wrong code'))
    await expect(auth.loginBySms({})).rejects.toThrow('wrong code')
    expect(auth.guestName).not.toBe('')
    authApi.loginBySms.mockResolvedValueOnce({ accessToken: 'real-token', role: 'USER' })
    await auth.loginBySms({})
    expect(auth.isGuest).toBe(false)
    expect(auth.guestName).toBe('')
    expect(auth.accessToken).toBe('real-token')
    await auth.logout()
    expect(auth.isGuest).toBe(true)
    expect(auth.guestName).toBe('')
  })
})
