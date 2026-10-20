export function ticketSaleState(ticket, now = Date.now()) {
  if (!ticket || ticket.enabled === false) return 'UNAVAILABLE'
  const start = Date.parse(ticket.saleStartTime), end = Date.parse(ticket.saleEndTime)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start || now > end) return 'UNAVAILABLE'
  if (now < start) return 'UPCOMING'
  return Number(ticket.availableStock ?? ticket.stock ?? 0) > 0 ? 'AVAILABLE' : 'SOLD_OUT'
}

export function formatOpening(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}
