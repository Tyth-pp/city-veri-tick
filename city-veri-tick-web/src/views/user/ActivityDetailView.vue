<template>
  <main class="page detail-page">
    <header class="topbar"><button class="icon-button" title="返回" @click="router.back()"><AppIcon name="arrow-left" /></button><div class="detail-title"><AppIcon name="ticket" size="16" /><h1>活动详情</h1></div><div class="top-actions"><button class="icon-button" title="分享活动" @click="share"><AppIcon name="share" /></button><span class="avatar"><AppIcon name="user" size="18" /></span></div></header>
    <div v-if="loading" class="loading">活动详情加载中...</div>
    <section v-else-if="error" class="container state-card error"><b>活动暂时无法打开</b><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></section>
    <div v-else-if="activity" class="container detail-content">
      <div class="hero"><ActivityCover class="hero-cover" :src="coverUrl || ''" :title="activity.title" /><span class="hero-status">官方活动 · 实名电子票</span><button class="hero-favorite" :class="{ active: activity && favorites.has(activity.id) }" title="收藏活动" @click="toggleFavorite"><AppIcon name="heart" /></button></div>
      <section class="card intro"><h2>{{ activity.title }}</h2><div class="intro-tags"><span>{{ sessionStatusText }}</span><em>官方直售 · 电子验票</em></div><div class="info-strip"><p><AppIcon name="calendar" size="15" /> {{ selectedSession ? formatRange(selectedSession) : '场次时间待公布' }}</p><small>具体检票时间请以主办方现场安排为准</small></div><div class="venue"><span><AppIcon name="pin" size="16" /></span><button v-if="activity.venueAddress" class="venue-copy" title="复制活动地址" @click="copyAddress"><b>{{ activity.venueName || '场地待定' }}</b><small>{{ activity.venueAddress || '地址待公布' }}</small><i>复制</i></button><div v-else><b>{{ activity.venueName || '场地待定' }}</b><small>{{ activity.venueAddress || '地址待公布' }}</small></div></div></section>
      <section v-if="nextOpening" class="card opening-notice"><AppIcon name="bell" size="19" /><div><b>即将开售</b><p>{{ formatOpening(nextOpening) }} 开售</p><small>订阅后，活动开售时会在用户端页面内提醒你。</small></div></section>
      <section><div class="section-heading"><h2 class="section-title">选择场次</h2><span class="muted">共 {{ sessions.length }} 场可选</span></div><div class="session-list"><button v-for="session in sessions" :key="session.id" class="session-card" :class="{selected: selectedSession?.id === session.id}" @click="selectSession(session)"><span><b>{{ formatRange(session) }}</b></span><strong><AppIcon v-if="selectedSession?.id === session.id" name="check-circle" size="18" /></strong></button><div v-if="!sessions.length" class="card inline-empty">暂无可售场次</div></div></section>
      <section><div class="section-heading"><h2 class="section-title">选择票种</h2></div><div class="ticket-list"><button v-for="ticket in tickets" :key="ticket.id" class="ticket-card" :class="{selected: selection?.ticketTypeId === ticket.id}" :disabled="!isAvailable(ticket)" @click="selectTicket(ticket)"><span class="ticket-copy"><b>{{ ticket.name }}</b><small>{{ ticketStatusText(ticket) }}</small></span><strong>¥{{ ticket.price }}</strong><span class="ticket-check"><AppIcon v-if="selection?.ticketTypeId === ticket.id" name="check" size="13" /></span></button><div v-if="selection" class="quantity-row"><span><b>购票数量</b><small>每单最多4张</small></span><div><button :disabled="selection.quantity <= 1" @click="changeQuantity(-1)"><AppIcon name="minus" size="16" /></button><b>{{ selection.quantity }}</b><button :disabled="selection.quantity >= quantityLimit" @click="changeQuantity(1)"><AppIcon name="plus" size="16" /></button></div></div><div v-if="!tickets.length" class="card inline-empty">暂无可售票种</div></div></section>
      <section class="card description"><h2>购票与入场保障</h2><div class="notice"><span><AppIcon name="user-check" size="18" /></span><div><b>实名信息核验入场</b><p>入场须提供电子票及与订单实名信息一致的身份证件。</p></div></div><div class="notice"><span><AppIcon name="refresh" size="18" /></span><div><b>整单退票规则</b><p>全部电子票未核销，且距场次开始至少48小时可申请整单退票；主办方审核通过后全额模拟退款，审核期间暂停入场。</p></div></div><div class="notice"><span><AppIcon name="shield-check" size="18" /></span><div><b>一票一码核验</b><p>核销码与订单实名信息共同核验，成功核销后不可重复使用。请妥善保管电子票。</p></div></div></section>
      <section class="card description structured-description"><h2>活动描述详情</h2><div v-if="activity.content" class="description-block"><span>01</span><div><b>活动内容</b><p>{{ activity.content }}</p></div></div><div v-if="activity.highlights" class="description-block"><span>02</span><div><b>活动亮点</b><p>{{ activity.highlights }}</p></div></div><div v-if="activity.notices" class="description-block"><span>03</span><div><b>注意事项</b><p>{{ activity.notices }}</p></div></div><p v-if="!activity.content && !activity.highlights && !activity.notices">{{ activity.description || '主办方暂未提供详细介绍。' }}</p></section>
    </div>
    <p v-if="feedback" class="feedback" role="status">{{ feedback }}</p><div v-if="activity" class="booking-dock"><div><small>{{ nextOpening ? '本场次开售时间' : selection ? `${selection.ticket.name} × ${selection.quantity}` : '请选择票种' }}</small><span v-if="nextOpening">{{ formatOpening(nextOpening) }}</span><strong v-else-if="selection">¥{{ totalPrice }}</strong><span v-else>{{ sessionStatusText }}</span></div><button v-if="nextOpening" class="primary-button" @click="toggleOpeningReminder"><AppIcon name="bell" size="16" />{{ openingSubscribed ? '已订阅 · 取消' : '提醒我开售' }}</button><button v-else class="primary-button" :disabled="!selection || !selectedSession || !isAvailable(selection.ticket)" @click="choose">{{ selection && isAvailable(selection.ticket) ? '立即购票' : sessionStatusText === '正在售票' ? '请选择票种' : '暂不可购票' }}</button></div>
  </main>
