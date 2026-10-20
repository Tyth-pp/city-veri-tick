<template>
  <main class="management-content">
    <div class="management-title">
      <div>
        <div class="eyebrow">资源中心 / 活动编辑</div>
        <h1>{{ isEdit ? "编辑活动" : "新建活动草稿" }} <span class="draft-badge">{{ ({DRAFT:'草稿',REJECTED:'已驳回',PENDING:'审核中',PUBLISHED:'已发布',OFFLINE:'已下架'})[form.status]||form.status }}</span></h1>
        <p>完善活动基础信息，再配置场次和票种。</p>
      </div>
      <div class="editor-actions"><RouterLink class="management-button light" to="/management/organizer/activities">取消</RouterLink><button class="management-button light" type="button" :disabled="saving||!canEditConfiguration" @click="save()">保存草稿</button><button class="management-button" type="button" :disabled="saving" @click="continueConfiguration">{{ isEdit ? '下一步：配置场次与票种' : '保存并继续' }} <AppIcon name="arrow-right" size="16" /></button></div>
    </div>
    <nav class="editor-steps"><div class="active"><b>01</b><span><strong>基础信息与场地</strong><small>活动名称、场地地址与封面</small></span></div><div><b>02</b><span><strong>演出场次与排期</strong><small>日期、场次与容量</small></span></div><div><b>03</b><span><strong>票种与库存</strong><small>票价、库存和销售时间</small></span></div><div><b>04</b><span><strong>检查并提交</strong><small>确认信息后提交审核</small></span></div></nav>
    <div class="editor-layout">
      <div class="editor-main">
    <form class="management-card activity-form" @submit.prevent="save"><fieldset :disabled="!canEditConfiguration" class="activity-fields">
      <div class="section-heading form-heading"><div><div class="form-section-title"><AppIcon name="clipboard-check" size="19" /> 活动基础信息</div><p>填写活动公开展示的基本内容。</p></div><span class="ready"><i></i>请手动保存草稿</span></div>
      <div class="form-grid">
        <div class="form-field full">
          <label>活动名称 <em>*</em></label
          ><input
            v-model.trim="form.title"
            required
            placeholder="请输入活动名称"
          />
        </div>
        <div class="form-field full"><label>活动分类 <em>*</em></label><select v-model="form.category" required><option value="" disabled>请选择活动分类</option><option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option></select></div>
        <div class="form-field full">
          <label>活动内容 <em>*</em></label
          ><textarea
            v-model.trim="form.content"
            required
            placeholder="介绍活动的主要内容和流程"
          ></textarea>
        </div>
        <div class="form-field full"><label>活动亮点 <em>*</em></label><textarea v-model.trim="form.highlights" required placeholder="说明活动最值得关注的特色"></textarea></div>
        <div class="form-field full"><label>注意事项 <em>*</em></label><textarea v-model.trim="form.notices" required placeholder="说明入场、退改或观演注意事项"></textarea></div>
        <div class="form-field">
          <label>场地名称</label
          ><input
            v-model.trim="form.venueName"
            placeholder="例如：城市体育馆"
          />
        </div>
        <div class="form-field">
          <label>场地地址</label
          ><input
            v-model.trim="form.venueAddress"
            placeholder="请输入详细地址"
          />
        </div>
        <div class="form-field full">
          <label>活动封面 <small>推荐 16:9，1280 × 720</small></label>
          <div class="cover-upload">
            <div class="cover-preview" :style="coverStyle">
              <span v-if="!coverUrl && !form.coverFileUrl">封面预览</span>
            </div>
            <label class="file-picker"><input type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="uploadCover" /><span class="file-picker-button">选择文件</span><span class="file-name">{{ selectedFileName || '未选择文件' }}</span></label><span class="cell-muted">支持 JPG、PNG、WEBP、GIF，最大 10MB</span>
          </div>
        </div>
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <div class="form-actions">
        <RouterLink class="management-button light" to="/management/organizer/activities"
          >取消</RouterLink
        ><button class="management-button" :disabled="saving">
          {{ saving ? "保存中..." : "保存草稿" }}</button
        ><button
          v-if="
            isEdit && (form.status === 'DRAFT' || form.status === 'REJECTED')
          "
          type="button"
          class="management-button"
          :disabled="saving"
          @click="submitForReview"
        >
          提交审核
        </button>
      </div>
    </fieldset></form>
    <section v-if="isEdit" class="management-card schedule-card">
      <div class="section-heading">
        <div>
          <div class="form-section-title">场次与票种</div>
          <p>每个场次可以配置多个票种，票种库存将用于下单和锁库存。</p>
        </div>
        <button
          class="management-button"
          type="button"
          :disabled="!canEditConfiguration"
          @click="openSessionForm"
        >
          ＋ 新建场次
        </button>
      </div>
        <div v-if="!canEditConfiguration" class="state-notice">当前活动处于“{{ form.status === 'PENDING' ? '审核中' : '已发布' }}”状态，场次和票种暂不可修改。</div>
        <p v-if="scheduleError && !sessionFormOpen && !ticketFormOpen" class="schedule-alert" role="alert"><AppIcon name="info" size="18" />{{ scheduleError }}</p>
      <div v-if="sessionFormOpen" class="inline-form">
        <h3>{{ editingSession ? "编辑场次" : "新建场次" }}</h3>
        <p v-if="scheduleError" class="schedule-alert" role="alert"><AppIcon name="info" size="18" />{{ scheduleError }}</p>
        <div class="form-grid">
          <div class="form-field">
            <label>开始时间</label
            ><div class="datetime-pair"><label><span>日期（年/月/日）</span><ChineseDateTimeInput lang="zh-CN" v-model="sessionForm.startDate" type="date" required aria-label="开始日期" /></label><label><span>时间（时/分）</span><ChineseDateTimeInput v-model="sessionForm.startClock" type="time" step="60" required aria-label="开始时间" /></label></div>
          </div>
          <div class="form-field">
            <label>结束时间</label
            ><div class="datetime-pair"><label><span>日期（年/月/日）</span><ChineseDateTimeInput lang="zh-CN" v-model="sessionForm.endDate" type="date" required aria-label="结束日期" /></label><label><span>时间（时/分）</span><ChineseDateTimeInput v-model="sessionForm.endClock" type="time" step="60" required aria-label="结束时间" /></label></div>
          </div>
          <div class="form-field">
            <label>统一售票开始</label>
            <div class="datetime-pair"><label><span>日期（年/月/日）</span><ChineseDateTimeInput v-model="sessionForm.saleStartDate" type="date" aria-label="售票开始日期" /></label><label><span>时间（时/分）</span><ChineseDateTimeInput v-model="sessionForm.saleStartClock" type="time" aria-label="售票开始时间" /></label></div>
          </div>
          <div class="form-field">
            <label>统一售票结束</label>
            <div class="datetime-pair"><label><span>日期（年/月/日）</span><ChineseDateTimeInput v-model="sessionForm.saleEndDate" type="date" aria-label="售票结束日期" /></label><label><span>时间（时/分）</span><ChineseDateTimeInput v-model="sessionForm.saleEndClock" type="time" aria-label="售票结束时间" /></label></div>
          </div>
          <div class="form-field">
            <label>容量</label><input v-model.number="sessionForm.capacity" type="number" min="1" required />
          </div>
        </div>
        <p class="sale-time-note">本场次所有票种共用此售票时间，售票结束不能晚于场次开始。修改后将同步更新本场次全部票种。</p>
        <div class="form-actions">
          <button
            class="management-button light"
            type="button"
            @click="closeSessionForm"
          >
            取消</button
          ><button
            class="management-button"
            type="button"
            :disabled="scheduleSaving"
            @click="saveSession"
          >
            保存场次
          </button>
        </div>
      </div>
      <div v-if="!sessions.length && !scheduleLoading" class="empty-panel">
        暂未创建场次，请先创建场次。
      </div>
      <div v-for="session in sessions" :key="session.id" class="session-panel">
        <div class="session-header">
          <div>
            <strong>{{
              formatRange(session.startTime, session.endTime)
            }}</strong
            ><small
              >容量 {{ session.capacity }} · 可用
              {{ session.availableCapacity ?? session.capacity }}</small
            >
            <small v-if="session.saleStartTime && session.saleEndTime">统一售票时间：{{ formatRange(session.saleStartTime, session.saleEndTime) }}</small>
            <small v-else class="legacy-sale-note">尚未设置统一售票时间，原票种时间保留；请编辑场次确认。</small>
          </div>
          <div class="action-row">
            <button
              class="table-action"
              type="button"
              :disabled="!canEditConfiguration"
              @click="editSession(session)"
            >
              编辑场次</button
            ><button
              class="table-action danger"
              type="button"
              :disabled="!canEditConfiguration"
              @click="removeSession(session)"
            >
              删除</button
            ><button
              class="table-action primary"
              type="button"
              :disabled="!canEditConfiguration"
              @click="openTicketForm(session)"
            >
              ＋ 添加票种
            </button>
          </div>
        </div>
        <div v-if="session.ticketTypes?.length" class="ticket-list">
          <div
            v-for="ticket in session.ticketTypes"
            :key="ticket.id"
            class="ticket-row"
          >
            <div class="ticket-info">
              <b>{{ ticket.name }}</b
              ><small
                >可售 {{ ticket.inventory?.stock ?? ticket.stock }} · 锁定
                {{ ticket.inventory?.lockedStock ?? ticket.lockedStock ?? 0 }} · 总量
                {{ ticket.inventory?.totalStock ?? ticket.stock }}</small
              >
            </div>
            <strong>¥{{ ticket.price }}</strong
            ><span class="ticket-state" :class="ticket.enabled ? 'is-on' : 'is-off'">{{ ticket.enabled ? "已启用" : "已停用" }}</span
            ><button class="table-action" type="button" :disabled="!canEditConfiguration" @click="openTicketEdit(session, ticket)">编辑</button
            ><button class="table-action" type="button" :disabled="!canEditConfiguration" @click="toggleTicket(ticket)">{{ ticket.enabled ? "停用" : "启用" }}</button
            ><button
              class="table-action danger"
              type="button"
              :disabled="!canEditConfiguration"
              @click="removeTicket(ticket)"
            >
              删除
            </button>
          </div>
        </div>
        <div v-else class="ticket-empty">该场次还没有票种</div>
      </div>
      <div v-if="ticketFormOpen" class="inline-form ticket-form">
        <h3>{{ editingTicket ? "编辑票种" : "为场次添加票种" }}</h3>
        <p v-if="scheduleError" class="schedule-alert" role="alert"><AppIcon name="info" size="18" />{{ scheduleError }}</p>
        <div class="form-grid">
          <div class="form-field">
            <label>票种名称</label
            ><input v-model.trim="ticketForm.name" placeholder="例如：普通票" />
          </div>
          <div class="form-field">
            <label>价格</label
            ><input
              v-model.number="ticketForm.price"
              type="number"
              min="0"
              step="0.01"
            />
          </div>
          <div class="form-field">
            <label>库存</label
            ><input v-model.number="ticketForm.stock" type="number" min="0" />
          </div>
          <div class="form-field">
            <label>启用状态</label
            ><select v-model="ticketForm.enabled">
              <option :value="true">销售中</option>
              <option :value="false">已停用</option>
            </select>
          </div>
        </div>
        <p v-if="selectedSession.saleStartTime" class="sale-time-note">沿用本场次统一售票时间：{{ formatRange(selectedSession.saleStartTime, selectedSession.saleEndTime) }}。如需调整，请编辑场次。</p>
        <p v-else class="sale-time-note">此票种保留原售票时间：{{ formatRange(editingTicket?.saleStartTime, editingTicket?.saleEndTime) }}。如需统一调整，请编辑场次。</p>
        <div class="form-actions">
          <button
            class="management-button light"
            type="button"
            @click="closeTicketForm"
          >
            取消</button
          ><button class="management-button" type="button" :disabled="scheduleSaving" @click="saveTicket">
            保存票种
          </button>
        </div>
      </div>
    </section>
      </div>
      <aside class="editor-aside">
        <section class="preview-card">
          <div class="aside-heading"><div><strong>手机预览</strong><small>活动信息会同步显示</small></div><span>H5</span></div>
          <div class="phone-preview">
            <div class="phone-top"><i></i><span>Venuo</span><b>•••</b></div>
            <div class="phone-cover" :style="coverStyle"><span v-if="!coverUrl && !form.coverFileUrl">活动封面</span></div>
            <div class="phone-content"><span class="preview-tag">{{ categories.find(item => item.value === form.category)?.label || '活动' }}</span><h3>{{ form.title || '你的活动名称' }}</h3><p>{{ form.venueName || '活动场地待填写' }}</p><p>{{ form.content || '补充活动简介，让用户更快了解活动亮点。' }}</p></div>
          </div>
        </section>
        <section class="editor-tip"><span class="tip-dot"></span><div><strong>编辑提示</strong><p>先完善活动名称、场地和封面，再继续配置场次与票种。</p></div></section>
      </aside>
    </div>
  </main>
