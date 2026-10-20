import { http } from "./http";

export const verifierApi = {
  detail: (verificationCode) =>
    http.get(
      `/controller/verify/tickets/${encodeURIComponent(verificationCode)}`,
    ),
  checkIn: (verificationCode, idCardNo) =>
    http.post("/controller/verify/tickets/check-in", { verificationCode, idCardNo }),
  records: () => http.get("/controller/verify/tickets/records"),
  statistics: () => http.get("/controller/verify/tickets/statistics"),
};
