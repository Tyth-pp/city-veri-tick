import {mount,flushPromises} from '@vue/test-utils'
import {reactive} from 'vue'
import {beforeEach,afterEach,it,expect,vi} from 'vitest'
import View from './MyCouponsView.vue'
import {couponApi} from '../../api/coupon'
const mocks=vi.hoisted(()=>({route:null,replace:vi.fn()}))
vi.mock('vue-router',()=>({useRoute:()=>mocks.route,useRouter:()=>({replace:mocks.replace,back:vi.fn()})}))
vi.mock('../../api/coupon',()=>({couponApi:{mine:vi.fn()}}))
const coupon={id:'9',name:'城市满减券',couponType:'FULL_REDUCTION',discountAmount:20,minOrderAmount:100,minPayAmount:.01,scopeType:'ACTIVITY',activityId:'88',activityTitle:'城市音乐会',useStartsAt:'2030-01-01T10:00:00',useEndsAt:'2030-02-01T10:00:00'}
const item=(id,state,group,extra={})=>({userCouponId:id,coupon,state,group,stateLabel:{AVAILABLE:'可使用',NOT_STARTED:'待生效',LOCKED:'订单占用',USED:'已使用',EXPIRED:'已过期'}[state],reason:state==='LOCKED'?'待付款订单占用':'实际券状态',...extra})
const wallet=()=>({usableCount:1,unusedCount:3,usedCount:1,invalidCount:1,items:[item('1','AVAILABLE','UNUSED'),item('2','NOT_STARTED','UNUSED'),item('3','LOCKED','UNUSED',{orderId:'101',canPay:true}),item('4','USED','USED',{orderId:'102'}),item('5','EXPIRED','INVALID')]})
const global={stubs:{RouterLink:{props:['to'],template:`<a :href="typeof to==='string'?to:to.path"><slot /></a>`}}}
let w
beforeEach(()=>{vi.clearAllMocks();mocks.route=reactive({query:{}});mocks.replace.mockImplementation(to=>{mocks.route.query=to.query});couponApi.mine.mockResolvedValue(wallet())})
afterEach(()=>w?.unmount())
it('shows server counts and distinct pending-use states with correct actions',async()=>{w=mount(View,{global});await flushPromises();expect(w.text()).toContain('当前可用 1 张');expect(w.findAll('.owned-coupon')).toHaveLength(3);expect(w.get('[data-state="AVAILABLE"] .use-coupon').attributes('href')).toBe('/activities/88');expect(w.find('[data-state="NOT_STARTED"] .use-coupon').exists()).toBe(false);expect(w.get('[data-state="LOCKED"] .use-coupon').attributes('href')).toBe('/orders/pay/101')})
it('honors tab query and opens the actual used order; expired tab has no use action',async()=>{mocks.route.query={tab:'used'};w=mount(View,{global});await flushPromises();expect(w.findAll('.owned-coupon')).toHaveLength(1);expect(w.get('.view-order').attributes('href')).toBe('/orders/102');await w.findAll('.coupon-tabs button')[2].trigger('click');await flushPromises();expect(w.get('.owned-coupon').attributes('data-state')).toBe('EXPIRED');expect(w.find('.use-coupon').exists()).toBe(false)})
it('displays a retry error instead of inventing an empty wallet',async()=>{couponApi.mine.mockRejectedValue(new Error('加载失败'));w=mount(View,{global});await flushPromises();expect(w.get('[role="alert"]').text()).toContain('加载失败');expect(w.find('.wallet-empty').exists()).toBe(false);expect(w.find('.wallet-intro b').exists()).toBe(false)})
it('does not enable payment for a timed-out locked coupon',async()=>{const data=wallet();data.items=[item('3','LOCKED','UNUSED',{orderId:'101',canPay:false,reason:'订单已超时，等待系统释放'})];couponApi.mine.mockResolvedValue(data);w=mount(View,{global});await flushPromises();expect(w.find('.use-coupon').exists()).toBe(false);expect(w.get('.view-order').attributes('href')).toBe('/orders/101')})
