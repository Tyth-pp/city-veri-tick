<template>
  <main class="content">
    <div class="title-row">
      <div>
        <div class="eyebrow">平台运营 / 活动审核 / 详情</div>
        <h1>{{ detail?.activity?.title || "活动详情" }}</h1>
        <p>核对活动内容、主办方及提审信息，完成活动审核。</p>
      </div>
      <RouterLink class="back-link" :to="{path:'/management/platform/activities',query:{...route.query}}">返回活动审核</RouterLink>
    </div>

    <div v-if="loading" class="panel state">活动详情加载中...</div>
    <div v-else-if="error" class="panel state error">{{ error }}<button @click="load">重新加载</button></div>
    <template v-else-if="detail">
      <section class="panel activity-summary">
        <div class="cover"><ActivityCover :src="detail.activity.coverFileUrl || ''" :title="detail.activity.title" /></div>
        <div class="summary-copy">
          <div class="status">{{ statusLabel(detail.activity.status) }}</div>
          <h2>{{ detail.activity.title }}</h2>
          <p>{{ detail.activity.description || "暂无活动介绍" }}</p>
          <dl>
            <div><dt>活动场地</dt><dd>{{ detail.activity.venueName || "未设置" }}</dd></div>
            <div><dt>场地地址</dt><dd>{{ detail.activity.venueAddress || "未设置" }}</dd></div>
            <div><dt>活动 ID</dt><dd>{{ detail.activity.id }}</dd></div>
            <div><dt>活动分类</dt><dd>{{ categoryLabel(detail.activity.category) }}</dd></div>
          </dl>
        </div>
      </section>

      <section class="panel">
        <div class="panel-heading"><h2>主办方信息</h2><RouterLink v-if="detail.organizer" :to="`/management/platform/organizers/${detail.organizer.id}`">查看主办方详情</RouterLink></div>
        <dl v-if="detail.organizer">
          <div><dt>机构名称</dt><dd>{{ detail.organizer.name || '未填写' }}</dd></div>
          <div><dt>机构状态</dt><dd>{{ organizerStatus(detail.organizer.status) }}</dd></div>
          <div><dt>主办方联系人</dt><dd>{{ detail.organizer.contactName || '未填写' }}</dd></div>
          <div><dt>联系电话</dt><dd>{{ detail.organizer.contactPhone || '未填写' }}</dd></div>
        </dl><p v-else class="muted">主办方资料不可用</p>
      </section>

      <section class="panel">
        <div class="panel-heading"><h2>本次提审信息</h2></div>
        <dl>
          <div><dt>本次提审人</dt><dd><StaffIdentity v-if="detail.submitter" :account="detail.submitter" :subtitle="detail.submitter.username" /><template v-else>{{ detail.submittedBy ? '提审账号已不可用' : '历史记录未记录' }}</template></dd></div>
          <div><dt>提审账号 ID</dt><dd>{{ detail.submitter?.id || detail.submittedBy || '历史记录未记录' }}</dd></div>
          <div><dt>提审人手机号</dt><dd>{{ detail.submitter ? (detail.submitter.phone || '未填写') : (detail.submittedBy ? '账号资料不可用' : '历史记录未记录') }}</dd></div>
          <div><dt>提交审核时间</dt><dd>{{ detail.submittedAt ? formatDate(detail.submittedAt) : '历史记录未记录' }}</dd></div>
        </dl>
      </section>

      <section class="panel activity-copy">
        <h2>活动内容</h2>
        <h3>活动介绍</h3><p>{{ detail.activity.content || detail.activity.description || '未填写' }}</p>
        <h3>活动亮点</h3><p>{{ detail.activity.highlights || '未填写' }}</p>
        <h3>注意事项</h3><p>{{ detail.activity.notices || '未填写' }}</p>
      </section>

      <section class="panel">
        <div class="panel-heading"><h2>场次与票种</h2><span>{{ detail.sessions.length }} 个场次，{{ detail.ticketTypes.length }} 个票种</span></div>
        <div v-if="!detail.sessions.length" class="muted">暂无场次</div>
        <div v-for="session in detail.sessions" :key="session.id" class="session">
          <div class="session-heading">
            <strong>{{ formatDate(session.startTime) }} - {{ formatDate(session.endTime) }}</strong>
            <span>{{ sessionStatus(session.status) }} · 容量 {{ session.capacity }} · 已售 {{ session.soldCount || 0 }}</span>
          </div>
          <p class="session-sale">场次统一售票时间：{{ formatDate(session.saleStartTime) }} — {{ formatDate(session.saleEndTime) }}</p>
          <table>
            <thead><tr><th>票种</th><th>价格</th><th>可售库存</th><th>锁定库存</th><th>售票时间</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="ticket in ticketsFor(session.id)" :key="ticket.id">
                <td>{{ ticket.name }}</td><td>¥{{ ticket.price }}</td>
                <td>{{ ticket.stock ?? 0 }}</td><td>{{ ticket.lockedStock ?? 0 }}</td>
                <td class="ticket-sale">{{ formatDate(ticket.saleStartTime) }}<br />至 {{ formatDate(ticket.saleEndTime) }}</td>
                <td>{{ ticket.enabled ? "启用" : "停用" }}</td>
              </tr>
              <tr v-if="!ticketsFor(session.id).length"><td colspan="6" class="muted">暂无票种</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <h2>审核信息</h2>
        <dl>
          <div><dt>当前状态</dt><dd>{{ statusLabel(detail.activity.status) }}</dd></div>
          <div><dt>审核人</dt><dd><StaffIdentity v-if="detail.reviewer" :account="detail.reviewer" :subtitle="detail.reviewer.username" /><template v-else>{{ detail.activity.reviewedBy ? '审核账号已不可用' : '暂无审核记录' }}</template></dd></div>
          <div><dt>审核时间</dt><dd>{{ detail.activity.reviewedAt ? formatDate(detail.activity.reviewedAt) : '暂无审核记录' }}</dd></div>
        </dl>
        <p v-if="detail.activity.rejectionReason" class="rejection-reason"><strong>{{ detail.activity.status==='OFFLINE'?'下架原因：':'驳回原因：' }}</strong>{{ detail.activity.rejectionReason }}</p>
        <div v-if="detail.activity.status==='PUBLISHED'" class="review-actions"><span>下架后停止售票，既有订单与票券记录保留。</span><button class="review-button reject" data-action="offline" @click="openReview('offline')">下架活动</button></div>
        <div v-if="detail.activity.status === 'PENDING'" class="review-actions"><span>请核对活动内容、场次及售票信息后再审核。</span><button type="button" class="review-button reject" data-action="reject" @click="openReview('reject')">驳回</button><button type="button" class="review-button primary" data-action="approve" @click="openReview('approve')">通过审核</button></div>
        <p v-if="notice" class="review-notice" role="status">{{ notice }}</p>
      </section>
    </template>
    <div v-if="reviewAction" class="review-overlay" @click.self="closeReview">
      <form class="review-dialog" role="dialog" aria-modal="true" aria-labelledby="review-title" @submit.prevent="confirmReview">
        <div class="dialog-heading"><h2 id="review-title">{{ reviewAction === 'approve' ? '确认通过审核' : reviewAction==='offline'?'下架活动':'驳回活动' }}</h2><button type="button" aria-label="关闭审核弹窗" :disabled="reviewing" @click="closeReview"><AppIcon name="close" size="20" /></button></div>
        <p>{{ reviewAction === 'approve' ? '通过后活动将发布，请确认活动内容和售票安排准确。' : reviewAction==='offline'?'请说明下架原因；既有订单和票券记录将保留。':'请说明需要主办方修改的具体内容，便于修改后重新提交。' }}</p>
        <label v-if="reviewAction !== 'approve'">{{ reviewAction==='offline'?'下架原因':'驳回原因' }}<textarea v-model="reason" name="reason" rows="4" maxlength="500" autofocus placeholder="填写具体原因，将告知主办方（最多500字）"></textarea></label>
        <p v-if="reviewError" class="review-error" role="alert">{{ reviewError }}</p>
        <div class="dialog-actions"><button type="button" class="review-button" :disabled="reviewing" @click="closeReview">取消</button><button class="review-button primary" :disabled="reviewing">{{ reviewing ? '提交中…' : '确认提交' }}</button></div>
      </form>
    </div>
  </main>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { platformActivityApi } from "../../api/platformActivity";
