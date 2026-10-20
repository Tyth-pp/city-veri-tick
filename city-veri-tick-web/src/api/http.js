import axios from 'axios'
import { showSessionFeedback } from '../utils/sessionFeedback'

export const http = axios.create({ baseURL: '/', timeout: 10000 })

export function isCurrentSessionUnauthorized(error, currentToken) {
  if (error.response?.status !== 401 || !currentToken) return false
  const headers = error.config?.headers
  const authorization = headers?.get?.('Authorization') || headers?.Authorization || headers?.authorization
  return authorization === `Bearer ${currentToken}`
}

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('city-veri-tick-token')
  const publicAuth = /^\/controller\/auth\/(login|sms\/)/.test(config.url || '')
  if (token && !publicAuth && !config.headers.Authorization) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && body.code !== undefined && body.code !== 1) {
    return Promise.reject(Object.assign(new Error(body.msg || '请求失败'), { code: body.code }))
  }
  return body?.data ?? body
}, async (error) => {
  const currentToken = sessionStorage.getItem('city-veri-tick-token')
  if (error.response?.data?.code === 40301 && currentToken
      && error.config?.headers?.Authorization === `Bearer ${currentToken}`) {
    window.dispatchEvent(new Event('city-password-required'))
  }
  if (isCurrentSessionUnauthorized(error, currentToken)) {
    showSessionFeedback(error.response?.data?.code, error.response?.data?.msg, currentToken)
  }
  const responseData = error.response?.data
  const statusMessage = {
    400: '请求参数有误，请检查填写内容',
    401: '登录状态已失效，请重新登录',
    403: '当前账号没有执行此操作的权限',
    404: '请求的资源不存在，请确认活动、场次或票种 ID',
    409: '当前操作与已有业务数据冲突',
    413: '图片超过 10MB 限制，请压缩后重新上传',
    500: '服务器处理失败，请检查后端日志'
  }[error.response?.status]
  return Promise.reject(Object.assign(new Error(
    responseData?.msg || responseData?.message || statusMessage || error.message || '网络请求失败'
  ), { status: error.response?.status, code: responseData?.code }))
})
