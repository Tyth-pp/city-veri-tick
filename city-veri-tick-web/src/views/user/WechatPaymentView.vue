<template>
  <main class="wechat-page">
    <header class="wechat-header"><button class="wechat-back" type="button" aria-label="返回" @click="router.back()"><AppIcon name="arrow-left" /></button><h1>支付订单</h1><button class="wechat-menu" type="button" aria-label="刷新支付状态" :disabled="refreshing||paying" @click="refresh"><AppIcon name="refresh" size="18" /></button></header>
    <section v-if="loading" class="wechat-state">订单加载中…</section>
    <section v-else-if="error" class="wechat-state error" role="alert"><b>支付订单无法打开</b><p>{{error}}</p><button type="button" @click="load">重新加载</button></section>
    <template v-else-if="order">
      <section class="wechat-summary">
        <div v-if="pending&&timer.valid.value" class="payment-countdown" :class="countdownTone"><div class="countdown-line"><AppIcon name="clock" size="15" /><span class="time-label">剩余支付时间</span><output class="time-digits" aria-label="支付剩余时间" role="timer" aria-live="off">{{timer.text.value}}</output></div><p v-if="timer.expired.value">支付时间已结束，正在确认订单状态</p><p v-else-if="timer.seconds.value<=60">即将超时，请尽快完成支付</p><p v-else>超时未支付，订单将自动关闭</p></div>
        <div v-else-if="pending" class="payment-closed"><AppIcon name="info" size="17" /><span>无法获取支付时限，请刷新后支付</span></div>
        <div v-else class="payment-closed"><AppIcon :name="order.status==='EXPIRED'?'clock':'info'" size="18" /><span>{{statusText}}</span></div>
        <strong>¥{{amount}}</strong><span>订单号 {{order.orderNo || order.orderId || order.id}}</span>
      </section>
      <section class="wechat-method"><span class="wechat-logo"><AppIcon name="wechat" size="24" /></span><span>微信支付</span><i><AppIcon name="check" size="13" /></i></section>
      <p v-if="actionError" class="payment-action-error" role="alert">{{actionError}}</p>
      <p class="wechat-hint">模拟支付，不发生真实扣款。{{pending?'支付成功后将返回订单详情':'订单状态已更新，请返回详情查看'}}</p>
      <button class="wechat-confirm" type="button" :disabled="paying||refreshing" @click="primaryAction">{{primaryText}}</button>
    </template>
  </main>
