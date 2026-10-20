<template>
  <main class="content">
    <div class="title">
      <div><div class="eyebrow">平台运营 / 优惠券管理</div><h1>优惠券管理</h1><p>配置优惠规则与适用范围，管理发放和领取进度。</p></div>
      <button class="primary" v-if="!showForm" @click="openForm">+ 创建优惠券</button>
    </div>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <section class="panel" v-if="showForm">
      <div class="form-heading"><h2>创建优惠券</h2><span>一笔订单限用一张优惠券，不叠加使用</span></div>
      <form novalidate @submit.prevent="create" @input="clearError" @change="clearError">
        <section class="form-section">
          <h3>基础信息</h3>
          <div class="form-grid">
            <Field id="name" label="优惠券名称" required :error="errors.name"><input id="name" v-model.trim="form.name" maxlength="64" placeholder="例如：城市演出满100减20券" :aria-invalid="!!errors.name" /></Field>
            <Field id="couponType" label="券类型" required :error="errors.couponType"><select id="couponType" v-model="form.couponType"><option value="FULL_REDUCTION">满减券</option><option value="DISCOUNT">折扣券</option><option value="NO_THRESHOLD">无门槛券</option></select></Field>
            <Field id="minOrderAmount" :label="form.couponType==='DISCOUNT'?'使用门槛（元）':'满减门槛（元）'" :required="form.couponType!=='NO_THRESHOLD'" :error="errors.minOrderAmount" :hint="form.couponType==='NO_THRESHOLD'?'无门槛券不要求订单满额':form.couponType==='DISCOUNT'?'填0表示无金额门槛':''"><input id="minOrderAmount" v-model="form.minOrderAmount" type="number" min="0" step="0.01" :disabled="form.couponType==='NO_THRESHOLD'" placeholder="例如：100" /></Field>
            <Field v-if="form.couponType!=='DISCOUNT'" id="discountAmount" label="优惠金额（元）" required :error="errors.discountAmount"><input id="discountAmount" v-model="form.discountAmount" type="number" min="0.01" step="0.01" placeholder="例如：20" /></Field>
            <Field v-else id="discountRate" label="折扣（折）" required :error="errors.discountRate" hint="输入8.5，即按原价的85%支付"><input id="discountRate" v-model="form.discountRate" type="number" min="0.1" max="9.99" step="0.01" placeholder="例如：8.5" /></Field>
            <Field v-if="form.couponType==='DISCOUNT'" id="maxDiscountAmount" label="优惠封顶" :required="!form.noDiscountCap" :error="errors.maxDiscountAmount" :hint="form.noDiscountCap?'按折扣计算，不设置固定减免上限；仍保留最低实付金额':'设置单笔订单最多可减免的金额，单位：元'"><div class="rule-controls"><select id="discountCapMode" v-model="form.noDiscountCap" aria-label="优惠封顶规则"><option :value="false">设置减免上限</option><option :value="true">不封顶</option></select><input id="maxDiscountAmount" v-model="form.maxDiscountAmount" type="number" min="0.01" step="0.01" :disabled="form.noDiscountCap" placeholder="例如：100" /></div></Field>
            <Field id="minPayAmount" label="最低实付金额（元）" required :error="errors.minPayAmount" hint="优惠抵扣后，订单至少支付此金额"><input id="minPayAmount" v-model="form.minPayAmount" type="number" min="0.01" step="0.01" placeholder="例如：0.01" /></Field>
            <Field id="claimMode" label="发放方式" required :error="errors.claimMode" hint="限量领取先到先得；定时秒杀在领取开始时间准时开放。优惠类型与发放方式独立设置。"><select id="claimMode" v-model="form.claimMode"><option value="NORMAL">普通领取</option><option value="LIMITED">限量领取</option><option value="FLASH_SALE">定时秒杀</option></select></Field>
            <Field id="totalQuantity" label="发放总数量（张）" required :error="errors.totalQuantity"><input id="totalQuantity" v-model="form.totalQuantity" type="number" min="1" step="1" placeholder="例如：1000" /></Field>
            <Field id="perUserLimit" label="每人限领" :required="!form.noPerUserLimit" :error="errors.perUserLimit" :hint="form.noPerUserLimit?'个人领取次数不限，仍受发放总量和领取时间约束；一单限用一张':'按当前账号累计领取次数限制，退券不重置次数'"><div class="rule-controls"><select id="perUserLimitMode" v-model="form.noPerUserLimit" aria-label="每人领取规则"><option :value="false">限制领取张数</option><option :value="true">不限领取次数</option></select><input id="perUserLimit" v-model="form.perUserLimit" type="number" min="1" :max="form.totalQuantity || undefined" step="1" :disabled="form.noPerUserLimit" placeholder="例如：1" /></div></Field>
          </div>
        </section>
        <section class="form-section">
          <h3>时间配置</h3><p class="section-help">领取和使用分别生效；停止领取后，已领券仍可在使用有效期内使用。</p>
          <h4>领券时间</h4><div class="form-grid">
            <Field id="startsAt" label="领取开始时间" required :error="errors.startsAt"><ChineseDateTimeInput id="startsAt" v-model="form.startsAt" type="datetime-local" /></Field>
            <Field id="endsAt" label="领取结束时间" required :error="errors.endsAt"><ChineseDateTimeInput id="endsAt" v-model="form.endsAt" type="datetime-local" /></Field>
          </div>
          <h4>使用有效期</h4><div class="form-grid">
            <Field id="useStartsAt" label="使用开始时间" required :error="errors.useStartsAt"><ChineseDateTimeInput id="useStartsAt" v-model="form.useStartsAt" type="datetime-local" /></Field>
            <Field id="useEndsAt" label="使用结束时间" required :error="errors.useEndsAt"><ChineseDateTimeInput id="useEndsAt" v-model="form.useEndsAt" type="datetime-local" /></Field>
          </div>
        </section>
        <section class="form-section">
          <h3>适用范围</h3>
          <div class="scope-options"><label><input type="radio" v-model="form.scopeType" value="ALL" /> 全平台</label><label><input type="radio" v-model="form.scopeType" value="ACTIVITY" /> 指定活动</label></div>
          <div class="activity-picker" v-if="form.scopeType==='ACTIVITY'">
            <Field id="activityId" label="适用活动" required :error="errors.activityId" hint="搜索并选择一个活动，该券仅可用于此活动下的票种">
              <div class="activity-search"><input v-model.trim="activityKeyword" aria-label="搜索活动" placeholder="输入活动名称搜索" @keydown.enter.prevent="loadActivities" /><button type="button" class="secondary" :disabled="activitiesLoading" @click="loadActivities">{{activitiesLoading?'查询中…':'搜索'}}</button></div>
              <select id="activityId" v-model="form.activityId"><option value="">请选择活动</option><option v-for="a in activities" :key="a.id" :value="String(a.id)">{{a.title}}（ID：{{a.id}}）</option></select>
            </Field><p v-if="activityError" class="form-error" role="alert">{{activityError}}</p>
          </div>
          <fieldset class="category-fieldset" :disabled="form.scopeType==='ACTIVITY'"><legend>适用活动分类</legend><p>不选表示全部分类可用；多选时，符合任一分类即可使用。</p>
            <div class="categories"><label v-for="item in couponCategories" :key="item.code"><input type="checkbox" :value="item.code" v-model="form.categoryCodes" />{{ item.label }}</label></div>
          </fieldset>
          <p v-if="form.scopeType==='ACTIVITY'" class="section-help">已指定活动，分类筛选不再参与匹配。</p>
        </section>
        <section class="form-section">
          <h3>高级规则</h3><div class="form-grid">
            <Field id="refundPolicy" label="退票后优惠券处理" required><select id="refundPolicy" v-model="form.refundPolicy"><option value="RETURN">退款成功后退回（仅限未过期且仍有效的券）</option><option value="NO_RETURN">退款成功后不退回</option></select></Field>
            <Field id="description" label="说明" :error="errors.description" hint="最多255字，向用户说明优惠用途和限制"><textarea id="description" v-model.trim="form.description" rows="4" maxlength="255" placeholder="例如：仅限指定活动使用，每笔订单限用一张" /></Field>
          </div>
        </section>
        <p v-if="formError" class="form-error" role="alert">{{formError}}</p>
        <div class="form-actions"><button class="primary" :disabled="saving">{{saving?'保存中…':'保存'}}</button><button type="button" class="secondary" :disabled="saving" @click="cancel">取消</button><span>保存后默认未启用，请在列表中确认启用投放</span></div>
      </form>
    </section>
    <section class="panel table-wrap">
      <p v-if="loading" class="empty">加载中…</p><p v-else-if="loadError" class="form-error">{{loadError}} <button class="action" @click="load">重试</button></p>
      <table v-else><thead><tr><th>优惠券 / 适用范围</th><th>优惠规则</th><th>已领 / 发放</th><th>时间配置</th><th>状态</th><th>操作</th></tr></thead><tbody>
        <tr v-for="c in coupons" :key="c.id"><td><b>{{c.name}}</b><small class="blue">{{couponClaimMode(c)}}</small><small>{{couponScope(c)}}</small><small>{{c.description || '—'}}</small></td><td><b class="blue">{{couponValue(c)}}</b><small>{{couponThreshold(c)}}</small><small v-if="c.couponType==='DISCOUNT'">{{couponCapLabel(c)}}</small><small>最低实付 ¥{{c.minPayAmount ?? '0.01'}}</small></td><td>{{c.claimedQuantity}} / {{c.totalQuantity}}<small>{{couponLimitLabel(c)}}</small></td><td><small>领取：{{fmt(c.startsAt)}} 至 {{fmt(c.endsAt)}}</small><small>使用：{{fmt(c.useStartsAt || c.startsAt)}} 至 {{fmt(c.useEndsAt || c.endsAt)}}</small></td><td><span class="status" :class="{inactive:c.status!=='ACTIVE'}">{{c.status==='ACTIVE'?'已启用':c.status==='DRAFT'?'未启用':'已停用'}}</span></td><td><button class="action" :disabled="toggling===c.id" @click="toggle(c)">{{c.status==='ACTIVE'?'停用投放':'启用投放'}}</button></td></tr>
      </tbody></table><div v-if="!loading&&!loadError&&!coupons.length" class="empty">暂无优惠券，创建后即可投放</div>
    </section>
  </main>
