<template>
<section class="card my-coupons-entry">
  <RouterLink to="/coupons/mine" class="my-coupons-link"><span class="coupon-icon"><AppIcon name="ticket" size="24" /></span><div><b>我的优惠券</b><p v-if="loading">正在加载优惠券…</p><p v-else-if="error">暂时无法获取可用数量，点击查看</p><p v-else><strong>{{usableCount}}</strong> 张当前可用 · 查看已领优惠券</p></div><AppIcon name="chevron-right" size="18" /></RouterLink>
  <RouterLink to="/coupons" class="claim-link"><span><AppIcon name="gift" size="15" /> 优惠券中心</span><span>去领券 <AppIcon name="chevron-right" size="14" /></span></RouterLink>
</section>
</template>
<script setup>
import {ref,onMounted,onUnmounted} from 'vue'
import AppIcon from './AppIcon.vue'
import {couponApi} from '../api/coupon'
const usableCount=ref(0),loading=ref(true),error=ref(false)
let request=0,poll
async function load(){const current=++request,token=sessionStorage.getItem('city-veri-tick-token');loading.value=true;error.value=false;try{const data=await couponApi.mine();if(!Number.isFinite(Number(data?.usableCount)))throw new Error('优惠券数量加载失败');if(current===request&&token===sessionStorage.getItem('city-veri-tick-token'))usableCount.value=Number(data.usableCount)}catch{if(current===request)error.value=true}finally{if(current===request)loading.value=false}}
function visible(){if(document.visibilityState==='visible')load()}
onMounted(()=>{load();document.addEventListener('visibilitychange',visible);poll=setInterval(()=>{if(document.visibilityState!=='hidden')load()},60000)})
onUnmounted(()=>{request++;clearInterval(poll);document.removeEventListener('visibilitychange',visible)})
</script>
<style scoped>
.my-coupons-entry{margin-top:14px;border-radius:15px;padding:0 16px;overflow:hidden}.my-coupons-link{display:flex;align-items:center;gap:12px;padding:18px 0;color:#152c4d}.coupon-icon{width:44px;height:44px;border-radius:13px;background:#edf3ff;display:grid;place-items:center;flex:none;color:#2463eb}.my-coupons-link>div{min-width:0;flex:1}.my-coupons-link b{font-size:16px}.my-coupons-link p{margin:6px 0 0;color:#8491a7;font-size:12px;line-height:1.5}.my-coupons-link p strong{font-weight:600;color:#2463eb}.my-coupons-link>.app-icon{color:#98a6bc;flex:none}.claim-link{border-top:1px solid #edf1f7;padding:12px 0;display:flex;align-items:center;justify-content:space-between;gap:12px;color:#7b8ba5;font-size:12px}.claim-link span{display:inline-flex;align-items:center;gap:5px}.claim-link>span:last-child{color:#2463eb}
</style>
