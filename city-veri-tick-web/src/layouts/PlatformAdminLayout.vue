<template>
  <div class="shell">
    <aside>
      <div class="brand"><img :src="logo" alt="Venuo" /><div><strong>Venuo</strong><span>PLATFORM ADMIN</span></div></div>
      <nav>
        <RouterLink to="/management/platform"
          ><AppIcon name="layout-dashboard" /><span>平台工作台</span></RouterLink
        ><RouterLink to="/management/platform/activities"
          ><AppIcon name="clipboard-check" /><span>活动审核</span></RouterLink
        ><RouterLink to="/management/platform/organizers"
          ><AppIcon name="building" /><span>主办方管理</span></RouterLink
        ><RouterLink to="/management/platform/users"
          ><AppIcon name="users" /><span>用户管理</span></RouterLink
        ><RouterLink to="/management/platform/coupons"
          ><AppIcon name="gift" /><span>优惠券管理</span></RouterLink
        ><RouterLink to="/management/platform/orders"
          ><AppIcon name="receipt" /><span>订单监管</span></RouterLink
        ><RouterLink to="/management/platform/verification"
          ><AppIcon name="shield-check" /><span>核销监管</span></RouterLink
        ><RouterLink to="/management/platform/dashboard"
          ><AppIcon name="chart" /><span>数据看板</span></RouterLink
        >
      </nav>
      <button class="logout" @click="logout"><AppIcon name="logout" size="18" /><span>退出登录</span></button>
    </aside>
    <section class="main">
      <header>
        <ManagementSearch platform />
        <div class="admin-user"><StaffAccountMenu /><ManagementMessages /></div>
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
.shell {
  min-height: 100vh;
  display: flex;
  background: #f5f8fc;
  color: #17223a;
}
.shell aside {
  width: 252px;
  flex: 0 0 252px;
  padding: 18px 14px;
  background: #fff;
  color: #3b4963;
  border-right: 1px solid #e2e8f1;
  display: flex;
  flex-direction: column;
}
.brand {
  display:flex;
  align-items:center;
  gap:10px;
  padding: 0 10px 30px;
}
.brand img{width:34px;height:34px;object-fit:cover;border-radius:9px;background:#f2f6ff}
.brand strong {
  display: block;
  font-size: 19px;
}
.brand span {
  display: block;
  color: #8793aa;
  font-size: 11px;
  margin-top: 5px;
  letter-spacing: 0.6px;
}
.shell nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.shell nav a {
  padding: 13px 14px;
  border-radius: 8px;
  color: #64738d;
  display: flex;
  gap: 13px;
  align-items: center;
}
.shell nav a.router-link-exact-active {
  background: #edf3ff;
  color: #1559dc;
  box-shadow: inset 3px 0 #1559dc;
}
.logout {
  margin-top: auto;
  display:flex;
  align-items:center;
  justify-content:flex-start;
  gap:12px;
  width:100%;
  border: 0;
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
  border-bottom: 1px solid #e1e5ef;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
}
.search {
  display:flex;
  align-items:center;
  gap:9px;
  background: #f4f7fc;
  border:1px solid #e0e7f1;
  color: #8290a8;
  border-radius: 9px;
  padding: 10px 14px;
  width: min(540px, 55%);
  font-size:13px;
}
.admin-user{display:flex;align-items:center;gap:9px}.admin-user>div{display:flex;flex-direction:column}.admin-user strong{font-size:13px;color:#25314a}.admin-user small{font-size:10px;color:#8b96aa}.admin-avatar{width:31px;height:31px;display:grid;place-items:center;border-radius:8px;background:#4f46e5;color:#fff;font-size:11px;font-weight:700}
.shell{height:100vh;min-height:0;overflow:hidden}
.shell aside{height:100vh;min-height:0;box-sizing:border-box;position:relative}
.main{height:100vh;min-height:0;display:flex;flex-direction:column;overflow:hidden}
.main > :deep(.content){flex:1;min-height:0;overflow:auto;overscroll-behavior:contain}
@media (max-width: 900px){.shell aside{width:76px;flex-basis:76px}.brand{padding-left:7px}.brand>div,.shell nav span,.logout span{display:none}.shell nav a{justify-content:center}.main header{padding:0 16px}.search{width:46px;font-size:0;padding:10px;justify-content:center}}
@media (max-width: 640px) {
  .shell aside {
    width: 62px;
    flex-basis: 62px;
    padding: 14px 7px;
  }
  .brand>div,.shell nav span,.logout span{display:none}
  .brand {
    height: 42px;
    padding: 0;
  }
  .shell nav a {
    justify-content: center;
    padding: 12px 4px;
  }
  .main header {
    padding: 0 14px;
  }
  .search {
    width: 46px;
    font-size: 0;
    padding: 10px;
  }
}
</style>
