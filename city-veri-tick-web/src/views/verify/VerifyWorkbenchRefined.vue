<template>
  <main class="verify-shell">
    <header class="verify-header">
      <div class="brand"><img :src="logo" alt="Venuo" /><strong>Venuo</strong><span>核销工作台</span></div>
      <div class="session-bar"><span class="current-activity"><AppIcon name="ticket" size="15" /> <b>{{ currentActivity }}</b></span><em>{{ loadingRecords ? "同步中" : dataError ? "同步失败" : lastSynced ? "最近同步 " + lastSynced : "尚未同步" }}</em><StaffAccountMenu /><button class="logout" type="button" @click="logout"><AppIcon name="logout" size="16" /> 退出</button></div>
    </header>

    <section class="workspace">
      <section class="scan-panel panel">
        <div class="panel-head"><div class="panel-title"><span class="title-icon"><AppIcon name="qr" size="23" /></span><div><h1>门票快速核验</h1><p>扫描二维码或输入核销码</p></div></div><div class="scan-options"><label>连续扫码 <input type="checkbox" v-model="continuous" /><i></i></label></div></div>
        <form @keydown.esc.prevent="clearInputs" class="scan-form identity-scan" @submit.prevent="submit"><label>核销码<input id="verification-code" ref="codeInput" v-model.trim="code" autocomplete="off" autofocus placeholder="扫描或输入核销码" /></label><label>身份证号<input v-model.trim="idCardNo" maxlength="18" autocomplete="off" placeholder="输入观演人18位身份证号" /></label><button type="submit" :disabled="loading || !code || !idCardNo">{{ loading ? '核验中...' : '确认核验 ↵' }}</button></form>
        <div v-if="result" class="scan-result" :class="result.ok ? 'success' : 'failure'"><span class="result-icon"><AppIcon :name="result.ok ? 'check-circle' : 'alert'" size="22" /></span><div><b>{{ result.ok ? '核销通过 · 允许通行' : '核销失败' }}</b><span>{{ result.message }}</span></div><em v-if="result.ok">请工作人员确认放行 <time>{{ result.time }}</time></em></div>
      </section>

      <section class="metric-grid actual-metrics"><div class="metric"><span class="metric-icon green"><AppIcon name="user-check" /></span><small>今日核销</small><strong>{{ stats.todayCheckedIn ?? '—' }}<i>张</i></strong></div><div class="metric"><span class="metric-icon blue"><AppIcon name="ticket" /></span><small>累计核销</small><strong>{{ stats.totalCheckedIn ?? '—' }}<i>张</i></strong></div></section>
      <p v-if="dataError" class="data-error" role="alert">{{ dataError }} <button type="button" @click="loadData">重试同步</button></p>

      <section class="recent-panel panel"><div class="recent-head"><div><h2>最近核销流水</h2><span>最近 50 条本人核销记录</span></div><div class="recent-actions"><label><AppIcon name="search" size="16" /><input v-model="recordKeyword" placeholder="搜索姓名或核销码" /></label><button type="button" @click="loadData"><AppIcon name="refresh" size="16" /> 刷新</button></div></div><div v-if="loadingRecords" class="table-state">核销流水加载中...</div><div v-else-if="!filteredRecords.length" class="table-state">暂无核销记录</div><div v-else class="table-wrap"><table><thead><tr><th>核验时间</th><th>票种</th><th>核验券码</th><th>观演人</th><th>状态</th></tr></thead><tbody><tr v-for="record in filteredRecords" :key="`${record.ticketId}-${record.checkedInAt}`" :class="{failed: record.ticketStatus !== 'CHECKED_IN'}"><td>{{ formatDate(record.checkedInAt) }}</td><td><b>{{ record.ticketTypeName || '普通票' }}</b></td><td class="code-text">{{ record.verificationCode || record.ticketNo || '—' }}</td><td>{{ record.viewerName || record.realName || '—' }}</td><td><span class="status-chip" :class="record.ticketStatus === 'CHECKED_IN' ? 'passed' : 'blocked'"><AppIcon :name="record.ticketStatus === 'CHECKED_IN' ? 'check-circle' : 'alert'" size="13" /> {{ record.ticketStatus === 'CHECKED_IN' ? '核验成功' : '核验失败' }}</span></td></tr></tbody></table></div><div class="connection-note"><span>{{ dataError ? "数据同步失败，已有核验结果不受影响" : "仅允许核销已分配活动的门票" }}</span><span>Enter 核验　·　Esc 清空</span></div></section>
    </section>
    <footer><span>© 2026 Venuo 核销终端</span><span>以服务端核验结果为准</span></footer>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { verifierApi } from '../../api/verifier'
