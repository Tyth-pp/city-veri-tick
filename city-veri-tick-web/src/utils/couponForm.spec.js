import { describe, it, expect } from 'vitest'
import { validateCouponForm, couponPayload } from './couponForm'
const valid = () => ({name:'城市优惠券',couponType:'FULL_REDUCTION',discountAmount:20,minOrderAmount:100,minPayAmount:.01,totalQuantity:100,perUserLimit:1,scopeType:'ALL',categoryCodes:[],startsAt:'2030-01-01T10:00',endsAt:'2030-01-02T10:00',useStartsAt:'2030-01-01T10:00',useEndsAt:'2030-01-03T10:00',refundPolicy:'RETURN'})
describe('coupon form business validation',()=>{
 it('returns individual field errors for amount and time',()=>{const f=valid();f.discountAmount=150;f.useEndsAt=f.useStartsAt;const e=validateCouponForm(f);expect(e.minOrderAmount).toBeTruthy();expect(e.useEndsAt).toBeTruthy()})
 it('requires a cap for discounts and an activity for specific scope',()=>{const f={...valid(),couponType:'DISCOUNT',discountRate:8.5,scopeType:'ACTIVITY'};const e=validateCouponForm(f);expect(e.maxDiscountAmount).toBeTruthy();expect(e.activityId).toBeTruthy();expect(e.discountRate).toBeFalsy()})
 it('normalizes hidden fields and translates 8.5 fold to 0.85',()=>{const f={...valid(),couponType:'DISCOUNT',discountRate:8.5,maxDiscountAmount:100,scopeType:'ACTIVITY',activityId:'123',categoryCodes:['MOVIE']};const p=couponPayload(f);expect(p.discountRate).toBe(.85);expect(p.discountAmount).toBeNull();expect(p.categoryCodes).toEqual([]);expect(p.startsAt).toBe('2030-01-01T10:00:00')})
 it('no threshold has no minimum order value',()=>{expect(couponPayload({...valid(),couponType:'NO_THRESHOLD'}).minOrderAmount).toBe(0)})
})

it('accepts limited/flash issuance independently from discount type',()=>{const f={...valid(),claimMode:'FLASH_SALE'};expect(validateCouponForm(f).claimMode).toBeUndefined();expect(couponPayload(f).claimMode).toBe('FLASH_SALE');expect(validateCouponForm({...f,claimMode:'BAD'}).claimMode).toBeTruthy()})

it('uses explicit unlimited selections without treating an entered zero cap as unlimited',()=>{
 const f={...valid(),couponType:'DISCOUNT',discountRate:9,maxDiscountAmount:0,noDiscountCap:true,noPerUserLimit:true};expect(validateCouponForm(f)).toEqual({});expect(couponPayload(f)).toMatchObject({maxDiscountAmount:null,perUserLimit:0});expect(couponPayload(f)).not.toHaveProperty('noDiscountCap');expect(validateCouponForm({...f,noDiscountCap:false}).maxDiscountAmount).toBeTruthy();expect(validateCouponForm({...valid(),totalQuantity:1000,perUserLimit:500})).toEqual({})
})
