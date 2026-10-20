<template>
  <section class="refund-section" aria-labelledby="refund-heading">
    <div class="refund-heading"><span class="refund-icon"><AppIcon name="refresh" size="20" /></span><div><h2 id="refund-heading">退款与售后</h2><p>整单退票 · 模拟退款</p></div></div>
    <p class="refund-rule">{{ order.refundRule || '全部电子票未核销，且距场次开始至少48小时，可申请整单退票。审核期间暂停入场。' }}</p>
    <p class="simulation-note">当前支付与退款均为模拟流程，不发生真实资金收付。</p>
    <template v-if="scope==='user'">
      <button v-if="order.refundEligible && !applying" type="button" class="refund-button" data-action="apply-refund" @click="applying=true;error=''">申请整单退票</button>
      <p v-else-if="!order.refundEligible" class="refund-hint">{{ order.refundBlockedReason || '当前订单暂不支持申请退票' }}</p>
      <form v-if="applying" class="refund-form" @submit.prevent="apply">
        <div class="refund-amount"><span>申请退票 {{ order.quantity }} 张</span><strong>¥{{ money(order.payAmount) }}</strong></div>
        <p>提交后等待主办方审核，全部电子票暂停核销；审核未通过后恢复使用。</p>
        <label>退票原因<textarea v-model="reason" @input="error=''" maxlength="500" rows="3" placeholder="请说明退票原因，最多500字" :disabled="busy" /></label>
        <div class="refund-actions"><button type="button" :disabled="busy" @click="applying=false;error=''">暂不申请</button><button class="primary" :disabled="busy" type="submit">{{ busy?'提交中…':'确认申请' }}</button></div>
      </form>
    </template>
    <div v-if="order.status==='REFUNDING'" class="review-notice"><AppIcon name="clock" size="18" /><span>等待主办方审核，电子票暂不可入场。</span></div>
    <div v-if="scope==='organizer' && order.status==='REFUNDING' && order.latestRefund?.status==='PENDING'" class="refund-actions review-actions">
      <button :disabled="busy" type="button" data-action="reject-refund" @click="review(false)">拒绝申请</button><button class="primary" :disabled="busy" type="button" data-action="approve-refund" @click="review(true)">{{ busy?'处理中…':'同意并模拟退款' }}</button>
    </div>
    <p v-if="error" role="alert" class="refund-error">{{ error }}</p>
    <div v-if="order.refundHistory?.length" class="refund-history">
      <h3>售后记录</h3>
      <article v-for="item in order.refundHistory" :key="item.id" class="refund-record">
        <div class="record-heading"><b :class="item.status.toLowerCase()">{{ labels[item.status] || '状态待确认' }}</b><strong>¥{{ money(item.amount) }}</strong></div>
        <dl><div><dt>申请时间</dt><dd>{{ date(item.requestedAt) }}</dd></div><div><dt>退票原因</dt><dd>{{ item.reason || '—' }}</dd></div><div v-if="item.processedAt"><dt>处理时间</dt><dd>{{ date(item.processedAt) }}</dd></div><div v-if="item.reviewReason"><dt>{{ item.status==='REJECTED'?'拒绝原因':'审核说明' }}</dt><dd>{{ item.reviewReason }}</dd></div><div v-if="scope!=='user' && item.reviewerId"><dt>审核人</dt><dd>{{ item.reviewerName || item.reviewerUsername || item.reviewerId }}<small v-if="item.reviewerName"> · {{ item.reviewerUsername }}</small></dd></div><div v-if="item.refundNo"><dt>模拟退款编号</dt><dd>{{ item.refundNo }}</dd></div></dl>
        <p v-if="item.status==='REJECTED'" class="record-note">本次申请未通过，订单保留原支付状态，电子票恢复使用；入场仍需符合活动规则。</p>
        <p v-if="item.status==='REFUNDED'" class="record-note">整单模拟退款已完成，本订单全部电子票已作废。</p>
      </article>
    </div>
    <p v-else class="refund-hint">暂无退款申请记录</p>
  </section>