</template>
<script setup>
import ChineseDateTimeInput from '../../components/ChineseDateTimeInput.vue'
import {askAction} from "../../utils/actionDialog"
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { organizerActivityApi } from "../../api/organizerActivity";
import { uploadImage } from "../../api/file";
import { organizerScheduleApi } from "../../api/organizerSchedule";
import AppIcon from "../../components/AppIcon.vue";
const route = useRoute(),
  router = useRouter(),
  isEdit = computed(() => Boolean(route.params.id)),
  saving = ref(false),
  error = ref(""),
  coverUrl = ref(""),
  selectedFileName = ref("");
const categories = [
  { value: "MOVIE", label: "电影" }, { value: "CONCERT", label: "演唱会" },
  { value: "PERFORMANCE", label: "演出" }, { value: "STAND_UP", label: "脱口秀" },
  { value: "EXHIBITION_ART", label: "展览艺术" }, { value: "MUSIC_FESTIVAL", label: "音乐节" },
  { value: "OPERA", label: "戏曲艺术" }, { value: "SPORTS", label: "体育运动" },
  { value: "THEATRE_OPERA", label: "话剧歌剧" },
];
const form = reactive({
  id: "",
  title: "",
  category: "",
  description: "",
  content: "",
  highlights: "",
  notices: "",
  coverFileId: null,
  coverFileUrl: "",
  venueName: "",
  venueAddress: "",
  status: "DRAFT",
});
const canEditConfiguration = computed(() =>
  ["DRAFT", "REJECTED"].includes(form.status),
);
const sessions = ref([]),
  scheduleLoading = ref(false),
  scheduleSaving = ref(false),
  scheduleError = ref(""),
  sessionFormOpen = ref(false),
  ticketFormOpen = ref(false),
  editingSession = ref(null),
  selectedSession = ref(null),
  editingTicket = ref(null);
