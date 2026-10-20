import {mount,flushPromises} from '@vue/test-utils'
import {expect,it,vi} from 'vitest'
import View from './TicketInventoryView.vue'
vi.mock('vue-router',()=>({useRoute:()=>({query:{}}),useRouter:()=>({replace:vi.fn()})}))
vi.mock('../../api/organizerActivity',()=>({organizerActivityApi:{list:async()=>[{id:'1',title:'城市音乐会',status:'PUBLISHED'}]}}))
vi.mock('../../api/organizerSchedule',()=>({organizerScheduleApi:{listSessions:async()=>[{id:'2',status:'ON_SALE',startTime:'2099-12-01T19:00:00+08:00'}],listTicketTypes:async()=>[{id:'3',name:'普通票',enabled:true,stock:10,saleStartTime:'2099-01-01T10:00:00+08:00',saleEndTime:'2099-12-01T18:00:00+08:00'},{id:'4',name:'VIP票',enabled:false,stock:10}],inventory:async()=>({stock:10,lockedStock:0,totalStock:10})}}))
it('search filters displayed rows and enabled upcoming tickets are not called on sale',async()=>{const w=mount(View,{global:{stubs:{RouterLink:true}}});await flushPromises();expect(w.text()).toContain('未到开售时间');expect(w.text()).toContain('已停用');await w.get('input[placeholder="搜索活动或票种"]').setValue('VIP');expect(w.findAll('tbody tr')).toHaveLength(1);expect(w.get('tbody').text()).not.toContain('普通票');await w.get('input').setValue('无匹配');expect(w.text()).toContain('暂无符合条件的票种');w.unmount()})
