import {mount,flushPromises} from '@vue/test-utils'
import {expect,it,vi} from 'vitest'
import View from '../views/platform/PlatformVerificationView.vue'
vi.mock('vue-router',()=>({useRoute:()=>({query:{orderId:'1'}})}))
vi.mock('../api/platformOrder',()=>({platformOrderApi:{verificationSummary:async()=>({totalTickets:2}),verificationRecords:async()=>[{ticketId:'1',orderId:'1',orderNo:'ORD1',activityId:'2',activityTitle:'目标活动',sessionId:'3',ticketStatus:'REFUNDED'},{ticketId:'2',orderId:'9',orderNo:'ORD9',activityId:'4',activityTitle:'不应显示的活动',sessionId:'5',ticketStatus:'CHECKED_IN'}]}}))
it('honors linked order filter and does not label refunded tickets as unused',async()=>{const w=mount(View,{global:{stubs:{RouterLink:{template:'<a><slot /></a>'}}}});await flushPromises();expect(w.get('tbody').text()).toContain('目标活动');expect(w.get('tbody').text()).not.toContain('不应显示的活动');expect(w.get('tbody').text()).toContain('已退款');expect(w.find('select[aria-label="筛选场次"]').exists()).toBe(true);expect(w.get('.metrics').text()).toContain('门票总数1');w.unmount()})
