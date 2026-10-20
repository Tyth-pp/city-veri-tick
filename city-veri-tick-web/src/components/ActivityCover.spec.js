import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ActivityCover from './ActivityCover.vue'

describe('activity covers', () => {
  it('shows a consistent fallback when a signed URL fails and recovers with a fresh URL', async () => {
    const wrapper = mount(ActivityCover, { props: { src: '/expired.jpg', title: '历史活动' } })
    await wrapper.find('img').trigger('error')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('暂无封面')
    await wrapper.setProps({ src: '/renewed.jpg' })
    expect(wrapper.find('img').attributes('src')).toBe('/renewed.jpg')
  })
  it('renders a fallback for missing covers without a broken image', () => {
    const wrapper = mount(ActivityCover, { props: { title: '历史活动' } })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('svg').exists()).toBe(true)
  })
})
