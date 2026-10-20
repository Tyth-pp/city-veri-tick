import {mount,flushPromises} from '@vue/test-utils'
import {reactive} from 'vue'
import {it,expect,vi,beforeEach,afterEach} from 'vitest'
import View from './WechatPaymentView.vue'
import {orderApi} from '../../api/order'
const mocks=vi.hoisted(()=>({route:null,replace:vi.fn(),issued:vi.fn()}))
vi.mock('vue-router',()=>({useRoute:()=>mocks.route,useRouter:()=>({replace:mocks.replace,back:vi.fn()})}))
vi.mock('../../api/order',()=>({orderApi:{detail:vi.fn(),pay:vi.fn(),synchronizeExpiry:vi.fn()}}))
vi.mock('../../stores/reminders',()=>({useRemindersStore:()=>({accountId:'7',confirmIssued:mocks.issued})}))
const pending=extra=>({orderId:'10',orderNo:'ORDER10',status:'PENDING_PAYMENT',payAmount:88,createdAt:'2030-01-01T07:47:00+08:00',expiredAt:'2030-01-01T08:02:00+08:00',serverTime:'2030-01-01T00:00:00Z',...extra})
let w
beforeEach(()=>{vi.clearAllMocks();mocks.route=reactive({params:{orderId:'10'}});orderApi.detail.mockResolvedValue(pending());orderApi.pay.mockResolvedValue(pending({status:'PAID'}));orderApi.synchronizeExpiry.mockResolvedValue(pending({status:'EXPIRED',serverTime:'2030-01-01T00:02:00Z'}))})
afterEach(()=>{w?.unmount();vi.useRealTimers()})
it('counts down from the real deadline and does not restart after a refresh',async()=>{
 vi.useFakeTimers({toFake:['setInterval','clearInterval','performance','Date']});vi.setSystemTime(new Date('2030-01-02T00:00:00Z'))
 w=mount(View);await flushPromises();expect(w.get('.time-digits').text()).toBe('02:00')
 await vi.advanceTimersByTimeAsync(30000);expect(w.get('.time-digits').text()).toBe('01:30')
 orderApi.detail.mockResolvedValue(pending({serverTime:'2030-01-01T00:00:30Z'}));await w.get('[aria-label="刷新支付状态"]').trigger('click');await flushPromises()
 expect(w.get('.time-digits').text()).toBe('01:30');expect(w.text()).not.toContain('13:33')
})
it('synchronizes expiry once at zero, disables payment and exposes order details',async()=>{
 vi.useFakeTimers({toFake:['setInterval','clearInterval','performance']});orderApi.detail.mockResolvedValue(pending({expiredAt:'2030-01-01T08:00:02+08:00'}))
 w=mount(View);await flushPromises();await vi.advanceTimersByTimeAsync(3000);await flushPromises()
 expect(orderApi.synchronizeExpiry).toHaveBeenCalledExactlyOnceWith('10');expect(orderApi.pay).not.toHaveBeenCalled();expect(w.text()).toContain('订单已超时')
 await w.get('.wechat-confirm').trigger('click');expect(mocks.replace).toHaveBeenCalledWith('/orders/10')
})
it('does not allow payment without valid server timing',async()=>{
 orderApi.detail.mockResolvedValue(pending({serverTime:null}));w=mount(View);await flushPromises();expect(w.text()).toContain('无法获取支付时限')
 await w.get('.wechat-confirm').trigger('click');await flushPromises();expect(orderApi.pay).not.toHaveBeenCalled()
})
it('only confirms payment while pending and returns the actual paid order',async()=>{
 w=mount(View);await flushPromises();await w.get('.wechat-confirm').trigger('click');await flushPromises();expect(orderApi.pay).toHaveBeenCalledExactlyOnceWith('10');expect(mocks.issued).toHaveBeenCalledOnce();expect(mocks.replace).toHaveBeenCalledWith('/orders/10')
})
it('preserves zero and a retry action if expiry synchronization fails',async()=>{
 orderApi.detail.mockResolvedValue(pending({expiredAt:'2030-01-01T08:00:00+08:00'}));orderApi.synchronizeExpiry.mockRejectedValue(new Error('订单状态同步失败'))
 w=mount(View);await flushPromises();expect(w.text()).toContain('订单状态同步失败');await w.get('.wechat-confirm').trigger('click');await flushPromises();expect(orderApi.pay).not.toHaveBeenCalled();expect(orderApi.synchronizeExpiry).toHaveBeenCalledTimes(2)
})
