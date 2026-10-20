import {mount,flushPromises} from '@vue/test-utils'
import {it,expect,vi} from 'vitest'
import ManagementOverview from './ManagementOverview.vue'
import {http} from '../api/http'
vi.mock('../api/http',()=>({http:{get:vi.fn()}}))
it('renders real zero results without fabricated trend or system health labels',async()=>{
 http.get.mockResolvedValue({paidOrders:0,paidRevenue:0,trend:[],tasks:[],categories:[],todaySessions:[]})
 const wrapper=mount(ManagementOverview,{props:{platform:true},global:{stubs:{RouterLink:{template:'<a><slot/></a>'}}}});await flushPromises()
 expect(wrapper.text()).toContain('暂无趋势数据');expect(wrapper.text()).not.toContain('系统正常');expect(wrapper.text()).toContain('模拟支付')
 expect(http.get).toHaveBeenCalledWith('/controller/admin/platform/dashboard/summary',{params:{days:7}})
 wrapper.unmount()
})
it('keeps successful data labeled with its actual period when changing periods fails',async()=>{
 http.get.mockResolvedValueOnce({days:7,generatedAt:'2026-10-16T10:00:00',paidRevenue:188,trend:[],tasks:[],categories:[],todaySessions:[]}).mockRejectedValueOnce(new Error('网络断开'))
 const wrapper=mount(ManagementOverview,{props:{platform:true},global:{stubs:{RouterLink:{template:'<a><slot/></a>'}}}});await flushPromises()
 await wrapper.get('[aria-label="统计周期"]').setValue(30);await flushPromises()
 expect(wrapper.get('.ops-metrics').text()).toContain('近7日支付金额');expect(wrapper.get('.ops-metrics').text()).not.toContain('近30日支付金额');wrapper.unmount()
})
