import {mount,flushPromises} from '@vue/test-utils'
import {it,expect,vi,afterEach} from 'vitest'
import ManagementMessages from './ManagementMessages.vue'
import {http} from '../api/http'
vi.mock('../api/http',()=>({http:{get:vi.fn(),post:vi.fn()}}))
vi.mock('vue-router',()=>({useRouter:()=>({push:vi.fn()})}))
let wrapper
afterEach(()=>{wrapper?.unmount();vi.useRealTimers();vi.clearAllMocks()})
const message=id=>({id,title:'新的活动待审核',content:'真实活动',targetPath:'/management/platform/activities/1'})
it('keeps historic unread messages in inbox without popping them on login, then pops only new events',async()=>{
 vi.useFakeTimers();http.get.mockResolvedValueOnce({items:[message('1')],unread:[message('1')],unreadCount:1}).mockResolvedValue({items:[message('2'),message('1')],unread:[message('2'),message('1')],unreadCount:2})
 wrapper=mount(ManagementMessages);await flushPromises()
 expect(wrapper.find('[role="status"]').exists()).toBe(false)
 await vi.advanceTimersByTimeAsync(15000);await flushPromises()
 expect(wrapper.get('[role="status"]').text()).toContain('新的活动待审核')
 await wrapper.get('[aria-label="关闭本次提醒"]').trigger('click')
 expect(http.post).not.toHaveBeenCalled()
 await vi.advanceTimersByTimeAsync(15000);await flushPromises()
 expect(wrapper.find('[role="status"]').exists()).toBe(false)
})
it('offers inbox retry when the first request fails',async()=>{
 http.get.mockRejectedValue(new Error('网络不可用'));wrapper=mount(ManagementMessages);await flushPromises()
 await wrapper.get('[aria-label="业务消息"]').trigger('click')
 expect(wrapper.text()).toContain('网络不可用');expect(wrapper.text()).toContain('重试')
})
it('pauses dismissal while hovered and expires the toast without marking its message read',async()=>{
 vi.useFakeTimers();http.get.mockResolvedValueOnce({items:[],unread:[],unreadCount:0}).mockResolvedValue({items:[message('3')],unread:[message('3')],unreadCount:1})
 wrapper=mount(ManagementMessages);await flushPromises();await vi.advanceTimersByTimeAsync(15000);await flushPromises()
 await wrapper.get('[role="status"]').trigger('mouseenter');await vi.advanceTimersByTimeAsync(9000)
 expect(wrapper.find('[role="status"]').exists()).toBe(true)
 await wrapper.get('[role="status"]').trigger('mouseleave');await vi.advanceTimersByTimeAsync(8100)
 expect(wrapper.find('[role="status"]').exists()).toBe(false);expect(http.post).not.toHaveBeenCalled()
})
it('does not mistake historical unread messages promoted beyond the first fifty for new events',async()=>{
 vi.useFakeTimers();const ids=Array.from({length:60},(_,i)=>String(i+1))
 http.get.mockResolvedValueOnce({items:[],unread:ids.slice(10).map(message),unreadIds:ids,unreadCount:60}).mockResolvedValue({items:[],unread:ids.slice(9,59).map(message),unreadIds:ids.slice(0,59),unreadCount:59})
 wrapper=mount(ManagementMessages);await flushPromises();await vi.advanceTimersByTimeAsync(15000);await flushPromises()
 expect(wrapper.find('[role="status"]').exists()).toBe(false)
})
