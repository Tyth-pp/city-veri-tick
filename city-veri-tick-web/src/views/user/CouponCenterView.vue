<template>
<main class="page coupon-page">
  <header class="topbar"><button class="back" aria-label="返回" @click="$router.back()"><AppIcon name="arrow-left" /></button><h1>优惠券中心</h1><RouterLink to="/coupons/mine" class="my-coupons-top">我的券</RouterLink></header>
  <section class="coupon-intro"><b>领券与限时抢券</b><p>领取后可在确认订单时使用，一单限用一张</p><button class="refresh-coupons" :disabled="loading" @click="load">{{loading?'刷新中…':'刷新优惠券'}}</button></section>
  <p v-if="error" class="error" role="alert">{{error}} <button @click="load">重试</button></p>
  <p v-if="notice" class="notice" role="status">{{notice}} <RouterLink to="/coupons/mine">查看我的优惠券 <AppIcon name="chevron-right" size="13" /></RouterLink></p>
  <section v-if="loading" class="state">优惠券加载中…</section>
  <section v-else-if="!coupons.length&&!error" class="empty">暂无可领取优惠券</section>
  <section v-for="c in coupons" :key="c.id" class="coupon-card">
    <div class="coupon-main"><div class="amount"><strong>{{couponValue(c)}}</strong><small>{{couponThreshold(c)}}</small></div>
      <div class="coupon-info"><span class="claim-mode" v-if="c.claimMode && c.claimMode!=='NORMAL'">{{couponClaimMode(c)}}</span><b>{{c.name}}</b><p>{{couponScope(c)}}</p><small v-if="c.couponType==='DISCOUNT'">{{couponCapLabel(c)}}</small><small>最低实付 ¥{{c.minPayAmount ?? '0.01'}} · {{couponLimitLabel(c)}}</small></div>
      <button :disabled="busy!==null || claimState(c)!=='AVAILABLE'" @click="claim(c)">{{busy===c.id?'领取中…':buttonText(c)}}</button>
    </div>
    <div class="claim-progress" v-if="c.claimMode && c.claimMode!=='NORMAL'"><div><span>{{claimState(c)==='UPCOMING'?'距开抢 '+countdown(c):claimState(c)==='ENDED'?'本轮领取已结束':'剩余 '+Math.max(0,Number(c.totalQuantity)-Number(c.claimedQuantity || 0))+' 张'}}</span><small>总量 {{c.totalQuantity}} 张</small></div><progress :value="Number(c.claimedQuantity || 0)" :max="Number(c.totalQuantity)" :aria-label="c.name+'领取进度'"></progress></div>
    <div class="coupon-details"><p><span>领取时间</span>{{format(c.startsAt)}} — {{format(c.endsAt)}}</p><p><span>使用有效期</span>{{format(c.useStartsAt || c.startsAt)}} — {{format(c.useEndsAt || c.endsAt)}}</p><p>{{c.refundPolicy==='NO_RETURN'?'退款成功后优惠券不退回':'退款成功后，未过期且仍有效的优惠券退回'}}</p><p v-if="c.description">{{c.description}}</p></div>
  </section>
