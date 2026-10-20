import { http } from './http'
export const authApi = {
  session: () => http.get('/controller/account/session'),
  changePassword: (payload) => http.post('/controller/account/password', payload),
  logout: (token) => http.post('/controller/auth/logout', null, { headers:{Authorization:`Bearer ${token}`} }),
  login: (payload) => http.post('/controller/auth/login', payload),
  sendSmsCode: (phone) => http.post('/controller/auth/sms/send', { phone }),
  loginBySms: (payload) => http.post('/controller/auth/sms/login', payload)
}
