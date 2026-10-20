import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import HelpCenterView from './HelpCenterView.vue'

const global = { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } }

describe('帮助中心', () => {
  it('展示五类常见问题并可以展开答案', () => {
    const wrapper = mount(HelpCenterView, { global })
    expect(wrapper.findAll('.faq-group')).toHaveLength(5)
    expect(wrapper.findAll('.faq-item')).toHaveLength(16)
    expect(wrapper.text()).toContain('退款进度在哪里查看？')
    expect(wrapper.text()).toContain('不发生真实资金退回')
    expect(wrapper.find('.faq-item summary').text()).toContain('购票前需要填写实名信息吗')
    expect(wrapper.find('.faq-item p').text()).toContain('「我的」页面')
  })

  it('按关键词筛选问题，并允许恢复全部问题', async () => {
    const wrapper = mount(HelpCenterView, { global })
    await wrapper.find('input[type="search"]').setValue('实名')
    expect(wrapper.findAll('.faq-item').length).toBeGreaterThan(0)
    expect(wrapper.findAll('.faq-item').length).toBeLessThan(15)
    await wrapper.find('input[type="search"]').setValue('没有这个问题')
    expect(wrapper.find('.help-empty').exists()).toBe(true)
    await wrapper.find('.help-empty button').trigger('click')
    expect(wrapper.findAll('.faq-group')).toHaveLength(5)
  })

  it('只显示问题标题命中的条目，不因分类标题或答案中的关键词扩大结果', async () => {
    const wrapper = mount(HelpCenterView, { global })
    await wrapper.find('input[type="search"]').setValue('订单')
    expect(wrapper.findAll('.faq-group')).toHaveLength(4)
    expect(wrapper.text()).toContain('支付成功后订单在哪里？')
    expect(wrapper.text()).toContain('订单为什么只显示当前账号的？')
    expect(wrapper.text()).not.toContain('购票前需要填写实名信息吗？')
    await wrapper.find('input[type="search"]').setValue('购票')
    expect(wrapper.findAll('.faq-item')).toHaveLength(1)
    expect(wrapper.text()).toContain('购票前需要填写实名信息吗？')
    expect(wrapper.text()).not.toContain('实名信息在哪里修改？')
  })

  it('客服入口只说明未开放的联系方式，不显示虚构邮箱或服务时间', async () => {
    const wrapper = mount(HelpCenterView, { global })
    await wrapper.find('.contact-button').trigger('click')
    expect(wrapper.find('[role="dialog"]').text()).toContain('客服联系方式即将开放')
    expect(wrapper.find('[role="dialog"]').text()).not.toContain('7×24')
    await wrapper.find('.contact-sheet button').trigger('click')
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })
})
