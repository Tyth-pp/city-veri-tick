import {mount,flushPromises} from '@vue/test-utils'
import {createPinia,setActivePinia} from 'pinia'
import {beforeEach,expect,it,vi} from 'vitest'
import View from './VerifyWorkbenchRefined.vue'
import {verifierApi as api} from '../../api/verifier'
vi.mock('vue-router',()=>({useRouter:()=>({replace:vi.fn()})}))
vi.mock('../../api/verifier',()=>({verifierApi:{statistics:vi.fn(),records:vi.fn(),detail:vi.fn(),checkIn:vi.fn()}}))
beforeEach(()=>{setActivePinia(createPinia());vi.resetAllMocks();api.statistics.mockResolvedValue({totalCheckedIn:8,todayCheckedIn:2});api.records.mockResolvedValue([]);api.detail.mockResolvedValue({ticketStatus:'UNUSED',ticketTypeName:'普通票'});api.checkIn.mockResolvedValue({checkedInAt:'2026-10-15T10:00:00+08:00'})})
const open=async()=>{const w=mount(View,{global:{stubs:{StaffAccountMenu:true}}});await flushPromises();return w}
it('shows only real statistics and explicit request failures',async()=>{api.records.mockRejectedValueOnce(new Error('核销流水请求失败'));const w=await open();expect(w.text()).toContain('核销流水请求失败');expect(w.text()).toContain('今日核销');expect(w.text()).toContain('累计核销');expect(w.text()).not.toContain('异常记录');expect(w.text()).not.toContain('服务正常');w.unmount()})
it('single scan preserves input, continuous scan clears it, and failed refresh never changes successful check-in into failure',async()=>{const w=await open();await w.get('input[type="checkbox"]').setValue(false);await w.get('#verification-code').setValue('code1');await w.get('input[maxlength="18"]').setValue('110101199001011234');api.records.mockRejectedValueOnce(new Error('刷新失败'));await w.get('form').trigger('submit');await flushPromises();expect(w.text()).toContain('核验通过');expect(w.text()).not.toContain('闸机已放行');expect(w.get('#verification-code').element.value).toBe('code1');await w.get('input[type="checkbox"]').setValue(true);await w.get('form').trigger('submit');await flushPromises();expect(w.get('#verification-code').element.value).toBe('');w.unmount()})