</template>
<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { activityApi } from '../../api/activity'
import { useOrderStore } from '../../stores/order'
import { useAuthStore } from '../../stores/auth'
import { useFavoritesStore } from '../../stores/favorites'
import guitarCover from '../../assets/covers/guitar-beginner.svg'
import basketballCover from '../../assets/covers/campus-basketball.svg'
import potteryCover from '../../assets/covers/pottery-workshop.svg'
import AppIcon from '../../components/AppIcon.vue'
import ActivityCover from '../../components/ActivityCover.vue'
import { useRemindersStore } from '../../stores/reminders'
import { ticketSaleState, formatOpening } from '../../utils/ticketSale'
const route=useRoute(),router=useRouter(),store=useOrderStore(),activity=ref(null),loading=ref(true),error=ref(''),selectedSession=ref(null),selection=ref(null),feedback=ref('')
const auth = useAuthStore(), favorites = useFavoritesStore()
function toggleFavorite() {
  if (auth.isGuest) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  favorites.toggle(activity.value)
}
const sessions=computed(()=>activity.value?.sessions||[]),tickets=computed(()=>selectedSession.value?.ticketTypes||[]),quantityLimit=computed(()=>Math.min(4,Number(selection.value?.ticket?.availableStock??selection.value?.ticket?.stock??99))),totalPrice=computed(()=>selection.value?((Number(selection.value.ticket?.price)||0)*selection.value.quantity).toFixed(2):'0.00')
const coverUrl=computed(()=>{const t=activity.value?.title||'';const url=activity.value?.coverFileUrl||(t.includes('吉他')?guitarCover:t.includes('篮球')?basketballCover:t.includes('陶艺')?potteryCover:null);return url || ''})
async function load(){loading.value=true;error.value='';try{activity.value=await activityApi.detail(route.params.activityId);selectedSession.value=sessions.value.find(s => s.ticketTypes?.some(t => ticketSaleState(t) === 'AVAILABLE'))||sessions.value[0]||null}catch(e){error.value=e.message||'网络请求失败'}finally{loading.value=false}}
function selectSession(s){selectedSession.value=s;selection.value=null}
function isAvailable(ticket){return ticketSaleState(ticket, now.value) === 'AVAILABLE'}
function selectTicket(ticket){if(isAvailable(ticket))selection.value={ticketTypeId:String(ticket.id),quantity:1,ticket}}
function changeQuantity(delta){if(!selection.value)return;selection.value={...selection.value,quantity:Math.min(quantityLimit.value,Math.max(1,selection.value.quantity+delta))}}
function choose(){if (!selection.value || !selectedSession.value || ticketSaleState(selection.value.ticket) !== 'AVAILABLE') { notify('当前票种尚未开售或已停止售票，请重新选择'); return } store.selectTicket({ticketTypeId:selection.value.ticketTypeId,sessionId:String(selectedSession.value.id),quantity:selection.value.quantity,ticket:selection.value.ticket,activity:activity.value,session:selectedSession.value});router.push('/orders/confirm')}
function notify(message){feedback.value=message;setTimeout(()=>feedback.value='',2500)}
async function share(){try{await navigator.clipboard?.writeText(window.location.href);notify('活动链接已复制')}catch{notify('暂时无法复制活动链接')}}
async function copyAddress(){try{await navigator.clipboard?.writeText(activity.value.venueAddress);notify('活动地址已复制')}catch{notify('暂时无法复制活动地址')}}
function formatDate(v){if(!v)return'';return String(v).replace('T',' ').replace(/([+-]\d\d:\d\d|\.\d+Z?)$/,'').replaceAll('-','/')}
function formatRange(s){const start=formatDate(s.startTime),end=formatDate(s.endTime);return start&&end?`${start} - ${end}`:start?`${start}（结束时间待公布）`:'场次时间待公布'}
const reminders = useRemindersStore(), now = ref(Date.now())
let saleClock
const nextOpening = computed(() => {
  if (tickets.value.some(ticket => ticketSaleState(ticket, now.value) === 'AVAILABLE')) return null
  return tickets.value.filter(ticket => ticketSaleState(ticket, now.value) === 'UPCOMING')
    .map(ticket => ticket.saleStartTime).sort((a, b) => Date.parse(a) - Date.parse(b))[0] || null
})
const sessionStatusText = computed(() => tickets.value.some(ticket => isAvailable(ticket)) ? '正在售票' : nextOpening.value ? '即将开售' : tickets.value.some(ticket => ticketSaleState(ticket, now.value) === 'SOLD_OUT') ? '已售罄' : '已停止售票')
const openingSubscribed = computed(() => {
  const subscription = reminders.subscriptions[String(activity.value?.id)]
  return Boolean(subscription && !subscription.delivered)
})
function ticketStatusText(ticket) {
  const state = ticketSaleState(ticket, now.value)
  return { AVAILABLE: '官方直售 · 电子票', UPCOMING: `${formatOpening(ticket.saleStartTime)} 开售`, SOLD_OUT: '已售罄', UNAVAILABLE: '已停止售票' }[state]
}
function toggleOpeningReminder() {
  if (!activity.value || !nextOpening.value) return
  if (!auth.isLoggedIn) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  if (openingSubscribed.value) { reminders.unsubscribe(activity.value.id); notify('已取消开售提醒'); return }
  if (!favorites.has(activity.value.id)) favorites.toggle(activity.value)
  reminders.subscribe(activity.value); notify('已收藏并订阅开售提醒')
}
onMounted(() => { saleClock = setInterval(() => { now.value = Date.now() }, 1000) })
onUnmounted(() => clearInterval(saleClock))
onMounted(load)
</script>
<style scoped>
.opening-notice{display:flex;align-items:flex-start;gap:10px;padding:14px;margin-top:14px;background:#edf4ff;color:#2563eb}.opening-notice>svg{flex-shrink:0}.opening-notice b{font-size:14px}.opening-notice p{margin:5px 0;font-size:13px}.opening-notice small{font-size:11px;line-height:1.6;color:#64748b}.booking-dock .primary-button{display:inline-flex;align-items:center;justify-content:center;gap:5px}

.hero-cover{position:absolute;inset:0}
.top-actions{display:flex;align-items:center}.avatar{width:32px;height:32px;display:grid;place-items:center;border-radius:50%;background:var(--primary);color:#fff}.detail-title{display:flex;align-items:center;gap:7px;margin-right:auto}.detail-title span{color:#2dd4bf}.detail-title h1{font-size:16px}.detail-content{padding-bottom:18px}.hero{aspect-ratio:16/9;background:#dfe8fb center/cover;position:relative;margin:0 -16px}.hero:after{content:'';position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,.12),transparent 40%,rgba(0,0,0,.52))}.hero-status,.hero-favorite{position:absolute;z-index:1;top:14px}.hero-status{left:14px;background:#087d5d;color:#d1fae5;border-radius:16px;padding:6px 11px;font-size:11px;font-weight:700}.hero-favorite{right:14px;border:0;border-radius:50%;width:38px;height:38px;background:rgba(15,23,42,.75);color:#fb7185;font-size:22px}.hero-favorite.active{background:#fff;color:#ef476f}.hero-favorite.active .app-icon{fill:currentColor}.intro{padding:14px;margin-top:-12px;position:relative;z-index:2}.intro h2{margin:0 0 8px;font-size:18px;line-height:1.35}.intro-tags{display:flex;align-items:center;gap:7px;font-size:11px}.intro-tags span{color:var(--primary);background:var(--primary-soft);padding:4px 7px;border-radius:4px}.intro-tags em{font-style:normal;color:var(--muted)}.intro-tags b{color:#b45309}.info-strip{background:#f0f3ff;border-radius:8px;padding:9px;margin-top:10px}.info-strip p{margin:0;color:#0f172a;font-size:12px}.info-strip small{display:block;color:var(--muted);font-size:10px;margin-top:3px}.venue{display:grid;grid-template-columns:18px 1fr auto;gap:2px 5px;margin:11px 0 0;font-size:12px}.venue>span{grid-row:span 2;color:#ef4444}.venue b{font-size:12px}.venue small{color:var(--muted);font-size:10px}.venue a{grid-column:3;grid-row:1 / span 2;align-self:center;color:var(--primary);font-size:11px}.section-heading{display:flex;justify-content:space-between;align-items:center}.section-title{margin-bottom:10px;font-size:14px}.session-list,.ticket-list{display:flex;flex-direction:column;gap:8px}.session-card,.ticket-card{width:100%;min-height:62px;border:1px solid transparent;border-radius:10px;background:#fff;text-align:left;display:flex;align-items:center;padding:10px 11px;transition:.15s}.session-card.selected,.ticket-card.selected{border:2px solid var(--primary);background:#f4f7ff}.session-card span,.ticket-copy{display:flex;flex-direction:column;gap:3px;flex:1}.session-card b,.ticket-copy b{font-size:13px;color:#0f172a}.session-card small,.ticket-copy small{color:#64748b;font-size:10px}.session-card i{font-style:normal;color:#0d9b70;font-size:10px}.session-card>strong{color:var(--primary)}.ticket-check{width:18px;height:18px;border:1px solid #cbd5e1;border-radius:50%;color:#fff;text-align:center;line-height:16px;font-size:11px}.selected .ticket-check{background:var(--primary);border-color:var(--primary)}.ticket-card strong{color:var(--primary);font-size:18px;margin-right:10px}.quantity-row{display:flex;align-items:center;justify-content:space-between;padding:8px 5px 2px;color:var(--muted);font-size:13px}.quantity-row span{display:flex;flex-direction:column;gap:2px}.quantity-row span small{font-size:10px}.quantity-row div{display:flex;align-items:center;gap:14px}.quantity-row button{width:32px;height:32px;border:0;border-radius:8px;background:#eef2ff;color:var(--primary);font-size:20px}.inline-empty{padding:25px;text-align:center;color:var(--muted)}.description{padding:14px;margin:20px 0}.description h2{margin:0 0 10px;font-size:16px}.description>p{color:var(--body);line-height:1.7;margin:0}.notice{display:flex;gap:9px;margin-top:8px;padding:9px;background:#f4f6ff;border-radius:9px;color:var(--primary)}.notice>span{font-size:16px}.notice b{font-size:12px}.notice p{margin:3px 0 0;color:var(--muted);font-size:10px;line-height:1.5}.program h3{font-size:13px;color:var(--primary);margin:12px 0 5px}.program p{font-size:11px;color:var(--body);margin:5px 0}.program-row{padding:9px;background:#f1f3ff;border-radius:9px;display:flex;flex-direction:column;gap:3px;font-size:11px}.program-row small{color:var(--muted)}.booking-dock{position:fixed;bottom:0;left:0;right:0;background:#fff;border-top:1px solid var(--line);box-shadow:0 -5px 18px rgba(30,55,90,.08);padding:8px max(16px,calc((100% - 780px)/2 + 16px)) calc(8px + env(safe-area-inset-bottom));display:flex;align-items:center;justify-content:space-between;z-index:15}.booking-dock small{display:block;color:var(--muted);font-size:10px}.booking-dock strong{display:block;color:var(--primary);font-size:21px;margin-top:2px}.booking-dock .primary-button{min-width:155px}
/* The address itself is the copy affordance to keep the venue row quiet. */
</style>
<style scoped>
.structured-description{padding-bottom:16px}.description-block{display:grid;grid-template-columns:28px 1fr;gap:8px;padding:11px 0;border-top:1px solid #edf0f7}.description-block:first-of-type{border-top:0}.description-block>span{color:var(--primary);font-size:11px;font-weight:800;padding-top:2px}.description-block b{display:block;color:#1e293b;font-size:13px}.description-block p{margin:4px 0 0;color:var(--body);font-size:12px;line-height:1.7;white-space:pre-wrap}
</style>
<style scoped>
.venue{display:grid;grid-template-columns:18px minmax(0,1fr);gap:2px 8px;align-items:start;margin:11px 0 0}.venue>span{grid-row:span 2;color:#64748b;margin-top:2px}.venue>div,.venue-copy{min-width:0}.venue b{display:block;font-size:13px;line-height:1.45;color:#1e293b}.venue small{display:block;margin-top:3px;color:var(--muted);font-size:11px;line-height:1.45;overflow-wrap:anywhere}.venue-copy{position:relative;display:block;width:100%;padding:0;border:0;background:transparent;text-align:left;cursor:pointer}.venue-copy i{position:absolute;right:0;top:50%;transform:translateY(-50%);padding:4px 8px;border-radius:5px;background:var(--primary-soft);color:var(--primary);font-size:10px;font-style:normal;opacity:0;pointer-events:none;transition:opacity .15s ease}.venue-copy:hover i,.venue-copy:focus-visible i{opacity:1}.venue-copy:hover b,.venue-copy:focus-visible b{color:var(--primary)}
</style>
<style scoped>
.detail-content{padding-bottom:150px}.copy-button{border:0;background:var(--primary-soft);color:var(--primary);padding:5px 8px;border-radius:5px;font-size:10px}.session-card b{line-height:1.7;overflow-wrap:anywhere}.ticket-card:disabled{opacity:.55}.booking-dock{bottom:64px;z-index:21;gap:10px}.booking-dock>div{flex:1;min-width:0}.booking-dock small,.booking-dock span{display:block;color:var(--muted);font-size:10px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.booking-dock span{font-size:13px;margin-top:4px}.booking-dock .primary-button{min-width:125px}.feedback{position:fixed;bottom:145px;left:50%;transform:translateX(-50%);background:#182537;color:#fff;padding:9px 14px;border-radius:7px;z-index:30;font-size:12px;white-space:nowrap}
.info-strip p{display:flex;align-items:center;gap:5px}.venue>span{display:flex;align-items:flex-start;justify-content:center}.notice>span{display:inline-flex;align-items:center;justify-content:center;flex:none}</style>
