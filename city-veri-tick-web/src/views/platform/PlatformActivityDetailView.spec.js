import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import View from './PlatformActivityDetailView.vue'
import { platformActivityApi as api } from '../../api/platformActivity'
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '1' } }) }))
vi.mock('../../api/platformActivity', () => ({ platformActivityApi: { detail: vi.fn(), approve: vi.fn(), reject: vi.fn() } }))
const fixture = () => ({activity:{id:'1',title:'城市音乐会',status:'PENDING',category:'CONCERT',content:'完整演出介绍',highlights:'活动亮点内容',notices:'实名入场须知'},sessions:[],ticketTypes:[],organizer:{id:'2',name:'城市文化公司',contactName:'李女士',contactPhone:'13800138000',status:'APPROVED'},submitter:{id:'7',name:'张明',username:'organizer_zhang',phone:'13900139000'},submittedAt:'2026-10-15T10:00:00+08:00',reviewer:null})
async function render(data=fixture()){api.detail.mockResolvedValue(data);const w=mount(View,{global:{stubs:{RouterLink:{template:'<a><slot /></a>'}}}});await flushPromises();return w}
beforeEach(()=>vi.resetAllMocks())
describe('活动审核详情',()=>{
 it('分别展示机构联系人和真实提审人，并呈现完整审核内容',async()=>{const w=await render();for(const text of ['城市文化公司','李女士','张明','organizer_zhang','13900139000','完整演出介绍','活动亮点内容','实名入场须知'])expect(w.text()).toContain(text);expect(w.find('.staff-name-avatar').text()).toBe('张')})
 it('历史数据不把创建人冒充提审人',async()=>{const data=fixture();data.submitter=null;data.submittedAt=null;const w=await render(data);expect(w.text()).toContain('历史记录未记录')})
 it('驳回必须填写原因，成功后重新加载真实状态',async()=>{const w=await render();await w.get('[data-action="reject"]').trigger('click');await w.get('form.review-dialog').trigger('submit');expect(api.reject).not.toHaveBeenCalled();expect(w.text()).toContain('请填写驳回原因');await w.get('textarea[name="reason"]').setValue('请补充场地信息');api.detail.mockResolvedValue({...fixture(),activity:{...fixture().activity,status:'REJECTED',rejectionReason:'请补充场地信息'}});await w.get('form.review-dialog').trigger('submit');await flushPromises();expect(api.reject).toHaveBeenCalledWith('1','请补充场地信息');expect(w.find('[data-action="reject"]').exists()).toBe(false);expect(w.text()).toContain('请补充场地信息')})
 it('审核通过前确认，失败保留详情和错误反馈',async()=>{const w=await render();await w.get('[data-action="approve"]').trigger('click');expect(api.approve).not.toHaveBeenCalled();api.approve.mockRejectedValue(new Error('活动状态已变化，请刷新'));await w.get('form.review-dialog').trigger('submit');await flushPromises();expect(api.approve).toHaveBeenCalledWith('1');expect(w.text()).toContain('活动状态已变化，请刷新');expect(w.text()).toContain('城市音乐会')})
})
