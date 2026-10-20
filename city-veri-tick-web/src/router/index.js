import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import LoginView from "../views/user/LoginView.vue";
import UserLayout from "../layouts/UserLayout.vue";
import HomeView from "../views/user/HomeView.vue";
import ActivityDetailView from "../views/user/ActivityDetailView.vue";
import OrderConfirmView from "../views/user/OrderConfirmView.vue";
import WechatPaymentView from "../views/user/WechatPaymentView.vue";
import OrderListView from "../views/user/OrderListView.vue";
import OrderDetailView from "../views/user/OrderDetailView.vue";
import ProfileView from "../views/user/ProfileViewRefined.vue";
import HelpCenterView from "../views/user/HelpCenterView.vue";
import AboutView from "../views/user/AboutView.vue";
import TicketView from "../views/user/TicketView.vue";
import FavoritesView from "../views/user/FavoritesView.vue";
import ReminderSettingsView from "../views/user/ReminderSettingsView.vue";
import OrganizerLayout from "../layouts/OrganizerLayout.vue";
import PlatformAdminLayout from "../layouts/PlatformAdminLayout.vue";
import ManagementWorkbenchView from "../views/management/ManagementWorkbenchRefined.vue";
import ActivityManagementView from "../views/management/ActivityManagementView.vue";
import ActivityEditorView from "../views/management/ActivityEditorView.vue";
import TicketInventoryView from "../views/management/TicketInventoryView.vue";
import OrganizerOrderView from "../views/management/OrganizerOrderView.vue";
import OrganizerOrderDetailView from "../views/management/OrganizerOrderDetailView.vue";
import VerificationManagementView from "../views/management/VerificationManagementView.vue";
import VerifyWorkbenchView from "../views/verify/VerifyWorkbenchRefined.vue";
import PlatformWorkbenchView from "../views/platform/PlatformWorkbenchRefined.vue";
import PlatformDashboardView from "../views/platform/PlatformDashboardView.vue";
import PlatformActivityManagementView from "../views/platform/PlatformActivityManagementView.vue";
import PlatformActivityDetailView from "../views/platform/PlatformActivityDetailView.vue";
import PlatformOrganizerManagementView from "../views/platform/PlatformOrganizerManagementView.vue";
import PlatformOrganizerDetailView from "../views/platform/PlatformOrganizerDetailView.vue";
import PlatformOrderView from "../views/platform/PlatformOrderView.vue";
import PlatformVerificationView from "../views/platform/PlatformVerificationView.vue";
import PlatformOrderDetailView from "../views/platform/PlatformOrderDetailView.vue";
import PlatformUserView from "../views/platform/PlatformUserView.vue";
import PlatformCouponView from "../views/platform/PlatformCouponView.vue";
import MyCouponsView from "../views/user/MyCouponsView.vue";
import CouponCenterView from "../views/user/CouponCenterView.vue";
import PlatformVerifierView from "../views/platform/PlatformVerifierView.vue";
import OrganizerStaffView from "../views/management/OrganizerStaffView.vue";
import ChangePasswordView from "../views/user/ChangePasswordView.vue";
import { authApi } from "../api/auth";

