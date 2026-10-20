import {computed,onMounted,onUnmounted,ref,watch} from 'vue'

// Historic local timestamps are China time; new order timestamps include an explicit offset.
export function orderTimeMillis(value) {
  if(typeof value !== 'string' || !value.trim())return NaN
  const text=value.trim().replace(' ','T')
  return Date.parse(/[zZ]$|[+-]\d{2}:?\d{2}$/.test(text)?text:text+'+08:00')
}
export function useOrderCountdown(order) {
  const deadline=ref(NaN),now=ref(NaN)
  let serverAnchor=NaN,monotonicAnchor=0,interval
  function update(){if(Number.isFinite(serverAnchor))now.value=serverAnchor+Math.max(0,performance.now()-monotonicAnchor)}
  watch(order,value=>{
    deadline.value=orderTimeMillis(value?.expiredAt)
    serverAnchor=orderTimeMillis(value?.serverTime)
    monotonicAnchor=performance.now();now.value=serverAnchor
  },{immediate:true,flush:'sync'})
  const valid=computed(()=>Number.isFinite(deadline.value)&&Number.isFinite(now.value))
  const seconds=computed(()=>valid.value?Math.max(0,Math.ceil((deadline.value-now.value)/1000)):null)
  const text=computed(()=>seconds.value===null?'—':`${String(Math.floor(seconds.value/60)).padStart(2,'0')}:${String(seconds.value%60).padStart(2,'0')}`)
  const expired=computed(()=>valid.value&&seconds.value===0)
  onMounted(()=>{interval=setInterval(update,250)})
  onUnmounted(()=>clearInterval(interval))
  return {seconds,text,valid,expired,update}
}
