import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import LoginView from './LoginView.vue'
import { useAuthStore } from '../../stores/auth'
import { authApi } from '../../api/auth'

const navigation = vi.hoisted(() => ({ query: {}, replace: vi.fn() }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: navigation.query }),
  useRouter: () => ({ replace: navigation.replace })
}))
vi.mock('../../api/auth', () => ({ authApi: { loginBySms: vi.fn(), login: vi.fn(), sendSmsCode: vi.fn() } }))

beforeEach(() => {
  sessionStorage.clear()
  setActivePinia(createPinia())
  vi.clearAllMocks()
  navigation.query = {}
  authApi.loginBySms.mockResolvedValue({ accessToken: 'registered-user-token', role: 'USER' })
})

async function submitLogin() {
  const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } })
  await wrapper.find('input[autocomplete="tel"]').setValue('13800138000')
  await wrapper.find('input[maxlength="6"]').setValue('123456')
  await wrapper.find('form').trigger('submit')
  await flushPromises()
  wrapper.unmount()
}

describe('login navigation', () => {
  it('opens home on the first login after leaving guest experience', async () => {
    const auth = useAuthStore()
    auth.enterGuest()
    auth.logout()
    await submitLogin()
    expect(authApi.loginBySms).toHaveBeenCalledTimes(1)
    expect(auth.isLoggedIn).toBe(true)
    expect(navigation.replace).toHaveBeenCalledWith('/home')
  })

  it('returns to the requested page after login', async () => {
    navigation.query = { redirect: '/orders/confirm' }
    await submitLogin()
    expect(navigation.replace).toHaveBeenCalledWith('/orders/confirm')
  })

  it.each(['', '/login', '//example.com', 'https://example.com', '/management/platform'])('falls back to home for invalid user redirect %s', async (redirect) => {
    navigation.query = { redirect }
    await submitLogin()
    expect(navigation.replace).toHaveBeenCalledWith('/home')
  })
})
