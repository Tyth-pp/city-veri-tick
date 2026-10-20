<template>
 <main class="ops-page">
  <div class="ops-heading"><div><div class="ops-eyebrow">{{ platform?'平台运营':'主办方运营' }} / {{ analytics?'数据分析':'工作台' }}</div><h1>{{ analytics?'数据看板':platform?'平台工作台':'工作台' }}</h1><p>{{ analytics?'按真实支付、退款和核销记录查看经营变化。':'从待办事项进入业务，查看活动与订单最新状态。' }}</p></div><div class="ops-actions"><select v-model.number="days" aria-label="统计周期" @change="load"><option :value="7">近7日</option><option :value="30">近30日</option></select><button :disabled="loading" @click="load"><AppIcon name="refresh" size="15"/>{{ loading?'更新中…':'刷新数据' }}</button><RouterLink class="ops-primary" :to="platform?base+'/activities?status=PENDING':base+'/activities/new'"><AppIcon :name="platform?'clipboard-check':'plus'" size="16"/>{{ platform?'审核待办':'新建活动' }}</RouterLink></div></div>
  <div class="ops-caption"><span>模拟支付与退款 · 金额不代表真实收款或结算收入</span><span v-if="stats.generatedAt">更新于 {{ format(stats.generatedAt) }}</span></div>
  <p v-if="error" class="ops-error" role="alert">{{ error }}<button @click="load">重试</button><span v-if="stats.generatedAt"> · 当前保留上次成功结果</span></p>
  <p v-if="loading&&!stats.generatedAt" class="ops-panel ops-empty">概览加载中…</p>
  <template v-if="stats.generatedAt||!loading&&!error">
   <section class="ops-metrics"><RouterLink v-for="m in metrics" :key="m.label" :to="m.to" class="ops-metric"><span class="ops-metric-icon"><AppIcon :name="m.icon"/></span><small>{{ m.label }}</small><strong>{{ m.value }}</strong><span>{{ m.note }} <AppIcon name="chevron-right" size="12"/></span></RouterLink></section>
   <template v-if="!analytics">
    <section class="ops-columns"><section class="ops-panel"><div class="ops-section-title"><div><h2>{{ platform?'待审核活动':'待处理与跟进' }}</h2><p>{{ platform?'查看资料后审核，提交时间按先后排列':'先处理退款与驳回事项，待审核活动可跟进进度' }}</p></div><RouterLink :to="base+'/activities'+(platform?'?status=PENDING':'')">查看全部</RouterLink></div>
     <RouterLink v-if="!platform&&num('pendingRefunds')" class="ops-callout" :to="base+'/orders?status=REFUNDING'"><AppIcon name="receipt"/><span><b>{{ num('pendingRefunds') }} 笔退款待审核</b><small>进入订单核对申请与票券状态</small></span><AppIcon name="chevron-right" size="16"/></RouterLink>
     <div v-if="!stats.tasks?.length" class="ops-empty">暂无活动待办</div><div v-for="a in stats.tasks" :key="a.id" class="ops-task"><div><b>{{ a.title }}</b><small>{{ status(a.status) }}<template v-if="a.organizerName"> · {{ a.organizerName }}</template></small><small v-if="a.submittedAt">{{ a.submitterName||'提交账号待核实' }} · {{ format(a.submittedAt) }}</small><small v-if="a.rejectionReason" class="ops-danger">{{ a.rejectionReason }}</small></div><RouterLink :to="base+'/activities/'+a.id+(platform?'':'/edit')">{{ platform?'查看审核':['DRAFT','REJECTED'].includes(a.status)?'继续完善':'查看进度' }}</RouterLink></div>
    </section><section class="ops-panel"><div class="ops-section-title"><div><h2>快捷办理</h2><p>直达对应业务与筛选结果</p></div></div><div class="ops-quick"><RouterLink v-for="q in quick" :key="q.label" :to="q.to"><AppIcon :name="q.icon"/><div><b>{{ q.label }}</b><small>{{ q.note }}</small></div><AppIcon name="chevron-right" size="15"/></RouterLink></div></section></section>
    <section v-if="!platform" class="ops-panel"><div class="ops-section-title"><div><h2>今日场次</h2><p>核对现场人员与入场记录，展示最多20场</p></div><RouterLink :to="base+'/verification'">核销数据</RouterLink></div><p v-if="!stats.todaySessions?.length" class="ops-empty">今日暂无已发布场次</p><div v-for="s in stats.todaySessions" :key="s.sessionId" class="ops-task"><div><b>{{ s.title }}</b><small>{{ format(s.startTime) }} — {{ String(s.endTime||'').slice(11,16) }}</small><small :class="{'ops-danger':!Number(s.verifierCount)}">{{ Number(s.verifierCount)?'已配置 '+s.verifierCount+' 名可用核销员':'尚未配置有效核销员，请及时分配' }}</small></div><div class="ops-inline"><RouterLink v-if="!Number(s.verifierCount)" :to="base+'/verifiers?activityId='+s.activityId">分配人员</RouterLink><RouterLink :to="base+'/verification?activityId='+s.activityId+'&sessionId='+s.sessionId">查看入场</RouterLink></div></div></section>
   </template>
   <section class="ops-columns"><ManagementTrend :rows="stats.trend||[]" money-chart/><ManagementTrend :rows="stats.trend||[]"/></section>
   <section class="ops-panel"><div class="ops-section-title"><div><h2>活动支付与核销排行</h2><p>近{{ appliedDays }}日 · 按支付金额排序，最多5项，金额为模拟支付</p></div></div><p v-if="!stats.ranking?.length" class="ops-empty">暂无活动交易或核销记录</p><div v-for="(a,index) in stats.ranking" :key="a.activityId" class="ops-task"><div><b>{{ index+1 }}. {{ a.title }}</b><small>支付 {{ money(a.payment) }} · 核销 {{ a.checked }} 张</small></div><div class="ops-inline"><RouterLink :to="{path:base+'/orders',query:{activityId:a.activityId,paymentDays:appliedDays}}">查看订单</RouterLink><RouterLink :to="{path:base+'/verification',query:{activityId:a.activityId,start:stats.fromDate,end:stats.toDate}}">查看核销</RouterLink></div></div></section>
   <section v-if="platform&&!analytics" class="ops-panel"><div class="ops-section-title"><div><h2>最近审核与账号操作</h2><p>展示活动最近一次审核及账号操作记录</p></div></div><p v-if="!stats.recentOperations?.length" class="ops-empty">暂无操作记录</p><div v-for="r in stats.recentOperations" :key="r.id" class="ops-task"><div><b>{{ r.title }}</b><small>{{ r.operatorName||'历史记录未记录操作人' }} · {{ format(r.time) }}</small></div><RouterLink :to="r.targetPath">查看记录</RouterLink></div></section>
   <section class="ops-columns"><section class="ops-panel"><div class="ops-section-title"><div><h2>活动分类分布</h2><p>全部活动 · 按实际分类统计</p></div></div><p v-if="!stats.categories?.length" class="ops-empty">暂无分类数据</p><div v-for="c in stats.categories" :key="c.name" class="ops-category"><div><span>{{ categoryName(c.name) }}</span><b>{{ c.count }} 个 · {{ percent(c.count) }}%</b></div><progress :value="Number(c.count)" :max="Math.max(1,num('totalActivities'))"></progress></div></section><section class="ops-panel"><div class="ops-section-title"><div><h2>票券状态概览</h2><p>累计状态 · 核销率分母为有效已出票券（含退款审核冻结票）</p></div><RouterLink :to="base+'/verification'">查看记录</RouterLink></div><div class="ops-ticket-grid"><div v-for="t in ticketMetrics" :key="t.label"><small>{{ t.label }}</small><strong>{{ t.value }}</strong></div></div></section></section>
  </template>
 </main>