import ActivityCover from '../../components/ActivityCover.vue';
import StaffIdentity from '../../components/StaffIdentity.vue';
import AppIcon from '../../components/AppIcon.vue';

const route = useRoute();
const detail = ref(null);
const loading = ref(true);
const error = ref("");
const labels = { DRAFT: "草稿", PENDING: "待审核", PUBLISHED: "已发布", REJECTED: "已驳回", OFFLINE: "已下架", CANCELLED: '已取消' };
const statusLabel = (value) => labels[value] || value || "未知";
const formatDate = (value) => value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "未设置";
const ticketsFor = (sessionId) => (detail.value?.ticketTypes || []).filter((ticket) => String(ticket.sessionId) === String(sessionId));
const categoryLabel = value => ({MOVIE:'电影',CONCERT:'演唱会',PERFORMANCE:'演出',STAND_UP:'脱口秀',EXHIBITION_ART:'展览艺术',MUSIC_FESTIVAL:'音乐节',OPERA:'戏曲艺术',SPORTS:'体育运动',THEATRE_OPERA:'话剧歌剧'}[value] || value || '未设置');
const organizerStatus = value => ({PENDING:'待审核',APPROVED:'已通过',REJECTED:'已驳回',DISABLED:'已停用'}[value] || value || '未设置');
const sessionStatus = value => ({DRAFT:'草稿',ON_SALE:'已开放',CLOSED:'已关闭',CANCELLED:'已取消'}[value] || value || '未设置');
const reviewAction=ref(''), reason=ref(''), reviewError=ref(''), reviewing=ref(false), notice=ref('');
function openReview(action){reviewAction.value=action;reason.value='';reviewError.value='';notice.value='';}
function closeReview(){if(!reviewing.value)reviewAction.value='';}
async function confirmReview(){
  if(reviewing.value)return;
  reviewError.value='';
  if(reviewAction.value !== 'approve' && !reason.value.trim()){reviewError.value=reviewAction.value==='offline'?'请填写下架原因':'请填写驳回原因';return;}
  reviewing.value=true;
  try{
    const action=reviewAction.value;
    if(action === 'approve')await platformActivityApi.approve(route.params.id);
    else if(action==='offline') await platformActivityApi.offline(route.params.id,reason.value.trim());
    else await platformActivityApi.reject(route.params.id,reason.value.trim());
    reviewAction.value='';notice.value=action === 'approve' ? '审核通过，活动已发布。' : action==='offline'?'活动已下架，已通知主办方。':'活动已驳回，主办方可修改后重新提交。';
    await load();
  }catch(e){reviewError.value=e?.message || '审核失败，请重试';}
  finally{reviewing.value=false;}
}
async function load() {
  loading.value = true;
  error.value = "";
  try { detail.value = await platformActivityApi.detail(route.params.id); }
  catch (e) { error.value = e?.message || "活动详情加载失败"; }
  finally { loading.value = false; }
}
onMounted(load);
</script>

