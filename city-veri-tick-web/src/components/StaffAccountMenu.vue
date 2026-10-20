<template><div class="staff-account-menu"><button class="staff-account-trigger" type="button" aria-label="查看账号资料" @click="open"><StaffIdentity :account="store.profile||fallback" :subtitle="subtitle" /></button><StaffProfileDialog v-if="visible" self @close="visible=false" @saved="notice='账号资料已更新'" /><span v-if="notice" class="staff-account-feedback" role="status">{{ notice }}</span></div></template>
<script setup>
import { computed,onMounted,onUnmounted,ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useStaffProfileStore } from '../stores/staffProfile'
import { roleLabel } from '../utils/staffIdentity'
import StaffIdentity from './StaffIdentity.vue'
import StaffProfileDialog from './StaffProfileDialog.vue'
const store=useStaffProfileStore(),auth=useAuthStore(),visible=ref(false),notice=ref('')
const fallback=computed(()=>({username:store.loading?'资料加载中…':roleLabel(auth.role)}))
const subtitle=computed(()=>{const p=store.profile;return [p?.organizers?.map(o=>o.name).join('、'),roleLabel(auth.role)].filter(Boolean).join(' · ')})
function open(){notice.value='';visible.value=true;store.load()}
function refresh(){if(document.visibilityState==='visible'&&!visible.value)store.load()}
onMounted(()=>{store.load();window.addEventListener('focus',refresh);document.addEventListener('visibilitychange',refresh)})
onUnmounted(()=>{window.removeEventListener('focus',refresh);document.removeEventListener('visibilitychange',refresh)})
</script>
