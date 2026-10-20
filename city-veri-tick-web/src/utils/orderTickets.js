export function unusedTickets(order) {
  return Array.isArray(order?.tickets)
    ? order.tickets.filter(ticket => String(ticket.status || '').toUpperCase() === 'UNUSED')
    : []
}

export function isPendingUse(order) {
  return String(order?.status || '').toUpperCase() === 'PAID' && unusedTickets(order).length > 0
}
