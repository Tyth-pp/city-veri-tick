import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import SessionFeedbackDialog from './SessionFeedbackDialog.vue'
import { showSessionFeedback, resetSessionFeedback } from '../utils/sessionFeedback'
const replace=vi.hoisted(()=>vi.fn().mockResolvedValue())
vi.mock('vue-router',()=>({useRouter:()=>({replace})}))
let wrapper
beforeEach(()=>{replace.mockClear();sessionStorage.clear();resetSessionFeedback()})
afterEach(()=>{wrapper?.unmount();resetSessionFeedback()})
it('shows account feedback until acknowledgement and only then redirects to login',async()=>{
  wrapper=mount(SessionFeedbackDialog,{global:{stubs:{Teleport:true}}})
  sessionStorage.setItem('city-veri-tick-token','token')
  showSessionFeedback(40102,'账号已停用，请联系平台管理员。','token');await flushPromises()
  expect(wrapper.get('[role="alertdialog"]').text()).toContain('账号已停用')
  expect(replace).not.toHaveBeenCalled()
  await wrapper.get('button').trigger('click');await flushPromises()
  expect(replace).toHaveBeenCalledWith(expect.objectContaining({path:'/login'}))
  expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
})
