<template>
  <div class="layout">
    <aside>
      <div class="brand"><img :src="logo" alt="Venuo" /><div><strong>Venuo</strong><span>主办方工作台　PRO</span></div></div>
      <RouterLink class="new-button" to="/management/organizer/activities/new"
        ><AppIcon name="plus" size="17" /> 新建活动</RouterLink
      >
      <nav>
        <RouterLink to="/management/organizer"><AppIcon name="layout-dashboard" /><span>工作台</span></RouterLink>
        <RouterLink to="/management/organizer/activities"><AppIcon name="calendar" /><span>活动管理</span></RouterLink>
        <RouterLink to="/management/organizer/ticket-inventory"><AppIcon name="ticket" /><span>票种与库存</span></RouterLink>
        <RouterLink to="/management/organizer/orders"><AppIcon name="receipt" /><span>订单管理</span></RouterLink>
        <RouterLink to="/management/organizer/verifiers"><AppIcon name="users" /><span>核销员管理</span></RouterLink>
        <RouterLink to="/management/organizer/verification"><AppIcon name="chart" /><span>核销数据</span></RouterLink>
      </nav>

      <button class="logout" @click="logout"><AppIcon name="logout" size="18" /><span>退出登录</span></button>
    </aside>
    <section class="main">
      <header>
        <ManagementSearch />
        <div class="organizer-user"><StaffAccountMenu /><ManagementMessages /></div>
      </header>
      <RouterView :key="route.path" />
    </section>
  </div>
</template>
<script setup>
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import AppIcon from "../components/AppIcon.vue";
import ManagementSearch from "../components/ManagementSearch.vue";
import ManagementMessages from "../components/ManagementMessages.vue";
import StaffAccountMenu from "../components/StaffAccountMenu.vue";
import logo from "../assets/venu-logo.svg";
const route = useRoute(), auth = useAuthStore(),
  router = useRouter();
