<template>
  <main class="content">
    <div class="title"><div><div class="eyebrow">平台运营 / 主办方管理</div><h1>主办方管理</h1><p>管理机构资料、管理员账号及旗下活动。</p></div><button class="staff-action staff-primary" @click="creating=true">创建主办方</button></div>
    <p v-if="notice" class="staff-notice" role="status">{{ notice }}</p>
    <StaffAccountForm v-if="creating" organization @close="creating=false" @created="created" />
    <section class="panel">
      <div class="filter-row"><input v-model.trim="keyword" placeholder="搜索名称、联系人、手机号或 ID" @keyup.enter="search" /><select v-model="status"><option value="">全部状态</option><option value="PENDING">待审核</option><option value="APPROVED">已通过</option><option value="REJECTED">已驳回</option><option value="DISABLED">已停用</option></select><button type="button" @click="search">查询</button></div>
      <div v-if="error" class="error">{{ error }} <button @click="load">重新加载</button></div><div v-else-if="loading" class="state">主办方加载中...</div><div v-else-if="!organizers.length" class="state">暂无符合条件的主办方</div>
      <table v-else><thead><tr><th>主办方</th><th>联系人</th><th>管理员</th><th>活动</th><th>机构状态</th><th>创建时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in organizers" :key="item.id"><td><b>{{ item.name }}</b><small>ID：{{ item.id }}</small></td><td>{{ item.contactName }}<small>{{ item.contactPhone }}</small></td><td>{{ item.administratorCount ?? '—' }}</td><td>{{ item.activityCount ?? '—' }}</td><td><span class="status" :class="`status-${item.status}`">{{ labels[item.status] || item.status }}</span></td><td>{{ formatDate(item.createdAt) }}</td><td><RouterLink class="detail-link" :to="`/management/platform/organizers/${item.id}`">查看档案</RouterLink></td></tr></tbody></table>
      <div v-if="!loading && !error && total" class="pagination"><span>共 {{ total }} 家，第 {{ page }} 页</span><div><button :disabled="page === 1" @click="previousPage">上一页</button><button :disabled="page * pageSize >= total" @click="nextPage">下一页</button></div></div>
    </section>
  </main>
</template>
<script setup>
import { onMounted, ref, watch } from "vue";
import {useRoute} from "vue-router";
const route=useRoute();
import { platformOrganizerApi } from "../../api/platformOrganizer";
import StaffAccountForm from "../../components/StaffAccountForm.vue";
const creating=ref(route.query.create==='1'),notice=ref("");
async function created(){creating.value=false;notice.value="主办方及管理员账号已创建，首次登录需修改密码。";page.value=1;await load();}
const organizers=ref([]),keyword=ref(""),status=ref(""),page=ref(1),pageSize=10,total=ref(0),loading=ref(false),error=ref("");
const labels={PENDING:"待审核",APPROVED:"已通过",REJECTED:"已驳回",DISABLED:"已停用"};
const formatDate=(value)=>value?new Date(value).toLocaleString("zh-CN",{hour12:false}):"-";
async function load(){loading.value=true;error.value="";try{const result=await platformOrganizerApi.list({keyword:keyword.value||undefined,status:status.value||undefined,page:page.value,pageSize});organizers.value=result?.records||[];total.value=result?.total||0;}catch(e){error.value=e?.message||"主办方加载失败";}finally{loading.value=false;}}
function search(){page.value=1;load();} function previousPage(){if(page.value>1){page.value--;load();}} function nextPage(){if(page.value*pageSize<total.value){page.value++;load();}}
onMounted(load);
</script>
<style scoped>
.title{display:flex;justify-content:space-between;align-items:center;gap:18px}
.content{padding:34px;max-width:1400px}.title{margin-bottom:28px}.eyebrow{color:#6f7690;font-size:13px;margin-bottom:10px}h1{margin:0 0 8px;font-size:30px}.title p{margin:0;color:#6e7387}.panel{background:#fff;border:1px solid #e1e5ef;border-radius:10px;padding:22px;overflow:auto}.filter-row{display:flex;gap:10px;margin-bottom:18px}.filter-row input,.filter-row select{border:1px solid #dfe2ee;border-radius:7px;padding:9px 11px;background:#fafaff;min-width:190px}.filter-row button,.detail-link,.pagination button{border:1px solid #cfd8f4;border-radius:6px;padding:8px 13px;background:#f5f7ff;color:#0d50dc;cursor:pointer;text-decoration:none}.panel table{width:100%;border-collapse:collapse;min-width:760px}.panel th,.panel td{text-align:left;padding:16px;border-bottom:1px solid #ececf3}.panel th{color:#61677a;font-size:13px}.panel small{display:block;color:#858b9d;margin-top:5px}.status{display:inline-block;padding:5px 9px;border-radius:12px;font-size:12px}.status-PENDING{background:#fff3dc;color:#a86b00}.status-APPROVED{background:#e0f8e9;color:#16834a}.status-REJECTED,.status-DISABLED{background:#ffe7e7;color:#c53a43}.state{color:#737b90;padding:18px 0}.error{color:#c53a43;padding:10px 0}.pagination{display:flex;justify-content:space-between;align-items:center;padding-top:18px;color:#737b90}.pagination button{margin-left:8px}.pagination button:disabled{opacity:.45;cursor:not-allowed}@media(max-width:700px){.content{padding:20px 14px}.filter-row{flex-wrap:wrap}.filter-row input{min-width:220px;width:100%}}
</style>