</template>
<script setup>
import {ref} from 'vue'
import AppIcon from './AppIcon.vue'
import {orderApi} from '../api/order'
import {organizerOrderApi} from '../api/organizerOrder'
import {askAction} from '../utils/actionDialog'
import {money} from '../utils/ticketPresentation'
const props=defineProps({order:{type:Object,required:true},scope:{type:String,default:'user'}})
const emit=defineEmits(['refresh']),applying=ref(false),reason=ref(''),error=ref(''),busy=ref(false)
const labels={PENDING:'等待审核',REJECTED:'申请未通过',REFUNDED:'模拟退款完成'}
const date=v=>v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'—'
async function apply(){
  if(busy.value)return
  error.value=''
  if(!reason.value.trim()){error.value='请填写退票原因';return}
  busy.value=true
  try{await orderApi.applyRefund(String(props.order.orderId),reason.value.trim());applying.value=false;reason.value='';emit('refresh')}
  catch(e){error.value=e.message||'退款申请提交失败，请重试'}finally{busy.value=false}
}
async function review(approved){
  if(busy.value)return
  busy.value=true;error.value=''
  try{
    const result=await askAction({title:approved?'同意整单模拟退款？':'拒绝退款申请',message:approved?`将按订单实付金额 ¥${money(props.order.latestRefund.amount)} 模拟退款，并作废全部电子票、回补库存。`:'拒绝后订单恢复已支付，电子票恢复使用。请向用户说明拒绝原因。',input:true,required:!approved,inputLabel:approved?'审核说明（选填）':'拒绝原因',requiredMessage:'请填写拒绝原因',confirmText:approved?'同意并模拟退款':'确认拒绝',placeholder:approved?'可填写审核说明':'请填写拒绝原因'})
    if(!result)return
    await organizerOrderApi.reviewRefund(String(props.order.orderId),String(props.order.latestRefund.id),approved,result.reason||'');emit('refresh')
  }catch(e){error.value=e.message||'审核失败，请刷新后重试'}finally{busy.value=false}
}
</script>
<style scoped>
.refund-section{padding:20px;background:#fff;border:1px solid #e0e7f1;border-radius:16px;margin-top:16px;margin-bottom:20px;color:#17243a}.refund-heading{display:flex;align-items:center;gap:10px}.refund-icon{display:grid;place-items:center;flex:none;width:38px;height:38px;border-radius:11px;color:#2161e8;background:#edf3ff}.refund-heading h2{margin:0;font-size:17px}.refund-heading p{margin:5px 0 0;font-size:11px;color:#74839b}.refund-rule,.simulation-note,.refund-hint,.refund-form>p,.record-note{font-size:12px;line-height:1.7;color:#74839b}.simulation-note{background:#f2f6fd;padding:9px 11px;border-radius:9px}.refund-button,.refund-actions button{border:1px solid #d6e1f4;border-radius:9px;padding:11px 15px;background:#fff;color:#2161e8;cursor:pointer;font-size:13px;line-height:1.4;min-height:42px}.refund-form{padding:14px;background:#f7f9fe;border:1px solid #e3eaf5;border-radius:12px;margin-top:14px}.refund-amount{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:13px}.refund-amount strong{color:#2161e8;font-size:22px}.refund-form label{display:flex;flex-direction:column;gap:8px;font-size:13px}.refund-form textarea{box-sizing:border-box;width:100%;resize:vertical;border:1px solid #d6e1f4;border-radius:9px;padding:11px;font:inherit;background:#fff;color:#17243a}.refund-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:13px;flex-wrap:wrap}.refund-actions button.primary{background:#2161e8;border-color:#2161e8;color:#fff}.refund-actions button:disabled{opacity:.55;cursor:wait}.refund-error{font-size:13px;line-height:1.6;color:#c33d46}.review-notice{display:flex;align-items:center;gap:8px;padding:12px;border-radius:9px;color:#b07018;background:#fff8e9;font-size:12px}.review-notice .app-icon{flex:none}.refund-history h3{font-size:14px;margin:20px 0 12px}.refund-record{padding:14px 0;border-top:1px solid #e7edf5}.record-heading{display:flex;justify-content:space-between;align-items:center;gap:12px;font-size:14px}.record-heading b{font-size:12px;border-radius:20px;padding:5px 9px;background:#edf3ff;color:#2161e8}.record-heading .rejected{background:#fff1ed;color:#b95136}.record-heading .refunded{background:#e4f7ef;color:#148a67}.refund-record dl{margin:12px 0 0}.refund-record dl>div{display:flex;align-items:flex-start;justify-content:space-between;gap:15px;margin:8px 0;font-size:12px;line-height:1.65}.refund-record dt{color:#74839b;flex:none}.refund-record dd{margin:0;text-align:right;overflow-wrap:anywhere;min-width:0;white-space:pre-wrap}.refund-record small{color:#74839b}.record-note{margin:10px 0 0}@media(min-width:900px){.refund-section{padding:24px}.refund-form{max-width:660px}.refund-record{max-width:850px}}@media(max-width:360px){.refund-section{padding:14px}.refund-actions button{flex:1;padding:10px 8px}}
</style>
