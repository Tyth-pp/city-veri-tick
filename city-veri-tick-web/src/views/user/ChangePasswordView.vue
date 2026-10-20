<template><main class="password-page"><form class="password-card" @submit.prevent="save"><span class="password-icon"><AppIcon name="shield" size="28" /></span><h1>修改初始密码</h1><p class="staff-hint">为保障账号安全，首次登录需设置您自己的密码后才能使用工作台。</p><div class="staff-grid"><label>当前初始密码<input v-model="currentPassword" type="password" required autocomplete="current-password" maxlength="64" /></label><label>新密码<input v-model="newPassword" type="password" required minlength="10" maxlength="64" autocomplete="new-password" placeholder="10–64位，包含字母和数字" /></label><label>确认新密码<input v-model="confirmation" type="password" required autocomplete="new-password" /></label></div><p v-if="error" class="staff-error" role="alert">{{ error }}</p><p v-if="notice" class="staff-notice" role="status">{{ notice }}</p><div class="staff-buttons"><button type="button" :disabled="saving" @click="leave">退出登录</button><button class="staff-primary" :disabled="saving">{{ saving ? '保存中…' : '保存并重新登录' }}</button></div></form></main></template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import AppIcon from '../../components/AppIcon.vue'
const router=useRouter(),auth=useAuthStore(),currentPassword=ref(''),newPassword=ref(''),confirmation=ref(''),saving=ref(false),error=ref(''),notice=ref('')
async function leave(){if(await auth.logout())await router.replace({path:'/login',query:{mode:'account'}})}
async function save(){
 if(saving.value)return
 error.value=''
 if(newPassword.value!==confirmation.value){error.value='两次输入的新密码不一致';return}
 if(newPassword.value===currentPassword.value){error.value='新密码不能与初始密码相同';return}
 if(!/[A-Za-z]/.test(newPassword.value)||!/[0-9]/.test(newPassword.value)){error.value='新密码需包含字母和数字';return}
 saving.value=true
 try{await authApi.changePassword({currentPassword:currentPassword.value,newPassword:newPassword.value});auth.clearSession();await router.replace({path:'/login',query:{mode:'account',passwordChanged:'1'}})}catch(e){error.value=e.message}finally{saving.value=false}
}
</script>
<style scoped>.password-page{min-height:100dvh;display:grid;place-items:center;padding:24px;background:#f5f8fc}.password-card{width:min(460px,100%);padding:32px;background:#fff;border:1px solid #e1e7f0;border-radius:14px;box-shadow:0 12px 35px #14203a12}.password-card h1{font-size:24px;color:#17233b}.password-icon{display:grid;place-items:center;width:58px;height:58px;background:#edf3ff;color:#2161e8;border-radius:15px}.staff-grid{grid-template-columns:1fr;margin-top:22px}</style>

