<template>
 <div class="management-messages" ref="root">
  <button class="message-bell" aria-label="业务消息" :aria-expanded="open" @click="open=!open"><AppIcon name="bell" size="20"/><span v-if="count">{{ count>99?'99+':count }}</span><i v-if="error" title="消息暂未更新"></i></button>
  <section v-if="open" class="message-inbox" aria-label="业务消息列表">
   <div class="inbox-heading"><div><h2>业务消息</h2><small>{{ count }} 条未读 · 每15秒更新</small></div><button aria-label="关闭消息列表" @click="open=false"><AppIcon name="close"/></button></div>
   <p v-if="error" class="message-error" role="alert">{{ error }} <button @click="load">重试</button></p>
   <p v-if="loading&&!items.length">消息加载中…</p><p v-else-if="!items.length&&!error" class="message-empty">暂无业务消息</p>
   <article v-for="m in items" :key="m.id" :class="{'unread-message':!Number(m.isRead)}"><span class="message-type"><AppIcon :name="m.type==='REFUND_REQUESTED'?'receipt':'clipboard-check'" size="18"/></span><div><b>{{ m.title }}</b><p>{{ m.content }}</p><small>{{ date(m.createdAt) }}</small><div class="message-actions"><button @click="visit(m)">查看详情 <AppIcon name="chevron-right" size="13"/></button><button v-if="!Number(m.isRead)" @click="markRead(m)">标记已读</button></div></div></article>
   <footer><button :disabled="page===1||loading" @click="page--;load()">上一页</button><span>第 {{ page }} 页</span><button :disabled="items.length<20||loading" @click="page++;load()">下一页</button></footer>
  </section>
  <section v-if="toast" class="management-toast" role="status" @mouseenter="hover=true" @mouseleave="hover=false" @focusin="focused=true" @focusout="focused=false">
   <span class="toast-icon"><AppIcon name="bell"/></span><div class="toast-copy"><b>{{ toast.title }}</b><p>{{ toast.content }}</p><button @click="toast.group?showInbox():visit(toast)">{{ toast.group?'查看消息':'查看详情' }} <AppIcon name="chevron-right" size="14"/></button></div><button class="toast-close" aria-label="关闭本次提醒" @click="toast=null"><AppIcon name="close" size="16"/></button><div class="toast-track"><span :style="{width:`${remaining/80}%`}"></span></div>
  </section>
 </div>
