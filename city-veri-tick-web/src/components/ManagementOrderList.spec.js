import {mount,flushPromises} from '@vue/test-utils'
import {it,expect,vi} from 'vitest'
import {createRouter,createMemoryHistory} from 'vue-router'
import ManagementOrderList from './ManagementOrderList.vue'
import {organizerOrderApi} from '../api/organizerOrder'
vi.mock('../api/organizerOrder',()=>({organizerOrderApi:{list:vi.fn()}}))
vi.mock('../api/platformOrder',()=>({platformOrderApi:{list:vi.fn()}}))
const orders=[{orderId:'1',orderNo:'PAID001',activityId:'20',activityTitle:'城市音乐会',sessionId:'30',quantity:1,payAmount:88,status:'PAID'},{orderId:'2',orderNo:'REFUND002',activityId:'21',activityTitle:'话剧之夜',sessionId:'31',quantity:1,payAmount:98,status:'REFUNDING'}]
it('opens the refund shortcut with the actual status filter and preserves it on the detail link',async()=>{
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{template:'<div/>'}}]});await router.push('/management/organizer/orders?status=REFUNDING')
 organizerOrderApi.list.mockResolvedValue(orders)
 const w=mount(ManagementOrderList,{global:{plugins:[router]}});await flushPromises()
 expect(w.findAll('tbody tr')).toHaveLength(1);expect(w.get('tbody').text()).toContain('REFUND002');expect(w.get('tbody').text()).not.toContain('PAID001')
 expect(w.get('tbody a').attributes('href')).toContain('status=REFUNDING')
 await router.replace({query:{activityId:'20'}});await flushPromises()
 expect(w.get('tbody').text()).toContain('PAID001');expect(w.get('tbody').text()).not.toContain('REFUND002');w.unmount()
})
