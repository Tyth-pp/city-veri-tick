<template>
  <main class="page guest-profile">
    <header class="topbar"><h1>我的</h1><RouterLink to="/login?redirect=/profile">登录 / 注册</RouterLink></header>
    <div class="container">
      <section class="guest-identity">
        <img :src="avatar" alt="游客头像" />
        <div><h2>{{ auth.guestName }}</h2><span class="guest-badge">游客</span><p>暂未绑定手机号</p></div>
      </section>
      <section class="guest-access">
        <h2>发现精彩活动</h2>
        <p>登录后即可购票、查看订单和电子票，以及设置个人资料。</p>
        <RouterLink class="primary-button" to="/login?redirect=/profile">登录 / 注册</RouterLink>
        <RouterLink class="browse-link" to="/home">继续浏览活动</RouterLink>
      </section>
      <nav class="guest-links">
        <RouterLink to="/orders">我的订单 <span>登录后查看</span></RouterLink>
        <RouterLink to="/legal/terms">用户服务协议 <AppIcon name="chevron-right" size="18" /></RouterLink>
        <RouterLink to="/legal/privacy">隐私政策 <AppIcon name="chevron-right" size="18" /></RouterLink>
      </nav>
      <button class="leave-guest" @click="leave">退出游客体验</button>
    </div>
  </main>
</template>
<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import AppIcon from '../../components/AppIcon.vue'
import avatar from '../../assets/guest-avatar.jpg'
const auth = useAuthStore(), router = useRouter()
auth.enterGuest()
async function leave() { if (await auth.logout()) router.replace('/login') }
</script>
<style scoped>
.guest-profile{background:#f8fafc}.topbar h1{font-size:21px;margin:0}.topbar a{font-size:14px;color:var(--primary)}
.container{padding-top:20px;padding-bottom:90px}.guest-identity{display:flex;align-items:center;gap:18px;padding:24px 0;border-bottom:1px solid var(--line)}
.guest-identity img{width:76px;height:76px;border-radius:50%;object-fit:cover;flex:none}.guest-identity div{min-width:0}.guest-identity h2{font-size:20px;margin:0 0 6px;overflow-wrap:anywhere}.guest-identity p{font-size:13px;color:var(--muted);margin:6px 0 0}.guest-badge{display:inline-block;padding:2px 8px;background:#e8efff;color:var(--primary);border-radius:4px;font-size:12px}
.guest-access{padding:28px 0}.guest-access h2{font-size:18px;margin:0 0 10px}.guest-access p{color:var(--muted);font-size:14px;line-height:1.8;margin:0 0 20px}.guest-access .primary-button{display:flex;align-items:center;justify-content:center;min-height:44px;border-radius:7px}.browse-link{display:block;text-align:center;color:var(--primary);font-size:14px;margin-top:16px}
.guest-links{border-top:1px solid var(--line)}.guest-links a{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:58px;border-bottom:1px solid var(--line);font-size:15px;color:var(--text)}.guest-links span{font-size:12px;color:var(--muted)}.leave-guest{display:block;margin:28px auto 0;border:0;background:transparent;color:#64748b;font-size:14px;cursor:pointer}
</style>
