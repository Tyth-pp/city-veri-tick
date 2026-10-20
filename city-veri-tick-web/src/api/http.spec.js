import { afterEach, describe, expect, it } from 'vitest'
import { http, isCurrentSessionUnauthorized } from './http'

const TOKEN_KEY = 'city-veri-tick-token'
const ROLE_KEY = 'city-veri-tick-role'

afterEach(() => sessionStorage.clear())

describe('unauthorized session handling', () => {
  it('ignores an old request after a new login', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'new-token')
    sessionStorage.setItem(ROLE_KEY, 'USER')
    const error = { response: { status: 401 }, config: { headers: { Authorization: 'Bearer old-token' } } }
    const rejected = http.interceptors.response.handlers[0].rejected

    await expect(rejected(error)).rejects.toThrow()
    expect(sessionStorage.getItem(TOKEN_KEY)).toBe('new-token')
    expect(sessionStorage.getItem(ROLE_KEY)).toBe('USER')
  })

  it('does not expire a session for a login request without a bearer token', () => {
    expect(isCurrentSessionUnauthorized({ response: { status: 401 }, config: { headers: {} } }, 'new-token')).toBe(false)
    expect(isCurrentSessionUnauthorized({ response: { status: 401 }, config: { headers: {} } }, null)).toBe(false)
  })

  it('detects a rejected request from the active session', () => {
    const error = { response: { status: 401 }, config: { headers: { Authorization: 'Bearer new-token' } } }
    expect(isCurrentSessionUnauthorized(error, 'new-token')).toBe(true)
  })
  it('keeps an explicitly captured logout token instead of replacing it with a newer session', () => {
    sessionStorage.setItem(TOKEN_KEY,'new-token')
    const request=http.interceptors.request.handlers[0].fulfilled
    expect(request({url:'/controller/auth/logout',headers:{Authorization:'Bearer old-token'}}).headers.Authorization).toBe('Bearer old-token')
    expect(request({url:'/controller/auth/login',headers:{}}).headers.Authorization).toBeUndefined()
  })
})
