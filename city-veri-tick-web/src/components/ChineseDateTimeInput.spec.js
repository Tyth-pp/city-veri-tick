import {mount,flushPromises} from '@vue/test-utils'
import {nextTick} from 'vue'
import {it,expect,afterEach,vi} from 'vitest'
import {DatePicker,TimePicker} from 'vant'
import Input from './ChineseDateTimeInput.vue'
const mountInput=options=>mount(Input,{...options,global:{stubs:{teleport:true}}})
let w
const wheel=async(column,deltaY)=>{column.element.dispatchEvent(new WheelEvent('wheel',{deltaY,bubbles:true,cancelable:true}));await flushPromises()}
afterEach(()=>{w?.unmount();document.body.innerHTML=''})
it.each([['datetime-local','年/月/日 时/分'],['date','年/月/日'],['time','时/分']])('shows Chinese placeholders for %s independently of browser locale',async(type,text)=>{w=mountInput({props:{type,modelValue:''}});expect(w.get('.date-display').attributes('placeholder')).toBe(text)})
it('formats a value in Chinese without timezone conversion',()=>{w=mountInput({props:{modelValue:'2030-10-18T19:30'}});expect(w.get('.date-display').element.value).toBe('2030年10月18日 19时30分')})
it('commits picker selections in the existing local ISO format and preserves cancel',async()=>{
 w=mountInput({attachTo:document.body,props:{modelValue:'2030-10-18T19:30'}});await w.get('.date-display').trigger('click');await nextTick()
 w.findComponent(DatePicker).vm.$emit('update:modelValue',['2030','10','20']);w.findComponent(TimePicker).vm.$emit('update:modelValue',['09','05']);await nextTick();await w.get('.date-picker-confirm').trigger('click')
 expect(w.emitted('update:modelValue').at(-1)).toEqual(['2030-10-20T09:05']);await w.get('.date-display').trigger('click');await w.get('.date-picker-cancel').trigger('click');expect(w.emitted('update:modelValue')).toHaveLength(1)
})
it('clears the real bound value and blocks opening while disabled',async()=>{
 w=mountInput({props:{type:'date',modelValue:'2030-10-18'}});await w.get('.date-clear').trigger('click');expect(w.emitted('update:modelValue').at(-1)).toEqual(['']);await w.setProps({disabled:true});await w.get('.date-display').trigger('click');expect(w.find('[role="dialog"]').exists()).toBe(false)
})
it('leaves unconfirmed defaults out of the model and closes with Escape',async()=>{
 w=mountInput({attachTo:document.body,props:{modelValue:''}});await w.get('.date-display').trigger('click');await w.get('.date-picker-dialog').trigger('keydown',{key:'Escape'});expect(w.emitted('update:modelValue')).toBeUndefined();expect(w.find('[role="dialog"]').exists()).toBe(false)
})

it('enforces a datetime boundary before committing the picker value',async()=>{
 w=mountInput({props:{modelValue:'2030-10-18T19:30',min:'2030-10-18T19:00'}});await w.get('.date-display').trigger('click')
 w.findComponent(TimePicker).vm.$emit('update:modelValue',['18','30']);await nextTick();await w.get('.date-picker-confirm').trigger('click')
 expect(w.emitted('update:modelValue')).toBeUndefined();expect(w.get('[role="alert"]').text()).toContain('范围要求')
 w.findComponent(TimePicker).vm.$emit('update:modelValue',['19','30']);await nextTick();await w.get('.date-picker-confirm').trigger('click')
 expect(w.emitted('update:modelValue').at(-1)).toEqual(['2030-10-18T19:30'])
})

it('moves the hovered column with a wheel event and commits the actual selection',async()=>{
 w=mountInput({attachTo:document.body,props:{modelValue:'2030-10-18T19:30'}});await w.get('.date-display').trigger('click')
 const column=i=>w.findAll('.van-picker-column')[i]
 await wheel(column(0),120);await wheel(column(3),-120);await column(4).trigger('wheel',{deltaY:120})
 expect(column(0).get('.van-picker-column__item--selected').text()).toBe('2031年');expect(column(3).get('.van-picker-column__item--selected').text()).toBe('18时');expect(w.emitted('update:modelValue')).toBeUndefined()
 await w.get('.date-picker-confirm').trigger('click');expect(w.emitted('update:modelValue').at(-1)).toEqual(['2031-10-18T18:31'])
})
it('clamps month/day transitions and time boundaries while supporting fine touchpad deltas',async()=>{
 w=mountInput({attachTo:document.body,props:{modelValue:'2030-01-31T23:59'}});await w.get('.date-display').trigger('click');const column=i=>w.findAll('.van-picker-column')[i]
 await wheel(column(1),120);expect(column(2).get('.van-picker-column__item--selected').text()).toBe('28日')
 await wheel(column(3),120);expect(column(3).get('.van-picker-column__item--selected').text()).toBe('23时')
 await wheel(column(4),-12);expect(column(4).get('.van-picker-column__item--selected').text()).toBe('59分');await wheel(column(4),-12);expect(column(4).get('.van-picker-column__item--selected').text()).toBe('58分')
 await w.get('.date-picker-cancel').trigger('click');expect(w.emitted('update:modelValue')).toBeUndefined()
})
