<template><div class="user-layout"><RouterView :key="route.path" /><UserReminderHost /><UserTabbar v-if="!route.meta.hideTabbar" /></div></template>
<script setup>import UserReminderHost from '../components/UserReminderHost.vue'; import { useRoute } from 'vue-router'; import UserTabbar from '../components/UserTabbar.vue'; const route=useRoute()</script>
