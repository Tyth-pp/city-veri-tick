import {mount,flushPromises} from '@vue/test-utils'
import {expect,it,vi} from 'vitest'
import Platform from '../views/platform/PlatformOrderDetailView.vue'
import Organizer from '../views/management/OrganizerOrderDetailView.vue'
vi.mock('vue-router',()=>({useRoute:()=>({params:{orderId:'1'}})}))
const data={orderId:'1',orderNo:'ORD1',status:'PAID',realName:'购票时姓名',idCardMasked:'110***********1234',activityId:'4',activityTitle:'城市音乐会',organizerId:'2',organizerName:'城市文化公司',venueName:'城市剧场',amount:100,discountAmount:10,payAmount:90,sessionStartTime:'2026-11-01T19:00:00+08:00',tickets:[{ticketId:'5',status:'CHECKED_IN',verifierName:'核销员张',checkedInAt:'2026-11-01T18:50:00+08:00'}]}
vi.mock('../api/platformOrder',()=>({platformOrderApi:{detail:vi.fn()}}))
vi.mock('../api/organizerOrder',()=>({organizerOrderApi:{detail:vi.fn()}}))
import {platformOrderApi} from '../api/platformOrder'
import {organizerOrderApi} from '../api/organizerOrder'
it.each([[Platform,platformOrderApi],[Organizer,organizerOrderApi]])('management detail renders identity snapshot, venue, actual fee breakdown and check-in operator',async(View,api)=>{api.detail.mockResolvedValue(data);const w=mount(View,{global:{stubs:{RouterLink:{template:'<a><slot /></a>'}}}});await flushPromises();for(const text of ['购票时姓名','110***********1234','城市剧场','城市文化公司','核销员张','已核销','优惠金额'])expect(w.text()).toContain(text);expect(w.text()).not.toContain('CHECKED_IN');w.unmount()})
