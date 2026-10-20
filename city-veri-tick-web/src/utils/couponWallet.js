import { couponCategories } from './couponForm'
export const couponTabs = [{key:'unused',label:'未使用',countKey:'unusedCount'},{key:'used',label:'已使用',countKey:'usedCount'},{key:'invalid',label:'已失效',countKey:'invalidCount'}]
export function couponDestination(c) {
  if(c.scopeType === 'ACTIVITY' && c.activityId) return {path:`/activities/${c.activityId}`}
  return {path:'/home',query:{couponName:c.name,...(c.categoryCodes?.length ? {couponCategories:c.categoryCodes.join(',')} : {})}}
}
export function couponCategoryFilter(value) {
  const codes = String(value || '').split(',')
  return couponCategories.filter(item=>codes.includes(item.code))
}
export const formatCouponTime = value => value ? String(value).slice(0,16).replace('T',' ') : '—'
