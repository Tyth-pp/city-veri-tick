import {it,expect} from 'vitest'
import {ticketState,orderLabel} from './ticketPresentation'
it('refund review suspends credentials and has a clear status',()=>{
 expect(ticketState({status:'UNUSED'},{status:'REFUNDING'})).toMatchObject({label:'退款审核中',active:false})
 expect(orderLabel('REFUNDING')).toBe('退款审核中')
})
