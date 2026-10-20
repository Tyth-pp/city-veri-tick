import {mount,flushPromises} from '@vue/test-utils'
import {beforeEach,it,expect,vi} from 'vitest'
import RefundSection from './RefundSection.vue'
const api=vi.hoisted(()=>({applyRefund:vi.fn(),reviewRefund:vi.fn(),askAction:vi.fn()}))
vi.mock('../api/order',()=>({orderApi:api}))
vi.mock('../api/organizerOrder',()=>({organizerOrderApi:api}))
vi.mock('../utils/actionDialog',()=>({askAction:api.askAction}))
const order={orderId:'10',status:'PAID',payAmount:88,quantity:2,refundEligible:true,refundRule:'全部未核销且提前48小时，支持整单模拟退款',refundHistory:[]}
beforeEach(()=>{vi.clearAllMocks();api.applyRefund.mockResolvedValue({status:'PENDING'});api.reviewRefund.mockResolvedValue({status:'REJECTED'})})
it('requires a reason and sends only order id and reason before refreshing authoritative data',async()=>{
 const w=mount(RefundSection,{props:{order,scope:'user'}})
 await w.get('[data-action="apply-refund"]').trigger('click');await w.get('form').trigger('submit')
 expect(api.applyRefund).not.toHaveBeenCalled();expect(w.text()).toContain('请填写退票原因')
 await w.get('textarea').setValue(' 行程变更 ');await w.get('form').trigger('submit');await flushPromises()
 expect(api.applyRefund).toHaveBeenCalledWith('10','行程变更');expect(w.emitted('refresh')).toHaveLength(1)
})
it('shows rejection history and restored usability without pretending payment was refunded',()=>{
 const w=mount(RefundSection,{props:{scope:'user',order:{...order,refundHistory:[{id:'3',status:'REJECTED',amount:88,reason:'行程变更',reviewReason:'资料不完整',requestedAt:'2026-10-15T10:00:00+08:00'}]}}})
 expect(w.text()).toContain('申请未通过');expect(w.text()).toContain('资料不完整');expect(w.find('[data-action="apply-refund"]').exists()).toBe(true)
})
it('only organizer can review pending requests and sends required rejection explanation',async()=>{
 const pending={...order,status:'REFUNDING',latestRefund:{id:'3',status:'PENDING',amount:88},refundHistory:[{id:'3',status:'PENDING',amount:88}]}
 const p=mount(RefundSection,{props:{scope:'platform',order:pending}});expect(p.find('[data-action="reject-refund"]').exists()).toBe(false)
 const w=mount(RefundSection,{props:{scope:'organizer',order:pending}});api.askAction.mockResolvedValue({reason:'资料不完整'})
 await w.get('[data-action="reject-refund"]').trigger('click');await flushPromises()
 expect(api.askAction.mock.calls[0][0].required).toBe(true)
 expect(api.reviewRefund).toHaveBeenCalledWith('10','3',false,'资料不完整');expect(w.emitted('refresh')).toHaveLength(1)
})
it('failed application keeps explanation and shows actual server message',async()=>{
 api.applyRefund.mockRejectedValue(new Error('该票已核销，不能退票'))
 const w=mount(RefundSection,{props:{order,scope:'user'}});await w.get('[data-action="apply-refund"]').trigger('click');await w.get('textarea').setValue('行程变更');await w.get('form').trigger('submit');await flushPromises()
 expect(w.text()).toContain('该票已核销，不能退票');expect(w.get('textarea').element.value).toBe('行程变更');expect(w.emitted('refresh')).toBeUndefined()
})
