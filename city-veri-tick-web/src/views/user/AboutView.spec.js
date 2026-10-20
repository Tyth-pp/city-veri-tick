import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AboutView from './AboutView.vue'

describe('AboutView', () => {
  it('展示平台简介、核心功能、适用场景和版本信息', () => {
    const wrapper = mount(AboutView, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
          AppIcon: { template: '<span />' },
        },
      },
    })

    expect(wrapper.text()).toContain('平台简介')
    expect(wrapper.text()).toContain('实名制电子票务与现场核验 SaaS 平台')
    expect(wrapper.text()).toContain('实名制票券管理')
    expect(wrapper.text()).toContain('现场票券核验')
    expect(wrapper.text()).toContain('数据统计与报表')
    expect(wrapper.text()).toContain('分级权限管理')
    expect(wrapper.text()).toContain('安全与加密机制')
    expect(wrapper.text()).toContain('适用业务场景')
    expect(wrapper.text()).toContain('Venuo v1.0.0')
  })
})
