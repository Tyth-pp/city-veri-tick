<template>
  <main class="page ticket-page">
    <header class="topbar"><button class="icon-button" aria-label="返回" @click="router.back()"><AppIcon name="arrow-left" /></button><h1>电子票</h1><button class="icon-button" aria-label="刷新票券状态" :disabled="loading" @click="load"><AppIcon name="refresh" size="20" /></button></header>
    <p v-if="loading" class="loading">电子票加载中…</p>
    <section v-else-if="error" class="container card state-card" role="alert"><AppIcon name="info" size="28" /><h2>电子票暂时无法打开</h2><p>{{ error }}</p><button class="secondary-button" @click="load">重新加载</button></section>
    <div v-else-if="ticket && order" class="container ticket-container">
      <section class="ticket-shell card"><div class="ticket-hero"><div class="ticket-heading"><span>Venuo / 入场凭证</span><b class="ticket-state" :class="state.tone">{{ state.label }}</b></div><h2>{{ order.activityTitle || '活动名称暂未提供' }}</h2><p>{{ order.ticketTypeName || '票种暂未提供' }}</p><small>一票一实名 · 仅限本人使用</small></div>
        <div class="session-info"><p><AppIcon name="calendar" size="19" /><b>{{ sessionRange(order) }}</b></p><p><AppIcon name="pin" size="19" /><b>{{ order.venueName || '场地暂未提供' }}</b></p></div>
        <div class="ticket-perforation"></div>
        <div class="credential"><h3>{{ state.active ? '请向工作人员出示入场凭证' : state.label }}</h3>
          <div v-if="state.active && ticket.verificationCode" class="qr-frame"><img v-if="qr" class="ticket-qr" :src="qr" alt="电子票核验二维码" /><div v-else class="qr-feedback"><AppIcon name="qr" size="34" /><p>{{ qrError ? '二维码生成失败' : '正在生成二维码…' }}</p><button v-if="qrError" class="retry-code" @click="generateQr">重新生成</button></div></div>
          <div v-else class="inactive-code"><AppIcon :name="ticket.status === 'CHECKED_IN' ? 'check-circle' : 'info'" size="42" /><p>{{ inactiveMessage }}</p></div>
          <template v-if="state.active"><small class="code-label">核销码</small><p class="verification-code">{{ ticket.verificationCode || '核销码暂未提供，请稍后刷新' }}</p><p class="credential-note">核验时需同时核对本人身份证</p></template>
        </div>
        <div class="identity"><AppIcon name="user" size="21" /><div><b>实名观众 {{ order.realName || '实名信息暂未提供' }}</b><p>身份证号 {{ order.idCardMasked || '—' }}</p></div></div>
        <div class="ticket-number"><span>票号</span><b>{{ ticket.ticketId }}</b></div>
      </section>
      <section class="ticket-notice card"><h2><AppIcon name="shield" size="18" />入场须知</h2><p>请携带与订单实名信息一致的本人身份证。</p><p>核验通过后票券将显示“已核销”，不可重复入场。</p><p v-if="!state.active">当前凭证不可用于再次入场。</p></section>
      <RouterLink :to="`/orders/${orderIdOf(order)}`" class="back-to-order">查看订单详情 <AppIcon name="chevron-right" size="15" /></RouterLink>
    </div>
  </main>
