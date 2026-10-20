<template><Teleport to="body"><div v-if="actionDialog" class="action-mask" @click.self="finishAction()" @keydown.esc="finishAction()"><form class="action-card" role="dialog" aria-modal="true" aria-labelledby="action-title" @submit.prevent="submit"><div class="heading"><h2 id="action-title">{{ actionDialog.title || '确认操作' }}</h2><button type="button" aria-label="关闭确认弹窗" @click="finishAction()"><AppIcon name="close" size="20" /></button></div><p>{{ actionDialog.message }}</p><label v-if="actionDialog.input">{{ actionDialog.inputLabel || '操作原因' }}<textarea ref="input" v-model="reason" @input="error=''" rows="4" maxlength="500" :placeholder="actionDialog.placeholder || '请填写具体原因（最多500字）'"></textarea></label><p v-if="error" class="action-validation" role="alert">{{ error }}</p><div class="buttons"><button ref="cancel" type="button" @click="finishAction()">取消</button><button class="primary">{{ actionDialog.confirmText || '确认' }}</button></div></form></div></Teleport></template>
<script setup>
import {ref,watch,nextTick,onUnmounted} from 'vue'
import {useRoute} from 'vue-router'
import {actionDialog,finishAction} from '../utils/actionDialog'
import AppIcon from './AppIcon.vue'
const route=useRoute(),reason=ref(''),error=ref(''),input=ref(null),cancel=ref(null)
let previousFocus
watch(actionDialog,async value=>{reason.value='';error.value='';if(value){previousFocus=document.activeElement;await nextTick();(input.value||cancel.value)?.focus()}else previousFocus?.focus?.()})
watch(()=>route.fullPath,()=>finishAction())
function submit(){if(actionDialog.value.required&&!reason.value.trim()){error.value=actionDialog.value.requiredMessage || '请填写操作原因';return}finishAction({reason:reason.value.trim()})}
onUnmounted(()=>finishAction())
</script>
<style scoped>
.action-mask{position:fixed;inset:0;z-index:1000;background:#0f172a66;display:flex;align-items:center;justify-content:center;padding:20px}.action-card{box-sizing:border-box;background:#fff;color:#17243a;padding:24px;border-radius:16px;width:480px;max-width:100%;box-shadow:0 24px 80px #142d5140}.heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.heading h2{font-size:20px;margin:0}.heading>button{border:0;background:#f2f5fc;border-radius:50%;padding:6px;display:flex;color:#64748b}.action-card>p{color:#64748b;font-size:14px;line-height:1.7;white-space:pre-wrap;overflow-wrap:anywhere}.action-card label{font-size:14px}.action-card textarea{display:block;box-sizing:border-box;width:100%;padding:12px;border:1px solid #d5e0f4;border-radius:8px;resize:vertical;font:inherit;line-height:1.6;margin-top:8px}.action-card textarea:focus{outline:2px solid #c9daff}.buttons{display:flex;justify-content:flex-end;gap:12px;margin-top:24px}.buttons button{padding:10px 20px;border:1px solid #d5e0f4;background:#fff;color:#385070;border-radius:8px;font:inherit;font-size:14px;cursor:pointer}.buttons .primary{background:#245cf5;border-color:#245cf5;color:#fff}.action-card .action-validation{color:#bd3245;margin:12px 0 0;padding:0;font-size:13px;line-height:1.6;text-align:left}
</style>
