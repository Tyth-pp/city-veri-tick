<template>
  <GuestProfileView v-if="auth.isGuest" />
  <main v-else class="page profile-page">
    <header class="topbar profile-topbar">
      <div class="profile-brand"><img :src="logo" alt="" /><h1>Profile</h1></div>
      <div class="profile-actions"><RouterLink to="/reminders" class="icon-button" title="活动与出票提醒"><AppIcon name="bell" /></RouterLink><span class="avatar top-avatar"><AppIcon name="user" size="18" /></span></div>
    </header>
    <div class="container">
      <section class="profile-head card"><div class="avatar photo"><img v-if="profile?.avatarUrl" :src="profile.avatarUrl" alt="个人头像" /><span v-else>{{ avatarLetter }}</span></div><div class="profile-copy"><h2><span :title="displayName">{{ displayName }}</span></h2><p>{{ maskedPhone }}</p><span class="club-tag"><AppIcon name="medal" size="14" /><span><b>City Club 会员</b><small>积分、等级和专属权益即将开放</small></span></span></div><button class="profile-action" title="编辑个人资料" aria-label="编辑个人资料" :disabled="!profile" @click="openEditor"><AppIcon name="edit" size="23" /></button></section>
      <p v-if="profileError" class="profile-load-error">{{ profileError }} <button @click="loadProfile">重试</button></p>
      <section class="card order-shortcut"><div class="shortcut-title"><b>我的订单</b><RouterLink to="/orders">全部订单 <AppIcon name="chevron-right" size="17" /></RouterLink></div><div class="quick-grid"><RouterLink to="/orders?filter=pending-payment"><span><AppIcon name="clock" /><i v-if="hasOrderCount('pendingPayment')">{{ orderCounts.pendingPayment }}</i></span><b>待付款</b></RouterLink><RouterLink to="/orders?filter=pending-use"><span><AppIcon name="ticket" /><i v-if="hasOrderCount('pendingUse')" class="blue">{{ orderCounts.pendingUse }}</i></span><b>待使用</b></RouterLink><RouterLink to="/orders?filter=completed"><span><AppIcon name="check-circle" /><i v-if="hasOrderCount('completed')" class="gray">{{ orderCounts.completed }}</i></span><b>已完成</b></RouterLink><RouterLink to="/orders?filter=after-sale"><span><AppIcon name="refresh" /><i v-if="hasOrderCount('afterSale')" class="orange">{{ orderCounts.afterSale }}</i></span><b>退款/售后</b></RouterLink></div></section>
      <TicketWallet :orders="walletOrders" :loading="ordersLoading" :error="ordersError" @retry="loadOrderCounts" />
      <MyCouponsEntry />
      <section class="card menu-list"><button @click="openIdentity"><span><AppIcon name="user-check" /></span><div><b>实名信息</b><small>{{ profile?.realNameVerified ? `${profile.realName} · ${profile.idCardMasked}` : '购票前请填写姓名和身份证号' }}</small></div><em>{{ profile?.realNameVerified ? '已填写' : '未实名' }}</em><AppIcon name="chevron-right" size="18" /></button><RouterLink to="/favorites" class="menu-link"><span class="red"><AppIcon name="heart" /></span><div><b>我的收藏活动</b><small>{{ favorites.items.length ? `已收藏 ${favorites.items.length} 场精彩活动` : '还没有收藏活动' }}</small></div><em class="count">{{ favorites.items.length }}</em><AppIcon name="chevron-right" size="18" /></RouterLink><button @click="clearSearch"><span><AppIcon name="history" /></span><div><b>搜索历史管理</b><small>本地留存记录</small></div><em class="clear-history">一键清空</em></button></section>
      <section class="card menu-list setting-list"><RouterLink to="/reminders" class="menu-link"><span><AppIcon name="bell" /></span><div><b>活动与出票提醒</b><small>提示音设置、开售订阅与提醒记录</small></div><AppIcon name="chevron-right" size="18" /></RouterLink><RouterLink to="/help" class="menu-link"><span><AppIcon name="headset" /></span><div><b>官方客服与帮助中心</b><small>购票、支付、核销和退票问题说明</small></div><AppIcon name="chevron-right" size="18" /></RouterLink><RouterLink to="/about" class="menu-link"><span><AppIcon name="info" /></span><div><b>关于 Venuo</b><small>实名制电子票务与现场核验平台</small></div><em class="version">v1.0.0</em><AppIcon name="chevron-right" size="18" /></RouterLink></section>
      <button class="logout-button" @click="logout"><AppIcon name="logout" size="19" /> 退出登录</button><p class="security-note">实名信息仅用于购票与入场核对</p>
    </div>
    <div v-if="editing" class="profile-dialog-backdrop" @click.self="editing = false">
      <form class="profile-dialog card" @submit.prevent="saveProfile">
        <div class="profile-dialog-title"><h2>编辑个人资料</h2><button type="button" aria-label="关闭" @click="editing = false">×</button></div>
        <label class="avatar-picker"><span class="avatar photo"><img v-if="draftAvatarUrl || profile?.avatarUrl" :src="draftAvatarUrl || profile.avatarUrl" alt="头像预览" /><span v-else>{{ avatarLetter }}</span></span><span>{{ uploading ? '上传中...' : '更换头像' }}</span><input type="file" accept="image/jpeg,image/png,image/webp,image/gif" :disabled="uploading" @change="onAvatarSelected" /></label>
        <label class="profile-field">昵称<input v-model.trim="draftNickname" maxlength="24" autocomplete="nickname" placeholder="请输入昵称" /></label>
        <p v-if="editError" class="profile-error">{{ editError }}</p>
        <div class="profile-dialog-actions"><button type="button" @click="editing = false">取消</button><button type="submit" :disabled="saving || uploading">{{ saving ? '保存中...' : '保存' }}</button></div>
      </form>
    </div>
    <div v-if="identityEditing" class="profile-dialog-backdrop" @click.self="identityEditing=false">
      <form class="profile-dialog card" @submit.prevent="saveIdentity">
        <div class="profile-dialog-title"><h2>实名信息</h2><button type="button" aria-label="关闭" @click="identityEditing=false">×</button></div>
        <label class="profile-field">真实姓名<input v-model.trim="draftRealName" required minlength="2" maxlength="32" autocomplete="name" /></label>
        <label class="profile-field">身份证号<input v-model.trim="draftIdCardNo" required maxlength="18" autocomplete="off" placeholder="请输入18位身份证号" /></label>
        <p class="profile-hint">仅用于购票和入场核对。修改后仅对新订单生效。</p>
        <p v-if="identityError" class="profile-error" role="alert">{{ identityError }}</p>
        <div class="profile-dialog-actions"><button type="button" @click="identityEditing=false">取消</button><button type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存实名信息' }}</button></div>
      </form>
    </div>
    <div v-if="clearSearchDialog" class="clear-search-backdrop" @click.self="clearSearchDialog=false">
      <section class="clear-search-sheet" role="dialog" aria-modal="true" aria-labelledby="clear-search-title">
        <div class="clear-search-handle"></div>
        <span class="clear-search-icon"><AppIcon name="history" size="22" /></span>
        <h2 id="clear-search-title">清空搜索历史？</h2>
        <p>清空后将无法恢复这台设备上的搜索记录。</p>
        <div class="clear-search-actions"><button type="button" class="clear-search-cancel" @click="clearSearchDialog=false">取消</button><button type="button" class="clear-search-confirm" @click="confirmClearSearch">确认清空</button></div>
      </section>
    </div>
    <div v-if="searchToast" class="search-toast" role="status"><AppIcon name="check-circle" size="17" /> 搜索历史已清空</div>
  </main>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { profileApi } from '../../api/profile'
