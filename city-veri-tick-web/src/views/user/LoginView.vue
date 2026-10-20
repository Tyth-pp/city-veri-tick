<template>
  <main class="login-page">
    <form class="card login-form" @submit.prevent="submit">
      <div class="login-brand"><img :src="logo" alt="Venuo" /><h1>欢迎使用 Venuo</h1><p>城市活动 · 一票通行</p></div>
      <div class="login-tabs"><button type="button" :class="{active: mode === 'phone'}" @click="mode='phone'">手机号登录</button><button type="button" :class="{active: mode === 'account'}" @click="mode='account'">后台账号登录</button></div>
      <template v-if="mode === 'phone'">
        <label>手机号<div class="phone-row"><span>+86</span><input v-model.trim="phoneForm.phone" inputmode="tel" autocomplete="tel" required placeholder="请输入常用手机号" /></div></label>
        <label>短信验证码<div class="code-row"><input v-model.trim="phoneForm.code" inputmode="numeric" maxlength="6" required placeholder="请输入6位验证码" /><button type="button" class="code-button" :disabled="codeLoading || countdown > 0" @click="sendCode">{{ countdown ? `${countdown}s后重发` : '获取验证码' }}</button></div></label>
        <p class="login-hint">未注册手机号验证后将自动创建账号</p>
      </template>
      <template v-else>
        <label>用户名<input v-model.trim="form.username" required placeholder="请输入后台账号" /></label>
        <label>密码<input v-model="form.password" required type="password" placeholder="请输入密码" /></label>
      </template>
      <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      <p v-if="route.query.passwordChanged === '1'" class="login-hint" role="status">密码修改成功，请使用新密码登录。</p>
      <button class="primary-button" :disabled="loading">
        {{ loading ? "登录中..." : mode === 'phone' ? '登录 / 注册' : '登录' }}
      </button>
      <RouterLink class="guest-link" to="/home" @click="auth.enterGuest()">游客体验（免登录） <span>→</span></RouterLink>
      <div class="agreement"><input id="login-agreement" v-model="agreed" type="checkbox" /><span><label for="login-agreement">我已阅读并同意</label> <RouterLink to="/legal/terms" target="_blank" rel="noopener">《用户服务协议》</RouterLink> 与 <RouterLink to="/legal/privacy" target="_blank" rel="noopener">《隐私政策》</RouterLink></span></div>
    </form>
  </main>