const emptySessionForm = () => ({ startDate: "", startClock: "", endDate: "", endClock: "", saleStartDate: "", saleStartClock: "", saleEndDate: "", saleEndClock: "", capacity: 100 });
const sessionForm = reactive(emptySessionForm());
const ticketForm = reactive({
  name: "",
  price: 0,
  stock: 100,
  enabled: true,
});
const coverStyle = computed(() => ({
  backgroundImage: `url(${coverUrl.value || form.coverFileUrl || ""})`,
}));
async function load() {
  if (!isEdit.value) return;
  try {
    const data = await organizerActivityApi.list();
    const current = data.find((x) => String(x.id) === String(route.params.id));
    if (!current) throw new Error("未找到该活动");
    Object.assign(form, current);
    coverUrl.value = current.coverFileUrl || "";
    await loadSessions();
  } catch (e) {
    error.value = e.message;
  }
}
async function loadSessions() {
  scheduleLoading.value = true;
  try {
    const result = await organizerScheduleApi.listSessions(
      String(route.params.id),
    );
    const list = Array.isArray(result) ? result : [];
    sessions.value = await Promise.all(
      list.map(async (session) => {
        const tickets =
          (await organizerScheduleApi.listTicketTypes(String(session.id))) ||
          [];
        const ticketTypes = await Promise.all(
          tickets.map(async (ticket) => {
            try {
              return {
                ...ticket,
                inventory: await organizerScheduleApi.inventory(
                  String(ticket.id),
                ),
              };
            } catch {
              return { ...ticket, inventory: null };
            }
          }),
        );
        return { ...session, ticketTypes };
      }),
    );
  } catch (e) {
    scheduleError.value = e?.message || "场次加载失败";
  } finally {
    scheduleLoading.value = false;
  }
}
function openSessionForm() {
  scheduleError.value = "";
  closeTicketForm();
  editingSession.value = null;
  Object.assign(sessionForm, emptySessionForm());
  sessionFormOpen.value = true;
}
function closeSessionForm() {
  sessionFormOpen.value = false;
  editingSession.value = null;
}
function editSession(session) {
  scheduleError.value = "";
  closeTicketForm();
  editingSession.value = session;
  Object.assign(sessionForm, {
    ...splitDateTime(session.startTime, "start"),
    ...splitDateTime(session.endTime, "end"),
    ...splitDateTime(session.saleStartTime || session.ticketTypes?.[0]?.saleStartTime, "saleStart"),
    ...splitDateTime(session.saleEndTime || session.ticketTypes?.[0]?.saleEndTime, "saleEnd"),
    capacity: session.capacity,
  });
  sessionFormOpen.value = true;
}
function formatDate(value) {
  return value ? value.slice(0, 16).replace("T", " ") : "";
}
function formatRange(start, end) {
  return `${formatDate(start)} - ${formatDate(end)}`;
}
function splitDateTime(value, prefix) {
  const [date = "", clock = ""] = String(value || "").replace("T", " ").split(" ");
  return { [`${prefix}Date`]: date, [`${prefix}Clock`]: clock.slice(0, 5) };
}
function joinDateTime(date, clock) { return date && clock ? `${date}T${clock}:00` : ""; }
async function saveSession() {
  scheduleError.value = "";
  const payload = {
    startTime: joinDateTime(sessionForm.startDate, sessionForm.startClock),
    endTime: joinDateTime(sessionForm.endDate, sessionForm.endClock),
    saleStartTime: joinDateTime(sessionForm.saleStartDate, sessionForm.saleStartClock),
    saleEndTime: joinDateTime(sessionForm.saleEndDate, sessionForm.saleEndClock),
    capacity: sessionForm.capacity,
  };
  if (!payload.startTime || !payload.endTime) scheduleError.value = "请完整填写场次开始和结束的日期、时间";
  else if (payload.endTime <= payload.startTime) scheduleError.value = "场次结束时间必须晚于开始时间";
  else if (!Number.isInteger(payload.capacity) || payload.capacity < 1) scheduleError.value = "场次容量必须是大于 0 的整数";
  else if (!payload.saleStartTime || !payload.saleEndTime) scheduleError.value = "请完整填写本场次的统一售票开始和结束时间";
  else if (payload.saleEndTime <= payload.saleStartTime) scheduleError.value = "售票结束时间必须晚于售票开始时间";
  else if (payload.saleEndTime > payload.startTime) scheduleError.value = "售票结束时间不能晚于场次开始时间";
  else if (editingSession.value && (editingSession.value.ticketTypes || []).reduce((sum, ticket) => sum + Number(ticket.stock || 0) + Number(ticket.lockedStock || 0), 0) > payload.capacity) scheduleError.value = "场次容量不能小于已分配的票种库存总和";
  if (scheduleError.value) return;
  scheduleSaving.value = true;
  try {
    if (editingSession.value)
      await organizerScheduleApi.updateSession(
        editingSession.value.id,
        payload,
      );
    else await organizerScheduleApi.createSession(route.params.id, payload);
    closeSessionForm();
    await loadSessions();
  } catch (e) {
    scheduleError.value = e?.message || "保存场次失败";
  } finally {
    scheduleSaving.value = false;
  }
}
async function removeSession(session) {
  if (!await askAction({title:"删除场次",message:"删除场次会同时影响其票种，确定继续吗？",confirmText:"确认删除"})) return;
  try {
    await organizerScheduleApi.deleteSession(session.id);
    await loadSessions();
  } catch (e) {
    scheduleError.value = e?.message || "删除场次失败";
  }
}
function openTicketForm(session) {
  scheduleError.value = "";
  if (!session.saleStartTime || !session.saleEndTime) {
    editSession(session);
    scheduleError.value = "请先为本场次设置统一售票时间，保存后即可添加票种";
    return;
  }
  closeSessionForm();
  selectedSession.value = session;
  editingTicket.value = null;
  Object.assign(ticketForm, {
    name: "",
    price: 0,
    stock: 100,
    enabled: true,
  });
  ticketFormOpen.value = true;
}
function openTicketEdit(session, ticket) {
  scheduleError.value = "";
  closeSessionForm();
  selectedSession.value = session;
  editingTicket.value = ticket;
  Object.assign(ticketForm, {
    name: ticket.name,
    price: Number(ticket.price),
    stock: ticket.stock,
    enabled: ticket.enabled,
  });
  ticketFormOpen.value = true;
}
function closeTicketForm() {
  ticketFormOpen.value = false;
  selectedSession.value = null;
  editingTicket.value = null;
}
async function saveTicket() {
  if (!selectedSession.value) return;
  scheduleError.value = "";
  const allocated = (selectedSession.value.ticketTypes || []).filter(t => String(t.id) !== String(editingTicket.value?.id)).reduce((sum, t) => sum + Number(t.stock || 0) + Number(t.lockedStock || 0), 0);
  if (!ticketForm.name.trim()) scheduleError.value = "请填写票种名称";
  else if (ticketForm.price === '' || !Number.isFinite(ticketForm.price) || ticketForm.price < 0 || Math.abs(ticketForm.price * 100 - Math.round(ticketForm.price * 100)) > 0.00001) scheduleError.value = "票价须为不小于 0 的金额，最多保留两位小数";
  else if (!Number.isInteger(ticketForm.stock) || ticketForm.stock < 0) scheduleError.value = "库存必须是不小于 0 的整数";
  else if (allocated + ticketForm.stock + Number(editingTicket.value?.lockedStock || 0) > selectedSession.value.capacity) scheduleError.value = "票种库存总和不能超过场次容量";
  else if ((selectedSession.value.ticketTypes || []).some(t => String(t.id) !== String(editingTicket.value?.id) && t.name.toLowerCase() === ticketForm.name.trim().toLowerCase())) scheduleError.value = "同一场次下票种名称不能重复";
  if (scheduleError.value) return;
  scheduleSaving.value = true;
  try {
    const payload = {
      name: ticketForm.name,
      price: ticketForm.price,
      stock: ticketForm.stock,
      enabled: ticketForm.enabled,
      ...(!selectedSession.value.saleStartTime ? { saleStartTime: editingTicket.value?.saleStartTime, saleEndTime: editingTicket.value?.saleEndTime } : {}),
    };
    if (editingTicket.value)
      await organizerScheduleApi.updateTicketType(
        editingTicket.value.id,
        payload,
      );
    else
      await organizerScheduleApi.createTicketType(
        selectedSession.value.id,
        payload,
      );
    closeTicketForm();
    await loadSessions();
  } catch (e) {
    scheduleError.value = e?.message || "保存票种失败";
  } finally {
    scheduleSaving.value = false;
  }
}
async function toggleTicket(ticket) {
  try {
    await organizerScheduleApi.updateTicketType(ticket.id, {
      name: ticket.name,
      price: ticket.price,
      stock: ticket.stock,
      saleStartTime: ticket.saleStartTime,
      saleEndTime: ticket.saleEndTime,
      enabled: !ticket.enabled,
    });
    await loadSessions();
  } catch (e) {
    scheduleError.value = e?.message || "更新票种状态失败";
  }
}
async function removeTicket(ticket) {
  if (!await askAction({title:"删除票种",message:"确定删除该票种吗？删除后无法恢复。",confirmText:"确认删除"})) return;
  try {
    await organizerScheduleApi.deleteTicketType(ticket.id);
    await loadSessions();
  } catch (e) {
    scheduleError.value = e?.message || "删除票种失败";
  }
}
async function uploadCover(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  selectedFileName.value = file.name;
  try {
    const result = await uploadImage(file);
    form.coverFileId = result.fileId;
    coverUrl.value = result.url || "";
    form.coverFileUrl = result.url || "";
  } catch (e) {
    error.value = e.message;
  }
}
async function save() {
  saving.value = true;
  error.value = "";
  try {
    const payload = {
      title: form.title,
      category: form.category,
      description: form.content,
      content: form.content,
      highlights: form.highlights,
      notices: form.notices,
      coverFileId: form.coverFileId || null,
      venueName: form.venueName,
      venueAddress: form.venueAddress,
    };
    if (isEdit.value)
      await organizerActivityApi.update(String(route.params.id), payload);
    else {
      const created=await organizerActivityApi.create(payload);
      if(!created?.id)throw new Error('活动已保存但未返回活动编号，请从活动列表查看');
      await router.push(`/management/organizer/activities/${created.id}/edit`);
      return true;
    }
    form.status='DRAFT';
    return true;
  } catch (e) {
    error.value = e.message;
    return false;
  } finally {
    saving.value = false;
  }
}
function continueConfiguration(){
  if(!isEdit.value){save();return}
  document.querySelector('.schedule-card')?.scrollIntoView?.({behavior:'smooth',block:'start'})
}
async function submitForReview() {
  if(saving.value)return
  const missing=[]
  for(const [key,label] of [['title','活动名称'],['category','活动分类'],['content','活动内容'],['highlights','活动亮点'],['notices','注意事项'],['venueName','场地名称'],['venueAddress','场地地址']])if(!String(form[key]||'').trim())missing.push(label)
  if(!form.coverFileId)missing.push('活动封面')
  if(!sessions.value.length)missing.push('至少一个场次')
  if(sessions.value.some(session=>!session.ticketTypes?.some(ticket=>ticket.enabled)))missing.push('每个场次至少一个启用票种')
  if(missing.length){error.value='请先完善：'+missing.join('、');document.querySelector('.form-error')?.scrollIntoView?.({behavior:'smooth',block:'center'});return}
  if(!await save())return
  saving.value=true;error.value=''
  try{await organizerActivityApi.submit(String(route.params.id));await router.push('/management/organizer/activities?status=PENDING')}
  catch(e){error.value=e.message||'提交审核失败'}
  finally{saving.value=false}
}
onMounted(load);
</script>
<style scoped>
.activity-fields{border:0;margin:0;padding:0;min-width:0}