import AppIcon from '../../components/AppIcon.vue'
import StaffAccountMenu from '../../components/StaffAccountMenu.vue'
import logo from '../../assets/venu-logo.svg'

const router = useRouter(), auth = useAuthStore(), code = ref(''), idCardNo = ref(''), loading = ref(false), loadingRecords = ref(false), result = ref(null), records = ref([]), recordKeyword = ref(''), codeInput = ref(null), stats = ref({})
const continuous=ref(true), dataError=ref(''), lastSynced=ref('')
function clearInputs(){code.value='';idCardNo.value='';result.value=null;codeInput.value?.focus()}
const currentActivity = computed(()=>stats.value.activityTitle||'按已分配活动权限核销')
const filteredRecords = computed(() => { const q = recordKeyword.value.trim().toLowerCase(); if (!q) return records.value; return records.value.filter(item => [item.ticketTypeName, item.verificationCode, item.ticketNo, item.viewerName, item.realName].some(value => String(value || '').toLowerCase().includes(q))) })
const formatDate = value => value ? String(value).slice(11, 19) : '—'
async function submit() {
 if (!code.value || !idCardNo.value || loading.value) return
 loading.value=true;result.value=null
 try {
  const response=await verifierApi.checkIn(code.value,idCardNo.value)
  result.value={ok:true,message:response?.message || '身份核验通过，请工作人员确认入场',time:response?.checkedInAt ? formatDate(response.checkedInAt) : ''}
  if(continuous.value){code.value='';idCardNo.value='';await nextTick();codeInput.value?.focus()}
  await loadData()
 } catch(error){result.value={ok:false,message:error?.message || '核验失败，请检查券码或身份证号'}}
 finally{loading.value=false}
}
async function loadData(){
 if(loadingRecords.value)return
 loadingRecords.value=true;dataError.value=''
 try {
  const [statistics,recent]=await Promise.all([verifierApi.statistics(),verifierApi.records()])
  stats.value=statistics || {};records.value=recent || []
  lastSynced.value=new Date().toLocaleTimeString('zh-CN',{hour12:false})
 }catch(error){dataError.value=error?.message || '核销数据同步失败，请重试'}
 finally{loadingRecords.value=false}
}
async function logout() { if (await auth.logout()) router.replace('/login') }
onMounted(loadData)
</script>

