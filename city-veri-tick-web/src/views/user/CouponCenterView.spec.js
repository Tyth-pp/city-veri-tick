import {mount,flushPromises} from '@vue/test-utils'
import {it,expect,vi,afterEach} from 'vitest'
import View from './CouponCenterView.vue'
import {couponApi} from '../../api/coupon'
vi.mock('../../api/coupon',()=>({couponApi:{available:vi.fn(),claim:vi.fn()}}))
let w
afterEach(()=>{w?.unmount();vi.useRealTimers();vi.clearAllMocks()})
it('shows upcoming flash coupons then enables claiming only when the opening time arrives',async()=>{
 vi.useFakeTimers();vi.setSystemTime(new Date('2030-01-01T10:00:00+08:00'))
 const c={id:'9',name:'城市秒杀券',claimMode:'FLASH_SALE',couponType:'NO_THRESHOLD',discountAmount:5,totalQuantity:10,claimedQuantity:0,perUserLimit:1,startsAt:'2030-01-01T10:00:02',endsAt:'2030-01-01T11:00:00',serverTime:'2030-01-01T02:00:00Z'}
 couponApi.available.mockResolvedValue([c]);w=mount(View,{global:{stubs:{RouterLink:true}}});await flushPromises()
 expect(w.get('.coupon-main button').attributes('disabled')).toBeDefined();expect(w.text()).toContain('距开抢')
 // Server time advances too on the boundary refresh.
 couponApi.available.mockResolvedValue([{...c,serverTime:'2030-01-01T02:00:02Z'}]);await vi.advanceTimersByTimeAsync(2100);await flushPromises()
 expect(w.get('.coupon-main button').attributes('disabled')).toBeUndefined()
})
it('keeps sold-out and personal-limit states distinct and cannot submit them',async()=>{
 couponApi.available.mockResolvedValue([{id:'1',name:'抢光券',claimMode:'LIMITED',totalQuantity:10,claimedQuantity:10,perUserLimit:1},{id:'2',name:'限领券',claimMode:'FLASH_SALE',totalQuantity:10,claimedQuantity:2,userClaimedQuantity:1,perUserLimit:1}]);w=mount(View,{global:{stubs:{RouterLink:true}}});await flushPromises()
 const buttons=w.findAll('.coupon-main button');expect(buttons[0].text()).toBe('已抢光');expect(buttons[1].text()).toBe('已达限领');expect(buttons.every(b=>b.attributes('disabled')!==undefined)).toBe(true);expect(couponApi.claim).not.toHaveBeenCalled()
})

it('allows repeated personal claims for an unlimited coupon and explains its uncapped rule',async()=>{
 couponApi.available.mockResolvedValue([{id:'1',name:'不限次数折扣券',couponType:'DISCOUNT',discountRate:.9,maxDiscountAmount:null,perUserLimit:0,userClaimedQuantity:5,totalQuantity:10,claimedQuantity:5}]);w=mount(View,{global:{stubs:{RouterLink:true}}});await flushPromises();expect(w.get('.coupon-main button').attributes('disabled')).toBeUndefined();expect(w.text()).toContain('优惠不封顶');expect(w.text()).toContain('每人不限领取次数');expect(w.text()).not.toContain('¥null')
})
