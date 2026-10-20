import { http } from "./http";

export const platformOrganizerApi = {
  create: (payload) => http.post('/controller/admin/platform/organizers', payload),
  createAdministrator: (id,payload) => http.post(`/controller/admin/platform/organizers/${id}/administrators`,payload),
  accounts: (id) => http.get(`/controller/admin/platform/organizers/${id}/accounts`),
  activities: (id) => http.get(`/controller/admin/platform/organizers/${id}/activities`),
  list: (params = {}) => http.get("/controller/admin/platform/organizers", { params }),
  detail: (id) => http.get(`/controller/admin/platform/organizers/${id}`),
};
