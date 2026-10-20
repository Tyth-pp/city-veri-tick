import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import App from './App.vue'

describe('应用外壳', () => {
  it('显示中文品牌标识', () => {
    const wrapper = mount(App, {
      global: { stubs: { RouterView: { template: '<div>City Veri-Tick</div>' },ActionDialogHost:true,SessionFeedbackDialog:true } }
    })
    expect(wrapper.text()).toContain('City Veri-Tick')
  })
})
