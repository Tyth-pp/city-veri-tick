<template><Teleport to="body"><div class="staff-overlay" @click.self="!saving&&$emit('close')"><section class="staff-modal staff-profile-modal" role="dialog" aria-modal="true" aria-labelledby="staff-profile-title">
<div class="staff-profile-title"><h2 id="staff-profile-title">{{ mode==='password'?'修改密码':self?'账号资料':'编辑工作人员资料' }}</h2><button class="staff-close" type="button" aria-label="关闭账号资料" :disabled="saving" @click="$emit('close')"><AppIcon name="close" size="20" /></button></div>
<template v-if="mode==='profile'">
<StaffIdentity :account="account" :subtitle="roleLabel(account.role||account.roleCodes)" />
<p v-if="self&&store.error" class="staff-error" role="alert">{{ store.error }} <button class="staff-action" @click="store.load">重新加载</button></p>
<p v-if="self&&store.loading" class="staff-hint">资料加载中…</p>
<template v-if="!self||store.profile"><dl class="staff-profile-facts"><div><dt>登录账号</dt><dd>{{ account.username }}</dd></div><div><dt>账号状态</dt><dd>{{ account.enabled?'正常':'已停用' }}</dd></div><div v-if="account.role!=='PLATFORM_ADMIN'&&account.organizers?.length"><dt>所属主办方</dt><dd>{{ account.organizers.map(o=>o.name).join('、') }}</dd></div></dl>
<form @submit.prevent="save"><fieldset :disabled="saving || (self && store.loading)"><div class="staff-grid"><label>姓名<input v-model.trim="name" required maxlength="32" autocomplete="name" /></label><label>手机号<input v-model.trim="phone" required pattern="1[3-9][0-9]{9}" maxlength="11" inputmode="tel" autocomplete="tel" /></label></div><p class="staff-hint">手机号作为联系电话；工作人员姓名用于账号展示。</p><p v-if="error" class="staff-error" role="alert">{{ error }}</p><div class="staff-buttons"><button v-if="self" type="button" @click="mode='password';error=''">修改密码</button><button type="button" @click="$emit('close')">取消</button><button class="staff-primary" type="submit">{{ saving?'保存中…':'保存资料' }}</button></div></fieldset></form></template>
</template>
<form v-else @submit.prevent="changePassword"><fieldset :disabled="saving"><p class="staff-hint">修改成功后，当前账号的已登记会话将失效，请使用新密码重新登录。</p><div class="staff-grid password-fields"><label>当前密码<input v-model="currentPassword" type="password" required maxlength="64" autocomplete="current-password" /></label><label>新密码<input v-model="newPassword" type="password" required minlength="10" maxlength="64" autocomplete="new-password" placeholder="10–64位，包含字母和数字" /></label><label>确认新密码<input v-model="confirmation" type="password" required maxlength="64" autocomplete="new-password" /></label></div><p v-if="error" class="staff-error" role="alert">{{ error }}</p><div class="staff-buttons"><button type="button" @click="mode='profile';error=''">返回资料</button><button class="staff-primary" type="submit">{{ saving?'提交中…':'确认修改' }}</button></div></fieldset></form>
</section></div></Teleport></template>
<script setup>
import { computed,ref,watch } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import StaffIdentity from './StaffIdentity.vue'
import { useStaffProfileStore } from '../stores/staffProfile'
import { useAuthStore } from '../stores/auth'
import { staffProfileApi } from '../api/staffProfile'
import { authApi } from '../api/auth'
import { roleLabel } from '../utils/staffIdentity'
const props=defineProps({self:Boolean,target:Object,scope:{type:String,default:'platform'}}),emit=defineEmits(['close','saved'])
const store=useStaffProfileStore(),auth=useAuthStore(),router=useRouter()
const account=computed(()=>props.self?store.profile||{}:props.target||{})
const name=ref(''),phone=ref(''),error=ref(''),saving=ref(false),mode=ref('profile'),currentPassword=ref(''),newPassword=ref(''),confirmation=ref('')
watch(account,a=>{name.value=a.name||'';phone.value=a.phone||''},{immediate:true})
async function save(){if(saving.value)return;saving.value=true;error.value='';try{const payload={name:name.value,phone:phone.value};if(props.self)await store.save(payload);else if(props.scope==='organizer')await staffProfileApi.saveVerifier(account.value.id,payload);else await staffProfileApi.saveManaged(account.value.id,payload);emit('saved');emit('close')}catch(e){error.value=e.message||'保存失败，请重试'}finally{saving.value=false}}
async function changePassword(){
 if(saving.value)return;error.value=''
 if(newPassword.value!==confirmation.value){error.value='两次输入的新密码不一致';return}
 if(newPassword.value===currentPassword.value){error.value='新密码不能与当前密码相同';return}
 if(!/[A-Za-z]/.test(newPassword.value)||!/[0-9]/.test(newPassword.value)){error.value='新密码需包含字母和数字';return}
 saving.value=true
 try{const token=auth.accessToken;await authApi.changePassword({currentPassword:currentPassword.value,newPassword:newPassword.value});if(token!==auth.accessToken)throw new Error('登录账号已变化');auth.clearSession();emit('close');await router.replace({path:'/login',query:{mode:'account',passwordChanged:'1'}})}catch(e){error.value=e.message||'修改失败'}finally{saving.value=false}
}
</script>
