<template>
  <main class="page help-page">
    <header class="topbar help-topbar">
      <RouterLink class="icon-button" to="/profile" aria-label="返回我的"><AppIcon name="arrow-left" size="21" /></RouterLink>
      <h1>帮助中心</h1>
      <span class="header-spacer" aria-hidden="true"></span>
    </header>

    <div class="container help-content">
      <div class="help-intro">
        <span class="help-intro-icon"><AppIcon name="headset" size="21" /></span>
        <div><h2>有什么可以帮你？</h2><p>查找购票、订单与入场问题</p></div>
      </div>

      <label class="help-search">
        <AppIcon name="search" size="19" />
        <input v-model.trim="query" type="search" placeholder="搜索问题关键词" aria-label="搜索帮助问题" />
        <button v-if="query" type="button" aria-label="清除搜索" @click="query = ''"><AppIcon name="close" size="16" /></button>
      </label>

      <div v-if="!filteredGroups.length" class="help-empty">
        <AppIcon name="search" size="25" />
        <p>没有找到相关问题</p>
        <button type="button" @click="query = ''">查看全部问题</button>
      </div>

      <section v-for="group in filteredGroups" :key="group.title" class="faq-group" :aria-label="group.title">
        <div class="group-heading"><span class="group-icon"><AppIcon :name="group.icon" size="18" /></span><h2>{{ group.title }}</h2></div>
        <div class="faq-list">
          <details v-for="item in group.items" :key="item.question" class="faq-item">
            <summary><span>{{ item.question }}</span><AppIcon name="chevron-right" size="17" /></summary>
            <p>{{ item.answer }}</p>
          </details>
        </div>
      </section>

      <section class="contact-section" aria-label="联系客服">
        <div><h2>还有疑问？</h2><p>常见问题未能解答时，可查看客服联系方式。</p></div>
        <button class="contact-button" type="button" @click="contactOpen = true"><AppIcon name="headset" size="18" /> 联系客服</button>
      </section>
    </div>

    <div v-if="contactOpen" class="contact-overlay" @click.self="contactOpen = false">
      <section class="contact-sheet" role="dialog" aria-modal="true" aria-labelledby="contact-title">
        <div class="sheet-handle"></div>
        <span class="sheet-icon"><AppIcon name="headset" size="23" /></span>
        <h2 id="contact-title">联系客服</h2>
        <p>客服联系方式即将开放，请先查看常见问题。</p>
        <button type="button" @click="contactOpen = false">知道了</button>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppIcon from '../../components/AppIcon.vue'

const query = ref('')
const contactOpen = ref(false)
const groups = [
  {
    title: '购票与实名', icon: 'user-check', items: [
      { question: '购票前需要填写实名信息吗？', answer: '需要。请在「我的」页面填写真实姓名和身份证号，完成后即可提交订单。' },
      { question: '实名信息在哪里修改？', answer: '进入「我的」页面，点击「实名信息」进行修改。已创建订单的实名信息以该订单记录为准。' },
      { question: '如何选择场次和票种？', answer: '在活动详情页选择场次和票种，确认价格及数量后再进入订单确认页。' }
    ]
  },
  {
    title: '支付与订单', icon: 'receipt', items: [
      { question: '在哪里查看我的订单？', answer: '进入「我的订单」，可按待付款、待使用、已完成等状态查看当前账号的订单。' },
      { question: '支付成功后订单在哪里？', answer: '支付成功后，可在「我的订单」的待使用分类查看订单及电子票。' },
      { question: '待付款订单如何继续支付？', answer: '进入「我的订单」的待付款分类，打开订单详情后点击支付。请以订单页面显示的状态为准。' }
    ]
  },
  {
    title: '电子票与入场', icon: 'ticket', items: [
      { question: '电子票在哪里查看？', answer: '进入已支付订单的详情页查看电子票；「我的」页面票夹也会展示最近购买的一张待使用票。' },
      { question: '入场时需要准备什么？', answer: '请打开对应电子票，并准备与订单实名信息一致的身份证件，按现场工作人员指引核验。' },
      { question: '一笔订单有多张票怎么办？', answer: '订单详情会列出该订单的每张电子票，请分别打开对应票券进行核验。' }
    ]
  },
  {
    title: '退票规则', icon: 'refresh', items: [
      { question: '购买后可以退票吗？', answer: '目前支持整单退票：已支付订单的全部电子票未核销，且距场次开始至少48小时，可在订单详情提交原因申请。申请期间暂停核销，由主办方审核；通过后按实付金额全额模拟退款，不发生真实资金退回。' },
      { question: '退款进度在哪里查看？', answer: '进入「我的订单 → 退款/售后」，打开对应订单查看申请与审核记录。拒绝会显示原因并恢复电子票使用；已退款票券不能入场。页面打开时会检查新的审核结果并显示提醒。' },
      { question: '在哪里查看订单状态？', answer: '在「我的订单」中打开对应订单，可查看当前订单状态及票券信息。' },
      { question: '活动场次有变化怎么办？', answer: '请以活动详情页和主办方公布的信息为准；如涉及退改，按该活动的具体规则处理。' }
    ]
  },
  {
    title: '账号使用', icon: 'user', items: [
      { question: '可以修改昵称和头像吗？', answer: '可以。进入「我的」页面，点击头像旁的编辑按钮修改个人资料。' },
      { question: '订单为什么只显示当前账号的？', answer: '「我的订单」仅展示当前登录账号创建的订单。请确认登录的是购票时使用的账号。' },
      { question: '如何清空搜索历史？', answer: '进入「我的」页面，点击「搜索历史管理」中的「一键清空」，确认后删除当前设备保存的搜索记录。' }
    ]
  }
]

