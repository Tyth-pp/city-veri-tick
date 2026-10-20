import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import ActivityEditorView from './ActivityEditorView.vue'
import ChineseDateTimeInput from '../../components/ChineseDateTimeInput.vue'
import { organizerScheduleApi as api } from '../../api/organizerSchedule'
import {organizerActivityApi as activities} from '../../api/organizerActivity'
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '1' } }), useRouter: () => ({ push: vi.fn() }) }))
vi.mock('../../api/organizerActivity', () => ({ organizerActivityApi: { list: vi.fn(async () => [{ id: '1', status: 'DRAFT' }]),update:vi.fn(),submit:vi.fn() } }))
vi.mock('../../api/file', () => ({ uploadImage: vi.fn() }))
vi.mock('../../api/organizerSchedule', () => ({ organizerScheduleApi: {
  listSessions: vi.fn(), listTicketTypes: vi.fn(async () => []), inventory: vi.fn(),
  createSession: vi.fn(), updateSession: vi.fn(), createTicketType: vi.fn(), updateTicketType: vi.fn()
} }))
beforeEach(() => { vi.clearAllMocks(); api.listSessions.mockResolvedValue([{ id: '2', startTime: '2030-01-02T18:00:00', endTime: '2030-01-02T20:00:00', capacity: 100, saleStartTime: '2029-12-01T10:00:00', saleEndTime: '2030-01-02T17:00:00' }]) })
const open = async () => { const w = mount(ActivityEditorView, { global: { stubs: { RouterLink: true } } }); await flushPromises(); return w }
const click = (w, text) => w.findAll('button').find(b => b.text().includes(text)).trigger('click')
it('blocks invalid session ranges before requesting the server and preserves the form', async () => {
  const w = await open(); await click(w, '新建场次')
  for (const [label, value] of Object.entries({ 开始日期: '2030-01-02', 开始时间: '18:00', 结束日期: '2030-01-02', 结束时间: '17:00' })) { w.findAllComponents(ChineseDateTimeInput).find(c => c.attributes('aria-label') === label || c.find(`[aria-label="${label}"]`).exists()).vm.$emit('update:modelValue', value); await flushPromises() }
  await click(w, '保存场次'); await flushPromises()
  expect(api.createSession).not.toHaveBeenCalled()
  expect(w.get('[role="alert"]').text()).toContain('场次结束时间必须晚于开始时间')
  expect(w.get('[aria-label="开始时间"]').element.value).toBe('18时00分'); w.unmount()
})
it('adds a ticket using the session sales window without asking for the same dates again', async () => {
  const w = await open(); await click(w, '添加票种')
  expect(w.find('.ticket-form [aria-label="销售开始日期"]').exists()).toBe(false)
  expect(w.get('.ticket-form').text()).toContain('沿用本场次统一售票时间')
  await w.get('.ticket-form input[placeholder="例如：普通票"]').setValue('普通票')
  await click(w, '保存票种'); await flushPromises()
  expect(api.createTicketType).toHaveBeenCalledWith('2', { name: '普通票', price: 0, stock: 100, enabled: true })
  w.unmount()
})
it('does not submit an incomplete draft and gives a checklist of missing information',async()=>{
 const w=await open();await click(w,'提交审核');await flushPromises()
 expect(activities.submit).not.toHaveBeenCalled();expect(w.text()).toContain('请先完善');expect(w.text()).toContain('封面');w.unmount()
})
it('saves the latest draft before submitting and stops when saving fails',async()=>{
 activities.list.mockResolvedValueOnce([{id:'1',status:'DRAFT',title:'真实活动',category:'CONCERT',content:'内容',highlights:'亮点',notices:'须知',coverFileId:'3',venueName:'剧院',venueAddress:'城市地址'}])
 api.listTicketTypes.mockResolvedValueOnce([{id:'3',enabled:true}]);activities.update.mockRejectedValueOnce(new Error('保存失败'))
 const w=await open();await click(w,'提交审核');await flushPromises()
 expect(activities.update).toHaveBeenCalled();expect(activities.submit).not.toHaveBeenCalled();expect(w.text()).toContain('保存失败');w.unmount()
})
