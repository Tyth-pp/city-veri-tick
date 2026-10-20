import {mount,flushPromises} from '@vue/test-utils'
import {it,expect,vi,afterEach} from 'vitest'
import Entry from './MyCouponsEntry.vue'
import {couponApi} from '../api/coupon'
vi.mock('../api/coupon',()=>({couponApi:{mine:vi.fn()}}))
const global={stubs:{RouterLink:{props:['to'],template:'<a :href="to"><slot /></a>'}}};let w;afterEach(()=>w?.unmount())
it('shows only the backend usable count and exposes both wallet and claim entries',async()=>{couponApi.mine.mockResolvedValue({usableCount:2,unusedCount:5});w=mount(Entry,{global});await flushPromises();expect(w.get('strong').text()).toBe('2');expect(w.get('.my-coupons-link').attributes('href')).toBe('/coupons/mine');expect(w.get('.claim-link').attributes('href')).toBe('/coupons')})
it('does not pretend there are zero coupons on a failed request',async()=>{couponApi.mine.mockRejectedValue(new Error('offline'));w=mount(Entry,{global});await flushPromises();expect(w.text()).toContain('暂时无法获取可用数量');expect(w.find('strong').exists()).toBe(false)})
