<template>
  <div class="chinese-date-input" :class="[{disabled,'time-only':type==='time','has-value':!!modelValue},attrs.class]" :style="attrs.style">
    <input ref="displayInput" v-bind="displayAttrs" class="date-display" type="text" readonly :value="displayValue" :placeholder="placeholder" :disabled="disabled" :aria-required="required||undefined" aria-haspopup="dialog" :aria-expanded="opened" @click="openPicker" @keydown.enter.prevent="openPicker" @keydown.space.prevent="openPicker" @keydown.down.prevent="openPicker" />
    <AppIcon class="date-input-icon" :name="type==='time'?'clock':'calendar'" size="16" />
    <button v-if="modelValue&&!disabled" type="button" class="date-clear" :aria-label="`清除${label}`" @click.stop="clearValue"><AppIcon name="close" size="13" /></button>
    <input ref="nativeInput" class="date-native" :data-native-for="attrs.id" :type="type" :value="modelValue" :min="min" :max="max" :step="step ?? (type==='date'?1:60)" :required="required" :disabled="disabled" tabindex="-1" aria-hidden="true" @input="nativeChanged" @invalid.prevent="openPicker" />
    <Teleport to="body">
      <div v-if="opened" class="date-picker-overlay" @mousedown.self="closePicker">
        <section ref="panel" class="date-picker-dialog" role="dialog" aria-modal="true" :aria-label="`选择${label}`" tabindex="-1" @keydown="dialogKeydown" @wheel="pickerWheel">
          <header><button ref="cancelButton" type="button" class="date-picker-cancel" @click="closePicker">取消</button><b>选择{{label}}</b><button type="button" class="date-picker-confirm" @click="confirm">确定</button></header>
          <div v-if="type!=='time'" class="date-picker-section"><p>日期（年 / 月 / 日）</p><DatePicker v-model="dateParts" :show-toolbar="false" :min-date="minDate" :max-date="maxDate" :formatter="formatter" :visible-option-num="5" :option-height="36" /></div>
          <div v-if="type!=='date'" class="date-picker-section"><p>时间（时 / 分）</p><TimePicker v-model="timeParts" :show-toolbar="false" :formatter="formatter" :visible-option-num="5" :option-height="36" /></div>
          <p v-if="pickerError" class="date-picker-error" role="alert">{{pickerError}}</p>
        </section>
      </div>
    </Teleport>
  </div>