</main>
</template>
<script setup>
import {onMounted,onUnmounted,ref} from 'vue'
import AppIcon from '../../components/AppIcon.vue'
import {couponApi} from '../../api/coupon'
import {couponValue,couponThreshold,couponScope,couponClaimMode,couponCapLabel,couponLimitLabel} from '../../utils/couponForm'
const coupons=ref([]),loading=ref(false),error=ref(''),notice=ref(''),busy=ref(null),now=ref(Date.now())
let offset=0,timer,poll,request=0
const boundaries=new Set()
const format=v=>v?String(v).slice(0,16).replace('T',' '):'—'
const millis=v=>v?Date.parse(/(?:Z|[+-]\d{2}:\d{2})$/.test(v)?v:String(v)+'+08:00'):NaN
function claimState(c){
 if(Number(c.perUserLimit ?? 1)>0 && Number(c.userClaimedQuantity || 0)>=Number(c.perUserLimit ?? 1))return 'LIMIT_REACHED'
 if(now.value>=millis(c.endsAt))return 'ENDED'
 if(now.value<millis(c.startsAt))return 'UPCOMING'
 if(Number(c.claimedQuantity || 0)>=Number(c.totalQuantity ?? Infinity))return 'SOLD_OUT'
 return 'AVAILABLE'
}
function buttonText(c){const state=claimState(c);return {UPCOMING:'未开始',LIMIT_REACHED:'已达限领',SOLD_OUT:'已抢光',ENDED:'已结束'}[state] || (c.claimMode==='FLASH_SALE'?'立即抢券':'领取')}
function countdown(c){const seconds=Math.max(0,Math.ceil((millis(c.startsAt)-now.value)/1000)),days=Math.floor(seconds/86400),hours=Math.floor(seconds%86400/3600),minutes=Math.floor(seconds%3600/60);return (days?days+'天 ':'')+[hours,minutes,seconds%60].map(v=>String(v).padStart(2,'0')).join(':')}
async function load(){
 const current=++request;loading.value=true;error.value=''
 try{const data=await couponApi.available()||[];if(current!==request)return;coupons.value=data;const server=Date.parse(data[0]?.serverTime);offset=Number.isFinite(server)?server-Date.now():0;now.value=Date.now()+offset}
 catch(e){if(current===request)error.value=e.message||'优惠券加载失败'}finally{if(current===request)loading.value=false}
}
async function claim(c){
 if(busy.value!==null||claimState(c)!=='AVAILABLE')return
 busy.value=c.id;error.value='';notice.value=''
 try{await couponApi.claim(c.id);c.userClaimedQuantity=Number(c.userClaimedQuantity || 0)+1;notice.value='领取成功，可在确认订单时选择使用';await load()}
 catch(e){const message=e.message||'领取失败，请在我的优惠券确认结果';await load();error.value=message}
 finally{busy.value=null}
}
function visibleRefresh(){if(!document.hidden)load()}
onMounted(()=>{
 load();timer=setInterval(()=>{
  now.value=Date.now()+offset
  if(document.hidden||loading.value)return
  for(const c of coupons.value){for(const [kind,value] of [['start',c.startsAt],['end',c.endsAt]]){const at=millis(value),key=c.id+':'+kind+':'+value;if(Number.isFinite(at)&&now.value>=at&&!boundaries.has(key)){boundaries.add(key);if(kind==='end'||c.claimState==='UPCOMING'){load();return}}}}
 },1000)
 poll=setInterval(visibleRefresh,30000);document.addEventListener('visibilitychange',visibleRefresh);window.addEventListener('focus',visibleRefresh)
})
onUnmounted(()=>{request++;clearInterval(timer);clearInterval(poll);document.removeEventListener('visibilitychange',visibleRefresh);window.removeEventListener('focus',visibleRefresh)})
</script>
<style scoped>
.coupon-page{min-height:100vh;background:#f6f8fc;padding-bottom:calc(88px + env(safe-area-inset-bottom))}.topbar{display:flex;align-items:center;justify-content:space-between;padding:14px 18px;background:#fff;border-bottom:1px solid #e7ebf3}.topbar h1{font-size:18px;margin:0}.topbar>span,.back{width:26px}.back{border:0;background:none;color:#1e3151;padding:0}.coupon-intro{padding:25px 20px 15px}.coupon-intro b{font-size:23px}.coupon-intro p{color:#72819a;margin:7px 0;font-size:13px;line-height:1.6}.coupon-card{margin:12px 16px;background:#fff;border-radius:14px;padding:18px 14px;box-shadow:0 4px 15px #183d7210}.coupon-main{display:flex;align-items:center;gap:12px}.amount{width:80px;flex-shrink:0;text-align:center;color:#2362e7;border-right:1px dashed #d7e0f2;padding-right:8px}.amount strong{font-size:26px;display:block}.amount small,.coupon-info small{color:#7d8ca4;font-size:11px;line-height:1.5}.coupon-info{flex:1;min-width:0}.coupon-info b{font-size:15px}.coupon-info p{margin:6px 0;color:#687993;font-size:12px;line-height:1.5}.coupon-info small{display:block}.coupon-card button{border:0;border-radius:18px;padding:9px 12px;color:#fff;background:#2465e8;white-space:nowrap;font-size:13px}.coupon-card button:disabled{background:#bfcbe0}.coupon-details{border-top:1px dashed #e1e7f2;margin-top:16px;padding-top:8px}.coupon-details p{font-size:11px;color:#8a97aa;line-height:1.7;margin:5px 0}.coupon-details p span{margin-right:10px;color:#6e7e98}.empty,.state{margin:30px 20px;text-align:center;color:#7d8ca4}.error,.notice{margin:12px 20px;font-size:13px;line-height:1.6;padding:10px;border-radius:8px}.error{color:#c54b59;background:#fff0f2}.notice{background:#e8f6f0;color:#11805f}.error button{border:0;color:#2362e7;background:none}
</style>

<style scoped>.my-coupons-top{font-size:12px;color:#2463eb;width:48px;text-align:right}.topbar{display:grid;grid-template-columns:60px 1fr 60px}.topbar h1{text-align:center}.topbar .my-coupons-top{justify-self:end}.notice a{display:inline-flex;align-items:center;color:#2463eb;margin-top:4px;gap:3px}</style>

<style scoped>
.coupon-intro{position:relative;padding-right:105px}.refresh-coupons{position:absolute;right:18px;top:30px;border:0;background:none;color:#2463eb;font-size:12px;cursor:pointer}.claim-mode{display:table;color:#2463eb;background:#edf3ff;border-radius:4px;font-size:10px;padding:2px 6px;margin-bottom:5px}.claim-progress{margin-top:15px;color:#3266b1;font-size:12px}.claim-progress>div{display:flex;justify-content:space-between;gap:10px}.claim-progress small{color:#8a97aa}.claim-progress progress{width:100%;height:5px;margin-top:8px;appearance:none;border:0;border-radius:6px;overflow:hidden;background:#edf2fc}.claim-progress progress::-webkit-progress-bar{background:#edf2fc}.claim-progress progress::-webkit-progress-value{background:#5b8aef;border-radius:6px}.claim-progress progress::-moz-progress-bar{background:#5b8aef}
</style>
