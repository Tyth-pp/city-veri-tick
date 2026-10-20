import {it,expect} from 'vitest'
import {couponDestination,couponCategoryFilter} from './couponWallet'
it('preserves a specific activity ID as a string',()=>{expect(couponDestination({scopeType:'ACTIVITY',activityId:'2109223140146241537'})).toEqual({path:'/activities/2109223140146241537'})})
it('keeps multiple category codes for OR filtering',()=>{const target=couponDestination({scopeType:'ALL',categoryCodes:['CONCERT','MOVIE'],name:'两类专享券'});expect(target.query.couponCategories).toBe('CONCERT,MOVIE');expect(couponCategoryFilter(target.query.couponCategories).map(c=>c.code)).toEqual(['MOVIE','CONCERT']);expect(couponCategoryFilter('UNKNOWN')).toEqual([])})