</template>
<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../stores/auth";
import { authApi } from "../../api/auth";
import logo from "../../assets/venu-logo.svg";
const mode = ref("phone");
const form = reactive({ username: "", password: "" });
const phoneForm = reactive({ phone: "", code: "" });
const error = ref("");
const loading = ref(false);
const codeLoading = ref(false);
const countdown = ref(0);
const agreed = ref(true);
let timer;
const router = useRouter();
const route = useRoute();
if (route.query.mode === 'account') mode.value = 'account';
const auth = useAuthStore();
function homeForRole() {
  return auth.role === "VERIFIER"
    ? "/verify"
    : auth.role === "PLATFORM_ADMIN"
      ? "/management/platform"
      : auth.role === "ORGANIZER_ADMIN"
        ? "/management/organizer"
        : "/home";
}
function redirectAllowed(path) {
  if (!path || !path.startsWith("/") || path.startsWith("//") || path.split(/[?#]/)[0] === "/login") return false;
  if (path === "/home") return auth.role === "USER";
  if (path === "/verify" || path.startsWith("/verify/")) return auth.role === "VERIFIER";
  if (path === "/management/platform" || path.startsWith("/management/platform/")) {
    return auth.role === "PLATFORM_ADMIN";
  }
  if (path === "/management/organizer" || path.startsWith("/management/organizer/")) {
    return auth.role === "ORGANIZER_ADMIN";
  }
  return auth.role === "USER";
}
async function submit() {
  if (loading.value) return;
  if (!agreed.value) {
    error.value = "请先阅读并同意用户协议与隐私政策";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    if (mode.value === "phone") await auth.loginBySms(phoneForm);
    else await auth.login(form);
    const home = homeForRole();
    const requestedPath = typeof route.query.redirect === "string" ? route.query.redirect : "";
    await router.replace(redirectAllowed(requestedPath) ? requestedPath : home);
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    error.value = "请输入有效的手机号";
    return;
  }
  codeLoading.value = true;
  error.value = "";
  try {
    const result = await authApi.sendSmsCode(phoneForm.phone);
    countdown.value = result?.cooldownSeconds || 60;
    timer = window.setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) window.clearInterval(timer);
    }, 1000);
  } catch (e) {
    error.value = e.message;
  } finally {
    codeLoading.value = false;
  }
}
onBeforeUnmount(() => timer && window.clearInterval(timer));
</script>
<style scoped>
.login-page {
  min-height: 100vh;
  padding: 24px;
  display: grid;
  place-items: center;
  background: #f5f7fb;
}
.login-brand {
  text-align: center;
  color: #071936;
  margin-bottom: 28px;
}
.login-brand img {
  margin: auto;
  display: block;
  width: 48px;
  height: 48px;
  border-radius: 10px;
}
.login-brand h1 {
  margin: 15px 0 6px;
  font-size: 24px;
  letter-spacing: 0;
}
.login-brand p {
  margin: 0;
  color: #8190a8;
  font-size: 13px;
}
.login-form {
  width: min(100%, 440px);
  padding: 40px;
  border: 1px solid #e2e9f2;
  border-radius: 12px;
  box-shadow: 0 12px 32px #20385b12;
  background: rgba(255,255,255,.96);
}
.login-form label {
  display: block;
  color: #203452;
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 18px;
}
.login-form input {
  min-width: 0;
  outline: 0;
  border: 1px solid #dce6f2;
  border-radius: 9px;
  padding: 12px 13px;
  color: #243754;
  background: #fff;
  font-size: 14px;
}
.login-form input:focus{border-color:#4e7ff0;box-shadow:0 0 0 3px #eaf1ff}
.login-form>label>input{display:block;width:100%;margin-top:7px}
.login-form button {
  width: 100%;
}
.login-tabs{display:grid;grid-template-columns:1fr 1fr;gap:3px;background:#f0f4f9;border-radius:9px;padding:4px;margin-bottom:26px}.login-tabs button{min-height:40px;border:0;border-radius:7px;background:transparent;color:#8190a8;font-weight:600;font-size:13px}.login-tabs button.active{background:#fff;color:#235eda;box-shadow:0 1px 5px #193b7515}.code-row,.phone-row{display:flex;align-items:center;gap:9px;margin-top:9px}.code-row input{flex:1}.phone-row{border:1px solid #dce6f2;border-radius:9px;background:#fff}.phone-row:focus-within{border-color:#4e7ff0;box-shadow:0 0 0 3px #eaf1ff}.phone-row span{padding-left:13px;color:#8190a8;font-size:13px;border-right:1px solid #e4ebf4}.phone-row input{flex:1;border:0!important;box-shadow:none!important;margin:0!important}.code-button{width:auto!important;flex:none;min-width:96px;min-height:44px!important;padding:0 12px!important;border:1px solid #d3e1ff!important;background:#f1f6ff!important;color:#2866dc!important;font-size:12px!important}.code-button:disabled{opacity:.55}.login-hint{margin:-4px 0 20px;color:#8b98ad;font-size:12px;line-height:1.5}.guest-link{display:block;text-align:center;margin-top:20px;color:#51647f;font-size:13px;font-weight:500}.guest-link span{color:#2563eb}.agreement{display:flex!important;align-items:center;justify-content:center;gap:4px;margin:24px 0 0!important;padding-top:20px;border-top:1px solid #edf1f6;color:#7d8da6!important;font-size:11px!important;font-weight:400!important;flex-wrap:wrap;text-align:center}.agreement input{width:14px;height:14px;margin:0;accent-color:#2563eb}.agreement a{color:#4d72b8}.login-link{color:var(--primary);font-size:12px;font-weight:700;white-space:nowrap}
.phone-row input{display:block;width:100%;margin-top:0;padding-left:10px}
.login-form .primary-button{min-height:46px;border-radius:9px;background:#2864e7;font-size:14px}
.error-text {
  color: #dc4450;
  font-size: 11px;
  margin: 4px 0 10px;
}
@media(max-width:480px){.login-page{padding:16px}.login-form{padding:28px 22px}.agreement{font-size:10px!important}}
.login-form .agreement{display:flex!important;align-items:flex-start;justify-content:center;flex-wrap:nowrap;gap:7px;text-align:left;line-height:1.8}
.login-form .agreement input{display:block;flex:0 0 14px;width:14px;height:14px;padding:0;margin:3px 0 0;border-radius:3px}
.login-form .agreement span{min-width:0}
.login-form .agreement label{display:inline;margin:0;color:inherit;font:inherit;cursor:pointer}
.login-form .agreement a{white-space:nowrap}
</style>
