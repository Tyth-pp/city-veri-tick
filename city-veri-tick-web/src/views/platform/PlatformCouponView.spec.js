import {mount,flushPromises} from '@vue/test-utils'
import {beforeEach,afterEach,it,expect,vi} from 'vitest'
import View from './PlatformCouponView.vue'
import {couponApi} from '../../api/coupon'
import {askAction} from '../../utils/actionDialog'
import {platformActivityApi} from '../../api/platformActivity'
vi.mock('../../utils/actionDialog',()=>({askAction:vi.fn()}))
vi.mock('../../api/coupon',()=>({couponApi:{adminList:vi.fn(),adminCreate:vi.fn(),adminStatus:vi.fn()}}))
vi.mock('../../api/platformActivity',()=>({platformActivityApi:{list:vi.fn()}}))
let w
beforeEach(()=>{vi.clearAllMocks();couponApi.adminList.mockResolvedValue([]);couponApi.adminCreate.mockResolvedValue({});platformActivityApi.list.mockResolvedValue({records:[{id:'123',title:'城市演唱会'}]})})
afterEach(()=>w?.unmount())
async function open(){w=mount(View);await flushPromises();await w.get('.title button').trigger('click')}
it('shows inline errors without sending an invalid request',async()=>{await open();await w.get('form').trigger('submit');expect(w.findAll('.field-error').length).toBeGreaterThan(4);expect(couponApi.adminCreate).not.toHaveBeenCalled()})
it('switches discount fields and disables category selection for a specific activity',async()=>{await open();await w.get('#couponType').setValue('DISCOUNT');expect(w.find('#discountAmount').exists()).toBe(false);expect(w.find('#maxDiscountAmount').exists()).toBe(true);await w.get('input[value="ACTIVITY"]').setValue();await flushPromises();expect(w.get('.category-fieldset').attributes()).toHaveProperty('disabled');expect(w.get('#activityId').text()).toContain('城市演唱会')})
it('submits configured no-threshold rules and clears hidden category filters',async()=>{await open();await w.get('#couponType').setValue('NO_THRESHOLD');for(const [key,value] of Object.entries({name:'十元专享券',discountAmount:10,totalQuantity:100,startsAt:'2030-01-01T10:00',endsAt:'2030-01-02T10:00',useStartsAt:'2030-01-01T10:00',useEndsAt:'2030-01-03T10:00'}))await w.get(['startsAt','endsAt','useStartsAt','useEndsAt'].includes(key)?`input[data-native-for="${key}"]`:'#'+key).setValue(value);await w.get('input[value="ACTIVITY"]').setValue();await flushPromises();await w.get('#activityId').setValue('123');await w.get('form').trigger('submit');await flushPromises();expect(couponApi.adminCreate).toHaveBeenCalledWith(expect.objectContaining({couponType:'NO_THRESHOLD',discountAmount:10,minOrderAmount:0,activityId:'123',categoryCodes:[],minPayAmount:.01}));expect(w.find('form').exists()).toBe(false)})

it('sends explicit uncapped and unlimited rules while disabling obsolete number fields',async()=>{
 await open();await w.get('#couponType').setValue('DISCOUNT');await w.get('#discountCapMode').setValue('true');await w.get('#perUserLimitMode').setValue('true');expect(w.get('#maxDiscountAmount').attributes('disabled')).toBeDefined();expect(w.get('#perUserLimit').attributes('disabled')).toBeDefined()
 for(const [key,value] of Object.entries({name:'九折不封顶券',minOrderAmount:500,discountRate:9,totalQuantity:20,startsAt:'2030-01-01T10:00',endsAt:'2030-01-02T10:00',useStartsAt:'2030-01-01T10:00',useEndsAt:'2030-01-03T10:00'}))await w.get(['startsAt','endsAt','useStartsAt','useEndsAt'].includes(key)?`input[data-native-for="${key}"]`:'#'+key).setValue(value)
 await w.get('form').trigger('submit');await flushPromises();expect(couponApi.adminCreate).toHaveBeenCalledWith(expect.objectContaining({discountRate:.9,maxDiscountAmount:null,perUserLimit:0}))
})

it('keeps a saved coupon unpublished until an explicit activation confirmation',async()=>{
 couponApi.adminList.mockResolvedValue([{id:'9',name:'待投放券',status:'DRAFT',couponType:'NO_THRESHOLD',totalQuantity:10,claimedQuantity:0}]);w=mount(View);await flushPromises();expect(w.text()).toContain('未启用');expect(couponApi.adminStatus).not.toHaveBeenCalled()
 askAction.mockResolvedValueOnce(false);await w.findAll('button').find(b=>b.text()==='启用投放').trigger('click');await flushPromises();expect(couponApi.adminStatus).not.toHaveBeenCalled()
 askAction.mockResolvedValueOnce(true);await w.findAll('button').find(b=>b.text()==='启用投放').trigger('click');await flushPromises();expect(couponApi.adminStatus).toHaveBeenCalledWith('9','ACTIVE')
})
