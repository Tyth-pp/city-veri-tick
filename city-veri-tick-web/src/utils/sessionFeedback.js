import { shallowRef } from 'vue'

export const sessionFeedback = shallowRef(null)
let clearSession = () => {}
export function setSessionClearer(clearer) { clearSession = clearer }
export function resetSessionFeedback() { sessionFeedback.value = null }
export function showSessionFeedback(code, message, token) {
  if (!token || sessionStorage.getItem('city-veri-tick-token') !== token || sessionFeedback.value) return
  const role = sessionStorage.getItem('city-veri-tick-role')
  const titles = { 40102:'账号已停用', 40103:'登录已被结束', 40104:'登录已结束' }
  const fallback = { 40102:'账号已停用，暂时无法使用，请联系平台管理员。', 40103:'当前登录已被平台管理员结束，请重新登录。', 40104:'当前登录已结束，请重新登录。' }
  const path = window.location.pathname + window.location.search
  sessionStorage.removeItem('city-veri-tick-token')
  sessionStorage.removeItem('city-veri-tick-role')
  clearSession()
  sessionFeedback.value = { title: titles[code] || '登录已过期或失效', message: message || fallback[code] || '请重新登录后继续操作。', portal: role && role !== 'USER' ? 'management' : 'user', redirect: path.startsWith('/login') ? '/home' : path, needsLogin: true }
}
export function showLogoutFailure(message) {
  if (sessionFeedback.value) return
  const role = sessionStorage.getItem('city-veri-tick-role')
  sessionFeedback.value = { title:'退出未完成', message: message || '暂时无法撤销当前登录，请稍后重试。', portal: role && role !== 'USER' ? 'management' : 'user', needsLogin:false }
}
