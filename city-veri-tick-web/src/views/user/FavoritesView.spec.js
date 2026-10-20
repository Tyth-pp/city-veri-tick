import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FavoritesView from './FavoritesView.vue'
import { useAuthStore } from '../../stores/auth'
import { useFavoritesStore } from '../../stores/favorites'
import { activityApi } from '../../api/activity'
vi.mock('../../api/activity', () => ({ activityApi: { preview: vi.fn() } }))
vi.mock('vue-router', () => ({ useRouter: () => ({ back: vi.fn() }), RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } }))
const global = { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } }
beforeEach(() => { localStorage.clear(); sessionStorage.clear(); setActivePinia(createPinia()); vi.clearAllMocks(); useAuthStore().accessToken = `h.${btoa(JSON.stringify({sub: '1'}))}.s` })
describe('favorites after events end', () => {
  it('opens upcoming activity details and lets the user subscribe before tickets go on sale', async () => {
    useFavoritesStore().toggle({ id: '1', title: '即将开售的展览' })
    activityApi.preview.mockResolvedValue({ activity: { id: '1', title: '即将开售的展览' }, availability: 'UPCOMING' })
    const wrapper = mount(FavoritesView, { global }); await flushPromises()
    expect(wrapper.find('.favorite-main').attributes('href')).toBe('/activities/1')
    expect(wrapper.text()).toContain('提醒我开售')
    wrapper.unmount()
  })
  it('refreshes an ended event cover and blocks purchase navigation while retaining the favorite', async () => {
    const favorites = useFavoritesStore()
    favorites.toggle({ id: '1', title: '旧名称', coverFileUrl: '/expired.jpg' })
    activityApi.preview.mockResolvedValue({ activity: { id: '1', title: '历史活动', coverFileUrl: '/fresh.jpg', endTime: '2020-01-01T18:00:00+08:00' }, availability: 'ENDED' })
    const wrapper = mount(FavoritesView, { global })
    await flushPromises()
    expect(wrapper.text()).toContain('活动已结束')
    expect(wrapper.find('img').attributes('src')).toBe('/fresh.jpg')
    expect(wrapper.find('.favorite-main').attributes('href')).toBeUndefined()
    expect(favorites.items).toHaveLength(1)
    wrapper.unmount()
  })
  it('keeps inaccessible favorites but does not navigate to unavailable purchase details', async () => {
    useFavoritesStore().toggle({ id: '1', title: '活动' })
    activityApi.preview.mockRejectedValue(Object.assign(new Error('Not found'), { status: 404 }))
    const wrapper = mount(FavoritesView, { global })
    await flushPromises()
    expect(wrapper.text()).toContain('活动已下架或不可访问')
    expect(wrapper.find('.favorite-main').attributes('href')).toBeUndefined()
    expect(useFavoritesStore().items).toHaveLength(1)
    wrapper.unmount()
  })
  it('keeps favorites and displays a retry state on network failure rather than claiming they were removed', async () => {
    useFavoritesStore().toggle({ id: '1', title: '活动', coverFileUrl: '/expired.jpg' })
    activityApi.preview.mockRejectedValue(new Error('网络断开'))
    const wrapper = mount(FavoritesView, { global })
    await flushPromises()
    expect(wrapper.text()).toContain('状态暂未更新')
    expect(wrapper.text()).toContain('重试')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(useFavoritesStore().items).toHaveLength(1)
    wrapper.unmount()
  })
})