</template>
<script setup>
import ChineseDateTimeInput from '../../components/ChineseDateTimeInput.vue'
import {onMounted,ref,watch,nextTick} from 'vue'
import Field from '../../components/CouponFormField.vue'
import {askAction} from '../../utils/actionDialog'
import {couponApi} from '../../api/coupon'
import {platformActivityApi} from '../../api/platformActivity'
import {couponCategories,emptyCouponForm,validateCouponForm,couponPayload,couponValue,couponThreshold,couponScope,couponClaimMode,couponCapLabel,couponLimitLabel} from '../../utils/couponForm'
const coupons=ref([]),loading=ref(false),saving=ref(false),showForm=ref(false),notice=ref(''),loadError=ref(''),formError=ref(''),errors=ref({}),toggling=ref(null)
const form=ref(emptyCouponForm()),activities=ref([]),activityKeyword=ref(''),activitiesLoading=ref(false),activityError=ref('')
const fmt=v=>v?String(v).slice(0,16).replace('T',' '):'—'
function openForm(){form.value=emptyCouponForm();errors.value={};formError.value='';showForm.value=true;notice.value=''}
function cancel(){showForm.value=false;form.value=emptyCouponForm();errors.value={};formError.value=''}
function clearError(e){if(e.target.id) delete errors.value[e.target.id];formError.value=''}
watch(()=>form.value.couponType,type=>{errors.value={};if(type==='NO_THRESHOLD')form.value.minOrderAmount=0;if(type==='FULL_REDUCTION'&&Number(form.value.minOrderAmount)===0)form.value.minOrderAmount='';if(type==='DISCOUNT'){form.value.discountAmount=''}else{form.value.maxDiscountAmount=''}})
watch(()=>form.value.noDiscountCap,()=>{delete errors.value.maxDiscountAmount})
watch(()=>form.value.noPerUserLimit,()=>{delete errors.value.perUserLimit})
watch(()=>form.value.scopeType,scope=>{delete errors.value.activityId;if(scope==='ACTIVITY'){form.value.categoryCodes=[];loadActivities()}else form.value.activityId=''})
async function load(){loading.value=true;loadError.value='';try{coupons.value=await couponApi.adminList()||[]}catch(e){loadError.value=e.message||'优惠券加载失败'}finally{loading.value=false}}
let activityRequest=0
async function loadActivities(){const req=++activityRequest;activitiesLoading.value=true;activityError.value='';try{const data=await platformActivityApi.list({keyword:activityKeyword.value,page:1,pageSize:100});if(req===activityRequest)activities.value=data?.records||[]}catch(e){if(req===activityRequest)activityError.value=e.message||'活动加载失败'}finally{if(req===activityRequest)activitiesLoading.value=false}}
async function create(){
  errors.value=validateCouponForm(form.value);formError.value=''
  if(Object.keys(errors.value).length){await nextTick();document.getElementById(Object.keys(errors.value)[0])?.focus();return}
  saving.value=true
  try{await couponApi.adminCreate(couponPayload(form.value));notice.value='优惠券已保存，当前未启用，请手动启用投放';cancel();await load()}
  catch(e){formError.value=e.message||'创建失败，请稍后重试'}finally{saving.value=false}
}
async function toggle(c){
  if(toggling.value!==null)return
  const enable=c.status!=='ACTIVE'
  toggling.value=c.id
  try{
    const confirmed=await askAction({title:enable?'启用优惠券投放':'停用优惠券投放',message:enable?`确认启用“${c.name}”？启用后将按配置的领取时间开放领取。`:`确认停用“${c.name}”？停用后将无法领取，已领取优惠券的使用也受投放状态限制。`,confirmText:enable?'启用投放':'停用投放'})
    if(!confirmed)return
    await couponApi.adminStatus(c.id,enable?'ACTIVE':'INACTIVE')
    notice.value=enable?'优惠券已启用，将按配置的领取时间开放领取':'优惠券已停用'
    await load()
  }catch(e){notice.value=e.message||'状态更新失败'}finally{toggling.value=null}
}
onMounted(load)
</script>
<style scoped>
.content{padding:34px;max-width:1500px;margin:0 auto;color:#152d50}.title{display:flex;justify-content:space-between;align-items:flex-start;gap:24px;margin-bottom:25px}.eyebrow{color:#7b8ba4;font-size:13px;margin-bottom:10px}h1{margin:0 0 8px;font-size:30px}.title p{margin:0;color:#75839a;font-size:14px}button{cursor:pointer;font:inherit;font-size:14px}button:disabled{opacity:.55;cursor:not-allowed}.primary{border:0;border-radius:8px;background:#2161e8;color:#fff;padding:12px 22px;white-space:nowrap}.secondary,.action{border:1px solid #d5deef;background:#fff;color:#416085;border-radius:8px;padding:11px 18px}.panel{background:#fff;border:1px solid #e1e7f0;border-radius:12px;padding:26px;margin-bottom:22px;box-shadow:0 4px 16px #17366b04}.form-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:4px}.form-heading h2{font-size:21px;margin:0}.form-heading span{color:#8794a8;font-size:13px}.form-section{padding:24px 0;border-bottom:1px solid #edf0f5}.form-section h3{font-size:16px;margin:0 0 20px;padding-left:10px;border-left:3px solid #2463eb}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:22px 28px}.form-section h4{font-size:14px;margin:20px 0 14px;color:#627795;font-weight:500}.section-help{font-size:13px;color:#8491a6;margin:-6px 0 18px;line-height:1.6}.scope-options{display:flex;gap:30px;margin:6px 0 20px}.scope-options label,.categories label{display:flex;align-items:center;gap:8px;font-size:14px;cursor:pointer}input[type=checkbox],input[type=radio]{accent-color:#2463eb;width:16px;height:16px;margin:0;flex-shrink:0}.category-fieldset{margin:20px 0 0;border:1px solid #e2e8f2;border-radius:9px;padding:18px 20px}.category-fieldset legend{font-size:14px;color:#344a6a;padding:0 6px}.category-fieldset p{font-size:12px;color:#8794aa;margin:0 0 18px}.categories{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px 12px}.category-fieldset:disabled{opacity:.45;background:#f7f9fc}.category-fieldset:disabled label{cursor:not-allowed}.activity-picker{max-width:100%}.activity-search{display:flex;gap:12px}.activity-search input{flex:1;min-width:0}.form-actions{display:flex;align-items:center;gap:12px;padding-top:24px}.form-actions span{font-size:12px;color:#8b98ac}.notice{padding:13px 18px;background:#eaf4ff;color:#2560b0;border-radius:8px}.form-error{color:#d33c50;font-size:13px;line-height:1.6}.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;min-width:900px}th,td{text-align:left;padding:16px;border-bottom:1px solid #edf0f5;font-size:13px;line-height:1.5;vertical-align:middle}th{background:#f7f9fd;color:#7890ad;font-weight:500}td small{display:block;color:#8594a9;margin-top:5px;font-size:12px}.blue{color:#2463eb}.status{padding:5px 9px;background:#def7ec;color:#098568;border-radius:14px;white-space:nowrap;font-size:12px}.status.inactive{background:#edf0f5;color:#8994a6}.empty{text-align:center;color:#7c899e;padding:25px}@media(max-width:900px){.categories{grid-template-columns:repeat(2,minmax(0,1fr))}.form-heading{display:block}.form-heading span{display:block;margin-top:8px}}@media(max-width:650px){.content{padding:20px 14px}.panel{padding:18px}.form-grid{grid-template-columns:1fr}.title{display:block}.title button{margin-top:15px}.form-actions{flex-wrap:wrap}.form-actions span{width:100%}}
</style>

<style scoped>.rule-controls{display:grid;grid-template-columns:minmax(145px,.9fr) minmax(0,1fr);gap:10px;align-items:center}.rule-controls>select,.rule-controls>input{width:100%;min-width:0;box-sizing:border-box}.rule-controls>input:disabled{background:#f0f3f8;color:#9aa6b8;cursor:not-allowed}@media(max-width:500px){.rule-controls{grid-template-columns:1fr}}</style>
