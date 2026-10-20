import { http } from "./http";
export const platformVerifierApi = {
  list: (params = {}) => http.get("/controller/admin/platform/verifiers", { params }),
  assign: (data) => http.post("/controller/admin/platform/verifiers", data),
  updateEnabled: (id, enabled) => http.patch(`/controller/admin/platform/verifiers/${id}`, null, { params: { enabled } }),
};
