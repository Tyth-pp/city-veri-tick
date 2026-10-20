<template>
  <main class="page about-page">
    <header class="about-topbar">
      <button class="back-button" type="button" aria-label="返回" @click="goBack"><AppIcon name="arrow-left" size="21" /></button>
      <div class="about-title"><h1>关于 Venuo</h1><span>v1.0.0</span></div>
      <span class="topbar-spacer" aria-hidden="true"></span>
    </header>

    <div class="about-container">
      <section class="about-hero card">
        <div class="about-mark"><AppIcon name="ticket" size="28" /></div>
        <div><p class="eyebrow">城市活动票务服务平台</p><h2>让每一次入场都真实、清晰、可追溯</h2><p class="hero-copy">Venuo 是面向城市文旅活动主办方、场馆运营方打造的实名制电子票务与现场核验 SaaS 平台。</p></div>
      </section>

      <section class="about-section">
        <div class="section-heading"><span class="section-icon"><AppIcon name="info" size="17" /></span><h2>平台简介</h2></div>
        <div class="about-copy card"><p>平台提供活动、场次、票种、购票、退票申请及现场核销管理。</p><p>电子票关联下单时的实名信息，核销时校验核销码与身份证号，并检查订单、票券状态及工作人员的活动授权。成功核销后，同一张票不能再次入场。</p><p>现场核销需要连接服务。网络异常时请重试并确认服务端结果，按现场工作人员指引入场。</p></div>
      </section>

      <section class="about-section">
        <div class="section-heading"><span class="section-icon"><AppIcon name="info" size="17" /></span><h2>核心功能介绍</h2></div>
        <div class="feature-list">
          <article v-for="feature in features" :key="feature.title" class="feature-item card"><span class="feature-icon"><AppIcon :name="feature.icon" size="20" /></span><div><h3>{{ feature.title }}</h3><p>{{ feature.description }}</p></div></article>
        </div>
      </section>

      <section class="about-section">
        <div class="section-heading"><span class="section-icon"><AppIcon name="pin" size="17" /></span><h2>适用业务场景</h2></div>
        <div class="scene-tags card"><span v-for="scene in scenes" :key="scene">{{ scene }}</span></div>
      </section>

      <section class="about-section version-section">
        <div class="section-heading"><span class="section-icon"><AppIcon name="calendar" size="17" /></span><h2>版本信息</h2></div>
        <div class="version-card card"><div><span>当前版本</span><strong>Venuo v1.0.0</strong></div><div><span>版本月份</span><strong>2026 年 10 月</strong></div><p>当前为业务验收版本，包含活动审核、购票、电子票、退票申请、优惠券及核销管理。支付与退款为模拟流程，不发生真实资金收付。</p></div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppIcon from '../../components/AppIcon.vue'

const router = useRouter()
const features = [
  { icon: 'ticket', title: '实名制票券管理', description: '主办方配置活动、场次和票种，用户填写实名信息后购票，订单保留下单时的实名快照。' },
  { icon: 'qr', title: '现场票券核验', description: '核销员扫描或输入核销码，填写观演人身份证号，经服务端验证后完成核销。' },
  { icon: 'chart', title: '数据统计与报表', description: '按实际订单、支付、退款及核销记录展示经营统计，支持按活动和场次查询记录。' },
  { icon: 'users', title: '分级权限管理', description: '平台管理员、主办方管理员、用户和核销员按角色及资源归属访问业务，账号操作与核销保留对应记录。' },
  { icon: 'shield-check', title: '安全与加密机制', description: '身份证号以哈希用于核验，页面展示脱敏号码。电子票使用唯一核销码，并由服务端检查状态、防止重复核销。' },
]
const scenes = ['城市演唱会', '话剧舞台剧', '体育赛事', '文博特展', '嘉年华', '行业峰会', '大型论坛', '园区限定活动']
function goBack() { if (window.history.length > 1) router.back(); else router.replace('/profile') }
</script>

<style scoped>
.about-page{min-height:100vh;background:#f8fafc;padding-bottom:84px}.about-topbar{position:sticky;top:0;z-index:4;height:58px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;background:rgba(255,255,255,.96);border-bottom:1px solid var(--line);backdrop-filter:blur(10px)}.back-button{display:grid;place-items:center;width:36px;height:36px;border:0;border-radius:10px;background:transparent;color:var(--text);cursor:pointer}.about-title{display:flex;align-items:baseline;gap:8px}.about-title h1{margin:0;font-size:18px;font-weight:700}.about-title span{color:var(--muted);font-size:11px}.topbar-spacer{width:36px}.about-container{width:min(100% - 28px,620px);margin:0 auto;padding-top:14px}.card{background:#fff;border:1px solid #e1e7f0;border-radius:14px;box-shadow:0 3px 12px rgba(39,59,91,.05)}.about-hero{display:flex;align-items:flex-start;gap:14px;padding:20px 18px;background:linear-gradient(135deg,#f1f5ff,#fff)}.about-mark{display:grid;place-items:center;width:52px;height:52px;flex:none;border-radius:16px;background:#2161e8;color:#fff;box-shadow:0 8px 18px rgba(33,97,232,.2)}.eyebrow{margin:1px 0 7px;color:#61739b;font-size:12px}.about-hero h2{margin:0;color:var(--text);font-size:20px;line-height:1.4}.hero-copy{margin:8px 0 0;color:var(--muted);font-size:13px;line-height:1.65}.about-section{margin-top:24px}.section-heading{display:flex;align-items:center;gap:8px;margin-bottom:10px}.section-heading h2{margin:0;color:var(--text);font-size:17px}.section-icon{display:grid;place-items:center;width:28px;height:28px;border-radius:9px;background:#eaf0ff;color:var(--primary)}.about-copy{padding:16px 17px}.about-copy p{margin:0;color:#52627d;font-size:13px;line-height:1.8}.about-copy p+p{margin-top:9px}.feature-list{display:grid;gap:10px}.feature-item{display:flex;align-items:flex-start;gap:12px;padding:15px 16px}.feature-icon{display:grid;place-items:center;width:36px;height:36px;flex:none;border-radius:11px;background:#edf2ff;color:var(--primary)}.feature-item h3{margin:0 0 5px;color:var(--text);font-size:15px}.feature-item p{margin:0;color:var(--muted);font-size:12px;line-height:1.65}.scene-tags{display:flex;flex-wrap:wrap;gap:9px;padding:16px}.scene-tags span{padding:8px 11px;border-radius:9px;background:#f0f4ff;color:#4265b1;font-size:12px}.version-card{padding:17px}.version-card>div{display:flex;align-items:center;justify-content:space-between;padding:2px 0}.version-card>div+div{margin-top:12px}.version-card span{color:var(--muted);font-size:13px}.version-card strong{color:var(--text);font-size:14px}.version-card p{margin:16px 0 0;padding-top:14px;border-top:1px solid var(--line);color:var(--muted);font-size:12px;line-height:1.7}@media(max-width:360px){.about-container{width:calc(100% - 20px)}.about-hero{padding:17px 14px}.about-hero h2{font-size:18px}.feature-item{padding:14px 12px}}
</style>
