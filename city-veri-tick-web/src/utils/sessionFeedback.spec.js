import { afterEach, expect, it, vi } from 'vitest'
import { showSessionFeedback, sessionFeedback, setSessionClearer, resetSessionFeedback } from './sessionFeedback'
import { http } from '../api/http'

afterEach(() => { sessionStorage.clear(); resetSessionFeedback(); setSessionClearer(() => {}) })
it('clears the active session immediately and retains the termination reason until acknowledged', () => {
  const clear = vi.fn(); setSessionClearer(clear)
  sessionStorage.setItem('city-veri-tick-token','token'); sessionStorage.setItem('city-veri-tick-role','ORGANIZER_ADMIN')
  showSessionFeedback(40102,'账号已停用','token')
  expect(sessionStorage.getItem('city-veri-tick-token')).toBeNull()
  expect(clear).toHaveBeenCalledOnce()
  expect(sessionFeedback.value.title).toBe('账号已停用')
  expect(sessionFeedback.value.portal).toBe('management')
})
it('does not let a stale request terminate a newly logged-in account', () => {
  sessionStorage.setItem('city-veri-tick-token','new')
  showSessionFeedback(40103,'强制下线','old')
  expect(sessionFeedback.value).toBeNull()
  expect(sessionStorage.getItem('city-veri-tick-token')).toBe('new')
})
it('uses a forced logout dialog for the current session and only opens one dialog', async () => {
  sessionStorage.setItem('city-veri-tick-token','token')
  const error = { response:{status:401,data:{code:40103,msg:'当前登录已被平台管理员结束，请重新登录。'}},config:{headers:{Authorization:'Bearer token'}} }
  const reject=http.interceptors.response.handlers[0].rejected
  await expect(reject(error)).rejects.toThrow('当前登录已被平台管理员结束')
  expect(sessionFeedback.value.title).toBe('登录已被结束')
  await expect(reject(error)).rejects.toThrow()
  expect(sessionFeedback.value.title).toBe('登录已被结束')
})
