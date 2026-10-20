<template>
  <div class="management-layout">
    <aside class="management-sidebar">
      <div class="management-brand"><strong>Venuo</strong><span>MANAGEMENT PORTAL</span></div>
      <RouterLink class="new-activity-button" to="/management/activities/new">＋ 新建活动</RouterLink>
      <nav class="management-nav">
        <RouterLink to="/management">▦ <span>工作台</span></RouterLink>
        <RouterLink to="/management/activities">▣ <span>活动管理</span></RouterLink>
        <RouterLink to="/management/ticket-inventory">▤ <span>票种与库存</span></RouterLink>
        <RouterLink to="/management/orders">▥ <span>订单管理</span></RouterLink>
        <RouterLink to="/management/verification">♢ <span>核销管理</span></RouterLink>
      </nav>
      <div class="management-sidebar-bottom"><a class="disabled">⚙ 设置</a><a class="disabled">? 帮助</a></div>
    </aside>
    <section class="management-main">
      <header class="management-header"><div class="global-search">⌕ <input placeholder="全局搜索订单、用户或活动..." /></div><div class="management-user"><StaffAccountMenu /><button title="退出登录" @click="logout">↪</button></div></header>
      <RouterView />
    </section>
  </div>
</template>
<script setup>
import StaffAccountMenu from '../components/StaffAccountMenu.vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore(), router = useRouter()

async function logout() { if (await auth.logout()) router.replace('/login') }
</script>
<style scoped>
.management-layout{min-height:100vh;display:flex;background:#f8f8fc;color:#202334}.management-sidebar{width:240px;flex:0 0 240px;background:#f0f0ff;border-right:1px solid #e0e1ef;display:flex;flex-direction:column;padding:20px 12px}.management-brand{padding:0 8px 30px;display:flex;flex-direction:column;gap:4px}.management-brand strong{font-size:18px;color:#1245cf}.management-brand span{font-size:12px;color:#62677b;letter-spacing:.6px}.new-activity-button{background:#0c43d0;color:#fff;text-align:center;padding:14px 10px;border-radius:10px;font-weight:700;margin-bottom:18px}.management-nav{display:flex;flex-direction:column;gap:6px}.management-nav a,.management-sidebar-bottom a{padding:13px 14px;border-radius:9px;color:#565b6f;display:flex;gap:14px;align-items:center;font-size:15px}.management-nav a.router-link-exact-active{background:#0d55ed;color:#fff}.management-nav a.disabled,.management-sidebar-bottom a.disabled{cursor:default;opacity:.75}.management-sidebar-bottom{margin-top:auto;border-top:1px solid #dedfee;padding-top:14px;display:flex;flex-direction:column;gap:4px}.management-main{min-width:0;flex:1}.management-header{height:64px;background:#fff;border-bottom:1px solid #e6e7f0;display:flex;align-items:center;justify-content:space-between;padding:0 32px}.global-search{width:min(430px,50%);background:#f0f0ff;border-radius:12px;padding:9px 14px;color:#72778b}.global-search input{border:0;outline:0;background:transparent;width:90%;font-size:14px}.management-user{display:flex;align-items:center;gap:12px}.management-user>span{font-size:22px}.management-user small{display:block;color:#7b8092;font-size:11px;margin-top:3px}.management-user button{border:0;background:transparent;font-size:22px;color:#4d5367}.management-content{padding:34px;max-width:1440px}.management-title{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:28px}.management-title h1{margin:0 0 8px;font-size:28px}.management-title p{margin:0;color:#6e7387}.management-card{background:#fff;border:1px solid #e2e3ee;border-radius:10px;box-shadow:0 3px 12px #1f2a5508}.management-button{border:0;border-radius:7px;padding:11px 18px;background:#0d50dc;color:#fff;font-weight:700}.management-button.light{background:#ebefff;color:#0d50dc}.management-button.danger{background:#fff;color:#d33d45;border:1px solid #e2a2a5}.management-button:disabled{opacity:.5}.management-table{width:100%;border-collapse:collapse}.management-table th,.management-table td{text-align:left;padding:16px 18px;border-bottom:1px solid #ececf3;vertical-align:middle}.management-table th{background:#fafaff;color:#61677a;font-size:13px}.activity-title-cell{display:flex;align-items:center;gap:12px}.activity-thumb{width:62px;height:44px;border-radius:6px;background:#e2e8ff center/cover}.activity-title-cell b{display:block}.activity-title-cell small{display:block;color:#7a7f91;margin-top:5px}.status-chip{display:inline-flex;padding:5px 10px;border-radius:20px;font-size:12px;font-weight:700}.status-DRAFT{background:#eef0f5;color:#656b7c}.status-PENDING{background:#fff2d7;color:#a66b00}.status-PUBLISHED{background:#ddf8e9;color:#138350}.status-REJECTED{background:#ffe4e4;color:#c53a43}.status-OFFLINE,.status-CANCELLED{background:#ececf1;color:#686c7b}.empty-panel{padding:70px 20px;text-align:center;color:#73798b}.filter-row{display:flex;gap:10px;flex-wrap:wrap}.filter-row input,.filter-row select,.form-field input,.form-field textarea{border:1px solid #dfe2ee;border-radius:8px;padding:11px 12px;background:#fff;outline:0}.filter-row input{min-width:260px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.form-field{display:flex;flex-direction:column;gap:8px}.form-field.full{grid-column:1/-1}.form-field label{font-weight:700;font-size:14px}.form-field textarea{min-height:130px;resize:vertical}.form-actions{display:flex;gap:10px;justify-content:flex-end;margin-top:24px}.form-error{color:#d33d45;margin-top:12px}.cover-preview{width:180px;height:100px;border-radius:8px;background:#e6ebff center/cover}.cover-upload{display:flex;align-items:center;gap:14px}.cover-upload input{border:0;padding:0}
@media(max-width:900px){.management-sidebar{width:190px;flex-basis:190px}.management-header{padding:0 18px}.management-content{padding:24px 18px}.management-table{min-width:900px}.management-card.table-wrap{overflow:auto}.form-grid{grid-template-columns:1fr}.form-field.full{grid-column:auto}}
@media(max-width:640px){.management-sidebar{width:66px;flex-basis:66px;padding:14px 8px}.management-brand span,.management-brand strong,.new-activity-button,.management-nav span,.management-sidebar-bottom{display:none}.management-brand{height:42px;padding:0}.management-nav a{justify-content:center;padding:12px 4px;font-size:20px}.management-header{height:58px}.global-search{width:52px;padding:9px;text-align:center}.global-search input{display:none}.management-user div{display:none}.management-content{padding:18px 12px}.management-title h1{font-size:22px}.management-title{gap:10px}}
/* Keep the sidebar fixed while the main pane owns all vertical scrolling. */
.management-layout{height:100vh;min-height:0;overflow:hidden}
.management-sidebar{overflow:hidden}
.management-main{min-height:0;display:flex;flex-direction:column;overflow:hidden}
.management-header{flex:0 0 64px}
.management-main>.management-content{box-sizing:border-box;width:100%;min-height:0;overflow:auto;overscroll-behavior:contain}
</style>

