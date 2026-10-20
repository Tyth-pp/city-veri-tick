<template>
  <Teleport to="body"><div v-if="feedback" class="session-backdrop"><section class="session-dialog" :class="feedback.portal" role="alertdialog" aria-modal="true" aria-labelledby="session-title" aria-describedby="session-message"><span class="session-symbol"><AppIcon name="shield" size="28" /></span><h2 id="session-title">{{ feedback.title }}</h2><p id="session-message">{{ feedback.message }}</p><button ref="acknowledgeButton" type="button" @click="acknowledge" @keydown.tab.prevent>我知道了</button></section></div></Teleport>
</template>
<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import { sessionFeedback, resetSessionFeedback } from '../utils/sessionFeedback'
const router = useRouter(), feedback = computed(() => sessionFeedback.value), acknowledgeButton = ref(null)
watch(feedback, async value => { if (value) { await nextTick(); acknowledgeButton.value?.focus() } })
async function acknowledge() {
  const current = feedback.value
  if (!current) return
  if (current.needsLogin) await router.replace({ path:'/login', query:{redirect:current.redirect, ...(current.portal === 'management' ? {mode:'account'} : {})} })
  resetSessionFeedback()
}
</script>
<style scoped>
.session-backdrop{position:fixed;inset:0;z-index:10000;display:flex;align-items:center;justify-content:center;padding:24px;background:rgba(18,32,56,.45);backdrop-filter:blur(4px);overscroll-behavior:contain}.session-dialog{width:min(350px,100%);padding:27px 23px 23px;background:#fff;border:1px solid #e1e9f6;border-radius:21px;box-shadow:0 24px 70px #15274638;text-align:center;color:#16233b}.session-symbol{display:grid;place-items:center;width:59px;height:59px;margin:0 auto 17px;background:#edf3ff;border-radius:18px;color:#2161e8}.session-dialog h2{margin:0 0 11px;font-size:20px}.session-dialog p{font-size:13px;color:#71809a;line-height:1.8;margin:0 0 23px;overflow-wrap:anywhere}.session-dialog button{width:100%;height:44px;border:0;border-radius:12px;color:#fff;background:#2161e8;font-size:14px;font-weight:600}.session-dialog button:focus-visible{outline:3px solid #a8c5ff;outline-offset:3px}.session-dialog.management{width:min(420px,100%);border-radius:12px;padding:30px}.management .session-symbol{border-radius:12px}.management button{border-radius:8px}
</style>
