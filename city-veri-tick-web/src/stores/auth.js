import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api/auth'
import { showLogoutFailure } from '../utils/sessionFeedback'

const TOKEN_KEY = 'city-veri-tick-token'
const ROLE_KEY = 'city-veri-tick-role'
const GUEST_KEY = 'venu-guest-name'

export const useAuthStore = defineStore('auth', () => {
  // sessionStorage isolates user, organizer, verifier, and platform sessions by browser tab.
  const accessToken = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const mustChangePassword = ref(sessionStorage.getItem('city-must-change-password') === 'true')
  function setPasswordRequirement(value) {
    mustChangePassword.value = Boolean(value)
    sessionStorage.setItem('city-must-change-password', String(mustChangePassword.value))
  }
  const role = ref(sessionStorage.getItem(ROLE_KEY) || '')
  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const guestName = ref(sessionStorage.getItem(GUEST_KEY) || '')
  const isGuest = computed(() => !isLoggedIn.value)

  function enterGuest() {
    if (isLoggedIn.value || guestName.value) return
    const adjectives = ['神秘的', '自在的', '慷慨的', '好奇的', '温柔的', '闪亮的', '勇敢的', '热情的']
    const nouns = ['幽灵', '红参', '旅人', '星河', '晚风', '青松', '海浪', '飞鸟']
    const random = new Uint32Array(2)
    crypto.getRandomValues(random)
    guestName.value = adjectives[random[0] % adjectives.length] + nouns[random[1] % nouns.length]
    sessionStorage.setItem(GUEST_KEY, guestName.value)
  }

  function clearGuest() {
    guestName.value = ''
    sessionStorage.removeItem(GUEST_KEY)
  }

  async function login(payload) {
    const data = await authApi.login(payload)
    accessToken.value = data.accessToken
    role.value = data.role || 'USER'
    setPasswordRequirement(data.mustChangePassword)
    sessionStorage.setItem(TOKEN_KEY, accessToken.value)
    sessionStorage.setItem(ROLE_KEY, role.value)
    clearGuest()
  }

  async function loginBySms(payload) {
    const data = await authApi.loginBySms(payload)
    accessToken.value = data.accessToken
    role.value = data.role || 'USER'
    setPasswordRequirement(data.mustChangePassword)
    sessionStorage.setItem(TOKEN_KEY, accessToken.value)
    sessionStorage.setItem(ROLE_KEY, role.value)
    clearGuest()
  }

  function clearSession() {
    mustChangePassword.value = false
    sessionStorage.removeItem('city-must-change-password')
    accessToken.value = ''
    role.value = ''
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(ROLE_KEY)
    clearGuest()
  }

  let logoutPromise
  function logout() {
    if (logoutPromise) return logoutPromise
    const token = accessToken.value
    logoutPromise = (async () => {
      try {
        if (token) await authApi.logout(token)
        if (accessToken.value !== token) return false
        clearSession()
        return true
      } catch (error) {
        if (accessToken.value === token) showLogoutFailure(error.message || '退出失败，请稍后重试。')
        return false
      }
    })().finally(() => { logoutPromise = undefined })
    return logoutPromise
  }

  return { setPasswordRequirement, mustChangePassword, clearSession, accessToken, role, isLoggedIn, isGuest, guestName, enterGuest, login, loginBySms, logout }
})
