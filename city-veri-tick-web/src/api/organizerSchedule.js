import { http } from './http'

export const organizerScheduleApi = {
  listSessions: (activityId) => http.get(`/controller/admin/organizer/activities/${activityId}/sessions`),
  createSession: (activityId, data) => http.post(`/controller/admin/organizer/activities/${activityId}/sessions`, data),
  updateSession: (sessionId, data) => http.put(`/controller/admin/organizer/sessions/${sessionId}`, data),
  deleteSession: (sessionId) => http.delete(`/controller/admin/organizer/sessions/${sessionId}`),
  listTicketTypes: (sessionId) => http.get(`/controller/admin/organizer/sessions/${sessionId}/ticket-types`),
  createTicketType: (sessionId, data) => http.post(`/controller/admin/organizer/sessions/${sessionId}/ticket-types`, data),
  updateTicketType: (ticketTypeId, data) => http.put(`/controller/admin/organizer/ticket-types/${ticketTypeId}`, data),
  deleteTicketType: (ticketTypeId) => http.delete(`/controller/admin/organizer/ticket-types/${ticketTypeId}`),
  inventory: (ticketTypeId) => http.get(`/controller/admin/organizer/ticket-types/${ticketTypeId}/inventory`)
}