const filteredGroups = computed(() => {
  const keyword = query.value.toLocaleLowerCase()
  if (!keyword) return groups
  return groups.map(group => ({
    ...group,
    // Match the question itself so category names and answer copy cannot make unrelated rows appear.
    items: group.items.filter(item => item.question.toLocaleLowerCase().includes(keyword))
  })).filter(group => group.items.length)
})
</script>

<style scoped>
.help-page{background:#f8fafc}
.help-topbar{justify-content:space-between}
.help-topbar .icon-button{font-size:inherit}
.header-spacer{width:44px;height:44px}
.help-content{padding-top:20px;padding-bottom:32px}
.help-intro{display:flex;align-items:center;gap:12px;margin-bottom:18px}
.help-intro-icon{display:grid;place-items:center;width:42px;height:42px;flex:none;border-radius:10px;background:#e7efff;color:var(--primary)}
.help-intro h2{margin:0 0 3px;font-size:19px;color:var(--text)}
.help-intro p{margin:0;color:var(--muted);font-size:13px}
.help-search{display:flex;align-items:center;gap:10px;height:48px;padding:0 13px;border:1px solid #dce5f3;border-radius:10px;background:#fff;color:#7182a0}
.help-search:focus-within{border-color:var(--primary);box-shadow:0 0 0 3px #eaf1ff}
.help-search input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:var(--text);font-size:14px}
.help-search input::placeholder{color:#94a3b8}
.help-search button{display:grid;place-items:center;width:28px;height:28px;border:0;background:transparent;color:#64748b}
.faq-group{margin-top:27px}
.group-heading{display:flex;align-items:center;gap:8px;margin-bottom:10px}
.group-icon{display:grid;place-items:center;width:28px;height:28px;border-radius:7px;background:#e9f0ff;color:var(--primary)}
.group-heading h2{margin:0;font-size:15px;color:var(--text)}
.faq-list{overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}
.faq-item+.faq-item{border-top:1px solid #edf1f6}
.faq-item summary{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:54px;padding:13px 15px;cursor:pointer;list-style:none;color:var(--text);font-size:14px;font-weight:600;line-height:1.5}
.faq-item summary::-webkit-details-marker{display:none}
.faq-item summary span{min-width:0}
.faq-item summary .app-icon{color:#8493ad;transition:transform .18s}
.faq-item[open] summary .app-icon{transform:rotate(90deg);color:var(--primary)}
.faq-item p{margin:0;padding:0 15px 16px;color:#64748b;font-size:13px;line-height:1.7;overflow-wrap:anywhere}
.help-empty{display:flex;align-items:center;flex-direction:column;gap:9px;padding:56px 12px;color:#8493ad}
.help-empty p{margin:0;font-size:14px}.help-empty button{border:0;background:transparent;color:var(--primary);font-size:13px;font-weight:600}
.contact-section{display:flex;align-items:center;justify-content:space-between;gap:15px;margin-top:28px;padding:17px 0;border-top:1px solid var(--line)}
.contact-section h2{margin:0 0 5px;font-size:15px}.contact-section p{margin:0;color:var(--muted);font-size:12px;line-height:1.5}
.contact-button{display:flex;align-items:center;justify-content:center;gap:6px;min-height:40px;flex:none;padding:0 12px;border:1px solid #c7d8f7;border-radius:8px;background:#edf3ff;color:var(--primary);font-size:12px;font-weight:700}
.contact-overlay{position:fixed;inset:0;z-index:30;display:flex;align-items:flex-end;justify-content:center;background:rgba(15,23,42,.42)}
.contact-sheet{width:min(100%,520px);padding:10px 20px calc(20px + env(safe-area-inset-bottom));border-radius:16px 16px 0 0;background:#fff;text-align:center;box-shadow:0 -12px 35px rgba(15,23,42,.16)}
.sheet-handle{width:38px;height:4px;margin:0 auto 18px;border-radius:99px;background:#dbe3f2}
.sheet-icon{display:grid;place-items:center;width:46px;height:46px;margin:0 auto 10px;border-radius:10px;background:#edf3ff;color:var(--primary)}
.contact-sheet h2{margin:0;font-size:18px}.contact-sheet p{margin:8px 0 20px;color:var(--muted);font-size:13px;line-height:1.6}
.contact-sheet button{width:100%;height:46px;border:0;border-radius:10px;background:var(--primary);color:#fff;font-size:15px;font-weight:700}
@media(max-width:420px){.contact-section{align-items:flex-start;flex-direction:column}.contact-button{width:100%}}
</style>
