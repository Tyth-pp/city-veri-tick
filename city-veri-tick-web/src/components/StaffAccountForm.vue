<template>
<Teleport to="body"><div class="staff-overlay"><form class="staff-modal" role="dialog" aria-modal="true" aria-labelledby="staff-form-title" @submit.prevent="save">
<h2 id="staff-form-title">{{ organization ? '创建主办方及管理员' : verifier ? '创建核销员账号' : '创建主办方管理员' }}</h2>
<p class="staff-hint">账号创建后需要首次登录修改初始密码。请通过安全渠道向本人交付账号。</p>
<fieldset :disabled="saving">
<template v-if="organization"><h3>机构资料</h3><div class="staff-grid">
<label>机构名称<input v-model.trim="org.name" required maxlength="128" /></label>
<label>统一社会信用代码（选填）<input v-model.trim="org.unifiedSocialCreditCode" maxlength="32" /></label>
<label>联系人<input v-model.trim="org.contactName" required maxlength="64" /></label>
<label>联系电话<input v-model.trim="org.contactPhone" required pattern="1[3-9][0-9]{9}" inputmode="tel" /></label>
</div></template>
<h3>{{ organization ? '管理员账号' : '账号资料' }}</h3><div class="staff-grid">
<label>{{ verifier ? '核销员姓名' : '管理员姓名' }}<input v-model.trim="staff.name" required maxlength="32" autocomplete="off" /></label>
<label>登录账号<input v-model.trim="staff.username" required pattern="[A-Za-z][A-Za-z0-9_]{3,31}" autocomplete="off" placeholder="4–32位字母、数字或下划线" /></label>
<label>手机号<input v-model.trim="staff.phone" required pattern="1[3-9][0-9]{9}" maxlength="11" inputmode="tel" /></label>
<label>初始密码<input v-model="staff.initialPassword" type="password" required minlength="10" maxlength="64" autocomplete="new-password" placeholder="10–64位，包含字母和数字" /></label>
</div>
<p v-if="error" class="staff-error" role="alert">{{ error }}</p>
<div class="staff-buttons"><button type="button" @click="$emit('close')">取消</button><button class="staff-primary" type="submit">{{ saving ? '创建中…' : '确认创建' }}</button></div>
</fieldset>
</form></div></Teleport>
</template>
<script setup>
import { reactive,ref } from 'vue'
import { platformOrganizerApi } from '../api/platformOrganizer'
import { organizerStaffApi } from '../api/organizerStaff'
const props=defineProps({organization:Boolean,verifier:Boolean,organizerId:[String,Number]})
const emit=defineEmits(['close','created'])
const org=reactive({name:'',unifiedSocialCreditCode:'',contactName:'',contactPhone:''})
const staff=reactive({username:'',name:'',phone:'',initialPassword:''}),saving=ref(false),error=ref('')
async function save(){
 if(saving.value)return
 if(!/[A-Za-z]/.test(staff.initialPassword)||!/[0-9]/.test(staff.initialPassword)){error.value='初始密码需包含字母和数字';return}
 saving.value=true;error.value=''
 try {
  const payload={...staff}
  const id=props.organization ? await platformOrganizerApi.create({...org,administrator:payload})
    :props.verifier ? await organizerStaffApi.create(payload)
    :await platformOrganizerApi.createAdministrator(props.organizerId,payload)
  staff.initialPassword='';emit('created',id)
 }catch(e){error.value=e.message||'创建失败，请重试'}finally{saving.value=false}
}
</script>

