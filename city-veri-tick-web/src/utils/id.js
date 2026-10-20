export function asId(value) {
  return value === null || value === undefined || value === '' ? '' : String(value)
}

export function orderIdOf(order) {
  return asId(order?.orderId ?? order?.id)
}