import { uploadImage } from '../../api/file'
import { orderApi } from '../../api/order'
import AppIcon from '../../components/AppIcon.vue'
import MyCouponsEntry from '../../components/MyCouponsEntry.vue'
import TicketWallet from '../../components/TicketWallet.vue'
import { isPendingUse } from '../../utils/orderTickets'
import logo from '../../assets/venu-logo.svg'
import GuestProfileView from './GuestProfileView.vue'
import { useFavoritesStore } from '../../stores/favorites'
const auth = useAuthStore(), router = useRouter(), favorites = useFavoritesStore()
const route = useRoute()
const profile = ref(null)
const profileError = ref('')
const editing = ref(false)
const identityEditing = ref(false), identityError = ref('')
const clearSearchDialog = ref(false), searchToast = ref(false)
const uploading = ref(false)
const saving = ref(false)
const editError = ref('')
const draftNickname = ref('')
const draftRealName = ref('')
const draftIdCardNo = ref('')
const draftAvatarFileId = ref(null)
const draftAvatarUrl = ref('')
const displayName = computed(() => profile.value?.nickname || '用户')
const avatarLetter = computed(() => displayName.value.slice(0, 1).toUpperCase())
const maskedPhone = computed(() => profile.value?.phone?.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') || '未绑定手机号')
const orderCounts = ref({ pendingPayment: 0, pendingUse: 0, completed: 0, afterSale: 0 })
const walletOrders = ref([])
const ordersLoading = ref(true), ordersError = ref(false)
const hasOrderCount = (key) => !ordersLoading.value && !ordersError.value && orderCounts.value[key] > 0
async function loadOrderCounts() {
  if (auth.isGuest) return
  ordersLoading.value = true
  ordersError.value = false
  try {
    const orders = await orderApi.list() || []
    const list = Array.isArray(orders) ? orders : orders.content || orders.records || []
    const counts = { pendingPayment: 0, pendingUse: 0, completed: 0, afterSale: 0 }
    walletOrders.value = list
    list.forEach((order) => {
      const status = String(order.status || '').toUpperCase()
      if (status === 'PENDING_PAYMENT') counts.pendingPayment += 1
      if (order.latestRefund || ['REFUNDING', 'REFUNDED', 'AFTER_SALE'].includes(status)) counts.afterSale += 1
      const tickets = Array.isArray(order.tickets) ? order.tickets : []
      const allTicketsChecked = tickets.length > 0 && tickets.every((ticket) => String(ticket.status || '').toUpperCase() === 'CHECKED_IN')
      if (isPendingUse(order)) counts.pendingUse += 1
      if (status === 'COMPLETED' || (status === 'PAID' && allTicketsChecked)) counts.completed += 1
    })
    orderCounts.value = counts
  } catch {
    ordersError.value = true
  } finally {
    ordersLoading.value = false
  }
}
onMounted(() => { loadProfile(); loadOrderCounts() })
async function loadProfile() {
  if (auth.isGuest) return
  profileError.value = ''
  try { profile.value = await profileApi.get(); if (route.query.identity === '1') openIdentity() }
  catch { profileError.value = '个人资料加载失败' }
}
function openEditor() {
  draftNickname.value = profile.value?.nickname || ''
  draftRealName.value = profile.value?.realName || ''
  draftIdCardNo.value = ''
  draftAvatarFileId.value = null
  draftAvatarUrl.value = ''
  editError.value = ''
  editing.value = true
}
async function onAvatarSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  editError.value = ''
  try {
    const result = await uploadImage(file)
    draftAvatarFileId.value = result.fileId
    draftAvatarUrl.value = result.url
  } catch (error) { editError.value = error.message }
  finally { uploading.value = false; event.target.value = '' }
}
async function saveProfile() {
  if (draftNickname.value.length < 2) { editError.value = '昵称至少需要 2 个字符'; return }
  saving.value = true
  editError.value = ''
  try {
    profile.value = await profileApi.update({ nickname: draftNickname.value, avatarFileId: draftAvatarFileId.value })
    editing.value = false
  } catch (error) { editError.value = error.message }
  finally { saving.value = false }
}
function clearSearch() { clearSearchDialog.value = true }
function confirmClearSearch() {
  localStorage.removeItem('city-recent-searches')
  clearSearchDialog.value = false
  searchToast.value = true
  window.setTimeout(() => { searchToast.value = false }, 2200)
}
function openIdentity() { if (!profile.value) return; draftRealName.value=profile.value.realName || ''; draftIdCardNo.value=''; identityError.value=''; identityEditing.value=true }
async function saveIdentity() {
  if (saving.value) return
  if (draftRealName.value.length < 2 || !/^\d{17}[\dXx]$/.test(draftIdCardNo.value)) { identityError.value='请填写姓名和正确的18位身份证号'; return }
  saving.value=true; identityError.value=''
  try {
    profile.value=await profileApi.update({realName:draftRealName.value,idCardNo:draftIdCardNo.value})
    identityEditing.value=false; draftIdCardNo.value=''
    if (route.query.returnTo === '/orders/confirm') router.replace('/orders/confirm')
  }
  catch(e) { identityError.value=e.message }
  finally { saving.value=false }
}
async function logout() { if (await auth.logout()) router.replace('/login') }
</script>
<style scoped>
.profile-page{background:#f8fafc}.profile-topbar{height:64px}.profile-brand{display:flex;align-items:center;gap:10px}.profile-brand img{width:32px;height:32px;object-fit:cover;border-radius:2px;background:#f4f7ff}.profile-brand h1{margin:0;font-size:21px;font-weight:700}.profile-actions{display:flex;align-items:center;gap:7px}.top-avatar{width:40px;height:40px}.avatar{border-radius:50%;display:inline-grid;place-items:center;background:var(--primary);color:#fff;font-size:18px}.container{padding-top:10px;padding-bottom:84px}.profile-head{display:flex;align-items:center;gap:12px;padding:20px}.profile-head .photo{width:66px;height:66px;border:3px solid var(--primary);font-size:25px;flex:none}.profile-copy{min-width:0}.profile-copy h2{display:flex;align-items:center;gap:5px;margin:0 0 4px;font-size:20px}.profile-copy h2 .app-icon{color:var(--primary)}.profile-copy p{margin:0 0 5px;color:var(--muted);font-size:14px}.club-tag{display:inline-flex;align-items:center;gap:5px;background:#e4e9ff;color:var(--primary);border-radius:14px;padding:5px 9px;font-size:12px;white-space:nowrap}.club-tag>span{display:flex;flex-direction:column;gap:2px}.club-tag b{font-size:12px;line-height:1.1}.club-tag small{font-size:10px;color:#6074b0;line-height:1.1}.profile-action{border:0;background:transparent;color:#64748b;padding:4px;margin-left:auto}.profile-action+.profile-action{margin-left:0}.order-shortcut{padding:17px 16px;margin-top:16px}.shortcut-title{display:flex;align-items:center;justify-content:space-between;font-size:18px}.shortcut-title a{display:flex;align-items:center;color:var(--muted);font-size:13px}.quick-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:18px 0 0}.quick-grid a{display:flex;flex-direction:column;align-items:center;gap:7px;color:var(--body)}.quick-grid a>span{position:relative;width:50px;height:50px;display:grid;place-items:center;border-radius:50%;background:#edf1ff;color:var(--primary)}.quick-grid i{position:absolute;right:-3px;top:-5px;width:19px;height:19px;display:grid;place-items:center;border-radius:50%;background:#ef4444;color:#fff;font-style:normal;font-size:11px}.quick-grid i.blue{background:var(--primary)}.quick-grid i.gray{background:#dbe2f3;color:#64748b}.quick-grid i.orange{background:#f97316}.quick-grid b{font-size:13px}.benefit-row button:disabled{opacity:.65;cursor:default}.benefit-row{display:flex;align-items:center;gap:12px;padding:15px;margin-top:14px}.benefit-icon{display:grid;place-items:center;background:#ffeadf;color:#b45309;padding:10px;border-radius:10px}.benefit-row div{flex:1;min-width:0}.benefit-row b{font-size:16px;white-space:nowrap}.benefit-row b em{font-style:normal;font-size:11px;font-weight:400;color:#ef4444;background:#ffe2dc;border-radius:5px;padding:3px 5px;margin-left:5px}.benefit-row p{margin:4px 0 0;color:var(--muted);font-size:13px}.benefit-row button{display:flex;align-items:center;border:0;background:#e5e9ff;color:var(--primary);border-radius:15px;padding:10px 11px;font-weight:700;white-space:nowrap}.menu-list{padding:4px 15px;margin-top:14px}.menu-list button{display:flex;align-items:center;gap:10px;width:100%;min-height:70px;border:0;border-bottom:1px solid var(--line);background:transparent;text-align:left;color:#64748b}.menu-list button:last-child{border-bottom:0}.menu-list button>span{width:27px;height:27px;display:grid;place-items:center;color:var(--primary);background:#e9edff;border-radius:9px;flex:none}.menu-list button>span.orange{color:#ff7b22;background:#fff0e6}.menu-list button>span.red{color:#ff4a4a}.menu-list button div{display:flex;flex-direction:column;gap:4px;flex:1;min-width:0}.menu-list b{font-size:16px;font-weight:500;color:var(--text)}.menu-list small{color:var(--muted);font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.menu-list em{font-style:normal;color:#64748b;font-size:11px;white-space:nowrap}.menu-list em.success{color:#008f68;background:#d9faec;border-radius:13px;padding:5px 8px}.menu-list em.count{color:var(--primary);background:#edf2ff;border-radius:13px;padding:5px 10px}.menu-list em.clear-history{color:#5e6d96;background:#eef1ff;border-radius:8px;padding:6px 9px}.toggle{width:46px;height:25px;border-radius:20px;background:var(--primary);position:relative;flex:none}.toggle:after{content:'';position:absolute;right:3px;top:3px;width:19px;height:19px;border-radius:50%;background:#fff}.logout-button{display:flex;align-items:center;justify-content:center;gap:6px;width:100%;min-height:48px;border:0;border-radius:12px;background:#fff;color:#ef4444;font-size:17px;font-weight:700;margin:24px 0 8px}.security-note{text-align:center;color:#64748b;font-size:12px;margin:0 0 20px}.version{background:#e8edff;border-radius:12px;padding:5px 10px!important}@media (max-width:350px){.profile-head{padding:16px 12px}.profile-head .photo{width:58px;height:58px}.profile-copy h2{font-size:18px}.club-tag{font-size:11px}.benefit-row{gap:8px;padding:13px 10px}.benefit-row b{font-size:14px}.benefit-row b em{font-size:10px;margin-left:2px}.menu-list{padding-left:10px;padding-right:10px}.menu-list button{gap:7px}.menu-list em{font-size:10px}}
.profile-head .photo img,.avatar-picker .photo img{width:100%;height:100%;object-fit:cover;border-radius:50%}
.profile-copy h2 span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.profile-copy h2 .app-icon{flex:none}
.profile-action{flex:none}
.profile-action:disabled{opacity:.4;cursor:not-allowed}
.profile-load-error{padding:10px 16px;margin:8px 0;color:#b91c1c;font-size:13px}
.profile-load-error button{border:0;background:transparent;color:var(--primary);font-weight:600;cursor:pointer}
.profile-dialog-backdrop{position:fixed;inset:0;z-index:30;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.42)}
.profile-dialog{width:min(100%,420px);padding:24px;background:#fff}
.profile-dialog-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px}
.profile-dialog-title h2{margin:0;font-size:20px}
.profile-dialog-title button{border:0;background:transparent;font-size:26px;color:#64748b;cursor:pointer}
.avatar-picker{display:flex;align-items:center;gap:16px;width:max-content;margin-bottom:24px;color:var(--primary);font-weight:600;cursor:pointer}
.avatar-picker .photo{width:72px;height:72px;overflow:hidden}
.avatar-picker input{position:absolute;width:1px;height:1px;opacity:0}
.profile-field{display:grid;gap:8px;font-size:14px;font-weight:600}
.profile-field input{width:100%;height:44px;padding:0 12px;border:1px solid #dbe2ed;border-radius:7px;font:inherit;outline:none}
.profile-field input:focus{border-color:var(--primary)}
.profile-error{margin:12px 0 0;color:#dc2626;font-size:13px}
.real-name-panel{display:flex;align-items:center;gap:10px;padding:14px 16px;margin-top:14px;cursor:pointer}.real-name-panel>div{flex:1;min-width:0}.real-name-panel b,.real-name-panel small{display:block}.real-name-panel b{font-size:15px}.real-name-panel small{margin-top:4px;color:var(--muted);font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.real-name-panel em{font-style:normal;font-size:11px}.real-name-panel em.success{color:#008f68;background:#d9faec;border-radius:13px;padding:5px 8px}.real-name-panel em.warning{color:#b45309;background:#fff1d6;border-radius:13px;padding:5px 8px}.profile-hint{margin:8px 0 0;color:#64748b;font-size:11px;line-height:1.5}
.profile-hint{margin:8px 0 0;color:#64748b;font-size:11px;line-height:1.5}.menu-list em.warning{color:#b45309;background:#fff1d6;border-radius:13px;padding:5px 8px}
.profile-dialog-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:24px}
.profile-dialog-actions button{min-width:84px;height:40px;border:1px solid #dbe2ed;border-radius:7px;background:#fff;color:var(--text);font-weight:600;cursor:pointer}
.profile-dialog-actions button[type=submit]{border-color:var(--primary);background:var(--primary);color:#fff}
.profile-dialog-actions button:disabled{opacity:.6;cursor:not-allowed}
 .menu-list button:disabled{opacity:1;cursor:default}
.menu-list .menu-link{display:flex;align-items:center;gap:10px;width:100%;min-height:70px;border-bottom:1px solid var(--line);color:#64748b}
.menu-list .menu-link>span{width:27px;height:27px;display:grid;place-items:center;flex:none;border-radius:9px;background:#e9edff;color:var(--primary)}
.menu-list .menu-link>div{display:flex;flex:1;min-width:0;flex-direction:column;gap:4px}
.clear-search-backdrop{position:fixed;inset:0;z-index:40;display:flex;align-items:flex-end;justify-content:center;background:rgba(15,23,42,.42);padding:0}
.clear-search-sheet{width:min(100%,520px);padding:10px 20px calc(20px + env(safe-area-inset-bottom));border-radius:22px 22px 0 0;background:#fff;box-shadow:0 -12px 35px rgba(15,23,42,.16);text-align:center;animation:clear-search-up .2s ease-out}
.clear-search-handle{width:38px;height:4px;margin:0 auto 18px;border-radius:99px;background:#dbe3f2}
.clear-search-icon{display:grid;place-items:center;width:48px;height:48px;margin:0 auto 10px;border-radius:16px;background:#eef2ff;color:var(--primary)}
.clear-search-sheet h2{margin:0;color:var(--text);font-size:18px}.clear-search-sheet p{margin:8px 0 20px;color:var(--muted);font-size:13px;line-height:1.6}
.clear-search-actions{display:grid;grid-template-columns:1fr 1fr;gap:10px}.clear-search-actions button{height:46px;border-radius:12px;font-size:15px;font-weight:700;cursor:pointer}.clear-search-cancel{border:1px solid #dce4f2;background:#f8fafc;color:#64748b}.clear-search-confirm{border:0;background:var(--primary);color:#fff}
.search-toast{position:fixed;left:50%;bottom:calc(88px + env(safe-area-inset-bottom));z-index:45;display:flex;align-items:center;gap:7px;transform:translateX(-50%);padding:11px 16px;border-radius:12px;background:#172554;color:#fff;box-shadow:0 8px 24px rgba(15,23,42,.2);font-size:13px;white-space:nowrap;animation:clear-search-toast .2s ease-out}
@keyframes clear-search-up{from{transform:translateY(20px);opacity:.5}to{transform:translateY(0);opacity:1}}@keyframes clear-search-toast{from{transform:translate(-50%,8px);opacity:0}to{transform:translate(-50%,0);opacity:1}}
</style>




