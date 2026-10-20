import { http } from './http'
export const organizerActivityApi = {
  list: () => http.get('/controller/admin/organizer/activities'),
  create: (data) => http.post('/controller/admin/organizer/activities', data),
  update: (id, data) => http.put(`/controller/admin/organizer/activities/${id}`, data),
  submit: (id) => http.post(`/controller/admin/organizer/activities/${id}/submit`),
  remove: (id) => http.delete(`/controller/admin/organizer/activities/${id}`)
}
