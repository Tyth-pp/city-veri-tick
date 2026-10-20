import { http } from "./http";
export const platformUserApi = {
  updateEnabled: (id, enabled, reason) => http.patch(`/controller/admin/platform/users/${id}/enabled`, {reason}, {params:{enabled}}),
  list: (params = {}) => http.get("/controller/admin/platform/users", { params }),
  operations: id => http.get(`/controller/admin/platform/users/${id}/operations`),
  forceLogout: (id, reason) => http.post(`/controller/admin/platform/users/${id}/force-logout`, {reason}),
};