.schedule-alert { display:flex; align-items:flex-start; gap:8px; padding:12px 14px; border:1px solid #ffd7b0; border-radius:8px; background:#fff8ed; color:#aa4b10; font-size:13px; line-height:1.6; }
.schedule-alert :deep(svg) { flex-shrink:0; margin-top:2px; }
.sale-time-note { padding:10px 12px; background:#edf4ff; border-radius:8px; color:#526b91; font-size:12px; line-height:1.7; }
.legacy-sale-note { color:#a56922!important; }
.eyebrow {
  color: #6f7690;
  font-size: 13px;
  margin-bottom: 10px;
}
.activity-form,
.schedule-card {
  padding: 26px;
}
.form-section-title {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 20px;
}
.form-field em {
  color: #d33d45;
  font-style: normal;
}
.cover-preview {
  display: grid;
  place-items: center;
  background: #e4e9fb center/cover no-repeat;
  border: 1px dashed #cbd2e7;
  color: #7180aa;
}
.cover-upload .cell-muted {
  max-width: 240px;
}
.schedule-card {
  margin-top: 22px;
}
.section-heading,
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}
.section-heading p {
  margin: 6px 0 0;
  color: #72798b;
  font-size: 13px;
}
.inline-form {
  margin-top: 20px;
  padding: 20px;
  background: #f7f8fd;
  border: 1px solid #e1e4ef;
  border-radius: 10px;
}
.inline-form h3 {
  margin: 0 0 18px;
  font-size: 16px;
}
.session-panel {
  margin-top: 16px;
  border: 1px solid #e0e3ee;
  border-radius: 9px;
  overflow: hidden;
}
.session-header {
  padding: 17px 18px;
  background: #fafaff;
}
.session-header strong,
.session-header small {
  display: block;
}
.session-header small {
  color: #7c8293;
  font-size: 12px;
  margin-top: 6px;
}
.ticket-list {
  border-top: 1px solid #e8eaf1;
}
.ticket-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 13px 18px;
  border-bottom: 1px solid #eef0f5;
}
.ticket-row:last-child {
  border-bottom: 0;
}
.ticket-row div {
  flex: 1;
}
.ticket-row small {
  display: block;
  color: #7c8293;
  font-size: 12px;
  margin-top: 4px;
}
.ticket-row strong {
  color: #0d50dc;
}
.ticket-empty {
  padding: 18px;
  color: #8a90a0;
  font-size: 13px;
}
.table-action.danger {
  color: #c53a43;
  border-color: #f0c8ca;
  background: #fff;
}
.ticket-form {
  margin-top: 18px;
}
.state-notice {
  margin-top: 16px;
  padding: 12px 14px;
  color: #7a5a12;
  background: #fff8e5;
  border: 1px solid #f0d58b;
  border-radius: 8px;
  font-size: 13px;
}
.ticket-info {
  min-width: 150px;
}
.ticket-state {
  min-width: 54px;
  font-size: 12px;
  text-align: center;
}
.ticket-state.is-on {
  color: #14805b;
}
.ticket-state.is-off {
  color: #8a90a0;
}
.table-action:disabled,
.management-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
@media (max-width: 700px) {
  .section-heading,
  .session-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .session-header {
    gap: 12px;
  }
  .action-row {
    width: 100%;
  }
}
.management-content{max-width:1120px!important;padding-bottom:90px!important}.management-title{align-items:flex-start!important}.management-title h1{display:flex;align-items:center;gap:9px}.draft-badge{font-size:11px;font-weight:500;color:#63718b;background:#edf1f8;border-radius:12px;padding:5px 8px;vertical-align:middle}.editor-actions{display:flex;align-items:center;gap:8px}.editor-actions button{display:flex;align-items:center;gap:5px}.editor-steps{display:grid;grid-template-columns:repeat(4,1fr);gap:0;background:#fff;border:1px solid #e1e7f0;border-radius:10px;margin:0 0 16px;padding:10px;box-shadow:0 3px 12px #273b5b08}.editor-steps>div{display:flex;align-items:center;gap:9px;padding:2px 12px;border-right:1px solid #edf0f5}.editor-steps>div:last-child{border-right:0}.editor-steps b{display:grid;place-items:center;width:30px;height:30px;border-radius:8px;background:#e8eefb;color:#71809a;font-size:11px}.editor-steps .active b{background:#1459dc;color:#fff}.editor-steps span{display:flex;flex-direction:column;gap:2px}.editor-steps strong{font-size:12px;color:#26344b}.editor-steps small{font-size:10px;color:#8a95a8}.activity-form,.schedule-card{border:1px solid #e1e7f0!important;border-radius:11px!important;box-shadow:0 3px 12px #273b5b08!important;padding:20px!important}.form-heading{margin-bottom:18px}.form-heading .form-section-title{display:flex;align-items:center;gap:7px;margin:0;font-size:17px}.form-heading p{margin:5px 0 0;color:#8793a8;font-size:12px}.ready{font-size:11px;color:#16845e;background:#e5f8ef;border-radius:13px;padding:6px 9px}.ready i{display:inline-block;width:6px;height:6px;background:#13a36d;border-radius:50%;margin-right:5px}.form-field label{color:#52617c;font-size:12px}.form-field label small{color:#8c97aa;font-weight:400;margin-left:7px}.form-field input,.form-field textarea,.form-field select{border:1px solid #d9e1ee!important;border-radius:8px!important;background:#fbfcff!important;min-height:42px!important;box-sizing:border-box}.form-field textarea{min-height:110px!important;padding:12px!important}.cover-upload{display:grid!important;grid-template-columns:230px 1fr;gap:14px;align-items:center;padding:14px;background:#f7f9fd;border:1px solid #e4eaf3;border-radius:9px}.cover-preview{width:230px;height:130px!important;border-radius:8px!important;background-color:#eef3ff!important}.cover-upload input{max-width:270px}.inline-actions{border-top:1px solid #edf0f5;margin-top:20px;padding-top:18px}.schedule-card{margin-top:14px!important}.section-heading p{font-size:12px}.session-panel{border-color:#dfe6f0!important}.session-header{background:#f7f9fd!important}.table-action.primary,.management-button:not(.light){background:#2161e8!important;color:#fff!important;border-color:#2161e8!important}@media(max-width:780px){.editor-steps{grid-template-columns:repeat(2,1fr);gap:8px}.editor-steps>div{border-right:0;padding:7px}.editor-actions{flex-wrap:wrap}.cover-upload{grid-template-columns:1fr}.cover-preview{width:100%;height:160px!important}}
.editor-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:16px;align-items:start}.editor-main{min-width:0}.activity-form{width:100%}.activity-form .form-grid,.inline-form .form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px 18px}.activity-form .form-field.full,.inline-form .form-field.full{grid-column:1/-1}.activity-form .form-field{min-width:0}.activity-form .form-field input,.activity-form .form-field textarea,.activity-form .form-field select,.inline-form .form-field input,.inline-form .form-field select{width:100%;box-sizing:border-box}.editor-aside{display:flex;flex-direction:column;gap:14px}.preview-card,.editor-tip{background:#fff;border:1px solid #e1e7f0;border-radius:11px;box-shadow:0 3px 12px #273b5b08;padding:16px}.aside-heading{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:12px}.aside-heading strong,.aside-heading small{display:block}.aside-heading strong{font-size:15px;color:#1d2940}.aside-heading small{margin-top:4px;color:#8793a8;font-size:11px}.aside-heading>span{padding:4px 7px;border-radius:5px;background:#edf3ff;color:#5270ad;font-size:10px}.phone-preview{overflow:hidden;border:5px solid #dce7fb;border-radius:24px;background:#fff;box-shadow:0 8px 18px #3f609426}.phone-top{height:30px;display:flex;align-items:center;gap:5px;padding:0 10px;color:#74809a;font-size:9px}.phone-top i{width:6px;height:6px;border-radius:50%;background:#2161e8}.phone-top b{margin-left:auto;font-size:10px}.phone-cover{height:145px;display:grid;place-items:center;background:#e8efff center/cover no-repeat;color:#6d82b2;font-size:12px}.phone-content{padding:13px}.preview-tag{display:inline-block;padding:3px 6px;border-radius:4px;background:#eaf1ff;color:#2161e8;font-size:9px}.phone-content h3{margin:8px 0 7px;color:#18253d;font-size:14px;line-height:1.35}.phone-content p{margin:5px 0;color:#7b879b;font-size:10px;line-height:1.45}.editor-tip{display:flex;gap:9px;background:#f1f6ff;border-color:#dce8fc}.tip-dot{width:8px;height:8px;flex:none;margin-top:4px;border-radius:50%;background:#2161e8;box-shadow:0 0 0 4px #dbe8ff}.editor-tip strong{font-size:12px;color:#244c9f}.editor-tip p{margin:5px 0 0;color:#687996;font-size:11px;line-height:1.5}.management-content{max-width:1500px!important}.activity-form{padding:22px!important}@media(max-width:1050px){.editor-layout{grid-template-columns:1fr}.editor-aside{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr)}.phone-preview{max-width:310px}}@media(max-width:780px){.activity-form .form-grid,.inline-form .form-grid{grid-template-columns:1fr}.activity-form .form-field.full,.inline-form .form-field.full{grid-column:auto}.editor-aside{display:flex}.editor-actions{width:100%}}
 /* Anchor the hidden upload input here so it cannot enlarge the document outside the scrolling pane. */
 .datetime-pair{display:grid;grid-template-columns:minmax(0,1.25fr) minmax(112px,.75fr);gap:8px}.datetime-pair input{min-width:0}.field-hint{display:block;margin-top:5px;color:#8793a8;font-size:11px}.file-picker{position:relative;display:flex;align-items:center;gap:9px;min-width:0;cursor:pointer}.file-picker input{position:absolute;top:0;left:0;width:1px;height:1px;clip-path:inset(50%);opacity:0;pointer-events:none}.file-picker-button{display:inline-flex;align-items:center;justify-content:center;height:38px;padding:0 14px;border:1px solid #c9d8f4;border-radius:8px;background:#eff5ff;color:#1f5bd8;font-size:12px;font-weight:700;white-space:nowrap;transition:.18s}.file-picker:hover .file-picker-button{background:#e3edff;border-color:#8eb2f1}.file-name{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#65728a;font-size:12px}.management-button,.table-action{border-radius:8px!important;font-weight:700!important;transition:transform .16s ease,box-shadow .16s ease,background .16s ease!important}.management-button:not(.light):not(:disabled),.table-action.primary:not(:disabled){box-shadow:0 5px 12px rgba(33,97,232,.18)!important}.management-button:not(:disabled):hover,.table-action:not(:disabled):hover{transform:translateY(-1px)}.management-button.light{background:#f5f7fb!important;border-color:#d8e0ec!important;color:#52617a!important}.management-button.light:hover{background:#edf2fa!important}.form-actions{display:flex;flex-wrap:wrap;gap:9px;align-items:center}.action-row{display:flex;flex-wrap:wrap;gap:7px;justify-content:flex-end}.table-action.danger{color:#c53a43!important;border-color:#f0c8ca!important;background:#fff7f7!important}.table-action.danger:hover{background:#fff0f1!important}@media(max-width:520px){.datetime-pair{grid-template-columns:1fr}.file-picker{align-items:flex-start;flex-direction:column}.action-row{justify-content:flex-start;width:100%}}
</style>
