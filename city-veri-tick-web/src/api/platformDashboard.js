import { http } from "./http";

export const platformDashboardApi = {
  summary: () => http.get("/controller/admin/platform/dashboard/summary"),
};
