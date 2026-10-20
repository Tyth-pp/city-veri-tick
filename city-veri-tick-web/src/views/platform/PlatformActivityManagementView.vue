<template>
  <main class="content">
    <div class="title">
      <div>
        <div class="eyebrow">平台运营 / 活动审核</div>
        <h1>活动审核</h1>
        <p>审核、发布和下架平台内全部活动。</p>
      </div>
    </div>
    <section class="panel">
      <div class="filter-row">
        <input v-model.trim="keyword" placeholder="搜索活动名称、场地或 ID" @keyup.enter="search" />
        <select v-model="status">
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="PENDING">待审核</option>
          <option value="PUBLISHED">已发布</option>
          <option value="REJECTED">已驳回</option>
          <option value="OFFLINE">已下架</option>
        </select>
        <button type="button" @click="search">查询</button>
      </div>
      <div v-if="error" class="error">
        {{ error }}<button @click="load">重新加载</button>
      </div>
      <div v-else-if="loading">活动加载中...</div>
      <div v-else-if="!activities.length">暂无活动</div>
      <table v-else>
        <thead>
          <tr>
            <th>活动</th>
            <th>状态</th>
            <th>主办方 / 提交负责人</th><th>提交时间</th>
            <th>场地</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="activity in activities" :key="activity.id">
            <td class="activity-cell">
              <RouterLink :to="{path:`/management/platform/activities/${activity.id}`,query:{...route.query}}">
                <span class="activity-cover" :style="coverStyle(activity)"><span v-if="!activity.coverFileUrl">活动</span></span><span><b>{{ activity.title || "未命名活动" }}</b></span>
              </RouterLink>
              <small>ID：{{ activity.id }}</small>
            </td>
            <td>{{ labels[activity.status] || activity.status }}</td>
            <td>{{ activity.organizerName || (activity.organizerId ? '机构 ID：'+activity.organizerId : '未记录') }}<small>{{ activity.submitterName || '历史记录未记录负责人' }}</small></td><td>{{ activity.submittedAt ? String(activity.submittedAt).replace('T',' ').slice(0,16) : '未提交或历史记录未记录' }}</td>
            <td>{{ activity.venueName || "未设置" }}</td>
            <td>
              <RouterLink class="detail-link" :to="{path:`/management/platform/activities/${activity.id}`,query:{...route.query}}">{{ activity.status==='PENDING'?'查看审核':'查看详情' }}</RouterLink>            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && !error && total" class="pagination">
        <span>共 {{ total }} 项，第 {{ page }} 页</span>
        <div>
          <button type="button" :disabled="page === 1" @click="previousPage">上一页</button>
          <button type="button" :disabled="page * pageSize >= total" @click="nextPage">下一页</button>
        </div>
      </div>
    </section>
  </main>
</template>
<script setup>
import { onMounted, ref, watch } from "vue";
import {useRoute,useRouter} from "vue-router";
const route=useRoute(),router=useRouter();
import { platformActivityApi } from "../../api/platformActivity";
const activities = ref([]),
  keyword = ref(""),
  status = ref(""),
  page = ref(1),
  pageSize = 10,
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  labels = {
    DRAFT: "草稿",
    PENDING: "待审核",
    PUBLISHED: "已发布",
    REJECTED: "已驳回",
    OFFLINE: "已下架",
  };
let request=0;
async function load() {
  const current=++request;
  loading.value = true;
  error.value = "";
  try {
    const result = await platformActivityApi.list({
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      page: page.value,
      pageSize,
    });
    if(current===request){activities.value = result?.records || [];total.value = result?.total || 0;}
  } catch (e) {
    if(current===request)error.value = e?.message || "加载失败";
  } finally {
    if(current===request)loading.value = false;
  }
}
function search() {
  page.value = 1;
  load();
}
function previousPage() {
  if (page.value > 1) {
    page.value -= 1;
    load();
  }
}
function nextPage() {
  if (page.value * pageSize < total.value) {
    page.value += 1;
    load();
  }
}
function coverStyle(activity) { return activity?.coverFileUrl ? { backgroundImage: `url(${activity.coverFileUrl})` } : {} }
watch(()=>route.query,q=>{keyword.value=String(q.keyword||'');status.value=String(q.status||'');page.value=Math.max(1,Number(q.page)||1);load()},{immediate:true});
watch([keyword,status,page],()=>{router.replace({query:{...route.query,keyword:keyword.value||undefined,status:status.value||undefined,page:page.value>1?page.value:undefined}})});
</script>
<style scoped>
.content {
  padding: 34px;
  max-width: 1400px;
}
.title {
  margin-bottom: 28px;
}
.eyebrow {
  color: #6f7690;
  font-size: 13px;
  margin-bottom: 10px;
}
.title h1 {
  margin: 0 0 8px;
  font-size: 30px;
}
.title p {
  margin: 0;
  color: #6e7387;
}
.panel {
  background: #fff;
  border: 1px solid #e1e5ef;
  border-radius: 10px;
  padding: 22px;
  overflow: auto;
}
.panel table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}
.panel th,
.panel td {
  text-align: left;
  padding: 16px;
  border-bottom: 1px solid #ececf3;
}
.panel th {
  color: #61677a;
  font-size: 13px;
}
.panel small {
  display: block;
  color: #858b9d;
  margin-top: 5px;
}
.panel button {
  margin-right: 8px;
  border: 1px solid #cfd8f4;
  border-radius: 6px;
  padding: 7px 11px;
  background: #f5f7ff;
  color: #0d50dc;
  cursor: pointer;
}
.filter-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.filter-row input,
.filter-row select {
  border: 1px solid #dfe2ee;
  border-radius: 7px;
  padding: 9px 11px;
  background: #fafaff;
  min-width: 190px;
}
.filter-row button,
.detail-link {
  border: 1px solid #cfd8f4;
  border-radius: 6px;
  padding: 8px 13px;
  background: #f5f7ff;
  color: #0d50dc;
  cursor: pointer;
  text-decoration: none;
}
.detail-link {
  display: inline-block;
  margin-right: 8px;
}
.activity-cell b,
.activity-cell small {
  display: block;
}
.activity-cell a {
  color: #202334;
  text-decoration: none;
}
.activity-cell a:hover {
  color: #0d50dc;
}
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  color: #737b90;
}
.pagination button {
  margin: 0 0 0 8px;
}
.error {
  color: #c53a43;
}
.error button {
  margin-left: 12px;
}
</style>
