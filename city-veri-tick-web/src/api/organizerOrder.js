import { http } from './http'

export const organizerOrderApi = {
  reviewRefund: (orderId,refundId,approved,reason) => http.post(`/controller/admin/organizer/orders/${orderId}/refunds/${refundId}/review`,{approved,reason}),
  list: (status) => http.get('/controller/admin/organizer/orders', { params: status ? { status } : {} }),
  detail: (orderId) => http.get(`/controller/admin/organizer/orders/${orderId}`),
  verificationSummary: () => http.get('/controller/admin/organizer/verification/summary'),
  verificationRecords: (checkedIn) => http.get('/controller/admin/organizer/verification/records', { params: checkedIn === '' ? {} : { checkedIn } })
}
