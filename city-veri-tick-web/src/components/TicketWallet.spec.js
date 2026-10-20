import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TicketWallet from './TicketWallet.vue'

const global = { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } }
const ticket = (ticketId, status = 'UNUSED') => ({ ticketId, status })
const order = (orderId, sessionStartTime, tickets, extra = {}) => ({
  orderId, status: 'PAID', activityTitle: `活动${orderId}`, venueName: '江南大剧院',
  ticketTypeName: '普通观演票', sessionStartTime, createdAt: '2030-09-01T12:00:00+08:00', tickets, ...extra
})

afterEach(() => vi.useRealTimers())

describe('真实电子票夹', () => {
  it('统计全部未使用票，但仅展示最近购买的一张票并直接打开其凭证', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2030-10-01T12:00:00+08:00'))
    const wrapper = mount(TicketWallet, { global, props: { orders: [
      order('later', '2030-10-15T15:00:00+08:00', [ticket('a')], { createdAt: '2030-09-03T12:00:00+08:00' }),
      order('first', '2030-10-02T18:30:00+08:00', [ticket('used', 'CHECKED_IN'), ticket('b'), ticket('c')], { coverFileUrl: '/cover.jpg' }),
      order('second', '2030-10-03T19:00:00', [ticket('d'), ticket('e', 'REFUNDED')]),
      order('checked', '2030-10-01T18:00:00+08:00', [ticket('f', 'CHECKED_IN')]),
      order('unpaid', '2030-10-01T18:00:00+08:00', [ticket('g')], { status: 'PENDING_PAYMENT' })
    ] } })
    expect(wrapper.find('.wallet-title a').text()).toContain('4 张待使用')
    const cards = wrapper.findAll('.wallet-ticket')
    expect(cards).toHaveLength(1)
    expect(cards[0].text()).toContain('活动later')
    expect(cards[0].text()).toContain('10月15日 15:00')
    expect(cards[0].text()).toContain('江南大剧院')
    expect(cards[0].text()).toContain('普通观演票 · 1 张待使用')
    expect(cards[0].find('img').exists()).toBe(false)
    expect(cards[0].find('a').attributes('href')).toBe('/tickets/a')
    expect(wrapper.text()).not.toContain('活动second')
    expect(wrapper.text()).not.toContain('活动first')
  })

  it('区分加载、失败和真实空状态，失败时不显示零张票并支持重试', async () => {
    const wrapper = mount(TicketWallet, { global, props: { loading: true } })
    expect(wrapper.text()).toContain('票夹加载中')
    expect(wrapper.text()).not.toContain('0 张')
    await wrapper.setProps({ loading: false, error: true })
    expect(wrapper.text()).toContain('票夹加载失败')
    expect(wrapper.text()).not.toContain('暂无待使用')
    expect(wrapper.text()).not.toContain('0 张')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
    await wrapper.setProps({ error: false })
    expect(wrapper.text()).toContain('暂无待使用电子票')
    expect(wrapper.text()).toContain('0 张待使用')
  })

  it('无封面或图片加载失败时显示票券占位，不虚构场馆和时间', async () => {
    const wrapper = mount(TicketWallet, { global, props: { orders: [
      order('1', null, [ticket('1')], { venueName: '', coverFileUrl: '/missing.jpg' })
    ] } })
    expect(wrapper.text()).toContain('场馆信息暂未提供')
    expect(wrapper.text()).toContain('场次时间暂未提供')
    await wrapper.find('img').trigger('error')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.wallet-cover svg').exists()).toBe(true)
  })
})