</template>
<script setup>
import {computed,onMounted,onUnmounted,ref,watch} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {orderApi} from '../../api/order'
import AppIcon from '../../components/AppIcon.vue'
import {useRemindersStore} from '../../stores/reminders'
import {useOrderCountdown} from '../../composables/useOrderCountdown'
const route=useRoute(),router=useRouter(),reminders=useRemindersStore()
const order=ref(null),loading=ref(true),refreshing=ref(false),paying=ref(false),error=ref(''),actionError=ref(''),refreshFailed=ref(false)
const timer=useOrderCountdown(order)
const pending=computed(()=>order.value?.status==='PENDING_PAYMENT')
const amount=computed(()=>Number(order.value?.payAmount ?? order.value?.amount ?? 0).toFixed(2))
const statusText=computed(()=>({EXPIRED:'订单已超时',CANCELLED:'订单已取消',PAID:'已完成支付',COMPLETED:'已完成支付',REFUNDING:'订单正在退款处理中',REFUNDED:'订单已退款'}[order.value?.status] || '订单当前不可支付'))
const countdownTone=computed(()=>timer.seconds.value<=60?'critical':timer.seconds.value<=180?'warning':'normal')
const canPay=computed(()=>pending.value&&timer.valid.value&&timer.seconds.value>0&&!refreshFailed.value&&!paying.value&&!refreshing.value)
const primaryText=computed(()=>paying.value?'支付中…':refreshing.value?'正在同步订单…':!pending.value?'查看订单':!timer.valid.value?'重新获取支付时限':timer.expired.value?'刷新订单状态':refreshFailed.value?'刷新后继续支付':'确认支付')
let request=0,poll,lastExpiryAttempt=''
const orderPath=id=>`/orders/${id}`
function current(id,token){return id===String(route.params.orderId)&&token===sessionStorage.getItem('city-veri-tick-token')}
async function fetchOrder(synchronize=false,initial=false,preserveError=false){
  const version=++request,id=String(route.params.orderId),token=sessionStorage.getItem('city-veri-tick-token')
  if(initial){loading.value=true;order.value=null;error.value='';lastExpiryAttempt=''}
  if(!preserveError)actionError.value=''
  refreshing.value=true
  try{
    const data=await(synchronize?orderApi.synchronizeExpiry(id):orderApi.detail(id))
    if(version!==request||!current(id,token))return
    if(!data)throw new Error('订单不存在')
    order.value=data;refreshFailed.value=false
    if(['PAID','COMPLETED'].includes(data.status))router.replace(orderPath(id))
  }catch(e){if(version===request&&current(id,token)){refreshFailed.value=true;if(initial)error.value=e.message||'订单加载失败';else actionError.value=e.message||'订单状态同步失败，请重试'}}
  finally{if(version===request){loading.value=false;refreshing.value=false}}
}
function load(){return fetchOrder(false,true)}
function refresh(){if(paying.value||refreshing.value)return;timer.update();return fetchOrder(pending.value&&timer.expired.value)}
watch([timer.seconds,()=>order.value?.status,refreshing,paying],([seconds,status,busy,payingNow])=>{
  if(seconds>0){lastExpiryAttempt='';return}
  if(seconds!==0||status!=='PENDING_PAYMENT'||busy||payingNow)return
  const key=`${route.params.orderId}:${order.value.expiredAt}`
  if(lastExpiryAttempt===key)return
  lastExpiryAttempt=key;fetchOrder(true)
})
async function confirmPay(){
  timer.update()
  if(!canPay.value)return
  const id=String(route.params.orderId),token=sessionStorage.getItem('city-veri-tick-token'),account=reminders.accountId
  paying.value=true;actionError.value=''
  try{
    const paid=await orderApi.pay(id)
    if(!current(id,token))return
    if(!['PAID','COMPLETED'].includes(paid?.status))throw new Error('支付结果尚未确认，请刷新订单状态')
    order.value=paid
    if(account===reminders.accountId)reminders.confirmIssued(paid)
    router.replace(orderPath(id))
  }catch(e){if(current(id,token)){actionError.value=e.message||'支付失败';timer.update();await fetchOrder(timer.expired.value,false,true)}}
  finally{paying.value=false}
}
function primaryAction(){if(paying.value||refreshing.value)return;if(!pending.value){router.replace(orderPath(String(route.params.orderId)));return}timer.update();if(!canPay.value){fetchOrder(timer.expired.value);return}confirmPay()}
function visible(){if(document.visibilityState==='visible'&&!paying.value&&!refreshing.value){timer.update();fetchOrder(pending.value&&timer.expired.value)}}
watch(()=>route.params.orderId,load,{immediate:true})
onMounted(()=>{document.addEventListener('visibilitychange',visible);window.addEventListener('focus',visible);poll=setInterval(visible,60000)})
onUnmounted(()=>{request++;clearInterval(poll);document.removeEventListener('visibilitychange',visible);window.removeEventListener('focus',visible)})
</script>
<style scoped>
.wechat-page{min-height:100vh;background:#f5f6fa;color:#202124;padding-bottom:92px}.wechat-header{height:150px;background:#202124;color:#fff;display:flex;align-items:center;justify-content:center;position:relative}.wechat-header h1{font-size:30px;margin:0}.wechat-header .wechat-back{position:absolute;left:25px;top:67px;border:0;background:transparent;color:#fff;font-size:54px;line-height:1}.wechat-menu{position:absolute;right:28px;top:42px;width:76px;height:54px;border:2px solid #85878b;border-radius:28px;display:grid;place-items:center;font-size:26px;letter-spacing:5px}.wechat-summary{height:302px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:17px}.wechat-summary small,.wechat-summary span{color:#9da1aa;font-size:22px}.wechat-summary strong{font-size:68px;line-height:1}.wechat-method{height:90px;background:#fff;display:flex;align-items:center;gap:18px;padding:0 36px;font-size:28px}.wechat-logo{display:grid;place-items:center;width:42px;height:42px;color:#1aad19}.wechat-method i{margin-left:auto;width:30px;height:30px;border-radius:50%;display:grid;place-items:center;background:#ffcc00;color:#4b3b00;font-style:normal;font-size:20px}.wechat-hint{text-align:center;color:#a0a4ad;font-size:24px;margin:190px 20px 0}.wechat-confirm{position:fixed;left:24px;right:24px;bottom:48px;height:82px;border:0;border-radius:10px;background:#ffc400;color:#382d00;font-size:29px;font-weight:700}.wechat-confirm:disabled{opacity:.8}.wechat-state{padding:80px 24px;text-align:center;color:#8b909a}.wechat-state.error{color:#c53a43}.wechat-state p{color:#8b909a}.wechat-state button{border:0;border-radius:8px;padding:10px 18px;background:#2161e8;color:#fff}
@media (max-width:640px){.wechat-header{height:76px}.wechat-header h1{font-size:18px}.wechat-header .wechat-back{left:20px;top:24px;font-size:34px}.wechat-menu{right:16px;top:17px;width:46px;height:34px;font-size:16px}.wechat-summary{height:235px}.wechat-summary small,.wechat-summary span{font-size:12px}.wechat-summary strong{font-size:40px}.wechat-method{height:64px;padding:0 18px;font-size:16px}.wechat-logo{width:30px;height:30px}.wechat-method i{width:18px;height:18px;font-size:12px}.wechat-hint{font-size:13px;margin-top:170px}.wechat-confirm{height:48px;bottom:22px;font-size:17px}}
.wechat-header .wechat-back,.wechat-menu,.wechat-method,.wechat-method i{display:flex;align-items:center;justify-content:center}.wechat-method .wechat-logo{display:flex}.wechat-method i{flex:none}</style>

<style scoped>
.wechat-menu{background:transparent;color:inherit;cursor:pointer;padding:0}.wechat-menu:disabled{opacity:.45;cursor:not-allowed}.payment-countdown{text-align:center}.countdown-line{display:inline-flex;align-items:center;justify-content:center;gap:8px;padding:8px 14px;border:1px solid #e2e6ee;border-radius:24px;background:#fff9;color:#8c98aa}.payment-countdown .time-label{font-size:12px;color:#8793a5}.time-digits{font-size:21px;font-weight:650;font-variant-numeric:tabular-nums;letter-spacing:1px;color:#58677d;line-height:1}.payment-countdown p{font-size:11px;color:#a0a8b6;margin:8px 0 0;line-height:1.5}.payment-countdown.warning .countdown-line{color:#d09324;background:#fff8e8;border-color:#f5e7c4}.warning .time-digits{color:#c7891e}.payment-countdown.critical .countdown-line{color:#df655e;background:#fff1ef;border-color:#f8d9d4}.critical .time-digits{color:#db534b}.critical p{color:#c78981}.payment-closed{display:flex;align-items:center;justify-content:center;gap:7px;color:#8d98a9;font-size:13px;line-height:1.5;margin:0 18px;text-align:center}.payment-action-error{margin:18px 20px;padding:12px 14px;background:#fff0ef;border:1px solid #f5dcd9;border-radius:9px;font-size:12px;line-height:1.6;text-align:center;color:#bd615d}.wechat-summary{min-height:235px;height:auto;padding:38px 12px 32px}.wechat-summary>span{overflow-wrap:anywhere;text-align:center;max-width:100%}.wechat-confirm:disabled{opacity:.5;cursor:not-allowed}
</style>
