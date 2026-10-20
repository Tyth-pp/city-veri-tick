import { mount,flushPromises } from '@vue/test-utils'
import { expect,it,vi } from 'vitest'
import OrganizerStaffView from './OrganizerStaffView.vue'
vi.mock('vue-router',()=>({useRoute:()=>({query:{}})}))
import { organizerStaffApi } from '../../api/organizerStaff'
vi.mock('../../api/organizerStaff',()=>({organizerStaffApi:{list:vi.fn(async()=>[{id:'8',username:'verify_one',nickname:'核销员',enabled:true,activityIds:['20']}]),activities:vi.fn(async()=>[{id:'20',title:'活动一'},{id:'21',title:'活动二'}]),create:vi.fn(),assign:vi.fn(async()=>{}),enabled:vi.fn(async()=>{})}}))
it('loads actual assignments and only saves the selected activity identifiers',async()=>{
 const w=mount(OrganizerStaffView,{global:{stubs:{Teleport:true,StaffAccountForm:true}}});await flushPromises()
 await w.findAll('button').find(b=>b.text()==='分配活动').trigger('click')
 const choices=w.findAll('input[type="checkbox"]')
 expect(choices[0].element.checked).toBe(true);expect(choices[1].element.checked).toBe(false)
 await choices[0].setValue(false);await w.findAll('input[type="checkbox"]')[1].setValue(true)
 await w.get('form').trigger('submit');await flushPromises()
 expect(organizerStaffApi.assign).toHaveBeenCalledWith('8',['21'])
 expect(w.text()).toContain('核销活动权限已保存')
 w.unmount()
})

it('opens the real creation form, cancels, preserves failures and reloads after accepted creation',async()=>{
 organizerStaffApi.create.mockRejectedValueOnce(new Error('登录账号已存在')).mockResolvedValueOnce('9')
 const w=mount(OrganizerStaffView,{global:{stubs:{Teleport:true}}});await flushPromises()
 const open=()=>w.findAll('button').find(b=>b.text().includes('创建核销员')).trigger('click')
 await open();expect(w.get('[role="dialog"]').text()).toContain('创建核销员账号')
 await w.get('[role="dialog"] button[type="button"]').trigger('click')
 expect(w.find('[role="dialog"]').exists()).toBe(false)
 await open()
 const fields=w.findAll('[role="dialog"] input')
 await fields[0].setValue('核销员甲');await fields[1].setValue('verifier_new');await fields[3].setValue('Initial123!')
 await w.get('[role="dialog"]').trigger('submit');await flushPromises()
 expect(w.get('[role="dialog"]').text()).toContain('登录账号已存在')
 expect(w.findAll('[role="dialog"] input')[1].element.value).toBe('verifier_new')
 const priorLoads=organizerStaffApi.list.mock.calls.length
 await w.get('[role="dialog"]').trigger('submit');await flushPromises()
 expect(organizerStaffApi.create).toHaveBeenLastCalledWith({name:'核销员甲',username:'verifier_new',phone:'',initialPassword:'Initial123!'})
 expect(w.find('[role="dialog"]').exists()).toBe(false)
 expect(organizerStaffApi.list.mock.calls.length).toBe(priorLoads+1)
 expect(w.text()).toContain('核销员账号已创建，请分配允许核销的活动。')
 w.unmount()
})
