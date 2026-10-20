<template><Teleport to="body"><div class="history-mask" @click.self="$emit('close')"><section class="history-card" role="dialog" aria-modal="true" aria-labelledby="history-title"><div class="heading"><div><h2 id="history-title">账号操作记录</h2><p>{{ target.name || target.username }} · {{ target.username }}</p></div><button aria-label="关闭操作记录" @click="$emit('close')"><AppIcon name="close" size="20" /></button></div><p class="hint">显示最近 100 条账号操作记录。</p><p v-if="loading">记录加载中…</p><p v-else-if="error" role="alert">{{ error }} <button @click="load">重新加载</button></p><p v-else-if="!records.length" class="hint">暂无已记录的账号操作</p><div v-else class="rows"><article v-for="r in records" :key="r.id"><div><strong>{{ labels[r.operation] || r.operation }}</strong><time>{{ date(r.createdAt) }}</time></div><p>操作人：{{ r.operatorName || r.operatorUsername || '账号 ID：'+r.operatorId }}<small v-if="r.operatorName">（{{ r.operatorUsername }}）</small></p><p>原因 / 备注：{{ r.reason || '未填写' }}</p></article></div></section></div></Teleport></template>
<script setup>
import {onMounted,ref} from 'vue'
import {platformUserApi} from '../api/platformUser'
import AppIcon from './AppIcon.vue'
const props=defineProps({target:{type:Object,required:true}});defineEmits(['close'])
const records=ref([]),loading=ref(true),error=ref(''),labels={DISABLE:'停用账号',ENABLE:'启用账号',FORCE_LOGOUT:'强制下线'}
const date=v=>v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'—'
async function load(){loading.value=true;error.value='';try{records.value=await platformUserApi.operations(props.target.id)||[]}catch(e){error.value=e.message||'操作记录加载失败'}finally{loading.value=false}}
onMounted(load)
</script>
<style scoped>
.history-mask{position:fixed;inset:0;z-index:1000;background:#0f172a66;display:flex;align-items:center;justify-content:center;padding:20px}.history-card{background:#fff;border-radius:16px;padding:24px;width:720px;max-width:100%;max-height:85vh;overflow:auto;color:#17243a}.heading{display:flex;justify-content:space-between;gap:15px}.heading h2{font-size:20px;margin:0}.heading p,.hint{color:#74829a;font-size:13px}.heading button{border:0;background:#f2f5fc;border-radius:50%;width:32px;height:32px;display:grid;place-items:center;color:#64748b}.rows article{padding:18px 0;border-top:1px solid #e5ebf5}.rows article>div{display:flex;justify-content:space-between;gap:12px}.rows time{color:#74829a;font-size:12px}.rows p{font-size:13px;line-height:1.7;white-space:pre-wrap;overflow-wrap:anywhere}
</style>
