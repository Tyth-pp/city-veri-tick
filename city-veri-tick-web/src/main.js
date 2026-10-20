import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { useAuthStore } from './stores/auth'
import { setSessionClearer } from './utils/sessionFeedback'
import { router } from './router/index.js'
import './styles/base.css'
import './styles/staff.css'
import './styles/staffIdentity.css'
import './styles/operations.css'

const app = createApp(App)
app.use(createPinia())
setSessionClearer(() => useAuthStore().clearSession())
window.addEventListener('city-password-required', () => {
  useAuthStore().setPasswordRequirement(true)
  router.replace('/change-password')
})
app.use(router)

router.onError((error) => {
  console.error('[router]', error)
  const root = document.querySelector('#app')
  if (root) root.innerHTML = `<main style="padding:40px;color:#c0392b;font-family:Arial,sans-serif"><h2>页面加载失败</h2><p>${String(error?.message || error)}</p><p>请刷新页面；如果仍失败，请把此错误信息发给开发人员。</p></main>`
})

app.config.errorHandler = (error) => {
  console.error('[vue]', error)
  const root = document.querySelector('#app')
  if (root && !root.innerHTML.trim()) root.innerHTML = `<main style="padding:40px;color:#c0392b;font-family:Arial,sans-serif"><h2>页面加载失败</h2><p>${String(error?.message || error)}</p></main>`
}

app.mount('#app')
