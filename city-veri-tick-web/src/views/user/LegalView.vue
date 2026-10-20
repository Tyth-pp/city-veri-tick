<template>
  <main class="legal-page">
    <header class="legal-header">
      <RouterLink class="legal-brand" to="/login"><img :src="logo" alt="" />Venuo</RouterLink>
      <RouterLink class="return-link" to="/login">返回登录</RouterLink>
    </header>
    <article class="legal-document">
      <header class="document-heading">
        <p>城市验票平台</p>
        <h1>{{ kind === 'terms' ? '用户服务协议' : '隐私政策' }}</h1>
        <p class="update-date">更新日期：2026 年 10 月 07 日</p>
      </header>
      <template v-if="kind === 'terms'">
        <section><h2>一、协议说明</h2><p>欢迎使用本城市验票平台（以下简称“平台”）。您勾选“我已阅读并同意”，即代表您已阅读、理解并同意本《用户服务协议》全部条款，自愿接受协议约束。如不同意，请停止使用本平台。</p></section>
        <section><h2>二、服务内容</h2><p>平台提供电子票核验、票务状态查询、入场核销等相关服务。平台提供订单与整单退票申请、主办方审核和处理记录查询。当前支付与退款均为模拟流程，不发生真实资金收付。退票资格以页面公布规则及服务端校验为准；活动变更等问题可联系主办方或平台客服。</p></section>
        <section><h2>三、用户责任</h2><ol><li>用户需保证提交信息真实、合法、有效，不得伪造、变造票券，不得使用平台从事违法活动。</li><li>用户妥善保管账号、验证码、电子票二维码 / 核销码，因个人保管不善造成的损失由用户自行承担。</li><li>禁止利用平台批量刷票、恶意重复核销、攻击系统，一经发现平台有权限制或终止账号使用，保留追究法律责任的权利。</li></ol></section>
        <section><h2>四、平台权利与义务</h2><ol><li>平台按照规则提供验票核销服务，保障系统正常运行；系统维护、升级时可临时暂停服务，并尽可能提前通知。</li><li>平台有权根据业务需要修订本协议，修订后在平台公示，继续使用即视为接受更新后的协议。</li><li>平台不对网络中断、设备故障、第三方原因导致验票失败承担赔偿责任。</li></ol></section>
        <section><h2>五、免责条款</h2><p>因不可抗力、通信故障、用户操作失误、主办方票务规则变更等原因，造成无法验票、验票异常的，平台不承担赔偿责任。</p></section>
        <section><h2>六、争议解决</h2><p>因本协议产生争议，双方优先协商；协商不成，向平台所在地人民法院提起诉讼。</p></section>
      </template>
      <template v-else>
        <section><h2>一、概述</h2><p>本隐私政策说明平台如何收集、使用、存储、保护您在使用城市验票平台过程中的个人信息，遵循《个人信息保护法》《网络安全法》等法律法规。勾选同意即代表您知晓并同意本政策。</p></section>
        <section><h2>二、收集信息</h2><p>为完成票务核验，我们可能收集：手机号、票券信息、核销记录、设备信息（设备型号、IP）；仅收集验票必需信息，不额外收集无关隐私信息。</p></section>
        <section><h2>三、信息使用</h2><ol><li>用于身份核验、票券有效性校验、入场核销、异常订单排查；</li><li>用于系统安全风控、故障排查；</li><li>不会未经您许可向第三方出售、出租您的个人信息；仅法律要求或活动主办方核验必要场景下才可提供。</li></ol></section>
        <section><h2>四、信息存储与保护</h2><p>数据仅保留业务必要周期，到期清理。平台采用加密、访问权限控制等技术保护数据安全，降低信息泄露风险，但无法做到绝对安全。</p></section>
        <section><h2>五、您的权利</h2><p>您有权查询、更正个人信息；可申请注销账号、删除相关记录；如有隐私相关疑问，可联系平台客服。</p></section>
        <section><h2>六、政策更新</h2><p>平台可适时更新本隐私政策，更新后页面公示，继续使用平台即视为同意新版本。</p></section>
      </template>
      <footer class="document-footer"><RouterLink :to="kind === 'terms' ? '/legal/privacy' : '/legal/terms'">{{ kind === 'terms' ? '查看隐私政策' : '查看用户服务协议' }}</RouterLink><RouterLink to="/login">返回登录</RouterLink></footer>
    </article>
  </main>
</template>

<script setup>
import logo from '../../assets/venu-logo.svg'
defineProps({ kind: { type: String, required: true } })
</script>

<style scoped>
.legal-page{min-height:100vh;background:#f5f7fb;color:#203452;padding:0 24px 48px;line-height:1.85}
.legal-header{max-width:860px;margin:auto;min-height:76px;display:flex;align-items:center;justify-content:space-between;gap:16px}
.legal-brand{display:flex;align-items:center;gap:10px;color:#071936;font-size:21px;font-weight:700;text-decoration:none}
.legal-brand img{width:32px;height:32px}
.return-link,.document-footer a{color:#2864e7;font-size:14px;text-decoration:none}
.legal-document{max-width:860px;margin:auto;background:#fff;border:1px solid #e2e9f2;border-radius:8px;padding:40px 48px}
.document-heading{padding-bottom:24px;border-bottom:1px solid #edf1f6;margin-bottom:28px}
.document-heading p{margin:0;color:#8190a8;font-size:14px}
.document-heading h1{margin:8px 0;font-size:26px;line-height:1.4;font-weight:700;letter-spacing:0;color:#071936}
.document-heading .update-date{font-size:13px}
section{margin:0 0 26px}
section h2{font-size:18px;margin:0 0 10px;font-weight:600;color:#071936;letter-spacing:0}
section p,section ol{margin:0;font-size:15px;overflow-wrap:anywhere}
section ol{padding-left:24px}
section li+li{margin-top:8px}
.document-footer{border-top:1px solid #edf1f6;padding-top:22px;display:flex;justify-content:space-between;gap:16px}
@media(max-width:600px){.legal-page{padding:0 16px 24px}.legal-header{min-height:64px}.legal-document{padding:26px 20px}.document-heading h1{font-size:24px}section p,section ol{font-size:14px}}
</style>