<style scoped>
.panel>h2{margin:0;font-size:18px}.panel-heading a{color:#0d50dc;font-size:13px;text-decoration:none}.activity-copy h3{font-size:14px;margin:22px 0 8px;color:#64748b}.activity-copy p{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8;margin:0}.session-sale,.ticket-sale{color:#64748b;font-size:12px;line-height:1.7}.rejection-reason{background:#fff3f2;color:#b42332;padding:14px;border-radius:8px;white-space:pre-wrap;overflow-wrap:anywhere}.review-actions{display:flex;align-items:center;justify-content:flex-end;gap:12px;border-top:1px solid #e5eaf3;margin-top:24px;padding-top:20px}.review-actions>span{margin-right:auto;font-size:13px;color:#64748b}.review-button{border:1px solid #d5e0f4;background:#fff;color:#385070;padding:10px 18px;border-radius:8px;font:inherit;font-size:14px;cursor:pointer}.review-button.primary{background:#245cf5;border-color:#245cf5;color:#fff}.review-button.reject{color:#c33c4b;border-color:#f5ced3;background:#fff8f8}.review-button:disabled{opacity:.55;cursor:wait}.review-overlay{position:fixed;inset:0;z-index:100;background:rgba(15,23,42,.4);display:flex;align-items:center;justify-content:center;padding:20px}.review-dialog{background:#fff;width:480px;max-width:100%;border-radius:16px;padding:24px;box-shadow:0 24px 80px #142d5140}.dialog-heading{display:flex;align-items:center;justify-content:space-between}.dialog-heading h2{font-size:20px;margin:0}.dialog-heading>button{border:0;background:#f1f5fc;color:#64748b;display:flex;padding:6px;border-radius:50%;cursor:pointer}.review-dialog>p{font-size:14px;color:#64748b;line-height:1.7}.review-dialog label{display:block;font-size:14px;color:#334155}.review-dialog textarea{display:block;box-sizing:border-box;width:100%;resize:vertical;border:1px solid #d5e0f4;border-radius:8px;padding:12px;font:inherit;line-height:1.6;margin-top:8px}.review-dialog textarea:focus{outline:2px solid #b9cfff;border-color:#245cf5}.dialog-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}.review-dialog .review-error{color:#c33c4b}.review-notice{background:#edf9f2;color:#087d52;padding:12px;border-radius:8px}dd{overflow-wrap:anywhere}.panel{overflow-x:auto}@media(max-width:700px){.review-actions{flex-wrap:wrap}.review-actions>span{width:100%}.panel-heading{gap:12px;align-items:flex-start}.panel-heading a{white-space:nowrap}}
.content { padding: 34px; max-width: 1400px; }
.title-row { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:24px; }
.eyebrow { color:#6f7690; font-size:13px; margin-bottom:10px; }
h1 { margin:0 0 8px; font-size:30px; }
.title-row p { margin:0; color:#6e7387; }
.back-link { color:#0d50dc; text-decoration:none; padding-top:28px; }
.panel { background:#fff; border:1px solid #e1e5ef; border-radius:10px; padding:22px; margin-bottom:20px; }
.activity-summary { display:flex; gap:24px; }
.cover { width:250px; min-height:170px; border-radius:8px; background:#e8edff center/cover no-repeat; flex:none; }
.activity-summary .cover{overflow:hidden}.activity-summary .cover :deep(.activity-cover){width:100%;height:100%;min-height:170px;border-radius:inherit}
.summary-copy { min-width:0; }
.summary-copy h2 { margin:10px 0; font-size:24px; }
.summary-copy p { color:#697188; line-height:1.7; }
.status { display:inline-block; padding:5px 10px; border-radius:14px; background:#e5edff; color:#0d50dc; font-size:12px; }
dl { display:grid; grid-template-columns:repeat(3, 1fr); gap:16px; margin:18px 0 0; }
dt { color:#858b9d; font-size:12px; margin-bottom:5px; } dd { margin:0; }
.panel-heading,.session-heading { display:flex; justify-content:space-between; align-items:center; }
.panel-heading h2 { margin:0 0 18px; font-size:18px; } .panel-heading span,.session-heading span { color:#737b90; font-size:13px; }
.session { border-top:1px solid #ececf3; padding:18px 0 2px; }
.session-heading { margin-bottom:12px; }
table { width:100%; border-collapse:collapse; } th,td { text-align:left; padding:12px; border-bottom:1px solid #ececf3; } th { color:#61677a; font-size:13px; }
.muted { color:#858b9d; padding:14px 0; } .state { color:#61677a; } .error { color:#c53a43; } .state button { margin-left:12px; }
@media(max-width:700px){.content{padding:20px 14px}.title-row,.activity-summary,.session-heading{display:block}.back-link{display:inline-block;padding-top:14px}.cover{width:100%;min-height:150px;margin-bottom:18px}dl{grid-template-columns:1fr 1fr}.panel{overflow:auto}table{min-width:620px}}
</style>