</template>
<script setup>
import {ref,onMounted,onUnmounted} from 'vue'
import {useRouter} from 'vue-router'
import {http} from '../api/http'
import AppIcon from './AppIcon.vue'
const router=useRouter(),root=ref(null),open=ref(false),items=ref([]),count=ref(0),page=ref(1),loading=ref(false),error=ref(''),toast=ref(null),remaining=ref(8000),hover=ref(false),focused=ref(false)
const seen=new Set();let initialized=false,poll,ticker,disposed=false
const date=value=>value?String(value).replace('T',' ').slice(0,19):''
async function load(){
 if(loading.value||disposed)return;loading.value=true
 try{
  const result=await http.get('/controller/management/messages',{params:{page:page.value}});if(disposed)return
  items.value=result.items||[];count.value=Number(result.unreadCount||0);error.value=''
  const unread=result.unread||[],ids=(result.unreadIds||unread.map(m=>m.id)).map(String),freshIds=ids.filter(id=>!seen.has(id))
  ids.forEach(id=>seen.add(id))
  if(initialized&&freshIds.length){const detail=freshIds.length===1?unread.find(m=>String(m.id)===freshIds[0]):null;toast.value=detail||{title:`新增 ${freshIds.length} 条业务消息`,content:'有新的审核或退款事项，请打开消息列表查看。',group:true};remaining.value=8000;hover.value=false;focused.value=false}
  initialized=true
 }catch(e){if(!disposed)error.value=e.message||'消息暂时无法更新'}finally{loading.value=false}
}
async function markRead(m){try{await http.post(`/controller/management/messages/${m.id}/read`);m.isRead=1;await load()}catch(e){error.value=e.message||'标记失败，请重试'}}
async function visit(m){if(!/^\/management\/(platform|organizer)\//.test(m.targetPath||''))return;await markRead(m);toast.value=null;open.value=false;router.push(m.targetPath)}
function showInbox(){toast.value=null;page.value=1;open.value=true;load()}
function outside(e){if(open.value&&!root.value?.contains(e.target))open.value=false}
function escape(e){if(e.key==='Escape'){open.value=false;toast.value=null}}
onMounted(()=>{load();poll=setInterval(()=>{if(document.visibilityState!=='hidden')load()},15000);ticker=setInterval(()=>{if(toast.value&&!hover.value&&!focused.value&&document.visibilityState!=='hidden'){remaining.value-=100;if(remaining.value<=0)toast.value=null}},100);document.addEventListener('click',outside);document.addEventListener('keydown',escape)})
onUnmounted(()=>{disposed=true;clearInterval(poll);clearInterval(ticker);document.removeEventListener('click',outside);document.removeEventListener('keydown',escape)})
</script>
<style scoped>
.management-messages{position:relative}.message-bell{position:relative;display:grid;place-items:center;width:38px;height:38px;background:#f3f6fc;border:1px solid #e0e7f2;border-radius:11px;color:#526682;cursor:pointer}.message-bell>span{position:absolute;right:-5px;top:-5px;min-width:17px;border-radius:12px;background:#df4d58;color:#fff;font-size:10px;padding:2px 4px}.message-bell>i{position:absolute;bottom:3px;right:3px;width:6px;height:6px;border-radius:50%;background:#d8871a}.message-inbox{position:absolute;right:0;top:48px;width:min(410px,calc(100vw - 32px));max-height:75vh;overflow:auto;background:#fff;border:1px solid #dce5f2;border-radius:16px;box-shadow:0 14px 45px #18335b26;z-index:90;color:#17233b}.inbox-heading{display:flex;justify-content:space-between;align-items:center;padding:20px;border-bottom:1px solid #edf1f7}.inbox-heading h2{font-size:17px;margin:0 0 5px}.message-inbox small{color:#8190a6;font-size:11px}.message-inbox button,.management-toast button{display:inline-flex;align-items:center;gap:3px;border:0;background:none;color:#2161e8;cursor:pointer;padding:5px;font-size:12px}.message-inbox article{display:flex;gap:12px;padding:16px 20px;border-bottom:1px solid #edf1f7}.message-inbox article>div{min-width:0;flex:1}.message-inbox b{font-size:13px}.message-inbox p{font-size:12px;line-height:1.8;overflow-wrap:anywhere;color:#697c97;margin:7px 0}.message-type,.toast-icon{display:grid;place-items:center;background:#eaf1ff;color:#2161e8;width:34px;height:34px;border-radius:10px;flex:none}.unread-message{background:#f6f9ff}.message-actions{display:flex;gap:12px;margin-top:6px}.message-inbox footer{display:flex;justify-content:space-between;align-items:center;padding:12px 20px;font-size:12px;color:#73829a}.message-empty,.message-error{padding:12px 20px}.message-inbox .message-error{color:#bb394c}.message-inbox button:disabled{opacity:.4;cursor:default}.management-toast{position:fixed;top:82px;right:24px;z-index:100;display:flex;gap:12px;width:min(395px,calc(100vw - 32px));padding:19px 36px 20px 18px;border:1px solid #d7e3fa;border-radius:14px;background:#fff;box-shadow:0 12px 40px #17376026;overflow:hidden;color:#152440}.toast-copy{flex:1;min-width:0}.toast-copy>b{font-size:14px}.toast-copy p{color:#70819b;font-size:12px;line-height:1.8;margin:8px 0;overflow-wrap:anywhere;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}.management-toast .toast-close{position:absolute;right:8px;top:9px;color:#8491a6}.toast-track{position:absolute;bottom:0;left:0;right:0;height:3px;background:#e9effa}.toast-track span{display:block;height:100%;background:#2865e8;transition:width .1s linear}
</style>