<style scoped>
.actual-metrics{grid-template-columns:repeat(2,minmax(0,1fr))!important}.data-error{padding:14px;background:#fff1f2;color:#b42332;border-radius:9px}.data-error button{border:0;background:#fff;color:inherit;padding:6px 10px;border-radius:6px}

.verify-shell{min-height:100vh;background:#f5f8fc;color:#18243a;display:flex;flex-direction:column}.verify-header{height:50px;background:#fff;border-bottom:1px solid #e5eaf2;display:flex;align-items:center;justify-content:space-between;padding:0 30px;box-sizing:border-box}.brand{display:flex;align-items:center;gap:8px;white-space:nowrap}.brand img{width:30px;height:30px;object-fit:cover;border-radius:8px;background:#eef5ff}.brand strong{font-size:16px}.brand span{font-size:11px;color:#1160d7;background:#e8f0ff;border-radius:12px;padding:5px 9px;font-weight:700}.session-bar{display:flex;align-items:center;gap:18px;color:#7a8599;font-size:11px}.session-bar>span{display:flex;align-items:center;gap:5px}.session-bar b{color:#28344d}.session-bar em{font-style:normal;color:#157d68;background:#e7f8ef;border-radius:13px;padding:6px 10px}.session-bar i,.connection-note i,footer i{display:inline-block;width:7px;height:7px;border-radius:50%;background:#11966d}.network-dot{background:#11966d!important;box-shadow:0 0 0 3px #d8f3e7}.operator-avatar{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;background:#edf2ff;color:#1552d2}.operator{display:flex;flex-direction:column;line-height:1.2}.operator small{font-size:9px;color:#9aa3b4}.logout{display:flex;align-items:center;gap:4px;border:0;background:#edf0fa;color:#65708a;border-radius:6px;padding:7px 10px;font-size:11px}.workspace{width:min(980px,calc(100% - 48px));margin:14px auto 40px;flex:1}.panel{background:#fff;border:1px solid #e1e7f0;border-radius:12px;box-shadow:0 3px 12px #1e2b460b}.scan-panel{border-top:3px solid #0d815d;padding:22px}.panel-head{display:flex;justify-content:space-between;align-items:flex-start}.panel-title{display:flex;gap:12px;align-items:center}.title-icon{width:38px;height:38px;border-radius:10px;background:#edf4ff;color:#075be1;display:grid;place-items:center}.panel-title h1{font-size:19px;margin:0 0 4px}.panel-title p{margin:0;color:#7d899f;font-size:12px}.scan-options{display:flex;align-items:center;gap:17px;color:#6c7891;font-size:11px}.scan-options span,.scan-options label{display:flex;align-items:center;gap:5px}.scan-options label{background:#f1f4ff;border-radius:7px;padding:6px 9px}.scan-options input{display:none}.scan-options label i{width:28px;height:16px;border-radius:9px;background:#1e61e6;position:relative}.scan-options label i:after{content:'';position:absolute;width:12px;height:12px;right:2px;top:2px;border-radius:50%;background:#fff}.scan-form{height:48px;display:flex;align-items:center;margin-top:17px;background:#f2f5ff;border:1px solid #e4e9f5;border-radius:10px;padding-left:12px;overflow:hidden}.scan-form input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:#52627e;font-size:13px}.scan-form small{color:#8792a7;white-space:nowrap;margin-right:14px}.scan-form button{height:36px;margin-right:9px;padding:0 17px;border:0;border-radius:8px;background:#1761e6;color:#fff;font-weight:700}.scan-form button:disabled{opacity:.55}.scan-result{display:flex;align-items:center;gap:10px;margin-top:11px;padding:10px;border-radius:9px}.scan-result.success{background:#dcf8ed;border:1px solid #b7ead5;color:#0b7655}.scan-result.failure{background:#fff0f0;border:1px solid #ffc9c9;color:#c33d46}.result-icon{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;background:currentColor;color:#fff}.scan-result>div{display:flex;align-items:center;gap:12px}.scan-result>div span{font-size:11px;color:#63718b}.scan-result em{margin-left:auto;font-style:normal;font-size:11px}.scan-result time{margin-left:4px}.metric-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-top:13px}.metric{position:relative;padding:14px 15px 12px;background:#fff;border:1px solid #e1e7f0;border-radius:10px;min-height:66px}.metric-icon{position:absolute;right:13px;top:13px;display:grid;place-items:center}.metric-icon.blue{color:#145fe1}.metric-icon.green{color:#16865f}.metric-icon.indigo{color:#3664dc}.metric-icon.red{color:#ff4d5a}.metric small,.metric strong,.metric em{display:block}.metric small{font-size:10px;color:#7c879a}.metric strong{font-size:23px;margin-top:8px}.metric strong i{font-size:11px;font-style:normal;font-weight:400;margin-left:4px}.metric em{font-size:10px;font-style:normal;color:#8692a7;margin-top:5px}.metric .green-text{color:#16865f}.danger-metric strong,.danger-metric em{color:#f0444f}.recent-panel{margin-top:13px;overflow:hidden}.recent-head{display:flex;justify-content:space-between;align-items:center;padding:15px 16px 12px}.recent-head h2{font-size:14px;margin:0;display:inline-block}.recent-head>div>span{margin-left:7px;background:#f0f2ff;color:#7582a5;border-radius:10px;padding:4px 7px;font-size:10px}.recent-actions{display:flex;gap:8px}.recent-actions label{display:flex;align-items:center;gap:6px;background:#f0f3ff;border-radius:7px;padding:7px 10px;color:#74809a}.recent-actions input{border:0;outline:0;background:transparent;width:145px;font-size:11px}.recent-actions button{display:flex;align-items:center;gap:4px;border:0;border-radius:7px;background:#edf1ff;color:#526798;padding:7px 10px;font-size:11px}.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;font-size:11px}th{background:#f6f8fd;color:#71809b;text-align:left;font-weight:500;padding:10px 13px}td{padding:11px 13px;border-top:1px solid #edf0f5;color:#475775}td b{color:#26344e}td small{display:block;color:#8490a5;margin-top:4px}.code-text{font-family:ui-monospace,SFMono-Regular,Consolas,monospace;color:#52617d}.type-chip{display:inline-block;margin-left:6px;padding:3px 6px;border-radius:5px;background:#eaf1ff;color:#2864d9;font-size:9px}.status-chip{display:inline-flex;align-items:center;gap:3px;border-radius:12px;padding:4px 8px;font-size:10px}.status-chip.passed{color:#087956;background:#d9f8e9}.status-chip.blocked{color:#d93c47;background:#ffe0e0}.failed td{background:#fffafa;color:#e14a52}.connection-note{display:flex;justify-content:space-between;padding:9px 13px;color:#7c89a0;font-size:10px;background:#fafbfe}.connection-note span{display:flex;align-items:center;gap:5px}footer{height:32px;display:flex;justify-content:space-between;align-items:center;padding:0 30px;background:#fff;color:#7c879b;font-size:10px;border-top:1px solid #e4e9f1}footer span:last-child{display:flex;align-items:center;gap:5px}.table-state{padding:34px;text-align:center;color:#8792a6;font-size:12px}@media (max-width:800px){.verify-header{height:auto;min-height:58px;padding:9px 14px;gap:12px;align-items:flex-start}.session-bar{gap:7px;flex-wrap:wrap;justify-content:flex-end}.session-bar>span:first-child,.operator{display:none}.workspace{width:calc(100% - 24px);margin-top:12px}.panel-head{flex-direction:column;gap:12px}.scan-options{width:100%;justify-content:flex-end}.metric-grid{grid-template-columns:repeat(2,1fr)}.recent-head{align-items:flex-start;gap:10px;flex-direction:column}.recent-actions{width:100%}.recent-actions label{flex:1}.recent-actions input{width:100%}footer{height:auto;padding:9px 14px;gap:8px;align-items:flex-start;flex-direction:column}footer span:last-child{display:none}}@media (max-width:480px){.brand strong{font-size:14px}.brand span{font-size:9px;padding:4px 6px}.session-bar>em{display:none}.scan-panel{padding:15px}.scan-result>div{align-items:flex-start;flex-direction:column;gap:2px}.scan-result em{display:none}.metric-grid{gap:7px}.metric{padding:11px 10px}.metric strong{font-size:20px}.recent-head h2{font-size:13px}.recent-actions input{width:100px}}
/* The verifier is an operational screen: use the available desktop width and keep
   the scan action, metrics, and audit stream visually substantial. */
.workspace{width:min(1280px,calc(100% - 72px));margin:24px auto 52px}
.verify-header{height:60px;padding:0 38px}
.brand img{width:34px;height:34px}.brand strong{font-size:18px}.brand span{font-size:12px;padding:6px 10px}
.session-bar{font-size:12px;gap:22px}.operator-avatar{width:32px;height:32px}.operator small{font-size:10px}
.scan-panel{padding:28px}.panel-title{gap:15px}.title-icon{width:46px;height:46px}.panel-title h1{font-size:24px;margin-bottom:6px}.panel-title p{font-size:14px}
.scan-options{font-size:13px}.scan-form{height:58px;margin-top:24px;padding-left:16px}.scan-form input{font-size:16px}.scan-form small{font-size:13px;margin-right:18px}.scan-form button{height:44px;padding:0 22px;font-size:14px}
.metric-grid{gap:16px;margin-top:18px}.metric{padding:20px 20px 18px;min-height:92px}.metric-icon{right:19px;top:19px}.metric small{font-size:13px}.metric strong{font-size:30px;margin-top:12px}.metric strong i{font-size:14px}.metric em{font-size:12px;margin-top:8px}
.recent-panel{margin-top:18px}.recent-head{padding:20px 22px 16px}.recent-head h2{font-size:18px}.recent-head>div>span{font-size:12px;padding:5px 8px}.recent-actions{gap:10px}.recent-actions label{padding:10px 13px}.recent-actions input{width:220px;font-size:13px}.recent-actions button{padding:10px 13px;font-size:13px}
table{font-size:13px}th{padding:14px 18px;font-size:13px}td{padding:17px 18px}.status-chip{padding:6px 10px;font-size:12px}.connection-note{padding:12px 18px;font-size:12px}
footer{height:40px;padding:0 38px;font-size:12px}
@media (max-width:800px){.workspace{width:calc(100% - 24px);margin-top:12px}.verify-header{height:auto;padding:9px 14px}.scan-panel{padding:18px}.panel-title h1{font-size:20px}.scan-form input{font-size:14px}.metric-grid{gap:10px}.metric{padding:14px 15px}.metric strong{font-size:24px}.recent-head{padding:15px 16px 12px}.recent-head h2{font-size:15px}th{padding:10px 13px}td{padding:11px 13px}footer{height:auto;padding:9px 14px;font-size:10px}}
.identity-scan{height:auto;display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr) auto;gap:16px;padding:16px;overflow:visible;background:#f6f8fd}
.identity-scan label{display:flex;flex-direction:column;gap:8px;font-size:13px;color:#52627e;min-width:0}
.identity-scan input{width:100%;box-sizing:border-box;min-height:44px;padding:10px 12px;background:#fff;border:1px solid #dce4f2;border-radius:6px}
.identity-scan button{align-self:end;margin:0;height:44px}
@media(max-width:650px){.identity-scan{grid-template-columns:minmax(0,1fr);gap:12px}.identity-scan button{width:100%}}
.scan-result.success .result-icon{background:#0b7655;color:#fff}
.scan-result.failure .result-icon{background:#c33d46;color:#fff}
</style>