</template>
<script setup>
import {computed,ref,onMounted} from 'vue'
import {http} from '../api/http'
import AppIcon from './AppIcon.vue'
import ManagementTrend from './ManagementTrend.vue'
const props=defineProps({platform:Boolean,analytics:Boolean}),days=ref(7),stats=ref({}),loading=ref(true),error=ref('')
const appliedDays=computed(()=>Number(stats.value.days)||days.value)
const base=computed(()=>`/management/${props.platform?'platform':'organizer'}`)
const num=key=>Number(stats.value[key]||0),money=value=>`¥${Number(value||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})}`
const format=value=>String(value||'').replace('T',' ').slice(0,16)
const status=value=>({DRAFT:'草稿',REJECTED:'已驳回',PENDING:'待平台审核'}[value]||value)
const categoryName=value=>({CONCERT:'演唱会',MUSIC:'音乐',MOVIE:'电影',DRAMA:'话剧',SPORTS:'体育',EXHIBITION:'展览',PERFORMANCE:'演出',STAND_UP:'脱口秀',EXHIBITION_ART:'展览艺术',MUSIC_FESTIVAL:'音乐节',OPERA:'戏曲艺术',THEATRE_OPERA:'话剧歌剧',OTHER:'其他'}[value]||value)
const percent=count=>num('totalActivities')?(Number(count)/num('totalActivities')*100).toFixed(1):'0'
const metrics=computed(()=>[
 {label:props.platform?'待审核活动':'已发布活动',value:num(props.platform?'pendingActivities':'publishedActivities'),icon:'calendar',note:props.platform?'进入待审核列表':`其中 ${num('sellingActivities')} 个正在售票`,to:base.value+'/activities'+(props.platform?'?status=PENDING':'?status=PUBLISHED')},
 {label:'今日支付订单',value:Number(stats.value.todayPayment?.paidOrders||0),icon:'receipt',note:'按今日支付时间统计',to:base.value+'/orders?paidToday=1'},
 {label:`近${appliedDays.value}日支付金额`,value:money(stats.value.paidRevenue),icon:'ticket',note:`${num('paidOrders')} 笔支付（含后续退款订单）`,to:base.value+'/orders?paymentDays='+appliedDays.value},
 {label:`近${appliedDays.value}日退款金额`,value:money(stats.value.refundAmount),icon:'refresh',note:'按成功退款完成时间统计',to:base.value+'/orders?status=REFUNDED&refundDays='+appliedDays.value},
 {label:'退款待审核',value:num('pendingRefunds'),icon:'clock',note:props.platform?'查看主办方处理进度':'进入退款申请处理',to:base.value+'/orders?status=REFUNDING'},
 {label:`近${appliedDays.value}日收付差额`,value:money(stats.value.netAmount),icon:'chart',note:'期间支付减退款，不等同利润',to:base.value+'/orders?paymentDays='+appliedDays.value}
])
const quick=computed(()=>props.platform?[
 {label:'审核待办',note:'只查看待审核活动',icon:'clipboard-check',to:base.value+'/activities?status=PENDING'},
 {label:'创建主办方',note:'建立机构并配置管理员',icon:'building',to:base.value+'/organizers?create=1'},
 {label:'账号查询',note:'启停账号与会话管理',icon:'users',to:base.value+'/users'},
 {label:'退款监管',note:'查询主办方审核进度',icon:'receipt',to:base.value+'/orders?status=REFUNDING'}
]:[
 {label:'新建活动',note:'配置资料、场次与票种',icon:'plus',to:base.value+'/activities/new'},
 {label:'修改被驳回活动',note:'查看原因并重新提交',icon:'edit',to:base.value+'/activities?status=REJECTED'},
 {label:'票种与库存',note:'查看可售和锁定库存',icon:'ticket',to:base.value+'/ticket-inventory'},
 {label:'核销员管理',note:'创建账号与分配活动',icon:'users',to:base.value+'/verifiers'}
])
const ticketMetrics=computed(()=>[{label:'累计出票',value:num('totalTickets')},{label:'已核销',value:num('checkedInTickets')},{label:'待入场',value:num('pendingUseTickets')},{label:'已结束未使用',value:num('endedUnusedTickets')},{label:'已退款',value:num('refundedTickets')},{label:'核销率',value:num('verificationRate')+'%'}])
let request=0
async function load(){const current=++request;loading.value=true;error.value='';try{const result=await http.get(`/controller/admin/${props.platform?'platform':'organizer'}/dashboard/summary`,{params:{days:days.value}});if(current===request)stats.value=result||{}}catch(e){if(current===request)error.value=e.message||'概览加载失败'}finally{if(current===request)loading.value=false}}
onMounted(load)
</script>
