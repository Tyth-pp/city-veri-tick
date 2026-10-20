import { mount,flushPromises } from '@vue/test-utils'
import { createPinia,setActivePinia } from 'pinia'
import { beforeEach,expect,it,vi } from 'vitest'
import PlatformUserView from './PlatformUserView.vue'
import { platformUserApi } from '../../api/platformUser'
vi.mock('../../api/platformUser',()=>({platformUserApi:{list:vi.fn(),updateEnabled:vi.fn(),forceLogout:vi.fn()}}))
vi.mock('vue-router',()=>({useRoute:()=>({query:{}})}))
beforeEach(()=>{setActivePinia(createPinia());vi.clearAllMocks();platformUserApi.list.mockResolvedValue([{id:'7',username:'organizer',roleCodes:'ORGANIZER_ADMIN',enabled:true},{id:'1',username:'platform',roleCodes:'PLATFORM_ADMIN',enabled:true}])})
it('offers organizer account disable with explicit confirmation and protects platform accounts',async()=>{
  const wrapper=mount(PlatformUserView,{global:{stubs:{Teleport:true,RouterLink:true}}});await flushPromises()
  expect(wrapper.findAll('.account-actions')).toHaveLength(1)
  await wrapper.get('.action.danger').trigger('click')
  expect(wrapper.get('[role="dialog"]').text()).toContain('禁止重新登录')
  expect(platformUserApi.updateEnabled).not.toHaveBeenCalled()
  await wrapper.get('.confirm-action').trigger('click');await flushPromises()
  expect(platformUserApi.updateEnabled).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('请填写停用原因')
  await wrapper.get('textarea[name="operationReason"]').setValue('主办方人员离职')
  await wrapper.get('.confirm-action').trigger('click');await flushPromises()
  expect(platformUserApi.updateEnabled).toHaveBeenCalledWith('7',false,'主办方人员离职')
  expect(wrapper.text()).toContain('账号已停用，已有登录会话已撤销')
  wrapper.unmount()
})
