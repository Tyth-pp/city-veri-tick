import { beforeEach, expect, test, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
vi.mock('../api/auth',()=>({authApi:{login:vi.fn(async()=>({accessToken:'staff-token',role:'VERIFIER',mustChangePassword:true})),logout:vi.fn(async()=>{})}}))
beforeEach(()=>{sessionStorage.clear();setActivePinia(createPinia())})
test('new staff password requirement survives reload and clears on session termination',async()=>{
  const auth=useAuthStore();await auth.login({username:'verifier',password:'initial'})
  expect(auth.mustChangePassword).toBe(true)
  setActivePinia(createPinia())
  expect(useAuthStore().mustChangePassword).toBe(true)
  useAuthStore().clearSession()
  expect(sessionStorage.getItem('city-must-change-password')).toBeNull()
})
