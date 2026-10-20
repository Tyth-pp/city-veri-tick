import {mount,flushPromises} from '@vue/test-utils'
import {beforeEach,afterEach,it,expect,vi} from 'vitest'
import OrderConfirmView from './OrderConfirmView.vue'
import {couponApi} from '../../api/coupon'
import {orderApi} from '../../api/order'
const mocks=vi.hoisted(()=>({createOrder:vi.fn(),replace:vi.fn()}))
vi.mock('../../stores/order',()=>({useOrderStore:()=>({selection:{ticketTypeId:'22',quantity:1,ticket:{price:100,name:'普通票'},activity:{title:'城市音乐会'}},createOrder:mocks.createOrder})}))
vi.mock('vue-router',()=>({useRouter:()=>({back:vi.fn(),replace:mocks.replace})}))
vi.mock('../../api/profile',()=>({profileApi:{get:vi.fn().mockResolvedValue({realName:'测试',realNameVerified:true})}}))
vi.mock('../../api/order',()=>({orderApi:{paymentPolicy:vi.fn().mockResolvedValue({paymentTimeoutSeconds:900})}}))
vi.mock('../../api/coupon',()=>({couponApi:{choices:vi.fn()}}))
let w
beforeEach(()=>{vi.clearAllMocks();mocks.createOrder.mockResolvedValue({orderId:'44'});couponApi.choices.mockResolvedValue([{coupon:{id:'9',name:'满100减20'},usable:true,discountAmount:20},{coupon:{id:'10',name:'其他活动专享券'},usable:false,reason:'仅限指定活动使用',discountAmount:0}])})
afterEach(()=>w?.unmount())
const global={stubs:{RouterLink:{template:'<a><slot /></a>'}}}
it('uses backend preview, disables ineligible coupons and submits exactly one coupon',async()=>{
 w=mount(OrderConfirmView,{global});await flushPromises();expect(couponApi.choices).toHaveBeenCalledWith('22',1)
 expect(w.get('option[value="10"]').attributes()).toHaveProperty('disabled');await w.get('#orderCoupon').setValue('9')
 expect(w.get('.amount-line strong').text()).toBe('¥80.00');expect(w.text()).not.toContain('新人专享立减券');expect(w.text()).not.toContain('城市惠民演艺补贴');expect(w.text()).not.toContain('平台系统服务费')
 await w.get('.agreement input').setValue(true);await w.get('.submit-dock button').trigger('click');await flushPromises()
 expect(mocks.createOrder).toHaveBeenCalledWith('9');expect(mocks.replace).toHaveBeenCalledWith('/orders/pay/44')
})
it('does not invent a discount when coupon loading fails',async()=>{
 couponApi.choices.mockRejectedValue(new Error('网络暂不可用'));w=mount(OrderConfirmView,{global});await flushPromises()
 expect(w.text()).toContain('网络暂不可用');expect(w.get('.amount-line strong').text()).toBe('¥100.00')
})

it('shows the backend payment window without starting a countdown or creating an order on entry',async()=>{
 w=mount(OrderConfirmView,{global});await flushPromises()
 expect(w.text()).not.toContain('14:55');expect(w.text()).toContain('15 分钟内完成支付');expect(mocks.createOrder).not.toHaveBeenCalled();expect(orderApi.paymentPolicy).toHaveBeenCalled()
})
