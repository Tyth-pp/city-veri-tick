export function formatTicketTime(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}:\d{2})/)
  return match ? `${match[1]}/${match[2]}/${match[3]} ${match[4]}` : '场次时间暂未提供'
}
export function sessionRange(order) {
  const start = formatTicketTime(order?.sessionStartTime)
  if (!order?.sessionEndTime) return start
  const end = formatTicketTime(order.sessionEndTime)
  return `${start}—${start.slice(0, 10) === end.slice(0, 10) ? end.slice(11) : end}`
}
export function ticketState(ticket, order, now = Date.now()) {
  if (order?.status === 'REFUNDING') return { label: '退款审核中', tone: 'muted', active: false }
  if (ticket?.status === 'CHECKED_IN') return { label: '已核销', tone: 'muted', active: false }
  if (ticket?.status === 'REFUNDED' || order?.status === 'REFUNDED') return { label: '已退款', tone: 'muted', active: false }
  if (!['PAID', 'COMPLETED'].includes(order?.status)) return { label: '不可使用', tone: 'muted', active: false }
  // Backend local date values use the venue's China time zone.
  const end = String(order?.sessionEndTime || '')
  const normalized = /[zZ]$|[+-]\d{2}:\d{2}$/.test(end) ? end : `${end}+08:00`
  if (end && Date.parse(normalized) <= now) return { label: '活动已结束', tone: 'muted', active: false }
  return ticket?.status === 'UNUSED'
    ? { label: '待使用', tone: 'available', active: true }
    : { label: '状态待确认', tone: 'muted', active: false }
}
export function orderLabel(status) {
  return { PENDING_PAYMENT: '待付款', PAID: '支付成功', COMPLETED: '已完成', CANCELLED: '已取消', EXPIRED: '已过期', REFUNDING: '退款审核中', REFUNDED: '已退款' }[status] || '状态待确认'
}
export function money(value) {
  return Number.isFinite(Number(value)) && value != null ? Number(value).toFixed(2) : '—'
}
