export const couponCategories = [
  {code:'MOVIE',label:'电影'}, {code:'CONCERT',label:'演唱会'}, {code:'PERFORMANCE',label:'演出'},
  {code:'STAND_UP',label:'脱口秀'}, {code:'EXHIBITION_ART',label:'展览艺术'}, {code:'MUSIC_FESTIVAL',label:'音乐节'},
  {code:'OPERA',label:'戏曲艺术'}, {code:'SPORTS',label:'体育运动'}, {code:'THEATRE_OPERA',label:'话剧歌剧'}
]
export const emptyCouponForm = () => ({name:'',description:'',couponType:'FULL_REDUCTION',claimMode:'NORMAL',discountAmount:'',discountRate:8.5,maxDiscountAmount:'',noDiscountCap:false,noPerUserLimit:false,minOrderAmount:'',minPayAmount:.01,totalQuantity:'',perUserLimit:1,startsAt:'',endsAt:'',useStartsAt:'',useEndsAt:'',scopeType:'ALL',activityId:'',categoryCodes:[],refundPolicy:'RETURN'})
const money = (v, positive = true) => v !== '' && v != null && Number.isFinite(Number(v)) && Number(v) <= 99999999.99 && (positive ? Number(v) > 0 : Number(v) >= 0) && /^\d+(\.\d{1,2})?$/.test(String(v))
export function validateCouponForm(f) {
  const e = {}
  if (!['NORMAL','LIMITED','FLASH_SALE'].includes(f.claimMode ?? 'NORMAL')) e.claimMode = '请选择有效的发放方式'
  if (!f.name?.trim()) e.name = '请填写优惠券名称'
  else if (f.name.trim().length > 64) e.name = '名称不能超过64字'
  if (!['FULL_REDUCTION','DISCOUNT','NO_THRESHOLD'].includes(f.couponType)) e.couponType = '请选择券类型'
  if (f.couponType === 'DISCOUNT') {
    if (!(Number(f.discountRate) >= 0.1 && Number(f.discountRate) < 10) || !/^\d+(\.\d{1,2})?$/.test(String(f.discountRate))) e.discountRate = '折扣范围为0.1至9.99，最多两位小数，例如8.5'
    if (!f.noDiscountCap && !money(f.maxDiscountAmount)) e.maxDiscountAmount = '请填写优惠封顶金额，最多两位小数'
  } else if (!money(f.discountAmount)) e.discountAmount = '请填写优惠金额，最多两位小数'
  if (f.couponType !== 'NO_THRESHOLD' && !money(f.minOrderAmount, false)) e.minOrderAmount = '请填写合法的使用门槛'
  if (f.couponType === 'FULL_REDUCTION' && (!(Number(f.minOrderAmount) > 0) || Number(f.minOrderAmount) < Number(f.discountAmount))) e.minOrderAmount = '满减门槛必须大于0且不能小于优惠金额'
  if (!money(f.minPayAmount)) e.minPayAmount = '最低实付至少0.01元，最多两位小数'
  if (!(Number.isInteger(Number(f.totalQuantity)) && Number(f.totalQuantity) > 0 && Number(f.totalQuantity) <= 2147483647)) e.totalQuantity = '请填写大于0的整数'
  if (!f.noPerUserLimit && !(Number.isInteger(Number(f.perUserLimit)) && Number(f.perUserLimit) >= 1 && Number(f.perUserLimit) <= Number(f.totalQuantity))) e.perUserLimit = '每人限领须为正整数，且不能超过发放总数量'
  for (const k of ['startsAt','endsAt','useStartsAt','useEndsAt']) if (!f[k] || !Number.isFinite(new Date(f[k]).getTime())) e[k] = '请选择日期和时间'
  if (!e.endsAt && !e.startsAt && f.endsAt <= f.startsAt) e.endsAt = '领券结束时间必须晚于开始时间'
  if (!e.useEndsAt && !e.useStartsAt && f.useEndsAt <= f.useStartsAt) e.useEndsAt = '使用结束时间必须晚于开始时间'
  if (!e.useEndsAt && !e.endsAt && f.useEndsAt < f.endsAt) e.useEndsAt = '使用结束不能早于领券结束'
  if (f.scopeType === 'ACTIVITY' && !f.activityId) e.activityId = '请选择一个适用活动'
  if ((f.description || '').length > 255) e.description = '说明不能超过255字'
  return e
}
const date = v => v.length === 16 ? v + ':00' : v
export function couponPayload(f) {
  const {noDiscountCap,noPerUserLimit,...fields}=f
  return {...fields, claimMode:f.claimMode || 'NORMAL', name:f.name.trim(), discountAmount:f.couponType === 'DISCOUNT' ? null : Number(f.discountAmount),
    discountRate:f.couponType === 'DISCOUNT' ? Number((Number(f.discountRate)/10).toFixed(4)) : null,
    maxDiscountAmount:f.couponType === 'DISCOUNT' && !noDiscountCap ? Number(f.maxDiscountAmount) : null,
    minOrderAmount:f.couponType === 'NO_THRESHOLD' ? 0 : Number(f.minOrderAmount), minPayAmount:Number(f.minPayAmount),
    totalQuantity:Number(f.totalQuantity),perUserLimit:noPerUserLimit ? 0 : Number(f.perUserLimit),
    activityId:f.scopeType === 'ACTIVITY' ? String(f.activityId) : null, categoryCodes:f.scopeType === 'ACTIVITY' ? [] : [...f.categoryCodes],
    startsAt:date(f.startsAt),endsAt:date(f.endsAt),useStartsAt:date(f.useStartsAt),useEndsAt:date(f.useEndsAt)}
}
export const couponValue = c => c.couponType === 'DISCOUNT' ? `${Number((Number(c.discountRate)*10).toFixed(2))}折` : `¥${Number(c.discountAmount)}`
export const couponThreshold = c => c.couponType === 'NO_THRESHOLD' || !Number(c.minOrderAmount) ? '无门槛' : `满 ¥${Number(c.minOrderAmount)} 可用`
export const couponScope = c => c.scopeType === 'ACTIVITY' ? `仅限：${c.activityTitle || '指定活动'}` : c.categoryCodes?.length ? c.categoryCodes.map(code=>couponCategories.find(x=>x.code===code)?.label || code).join('、') : '全部活动可用'

export const couponClaimMode = c => ({NORMAL:'普通领取',LIMITED:'限量领取',FLASH_SALE:'定时秒杀'}[c.claimMode || 'NORMAL'] || '普通领取')

export const couponCapLabel = c => c.maxDiscountAmount == null ? '优惠不封顶' : `最多减免 ¥${Number(c.maxDiscountAmount)}`
export const couponLimitLabel = c => Number(c.perUserLimit) === 0 && c.perUserLimit != null ? '每人不限领取次数' : `每人限领 ${c.perUserLimit ?? 1} 张`
