import { mount, flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import TicketView from './TicketView.vue'
import OrderDetailView from './OrderDetailView.vue'
import { orderApi } from '../../api/order'
import QRCode from 'qrcode'

const mocks = vi.hoisted(() => ({ route: null, push: vi.fn(), back: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, useRouter: () => ({ push: mocks.push, back: mocks.back }) }))
vi.mock('../../api/order', () => ({ orderApi: { list: vi.fn(), detail: vi.fn(), cancel: vi.fn() } }))
vi.mock('qrcode', () => ({ default: { toDataURL: vi.fn().mockResolvedValue('data:image/png;base64,real-code') } }))
const global = { stubs: { RouterLink: { props: ['to'], template: '<a :href="typeof to === \'string\' ? to : to.path"><slot /></a>' } } }
const order = () => ({ orderId: '10', orderNo: 'NO123', status: 'PAID', activityId:'30', activityTitle: '华韵戏曲之夜', coverFileUrl:'/cover.jpg', venueName:'江南大剧院', realName:'彭于晏', idCardMasked:'360********1234', ticketTypeName:'VIP 前排票', payAmount:188, quantity:1, sessionStartTime:'2030-10-15T15:00:00', sessionEndTime:'2030-10-15T18:00:00', tickets:[{ ticketId:'20', status:'UNUSED', verificationCode:'real-verification-code' }] })
let wrapper
beforeEach(() => { vi.clearAllMocks(); mocks.route = reactive({ params:{ticketId:'20', orderId:'10'}, query:{} }); orderApi.list.mockResolvedValue([order()]); orderApi.detail.mockResolvedValue(order()); QRCode.toDataURL.mockResolvedValue('data:image/png;base64,real-code') })
afterEach(() => wrapper?.unmount())

it('opens a ticket from the wallet URL without an order query and renders actual order and ticket data', async () => {
  wrapper = mount(TicketView, {global}); await flushPromises()
  expect(orderApi.detail).not.toHaveBeenCalledWith('')
  expect(wrapper.text()).toContain('华韵戏曲之夜')
  expect(wrapper.text()).toContain('2030/10/15')
  expect(wrapper.text()).toContain('彭于晏')
  expect(wrapper.text()).toContain('real-verification-code')
  expect(QRCode.toDataURL).toHaveBeenCalledWith('real-verification-code', expect.any(Object))
  expect(wrapper.find('.ticket-qr').exists()).toBe(true)
})
it.each(['CHECKED_IN','REFUNDED'])('does not expose an active entry QR for %s tickets', async status => {
  const value = order(); value.tickets[0].status = status; orderApi.detail.mockResolvedValue(value)
  mocks.route.query.orderId='10'; wrapper = mount(TicketView,{global}); await flushPromises()
  expect(wrapper.text()).toContain(status==='CHECKED_IN' ? '已核销' : '已退款')
  expect(wrapper.find('.ticket-qr').exists()).toBe(false)
  expect(QRCode.toDataURL).not.toHaveBeenCalled()
})
it('shows an error for an unrelated ticket rather than inventing a ticket card', async () => {
  mocks.route.params.ticketId='999'; wrapper = mount(TicketView,{global}); await flushPromises()
  expect(wrapper.text()).toContain('电子票暂时无法打开')
  expect(wrapper.find('.ticket-shell').exists()).toBe(false)
})
it('retains historical ticket information but disables the entry QR when the session has ended', async () => {
  const value = order(); value.sessionStartTime='2020-10-15T15:00:00+08:00'; value.sessionEndTime='2020-10-15T18:00:00+08:00'
  orderApi.detail.mockResolvedValue(value)
  wrapper = mount(TicketView,{global}); await flushPromises()
  expect(wrapper.text()).toContain('华韵戏曲之夜')
  expect(wrapper.text()).toContain('活动已结束')
  expect(wrapper.find('.ticket-qr').exists()).toBe(false)
  expect(QRCode.toDataURL).not.toHaveBeenCalled()
})
it('shows a retry error when the API fails without creating a fictional ticket', async () => {
  orderApi.list.mockRejectedValue(new Error('网络断开'))
  wrapper = mount(TicketView,{global}); await flushPromises()
  expect(wrapper.text()).toContain('网络断开')
  expect(wrapper.text()).toContain('重新加载')
  expect(wrapper.find('.ticket-shell').exists()).toBe(false)
})
it('keeps real ticket information when QR generation fails and offers retry', async () => {
  QRCode.toDataURL.mockRejectedValue(new Error('encode failed'))
  wrapper = mount(TicketView,{global}); await flushPromises()
  expect(wrapper.text()).toContain('real-verification-code')
  expect(wrapper.text()).toContain('二维码生成失败')
})
it('reloads ticket information when navigating to a different ticket on the same page component', async () => {
  mocks.route.query.orderId='10'; wrapper=mount(TicketView,{global}); await flushPromises()
  mocks.route.params.ticketId='999'; await flushPromises()
  expect(wrapper.find('.ticket-shell').exists()).toBe(false)
  expect(wrapper.text()).toContain('电子票暂时无法打开')
})
it('renders the redesigned order with the actual cover, amount and ticket status', async () => {
  const value=order(); value.tickets[0].status='CHECKED_IN'; orderApi.detail.mockResolvedValue(value)
  wrapper=mount(OrderDetailView,{global}); await flushPromises()
  expect(wrapper.find('img').attributes('src')).toBe('/cover.jpg')
  expect(wrapper.text()).toContain('¥188.00')
  expect(wrapper.text()).toContain('已核销')
  expect(wrapper.find('.ticket-preview').attributes('href')).toBe('/tickets/20?orderId=10')
})
it('keeps pending payment actions and hides the issued ticket section for unpaid orders', async () => {
  orderApi.detail.mockResolvedValue({...order(), status:'PENDING_PAYMENT', tickets:[]})
  wrapper=mount(OrderDetailView,{global}); await flushPromises()
  expect(wrapper.text()).toContain('待付款')
  expect(wrapper.find('.ticket-preview').exists()).toBe(false)
  await wrapper.get('[data-action="pay"]').trigger('click')
  expect(mocks.push).toHaveBeenCalledWith('/orders/pay/10')
})

it.each([1, 2])('shows completed after all %i purchased tickets have been checked in', async quantity => {
  const value = {...order(), quantity, tickets: Array.from({length: quantity}, (_, i) => ({ticketId: String(20 + i), status: 'CHECKED_IN'}))}
  orderApi.detail.mockResolvedValue(value)
  wrapper = mount(OrderDetailView, {global}); await flushPromises()
  expect(wrapper.get('.status-banner h2').text()).toBe('已完成')
  expect(wrapper.get('.status-banner p').text()).toContain('全部电子票已核销')
  expect(wrapper.get('.status-banner').text()).not.toContain('请凭票')
  expect(wrapper.text()).toContain('实付金额')
})

it('shows partial use progress without marking the whole order completed', async () => {
  orderApi.detail.mockResolvedValue({...order(), quantity: 2, tickets: [{ticketId: '20', status: 'CHECKED_IN'}, {ticketId: '21', status: 'UNUSED'}]})
  wrapper = mount(OrderDetailView, {global}); await flushPromises()
  expect(wrapper.get('.status-banner h2').text()).toBe('部分已使用')
  expect(wrapper.get('.status-banner p').text()).toContain('已核销 1 / 2 张')
})

it.each([
  {status: 'PAID', quantity: 2, tickets: [{ticketId: '20', status: 'CHECKED_IN'}], label: '支付成功'},
  {status: 'PAID', quantity: 1, tickets: [], label: '支付成功'},
  {status: 'REFUNDING', quantity: 1, tickets: [{ticketId: '20', status: 'CHECKED_IN'}], label: '退款审核中'},
  {status: 'REFUNDED', quantity: 1, tickets: [{ticketId: '20', status: 'REFUNDED'}], label: '已退款'},
  {status: 'COMPLETED', quantity: 1, tickets: [{ticketId: '20', status: 'CHECKED_IN'}], label: '已完成'},
])('respects $status and does not infer completion from incomplete ticket records ($label)', async ({label, ...data}) => {
  orderApi.detail.mockResolvedValue({...order(), ...data})
  wrapper = mount(OrderDetailView, {global}); await flushPromises()
  expect(wrapper.get('.status-banner h2').text()).toBe(label)
  if (label === '已完成') expect(wrapper.get('.status-banner').text()).not.toContain('请凭票')
})

it('updates the banner after refreshing a newly checked-in order', async () => {
  wrapper = mount(OrderDetailView, {global}); await flushPromises()
  expect(wrapper.get('.status-banner h2').text()).toBe('支付成功')
  const value = order(); value.tickets[0].status = 'CHECKED_IN'
  orderApi.detail.mockResolvedValue(value)
  await wrapper.get('[aria-label="刷新订单状态"]').trigger('click'); await flushPromises()
  expect(wrapper.get('.status-banner h2').text()).toBe('已完成')
})

it('shows the actual coupon name and stored discount in order details', async () => {
  orderApi.detail.mockResolvedValue({...order(),amount:208,discountAmount:20,payAmount:188,couponId:'9',couponName:'满200减20券'})
  wrapper=mount(OrderDetailView,{global});await flushPromises()
  expect(wrapper.get('.order-discount').text()).toContain('满200减20券')
  expect(wrapper.get('.order-discount').text()).toContain('-¥20.00')
  expect(wrapper.get('.order-discount').text()).toContain('¥188.00')
})
