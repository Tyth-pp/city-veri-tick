import { http } from "./http";

export const platformOrderApi = {
  list: (status) => http.get("/controller/admin/platform/orders", { params: status ? { status } : {} }),
  detail: (orderId) => http.get(`/controller/admin/platform/orders/${orderId}`),
  verificationSummary: () => http.get("/controller/admin/platform/verification/summary"),
  verificationRecords: (checkedIn) => http.get("/controller/admin/platform/verification/records", { params: checkedIn === "" ? {} : { checkedIn } }),
};
