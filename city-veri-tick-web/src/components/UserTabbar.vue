<template>
  <nav class="tabbar" aria-label="用户端主导航">
    <RouterLink to="/home"><span class="tab-icon"><AppIcon name="home" /></span><small>首页</small></RouterLink>
    <RouterLink to="/orders"><span class="tab-icon"><AppIcon name="ticket" /></span><small>订单</small></RouterLink>
    <RouterLink to="/profile"><span class="tab-icon"><AppIcon name="user" /></span><small>我的</small></RouterLink>
  </nav>
</template>
<script setup>import AppIcon from './AppIcon.vue'</script>
<style scoped>
.tabbar{position:fixed;bottom:0;left:0;right:0;height:64px;padding-bottom:env(safe-area-inset-bottom);background:rgba(255,255,255,.96);backdrop-filter:blur(18px);border-top:1px solid var(--line);display:flex;justify-content:center;gap:clamp(56px,20vw,104px);z-index:20}.tabbar a{min-width:54px;color:var(--muted);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;font-size:11px}.tabbar a.router-link-active{color:var(--primary);font-weight:700}.tab-icon{height:24px;display:grid;place-items:center;line-height:22px}.tabbar small{font-size:11px}
</style>
