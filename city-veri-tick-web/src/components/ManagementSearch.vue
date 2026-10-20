<template><form class="management-search" role="search" @submit.prevent="search"><AppIcon name="search" size="17"/><select v-model="kind" aria-label="搜索业务类型"><option value="activities">活动</option><option value="orders">订单</option><option v-if="platform" value="users">账号</option><option v-else value="ticket-inventory">票种</option></select><input v-model.trim="keyword" :placeholder="kind==='orders'?'输入订单号或订单ID':'输入名称或关键词'" aria-label="管理业务搜索" maxlength="100"/><button type="submit">查询</button></form></template>
<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import AppIcon from './AppIcon.vue'
const props=defineProps({platform:Boolean}),router=useRouter(),kind=ref('activities'),keyword=ref('')
function search(){router.push({path:`/management/${props.platform?'platform':'organizer'}/${kind.value}`,query:keyword.value?{keyword:keyword.value}:{}})}
</script>
<style scoped>.management-search{display:flex;align-items:center;gap:9px;padding:7px 10px;border:1px solid #dce5f2;border-radius:10px;background:#f5f8fd;width:min(540px,55vw);color:#8294b0}.management-search select,.management-search input{border:0;background:transparent;outline:none;color:#3b506f;font-size:12px;min-width:0}.management-search input{flex:1;width:100px}.management-search button{border:0;background:#e7efff;border-radius:6px;color:#2161e8;font-size:12px;padding:6px 10px;cursor:pointer}@media(max-width:650px){.management-search{width:100%}.management-search>select{max-width:64px}}</style>
