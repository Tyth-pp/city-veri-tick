import { mount,flushPromises } from '@vue/test-utils'
import { expect,it,vi } from 'vitest'
import StaffAccountForm from './StaffAccountForm.vue'
import { organizerStaffApi } from '../api/organizerStaff'
vi.mock('../api/organizerStaff',()=>({organizerStaffApi:{create:vi.fn()}}))
vi.mock('../api/platformOrganizer',()=>({platformOrganizerApi:{}}))
it('preserves failed creation form and only reports success after the backend accepts',async()=>{
  organizerStaffApi.create.mockRejectedValueOnce(new Error('登录账号已存在')).mockResolvedValueOnce('8')
  const w=mount(StaffAccountForm,{props:{verifier:true},global:{stubs:{Teleport:true}}})
  const fields=w.findAll('input')
  await fields[0].setValue('检票员');await fields[1].setValue('verifier_one');await fields[3].setValue('Initial123!')
  await w.get('form').trigger('submit');await flushPromises()
  expect(w.text()).toContain('登录账号已存在')
  expect(w.emitted('created')).toBeUndefined()
  expect(fields[1].element.value).toBe('verifier_one')
  await w.get('form').trigger('submit');await flushPromises()
  expect(organizerStaffApi.create).toHaveBeenLastCalledWith({name:'检票员',username:'verifier_one',phone:'',initialPassword:'Initial123!'})
  expect(w.emitted('created')[0]).toEqual(['8'])
  expect(w.findAll('input')[3].element.value).toBe('')
  w.unmount()
})