</template>
<script setup>
import {computed,nextTick,ref,useAttrs,onUnmounted} from 'vue'
import {DatePicker,TimePicker} from 'vant'
import 'vant/es/style/base.css'
import 'vant/es/picker/index.css'
import 'vant/es/loading/index.css'
import AppIcon from './AppIcon.vue'
defineOptions({inheritAttrs:false})
const props=defineProps({modelValue:{type:String,default:''},type:{type:String,default:'datetime-local',validator:v=>['date','time','datetime-local'].includes(v)},disabled:Boolean,required:Boolean,min:String,max:String,step:[String,Number]})
const emit=defineEmits(['update:modelValue'])
const attrs=useAttrs(),opened=ref(false),displayInput=ref(null),nativeInput=ref(null),panel=ref(null),cancelButton=ref(null),pickerError=ref('')
const dateParts=ref([]),timeParts=ref([])
let wheelDeltas=new WeakMap()
const label=computed(()=>attrs['aria-label'] || ({date:'日期',time:'时间','datetime-local':'日期时间'}[props.type]))
const placeholder=computed(()=>({date:'年/月/日',time:'时/分','datetime-local':'年/月/日 时/分'}[props.type]))
const displayAttrs=computed(()=>Object.fromEntries(Object.entries(attrs).filter(([key])=>!['class','style','placeholder'].includes(key))))
const pad=v=>String(v).padStart(2,'0')
const displayValue=computed(()=>{
 const date=props.modelValue.match(/^(\d{4})-(\d{2})-(\d{2})/),time=props.modelValue.match(/(?:^|T)(\d{2}):(\d{2})/)
 const d=date?`${date[1]}年${date[2]}月${date[3]}日`:'',t=time?`${time[1]}时${time[2]}分`:''
 return props.type==='date'?d:props.type==='time'?t:d&&t?`${d} ${t}`:''
})
function makeDate(year,month,day){const d=new Date(2000,month,day);d.setFullYear(year);return d}
const minDate=computed(()=>{const m=props.min?.match(/^(\d{4})-(\d{2})-(\d{2})/);return m?makeDate(+m[1],+m[2]-1,+m[3]):makeDate(Math.max(1,Math.min(1900,Number(dateParts.value[0]||1900)-10)),0,1)})
const maxDate=computed(()=>{const m=props.max?.match(/^(\d{4})-(\d{2})-(\d{2})/);return m?makeDate(+m[1],+m[2]-1,+m[3]):makeDate(Math.min(9999,Math.max(new Date().getFullYear()+100,Number(dateParts.value[0]||0)+10)),11,31)})
const formatter=(type,option)=>({...option,text:`${option.value}${({year:'年',month:'月',day:'日',hour:'时',minute:'分'})[type] || ''}`})
async function openPicker(){
 if(props.disabled||opened.value)return
 const now=new Date(),d=props.modelValue.match(/^(\d{4})-(\d{2})-(\d{2})/),t=props.modelValue.match(/(?:^|T)(\d{2}):(\d{2})/)
 dateParts.value=d?d.slice(1,4):[String(now.getFullYear()),pad(now.getMonth()+1),pad(now.getDate())]
 timeParts.value=t?t.slice(1,3):[pad(now.getHours()),pad(now.getMinutes())]
 wheelDeltas=new WeakMap();pickerError.value='';opened.value=true;await nextTick();cancelButton.value?.focus()
}
async function closePicker(){opened.value=false;await nextTick();displayInput.value?.focus()}
async function update(value){
 emit('update:modelValue',value);await nextTick()
 // Keep delegated form error clearing and ordinary change listeners working.
 displayInput.value?.dispatchEvent(new Event('input',{bubbles:true}));displayInput.value?.dispatchEvent(new Event('change',{bubbles:true}))
}
async function confirm(){
 const date=dateParts.value.join('-'),time=timeParts.value.join(':'),value=props.type==='date'?date:props.type==='time'?time:`${date}T${time}`
 const native=nativeInput.value,previous=native.value;native.value=value
 if(!native.checkValidity()){pickerError.value='请选择有效的日期和时间，并符合此字段的范围要求';native.value=previous;return}
 await update(value);closePicker()
}
function clearValue(){if(!props.disabled)update('')}
function nativeChanged(e){update(e.target.value)}
// Vant's columns handle touch and option clicks, but do not handle desktop wheel events.
function pickerWheel(e){
 if(e.ctrlKey||!e.deltaY)return
 const column=e.target.closest?.('.van-picker-column')
 if(!column||!panel.value?.contains(column))return
 e.preventDefault();e.stopPropagation()
 const delta=e.deltaY*(e.deltaMode===1?16:e.deltaMode===2?180:1),stamp=performance.now(),previous=wheelDeltas.get(column)
 const sum=(previous&&stamp-previous.at<200&&Math.sign(previous.delta)===Math.sign(delta)?previous.delta:0)+delta
 wheelDeltas.set(column,{delta:sum,at:stamp})
 // Accumulate fine touchpad movement; one mouse notch advances one enabled option.
 if(Math.abs(sum)<24)return
 wheelDeltas.set(column,{delta:0,at:stamp})
 const options=[...column.querySelectorAll('.van-picker-column__item')],current=options.findIndex(item=>item.classList.contains('van-picker-column__item--selected'))
 if(current<0)return
 const direction=Math.sign(sum)
 let next=current+direction
 while(next>=0&&next<options.length&&options[next].classList.contains('van-picker-column__item--disabled'))next+=direction
 // Use the picker's normal option selection so date clamping and its change events still run.
 if(next>=0&&next<options.length)options[next].click()
}
function dialogKeydown(e){
 if(e.key==='Escape'){e.preventDefault();closePicker();return}
 if(e.key!=='Tab')return
 const list=[...panel.value.querySelectorAll('button:not(:disabled),[tabindex="0"],input:not(:disabled),select:not(:disabled)')]
 const first=list[0],last=list.at(-1)
 if(e.shiftKey&&document.activeElement===first){e.preventDefault();last?.focus()}
 else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first?.focus()}
}
onUnmounted(()=>{opened.value=false})
</script>
<style scoped>
.chinese-date-input{position:relative;display:block;min-width:0;width:100%;flex:1}.chinese-date-input input.date-display{width:100%;min-width:0;min-height:42px;box-sizing:border-box;padding:10px 52px 10px 12px;border:1px solid #dce4f2;border-radius:8px;background:#fff;color:#243b5b;font:inherit;font-size:13px;line-height:1.5;cursor:pointer;outline:none;text-overflow:ellipsis}.chinese-date-input input.date-display::placeholder{color:#91a0b7;opacity:1}.chinese-date-input input.date-display:focus{border-color:#2463eb;box-shadow:0 0 0 3px #2463eb0d}.date-input-icon{position:absolute;right:12px;top:50%;transform:translateY(-50%);color:#8192ad;pointer-events:none}.chinese-date-input .date-clear{position:absolute;right:29px;top:50%;transform:translateY(-50%);display:grid;place-items:center;width:24px;height:28px;padding:0!important;border:0!important;box-shadow:none!important;background:transparent!important;color:#9aa7bc;cursor:pointer;z-index:1}.chinese-date-input.disabled input.date-display{background:#f3f5f9;color:#a0abbd;cursor:not-allowed}.chinese-date-input input.date-native{position:absolute!important;width:1px!important;height:1px!important;min-width:0!important;min-height:0!important;padding:0!important;border:0!important;clip-path:inset(50%);opacity:0;pointer-events:none;left:0;bottom:0}
.time-only.has-value input.date-display{padding-right:30px}.time-only.has-value .date-input-icon{display:none}.time-only.has-value .date-clear{right:5px}
.date-picker-overlay{position:fixed;inset:0;z-index:2100;display:flex;align-items:center;justify-content:center;background:#172c4e66;padding:20px;font-family:inherit}.date-picker-dialog{width:400px;max-width:100%;max-height:calc(100dvh - 40px);overflow-y:auto;background:#fff;border-radius:16px;box-shadow:0 16px 60px #122d4b33;outline:none;color:#223957;--van-picker-option-font-size:15px;--van-picker-background:#fff;--van-picker-mask-color:linear-gradient(180deg,#ffffffeb,#ffffff66),linear-gradient(0deg,#ffffffeb,#ffffff66)}.date-picker-dialog header{display:flex;align-items:center;justify-content:space-between;padding:17px 16px;border-bottom:1px solid #edf1f7;gap:12px}.date-picker-dialog header b{font-size:15px}.date-picker-dialog header button{border:0;background:transparent;padding:5px 3px;font-size:13px;cursor:pointer}.date-picker-cancel{color:#8492aa}.date-picker-confirm{color:#2463eb;font-weight:600}.date-picker-section{padding:10px 12px 8px}.date-picker-section+.date-picker-section{border-top:1px solid #edf1f7}.date-picker-section>p{margin:5px 10px 8px;color:#8a9ab3;font-size:12px}.date-picker-error{margin:4px 20px 16px;font-size:12px;line-height:1.5;color:#c24f5d}@media(max-width:500px){.date-picker-overlay{align-items:flex-end;padding:12px}.date-picker-dialog{width:100%;max-height:calc(100dvh - 24px)}}
</style>
