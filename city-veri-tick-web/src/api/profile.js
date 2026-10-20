import { http } from './http'

export const profileApi = {
  get: () => http.get('/controller/user/profile'),
  update: (payload) => http.put('/controller/user/profile', payload)
}