</template>
<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { orderApi } from '../../api/order'
import AppIcon from '../../components/AppIcon.vue'
import { orderIdOf } from '../../utils/id'
import { sessionRange, ticketState } from '../../utils/ticketPresentation'
const route = useRoute(), router = useRouter(), order = ref(null), ticket = ref(null), loading = ref(true), error = ref(''), qr = ref(''), qrError = ref(false), now = ref(Date.now())
let request = 0, encoding = 0, poll
const state = computed(() => ticketState(ticket.value, order.value, now.value))
const inactiveMessage = computed(() => order.value?.status === 'REFUNDING' ? '退款申请审核中，入场凭证暂停使用；审核未通过后恢复' : ticket.value?.status === 'CHECKED_IN' ? '本票已完成核验，不可重复入场' : state.value.label === '已退款' ? '本票已退款，入场凭证已停用' : state.value.label === '活动已结束' ? '活动场次已结束，入场凭证已停用' : '暂无有效入场凭证，请核对订单状态或刷新')
function findTicket(value, id) { return value?.tickets?.find(item => String(item.ticketId) === id) }
async function load(silent = false) {
  // Event handlers may pass an Event; only an explicit true enables quiet refresh.
  silent = silent === true
  const current = ++request, id = String(route.params.ticketId)
  if (!silent) { loading.value = true; order.value = null; ticket.value = null }
  error.value = ''; now.value = Date.now()
  try {
    let value
    if (route.query.orderId) value = await orderApi.detail(String(route.query.orderId))
    else {
      const result = await orderApi.list()
      const list = Array.isArray(result) ? result : result?.records || result?.content || []
      const match = list.find(item => findTicket(item, id))
      if (!match || !orderIdOf(match)) throw new Error('当前账号没有这张电子票，请从本人订单重新打开')
      value = await orderApi.detail(orderIdOf(match))
    }
    if (current !== request) return
    const actual = findTicket(value, id)
    if (!actual) throw new Error('当前订单不存在这张电子票，请核对票号')
    order.value = value; ticket.value = actual
  } catch (e) { if (current === request) { order.value = null; ticket.value = null; error.value = e.message || '网络请求失败' } }
  finally { if (current === request) loading.value = false }
}
async function generateQr() {
  const current = ++encoding, code = ticket.value?.verificationCode
  qr.value = ''; qrError.value = false
  if (!state.value.active || !code) return
  try { const image = await QRCode.toDataURL(String(code), { errorCorrectionLevel:'M', margin:4, width:600, color:{ dark:'#16233b', light:'#ffffff' } }); if (current === encoding) qr.value = image }
  catch { if (current === encoding) qrError.value = true }
}
watch(() => [route.params.ticketId, route.query.orderId], () => load(), { immediate: true })
watch(() => [ticket.value?.verificationCode, state.value.active], generateQr)
function onVisible() { if (document.visibilityState === 'visible') load(true) }
onMounted(() => { document.addEventListener('visibilitychange', onVisible); poll = setInterval(() => { now.value = Date.now(); if (document.visibilityState !== 'hidden' && !loading.value) load(true) }, 15000) })
onUnmounted(() => { request++; encoding++; clearInterval(poll); document.removeEventListener('visibilitychange', onVisible) })
</script>
<style scoped>
.ticket-page{background:#f5f7fb;padding-bottom:calc(24px + env(safe-area-inset-bottom))}.ticket-container{padding-top:16px}.ticket-shell{border-radius:20px;border:1px solid #e7edf5;box-shadow:0 2px 10px #1c3b6905;overflow:hidden}.ticket-hero{position:relative;background:#2161e8;padding:19px 17px 24px;color:#fff;overflow:hidden}.ticket-hero:after{content:'';position:absolute;right:19px;top:65px;width:65px;height:65px;border:1px solid #ffffff25;border-radius:50%;pointer-events:none}.ticket-hero:before{content:'';position:absolute;right:30px;top:76px;width:43px;height:43px;border:1px solid #ffffff25;border-radius:50%;pointer-events:none}.ticket-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;position:relative;z-index:1}.ticket-heading>span{font-size:11px;font-weight:600;color:#dbe8ff;letter-spacing:.3px}.ticket-state{font-size:10px;font-weight:600;padding:6px 11px;white-space:nowrap;border-radius:20px;background:#edf0f5;color:#6a7890}.ticket-state.available{background:#e3f7ee;color:#128963}.ticket-hero h2{position:relative;z-index:1;margin:25px 0 10px;font-size:24px;line-height:1.4;overflow-wrap:anywhere;max-width:100%}.ticket-hero p{position:relative;margin:0 0 20px;color:#e8f0ff;font-size:14px;overflow-wrap:anywhere}.ticket-hero small{font-size:10px;color:#dbe8ff}.session-info{padding:20px 17px}.session-info p{display:flex;align-items:flex-start;gap:10px;margin:0 0 14px;font-size:13px;line-height:1.6}.session-info p:last-child{margin-bottom:0}.session-info .app-icon{color:#2161e8;flex:none;margin-top:1px}.session-info b{font-weight:600;overflow-wrap:anywhere;min-width:0}.ticket-perforation{position:relative;height:1px;border-top:1px dashed #dce6f4;margin:0 9px}.ticket-perforation:before,.ticket-perforation:after{content:'';position:absolute;top:-10px;width:18px;height:18px;border-radius:50%;background:#f5f7fb}.ticket-perforation:before{left:-19px}.ticket-perforation:after{right:-19px}.credential{padding:22px 16px 20px;text-align:center}.credential h3{font-size:13px;margin:0 0 18px}.qr-frame{width:min(228px,100%);aspect-ratio:1;margin:auto;border:1px solid #e3ebf6;border-radius:15px;padding:8px;display:grid;place-items:center;background:#fff;overflow:hidden}.ticket-qr{display:block;width:100%;height:auto;aspect-ratio:1}.qr-feedback{color:#74839b;font-size:12px;display:flex;flex-direction:column;align-items:center;gap:6px}.retry-code{border:0;border-radius:8px;padding:7px 12px;color:#2161e8;background:#eef3ff;font-size:11px}.code-label{display:block;font-size:10px;color:#74839b;margin-top:20px}.verification-code{margin:9px 0;font-size:16px;line-height:1.6;font-weight:600;color:#16233b;font-family:ui-monospace,Consolas,monospace;overflow-wrap:anywhere;letter-spacing:.4px}.credential-note{font-size:10px;color:#74839b;margin:0}.inactive-code{padding:23px 12px;background:#f5f7fb;border-radius:14px;color:#74839b;display:flex;align-items:center;flex-direction:column;gap:10px}.inactive-code p{font-size:12px;line-height:1.6;margin:0}.identity{margin:0 16px;padding:13px 12px;display:flex;align-items:center;gap:11px;border-radius:12px;background:#f1f5fd}.identity>.app-icon{flex:none;color:#2161e8}.identity>div{min-width:0}.identity b{font-size:12px;overflow-wrap:anywhere}.identity p{font-size:10px;color:#74839b;margin:6px 0 0;overflow-wrap:anywhere}.ticket-number{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:19px 17px 23px;font-size:11px;line-height:1.5}.ticket-number>span{color:#74839b;flex:none}.ticket-number b{font-weight:500;text-align:right;overflow-wrap:anywhere;min-width:0}.ticket-notice{padding:17px;margin-top:16px;border-radius:16px;box-shadow:none}.ticket-notice h2{display:flex;align-items:center;gap:9px;font-size:14px;margin:0 0 13px}.ticket-notice h2 .app-icon{color:#2161e8;flex:none}.ticket-notice p{font-size:11px;color:#74839b;line-height:1.7;margin:6px 0 0}.back-to-order{display:flex;align-items:center;justify-content:center;gap:5px;padding:20px;color:#2161e8;font-size:12px}.state-card{text-align:center;padding:32px 20px;margin-top:18px}.state-card>.app-icon{color:#74839b}.state-card h2{font-size:18px}.state-card p{font-size:13px;color:#74839b}
</style>