async function logout() {
  if (!await auth.logout()) return;
  router.replace("/login");
}
</script>
<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  background: #f5f8fc;
  color: #17223a;
}
.layout aside {
  width: 252px;
  flex: 0 0 252px;
  padding: 18px 14px;
  background: #fff;
  border-right: 1px solid #e2e8f1;
  display: flex;
  flex-direction: column;
}
.brand {
  display:flex;align-items:center;gap:10px;padding:0 10px 30px;
}
.brand img{width:34px;height:34px;object-fit:cover;border-radius:9px;background:#f2f6ff}
.brand strong {
  display: block;
  color: #15213a;
  font-size: 18px;
}
.brand span {
  display: block;
  color: #2560d7;
  font-size: 11px;
  margin-top: 5px;
}
.new-button {
  display:flex;align-items:center;justify-content:center;gap:6px;background: #2161e8;
  color: #fff;
  text-align: center;
  padding: 14px 10px;
  border-radius: 9px;
  font-weight: 700;
  margin-bottom: 18px;
}
.layout nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.layout nav a {
  padding: 13px 14px;
  border-radius: 9px;
  color: #64738d;
  display: flex;
  gap: 14px;
  align-items: center;
}
.layout nav a.router-link-exact-active {
  background: #edf3ff;
  color: #1559dc;
  box-shadow:inset 3px 0 #1559dc;
}
.logout {
  margin-top: auto;
  display:flex;align-items:center;gap:12px;width:100%;border:0;
  background: transparent;
  color: #64738d;
  border-radius: 7px;
  padding: 10px;
}
.main {
  min-width: 0;
  flex: 1;
}
.main header {
  height: 66px;
  background: #fff;
  border-bottom: 1px solid #e6e7f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
}
.search {
  display:flex;align-items:center;gap:9px;background:#f4f7fc;border:1px solid #e0e7f1;color:#8290a8;border-radius:9px;padding:10px 14px;width:min(540px,55%);font-size:13px;
}
.organizer-user{display:flex;align-items:center;gap:9px}.organizer-user>div{display:flex;flex-direction:column}.organizer-user strong{font-size:13px;color:#25314a}.organizer-user small{font-size:10px;color:#8b96aa}.user-avatar{width:32px;height:32px;display:grid;place-items:center;border-radius:50%;background:#1559dc;color:#fff}
.layout{height:100vh;min-height:0;overflow:hidden}
.layout aside{height:100vh;min-height:0;box-sizing:border-box;position:relative}
.main{height:100vh;min-height:0;display:flex;flex-direction:column;overflow:hidden}
.main > :deep(.management-content){flex:1;min-height:0;overflow:auto;overscroll-behavior:contain;width:100%;max-width:1500px;box-sizing:border-box;margin:0 auto;padding:28px 34px;color:#17223a}
.main > :deep(.management-title){display:flex;justify-content:space-between;align-items:flex-start;gap:24px;margin-bottom:22px}
.main > :deep(.management-title .eyebrow){color:#8794aa;font-size:12px;line-height:1.2;margin:0 0 7px}
.main > :deep(.management-title h1){font-size:28px!important;line-height:1.2!important;margin:0 0 6px!important;font-weight:700!important}
.main > :deep(.management-title p){font-size:13px!important;line-height:1.6!important;margin:0!important}
.main > :deep(.management-title h1){margin:0 0 7px;font-size:28px;line-height:1.2;color:#15213a;letter-spacing:0}
.main > :deep(.management-title p){margin:0;color:#75829a;font-size:13px;line-height:1.6}
.main > :deep(.management-card){background:#fff;border:1px solid #e1e7f0;border-radius:11px;box-shadow:0 3px 12px #273b5b0a}
.main > :deep(.filter-row){display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.main > :deep(.management-button){display:inline-flex;align-items:center;justify-content:center;min-height:42px;border:1px solid #2161e8;border-radius:8px;padding:0 18px;background:#2161e8;color:#fff;font-size:13px;font-weight:700;cursor:pointer}
.main > :deep(.management-button.light){border-color:#d9e3f6;background:#edf3ff;color:#2161e8}
.main > :deep(.management-table){width:100%;border-collapse:collapse;background:#fff}
.main > :deep(.management-table th),.main > :deep(.management-table td){text-align:left;padding:16px 18px;border-bottom:1px solid #edf0f5;vertical-align:middle}
.main > :deep(.management-table th){background:#f7f9fd;color:#65738c;font-size:12px;font-weight:700}
.main > :deep(.management-table td){font-size:13px;color:#26344b}
.main > :deep(.status-chip){display:inline-flex;align-items:center;padding:5px 10px;border-radius:999px;font-size:12px;font-weight:700}
.main > :deep(.status-PUBLISHED){background:#ddf8e9;color:#138350}.main > :deep(.status-OFFLINE),.main > :deep(.status-CANCELLED){background:#ececf1;color:#686c7b}.main > :deep(.status-PENDING),.main > :deep(.status-PENDING_PAYMENT){background:#fff2d7;color:#a66b00}.main > :deep(.status-PAID){background:#ddf8e9;color:#138350}
.main > :deep(.table-wrap){overflow:hidden}
@media(max-width:900px){.layout aside{width:76px;flex-basis:76px}.brand>div,.layout nav span,.new-button span,.logout span{display:none}.brand{padding-left:7px}.layout nav a{justify-content:center}.main header{padding:0 16px}.search{width:46px;font-size:0;padding:10px;justify-content:center}}
@media (max-width: 640px) {
  .layout aside {
    width: 62px;
    flex-basis: 62px;
    padding: 14px 7px;
  }
  .brand>div,.layout nav span,.new-button span,.logout span{display:none}
  .brand {
    height: 42px;
    padding: 0;
  }
  .layout nav a {
    justify-content: center;
    padding: 12px 4px;
  }
  .main header {
    padding: 0 14px;
  }
  .search {
    width: 48px;
    font-size: 0;
    padding: 10px;
  }
  .main > :deep(.management-content){padding:20px 14px}
  .main > :deep(.management-title){flex-direction:column;gap:13px}
  .main > :deep(.management-title h1){font-size:24px}
}
</style>
