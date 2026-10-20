<template>
<main class="management-content">
<div class="management-title"><div><div class="eyebrow">人员管理 / 核销员管理</div><h1>核销员管理</h1><p>创建本机构核销账号，并分配允许核销的活动。</p></div><button class="management-button" @click="creating=true"><AppIcon name="plus" size="16" /> 创建核销员</button></div>
<p v-if="route.query?.activityId" class="staff-hint">当前为活动「{{ activities.find(a=>String(a.id)===String(route.query.activityId))?.title||route.query.activityId }}」配置核销人员，请选择账号的“分配活动”。</p>
<p v-if="notice" class="staff-notice" role="status">{{ notice }}</p>
<p v-if="error" class="staff-error" role="alert">{{ error }} <button class="staff-action" @click="load">重试</button></p>
<section class="management-card staff-list"><p v-if="loading" class="staff-hint">加载中…</p><p v-else-if="!accounts.length" class="staff-hint">暂无核销员，请先创建账号，再分配活动。</p>
<div v-else class="staff-table-scroll"><table class="staff-table"><thead><tr><th>核销员</th><th>联系方式</th><th>账号状态</th><th>核销活动</th><th>操作</th></tr></thead>
<tbody><tr v-for="item in accounts" :key="item.id"><td><StaffIdentity :account="item" :subtitle="item.username" /><small v-if="!item.name || !item.phone">资料待补全</small></td><td>{{ item.phone || '未填写' }}</td><td>{{ item.enabled ? '正常' : '已停用' }}</td><td><button class="staff-action" @click="viewPermissions(item)">{{ item.activityIds?.length || 0 }} 个活动 · 查看</button></td><td><div class="staff-cell-actions"><button class="staff-action" @click="profileTarget=item">编辑资料</button><button class="staff-action" @click="openAssignments(item)">分配活动</button><button class="staff-action" @click="pending=item">{{ item.enabled ? '停用账号' : '启用账号' }}</button></div></td></tr></tbody></table></div></section>
<StaffProfileDialog v-if="profileTarget" :target="profileTarget" scope="organizer" @close="profileTarget=null" @saved="profileSaved" /><StaffAccountForm v-if="creating" verifier @close="creating=false" @created="created" />
<Teleport to="body"><div v-if="permissionTarget" class="staff-overlay"><section class="staff-modal" role="dialog" aria-modal="true"><div class="permission-heading"><h2>核销活动权限</h2><button class="staff-action" @click="permissionTarget=null">关闭</button></div><p class="staff-hint">{{ permissionTarget.name || permissionTarget.username }} · {{ permissionTarget.username }}。权限按活动分配，覆盖该活动下的场次。</p><p v-if="permissionLoading">场次加载中…</p><p v-else-if="permissionError" class="staff-error">{{ permissionError }} <button @click="viewPermissions(permissionTarget)">重试</button></p><p v-else-if="!permissionActivities.length" class="staff-hint">尚未分配核销活动。</p><article v-for="a in permissionActivities" v-else :key="a.id" class="permission-activity"><strong>{{ a.title }}</strong><span> · {{ activityStatus(a.status) }}</span><p v-for="session in a.sessions" :key="session.id" class="staff-hint">{{ formatSession(session) }}</p><p v-if="!a.sessions.length" class="staff-hint">暂无场次</p></article></section></div><div v-if="editing" class="staff-overlay"><form class="staff-modal" role="dialog" aria-modal="true" @submit.prevent="assign"><h2>分配核销活动</h2><p class="staff-hint">{{ editing.name || editing.username }} · {{ editing.username }}。仅可选择本机构活动；取消勾选会撤销该活动权限，历史核销记录保留。</p><div class="staff-choices"><label v-for="a in activities" :key="a.id"><input v-model="selected" type="checkbox" :value="String(a.id)" :disabled="saving" />{{ a.title }}</label><p v-if="!activities.length" class="staff-hint">本机构暂无活动。</p></div><p v-if="actionError" class="staff-error">{{ actionError }}</p><div class="staff-buttons"><button type="button" :disabled="saving" @click="editing=null">取消</button><button class="staff-primary" :disabled="saving">{{ saving ? '保存中…' : '保存分配' }}</button></div></form></div>
<div v-if="pending" class="staff-overlay"><section class="staff-modal" role="dialog" aria-modal="true"><h2>{{ pending.enabled ? '停用核销员账号' : '启用核销员账号' }}</h2><p class="staff-hint">{{ pending.name || pending.username }} · {{ pending.username }}</p><p class="staff-hint">{{ pending.enabled ? '停用后将撤销登录会话，禁止继续核销和重新登录。历史核销记录保留。' : '启用后允许重新登录，原有会话不会恢复。' }}</p><label class="staff-hint reason-field">{{ pending.enabled?'停用原因（必填）':'操作备注（选填）' }}<textarea v-model="operationReason" rows="3" maxlength="500" placeholder="请填写原因或备注"></textarea></label><p v-if="actionError" class="staff-error">{{ actionError }}</p><div class="staff-buttons"><button :disabled="saving" @click="pending=null">取消</button><button class="staff-primary" :disabled="saving" @click="changeEnabled">{{ saving ? '处理中…' : '确认' }}</button></div></section></div></Teleport>
</main>
</template>
<script setup>
import { onMounted,ref,watch,computed } from 'vue'
import {useRoute} from 'vue-router'
const route=useRoute()
import AppIcon from '../../components/AppIcon.vue'
import StaffAccountForm from '../../components/StaffAccountForm.vue'
import { organizerStaffApi } from '../../api/organizerStaff'
import StaffIdentity from '../../components/StaffIdentity.vue'
import StaffProfileDialog from '../../components/StaffProfileDialog.vue'
import {organizerScheduleApi} from '../../api/organizerSchedule'
const operationReason=ref(''),permissionTarget=ref(null),permissionActivities=ref([]),permissionLoading=ref(false),permissionError=ref('')
const activityStatus=v=>({DRAFT:'草稿',PENDING:'待审核',PUBLISHED:'已发布',REJECTED:'已驳回',OFFLINE:'已下架',CANCELLED:'已取消'}[v]||v||'资料不可用')
const formatSession=s=>[s.startTime,s.endTime].map(v=>v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'未设置').join(' 至 ')
let permissionRequest=0
async function viewPermissions(item){const request=++permissionRequest;permissionTarget.value=item;permissionLoading.value=true;permissionError.value='';permissionActivities.value=[];try{const result=await Promise.all((item.activityIds||[]).map(async id=>{const activity=activities.value.find(a=>String(a.id)===String(id));return {...(activity||{id,title:'活动资料不可用'}),sessions:await organizerScheduleApi.listSessions(id)||[]}}));if(request===permissionRequest)permissionActivities.value=result}catch(e){if(request===permissionRequest)permissionError.value=e.message||'权限详情加载失败'}finally{if(request===permissionRequest)permissionLoading.value=false}}
const profileTarget=ref(null)
async function profileSaved(){notice.value='工作人员资料已更新';await load()}
const accounts=ref([]),activities=ref([]),loading=ref(false),creating=ref(false),editing=ref(null),pending=ref(null),selected=ref([]),saving=ref(false),error=ref(''),actionError=ref(''),notice=ref('')
watch(pending,()=>{actionError.value='';operationReason.value=''})
async function load(){loading.value=true;error.value='';try{const result=await Promise.all([organizerStaffApi.list(),organizerStaffApi.activities()]);accounts.value=result[0];activities.value=result[1]}catch(e){error.value=e.message}finally{loading.value=false}}
async function created(){creating.value=false;notice.value='核销员账号已创建，请分配允许核销的活动。';await load()}
function openAssignments(item){editing.value=item;selected.value=(item.activityIds||[]).map(String);actionError.value=''}
async function assign(){if(saving.value)return;saving.value=true;actionError.value='';try{await organizerStaffApi.assign(editing.value.id,selected.value);editing.value=null;notice.value='核销活动权限已保存。';await load()}catch(e){actionError.value=e.message}finally{saving.value=false}}
async function changeEnabled(){if(saving.value)return;if(pending.value.enabled&&!operationReason.value.trim()){actionError.value='请填写停用原因';return;}saving.value=true;actionError.value='';try{await organizerStaffApi.enabled(pending.value.id,!pending.value.enabled,operationReason.value.trim());pending.value=null;notice.value='账号状态已更新。';await load()}catch(e){actionError.value=e.message}finally{saving.value=false}}
onMounted(load)
</script>
<style scoped>.reason-field{display:block}.reason-field textarea{display:block;box-sizing:border-box;width:100%;padding:10px;margin-top:7px;border:1px solid #d5e0f4;border-radius:8px;font:inherit}.permission-heading{display:flex;align-items:center;justify-content:space-between}.permission-activity{border-top:1px solid #e1e8f4;padding:15px 0}.permission-activity span{font-size:12px;color:#74829a}.staff-list{padding:20px}.staff-table-scroll{overflow:auto}.staff-table{min-width:760px}.management-button{gap:7px}</style>

