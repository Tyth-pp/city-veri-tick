import { createPinia,setActivePinia } from 'pinia'
import { beforeEach,expect,it,vi } from 'vitest'
import { useStaffProfileStore } from './staffProfile'
import { useAuthStore } from './auth'
import { staffProfileApi } from '../api/staffProfile'
vi.mock('../api/staffProfile',()=>({staffProfileApi:{self:vi.fn(),saveSelf:vi.fn()}}))
beforeEach(()=>{sessionStorage.clear();setActivePinia(createPinia());vi.clearAllMocks()})
it('discards stale profile responses when the session changes',async()=>{
 const auth=useAuthStore();auth.accessToken='first';auth.role='VERIFIER';const store=useStaffProfileStore()
 let resolve;staffProfileApi.self.mockReturnValue(new Promise(r=>{resolve=r}))
 const pending=store.load();auth.clearSession();resolve({name:'旧账号'});await pending
 expect(store.profile).toBeNull()
})
it('updates shared identity only after accepted save and keeps state on failure',async()=>{
 const auth=useAuthStore();auth.accessToken='first';auth.role='ORGANIZER_ADMIN';const store=useStaffProfileStore()
 store.profile={id:'7',name:'张明'};staffProfileApi.saveSelf.mockRejectedValueOnce(new Error('手机号已存在')).mockResolvedValueOnce({id:'7',name:'欧阳明'})
 await expect(store.save({name:'欧阳明',phone:'13800138000'})).rejects.toThrow('手机号已存在');expect(store.profile.name).toBe('张明')
 await store.save({name:'欧阳明',phone:'13800138000'});expect(store.profile.name).toBe('欧阳明')
})
