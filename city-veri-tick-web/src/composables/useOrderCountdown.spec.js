import {describe,it,expect,vi,afterEach} from 'vitest'
import {ref} from 'vue'
import {mount} from '@vue/test-utils'
import {useOrderCountdown} from './useOrderCountdown'
let wrapper
function setup(order){let timer;wrapper=mount({setup(){timer=useOrderCountdown(ref(order));return()=>null}});return timer}
afterEach(()=>{wrapper?.unmount();vi.useRealTimers()})
describe('server calibrated order countdown',()=>{
 it('uses server time rather than a device clock that is one day ahead',()=>{vi.useFakeTimers({toFake:['setInterval','clearInterval','performance','Date']});vi.setSystemTime(new Date('2030-01-02T00:00:00Z'));const timer=setup({expiredAt:'2030-01-01T08:15:00+08:00',serverTime:'2030-01-01T00:00:00Z'});expect(timer.seconds.value).toBe(900);expect(timer.text.value).toBe('15:00');vi.advanceTimersByTime(61000);expect(timer.text.value).toBe('13:59')})
 it('clamps at zero after a long pause and treats the exact cutoff as expired',()=>{vi.useFakeTimers({toFake:['setInterval','clearInterval','performance']});const timer=setup({expiredAt:'2030-01-01T08:00:02',serverTime:'2030-01-01T00:00:00Z'});vi.advanceTimersByTime(3000);expect(timer.seconds.value).toBe(0);expect(timer.text.value).toBe('00:00');expect(timer.expired.value).toBe(true)})
 it('does not start a fictitious fifteen minutes if the server timestamp is missing',()=>{const timer=setup({expiredAt:'2030-01-01T08:15:00+08:00'});expect(timer.valid.value).toBe(false);expect(timer.seconds.value).toBeNull();expect(timer.text.value).toBe('—')})
})