// Historical bookmarks must enter the current role-specific layout, never render a second UI.
function legacyManagementRedirect(to) {
  const role = useAuthStore().role;
  if (role === 'USER') return '/home';
  if (role === 'VERIFIER') return '/verify';
  let suffix = to.path.replace(/^\/management\/?/, '');
  if (role === 'PLATFORM_ADMIN') {
    if (suffix === 'activities/new' || suffix === 'ticket-inventory') suffix = 'activities';
    suffix = suffix.replace(/^(activities\/[^/]+)\/edit$/, '$1');
    if (!/^(activities(?:\/[^/]+)?|orders(?:\/[^/]+)?|verification|organizers(?:\/[^/]+)?|users|verifiers|dashboard)$/.test(suffix)) suffix = '';
    return { path: '/management/platform' + (suffix ? '/' + suffix : ''), query: to.query, hash: to.hash };
  }
  if (!/^(activities(?:\/new|\/[^/]+\/edit)?|ticket-inventory|orders(?:\/[^/]+)?|verification|verifiers)$/.test(suffix)) suffix = '';
  return { path: '/management/organizer' + (suffix ? '/' + suffix : ''), query: to.query, hash: to.hash };
}
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/home" },
    { path: "/login", component: LoginView },
    { path: "/change-password", component: ChangePasswordView, meta: { requiresAuth:true } },
    { path: "/legal/terms", component: () => import("../views/user/LegalView.vue"), props: { kind: "terms" } },
    { path: "/legal/privacy", component: () => import("../views/user/LegalView.vue"), props: { kind: "privacy" } },
    { path: "/management", redirect: legacyManagementRedirect },
    { path: "/management/:legacyPath(.*)*", redirect: legacyManagementRedirect },
    {
      path: "/management/organizer",
      component: OrganizerLayout,
      meta: { requiresAuth: true, roles: ["ORGANIZER_ADMIN"] },
      children: [
        { path: "verifiers", component: OrganizerStaffView },
        { path: "", component: ManagementWorkbenchView },
        { path: "activities", component: ActivityManagementView },
        { path: "activities/new", component: ActivityEditorView },
        { path: "activities/:id/edit", component: ActivityEditorView },
        { path: "ticket-inventory", component: TicketInventoryView },
        { path: "orders", component: OrganizerOrderView },
        { path: "orders/:orderId", component: OrganizerOrderDetailView },
        { path: "verification", component: VerificationManagementView },
      ],
    },
    {
      path: "/management/platform",
      component: PlatformAdminLayout,
      meta: { requiresAuth: true, roles: ["PLATFORM_ADMIN"] },
      children: [
        { path: "", component: PlatformWorkbenchView },
        { path: "dashboard", component: PlatformDashboardView },
        { path: "activities", component: PlatformActivityManagementView },
        { path: "activities/:id", component: PlatformActivityDetailView },
        { path: "organizers", component: PlatformOrganizerManagementView },
        { path: "organizers/:id", component: PlatformOrganizerDetailView },
        { path: "orders", component: PlatformOrderView },
        { path: "orders/:orderId", component: PlatformOrderDetailView },
        { path: "users", component: PlatformUserView },
        { path: "coupons", component: PlatformCouponView },
        { path: "verifiers", component: PlatformVerifierView },
        { path: "verification", component: PlatformVerificationView },
      ],
    },
    {
      path: "/verify",
      component: VerifyWorkbenchView,
      meta: { requiresAuth: true, verifier: true },
    },
    {
      path: "/",
      component: UserLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "home", component: HomeView, meta: { requiresAuth: false, guestAllowed: true } },
        { path: "activities/:activityId", component: ActivityDetailView, meta: { requiresAuth: false, guestAllowed: true } },
        { path: "orders/confirm", component: OrderConfirmView },
        { path: "orders/pay/:orderId", component: WechatPaymentView, meta: { hideTabbar: true } },
        { path: "orders", component: OrderListView },
        { path: "orders/:orderId", component: OrderDetailView },
        { path: "tickets/:ticketId", component: TicketView, meta: { hideTabbar: true } },
        { path: "profile", component: ProfileView, meta: { requiresAuth: false, guestAllowed: true } },
        { path: "favorites", component: FavoritesView },
        { path: "reminders", component: ReminderSettingsView },
        { path: "coupons", component: CouponCenterView },
        { path: "coupons/mine", component: MyCouponsView },
        { path: "help", component: HelpCenterView, meta: { requiresAuth: false, guestAllowed: true } },
        { path: "about", component: AboutView, meta: { requiresAuth: false, guestAllowed: true } },
      ],
    },
  ],
});
router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if(auth.isLoggedIn && auth.role !== 'USER' && to.path !== '/login') {
    try { const status=await authApi.session();auth.setPasswordRequirement(status.mustChangePassword); }
    catch { if(!auth.isLoggedIn)return false; }
  }
  if(auth.isLoggedIn && auth.mustChangePassword && to.path !== '/change-password') return '/change-password';
  if(to.path === '/change-password' && auth.isLoggedIn && !auth.mustChangePassword) return '/home';
  if (to.meta.guestAllowed && !auth.isLoggedIn) auth.enterGuest();
  const roleHome =
    auth.role === "PLATFORM_ADMIN"
      ? "/management/platform"
      : auth.role === "ORGANIZER_ADMIN"
        ? "/management/organizer"
        : auth.role === "VERIFIER"
          ? "/verify"
        : "/home";
  if (to.path === "/home" && auth.isLoggedIn && auth.role !== "USER")
    return roleHome;
  if (to.meta.roles && (!auth.isLoggedIn || !to.meta.roles.includes(auth.role)))
    return auth.isLoggedIn
      ? roleHome
      : { path: "/login", query: { redirect: to.fullPath } };
  if (to.meta.verifier && (!auth.isLoggedIn || auth.role !== "VERIFIER"))
    return auth.isLoggedIn
      ? "/home"
      : { path: "/login", query: { redirect: to.fullPath } };
  if (to.meta.requiresAuth && !auth.isLoggedIn)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (to.path === "/login" && auth.isLoggedIn) return roleHome;
});

router.afterEach((to) => {
  if (typeof document === "undefined") return;
  const path = to.path;
  let section = "活动购票";
  if (path === "/login") {
    section = "登录";
  } else if (path === "/legal/terms") {
    section = "用户服务协议";
  } else if (path === "/legal/privacy") {
    section = "隐私政策";
  } else if (path === "/verify" || path.startsWith("/verify/")) {
    section = "现场核销";
  } else if (path.startsWith("/management/platform")) {
    section = path.includes("dashboard")
      ? "平台数据"
      : path.includes("activities")
        ? "活动审核"
        : path.includes("orders")
          ? "订单监管"
          : path.includes("users")
            ? "用户管理"
            : path.includes("organizers")
              ? "主办方管理"
              : "平台管理";
  } else if (path === "/management" || path.startsWith("/management/organizer")) {
    section = path.includes("ticket-inventory")
      ? "票种与库存"
      : path.includes("orders")
        ? "订单管理"
        : path.includes("activities")
          ? "活动管理"
          : path.includes("verification")
            ? "核销数据"
            : "主办方工作台";
  }
  document.title = `Venuo · ${section}`;
});

router.onError((error) => {
  console.error("[router]", error);
});
