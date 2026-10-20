import { http } from "./http";
export const platformActivityApi = {
  list: (params = {}) =>
    http.get("/controller/admin/platform/activities", { params }),
  detail: (id) => http.get(`/controller/admin/platform/activities/${id}`),
  approve: (id) =>
    http.post(`/controller/admin/platform/activities/${id}/approve`),
  reject: (id, reason) =>
    http.post(`/controller/admin/platform/activities/${id}/reject`, { reason }),
  offline: (id, reason) =>
    http.post(`/controller/admin/platform/activities/${id}/offline`, {reason}),
};
