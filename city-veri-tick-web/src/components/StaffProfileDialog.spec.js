import { mount,flushPromises } from '@vue/test-utils'
import { createPinia,setActivePinia } from 'pinia'
import { beforeEach,expect,it,vi } from 'vitest'
import StaffProfileDialog from './StaffProfileDialog.vue'
import { staffProfileApi } from '../api/staffProfile'
import { useStaffProfileStore } from '../stores/staffProfile'
vi.mock('../api/staffProfile',()=>({staffProfileApi:{saveVerifier:vi.fn(),saveManaged:vi.fn(),saveSelf:vi.fn()}}))
vi.mock('vue-router',()=>({useRouter:()=>({replace:vi.fn()})}))
beforeEach(()=>{setActivePinia(createPinia());vi.clearAllMocks()})
it('retains managed form on rejection and only emits saved after acceptance',async()=>{
 staffProfileApi.saveVerifier.mockRejectedValueOnce(new Error('手机号已被使用')).mockResolvedValueOnce(null)
 const w=mount(StaffProfileDialog,{props:{target:{id:'8',username:'verify_one',name:'张明',phone:'13800138000',role:'VERIFIER'},scope:'organizer'},global:{stubs:{Teleport:true}}})
 await w.get('input[autocomplete="name"]').setValue('欧阳明');await w.get('form').trigger('submit');await flushPromises()
 expect(w.text()).toContain('手机号已被使用');expect(w.emitted('saved')).toBeUndefined();expect(w.get('input[autocomplete="name"]').element.value).toBe('欧阳明')
 await w.get('form').trigger('submit');await flushPromises()
 expect(staffProfileApi.saveVerifier).toHaveBeenLastCalledWith('8',{name:'欧阳明',phone:'13800138000'});expect(w.emitted('saved')).toHaveLength(1);w.unmount()
})
it('requires matching new passwords before making a password request',async()=>{
 useStaffProfileStore().profile={id:'7',username:'admin',role:'PLATFORM_ADMIN'}
 const w=mount(StaffProfileDialog,{props:{self:true},global:{stubs:{Teleport:true}}})
 await w.findAll('button').find(b=>b.text()==='修改密码').trigger('click')
 const fields=w.findAll('input');await fields[0].setValue('OldPassword123');await fields[1].setValue('NewPassword123');await fields[2].setValue('Different123')
 await w.get('form').trigger('submit');expect(w.text()).toContain('两次输入的新密码不一致');w.unmount()
})
