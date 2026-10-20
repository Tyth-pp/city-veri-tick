import { beforeEach, expect, it, vi } from 'vitest'
import { createPinia,setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { authApi } from '../api/auth'
import { sessionFeedback, resetSessionFeedback } from '../utils/sessionFeedback'
vi.mock('../api/auth',()=>({authApi:{logout:vi.fn()}}))
beforeEach(()=>{sessionStorage.clear();resetSessionFeedback();setActivePinia(createPinia());vi.clearAllMocks()})
it('calls the backend logout before clearing the browser token', async()=>{
  const store=useAuthStore();store.accessToken='token';sessionStorage.setItem('city-veri-tick-token','token')
  authApi.logout.mockImplementation(async()=>{expect(store.accessToken).toBe('token')})
  expect(await store.logout()).toBe(true)
  expect(authApi.logout).toHaveBeenCalledWith('token')
  expect(store.accessToken).toBe('')
})
it('does not erase a newer session when a previous logout finishes', async()=>{
  const store=useAuthStore();store.accessToken='old'
  authApi.logout.mockImplementation(async()=>{store.accessToken='new';sessionStorage.setItem('city-veri-tick-token','new')})
  expect(await store.logout()).toBe(false)
  expect(store.accessToken).toBe('new')
})
it('retains login state and shows a retryable message if server revocation fails',async()=>{
  const store=useAuthStore();store.accessToken='token';sessionStorage.setItem('city-veri-tick-token','token')
  authApi.logout.mockRejectedValue(new Error('网络断开，请稍后重试'))
  expect(await store.logout()).toBe(false)
  expect(store.accessToken).toBe('token')
  expect(sessionFeedback.value.title).toBe('退出未完成')
  expect(sessionFeedback.value.needsLogin).toBe(false)
})
