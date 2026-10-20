<template>
  <svg class="app-icon" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path v-if="name === 'search'" d="m21 21-4.35-4.35M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
    <path v-else-if="name === 'bell'" d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    <template v-else-if="name === 'user'"><circle cx="12" cy="8" r="3.2" /><path d="M5.5 20a6.5 6.5 0 0 1 13 0M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0Z" /></template>
    <path v-else-if="name === 'arrow-left'" d="m14.5 5-7 7 7 7M8 12h12" />
    <path v-else-if="name === 'share'" d="M7.8 11 16.2 6M7.8 13l8.4 5M20 5.5a2 2 0 1 1-4 0 2 2 0 0 1 4 0ZM8 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0Zm12 6.5a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z" />
    <path v-else-if="name === 'more'" d="M12 5.5h.01M12 12h.01M12 18.5h.01" stroke-width="3" />
    <path v-else-if="name === 'heart'" d="M20.8 8.8c0 5-8.8 10-8.8 10s-8.8-5-8.8-10a4.8 4.8 0 0 1 8.8-2.7 4.8 4.8 0 0 1 8.8 2.7Z" />
    <template v-else-if="name === 'calendar'"><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M8 3v4M16 3v4M4 10h16" /></template>
    <template v-else-if="name === 'pin'"><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2.2" /></template>
    <template v-else-if="name === 'ticket'"><path d="M4 6.5A2.5 2.5 0 0 0 4 11v2a2.5 2.5 0 0 0 0 4.5V20h16v-2.5a2.5 2.5 0 0 0 0-4.5v-2a2.5 2.5 0 0 0 0-4.5V4H4v2.5Z" /><path d="M12 7v1M12 11v1M12 15v1" /></template>
    <path v-else-if="name === 'home'" d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1V10Z" />
    <path v-else-if="name === 'orders'" d="M5 3h14v18H5zM8 7h8M8 11h8M8 15h5" />
    <path v-else-if="name === 'edit'" d="m4 16.5-.7 3.7 3.7-.7L18.8 7.7a2.6 2.6 0 0 0-3.7-3.7L4 16.5Z" />
    <path v-else-if="name === 'shield'" d="M12 3 19 6v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3Z" />
    <path v-else-if="name === 'info'" d="M12 16v-4M12 8h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
    <path v-else-if="name === 'download'" d="M12 3v12m0 0 4-4m-4 4-4-4M4 20h16" />
    <path v-else-if="name === 'qr'" d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 14h2v2h-2zM14 18h6v2h-6z" />
    <path v-else-if="name === 'logout'" d="M10 17l5-5-5-5M15 12H3M21 4v16" />
    <path v-else-if="name === 'star'" d="m12 3 2.7 5.4 6 .9-4.3 4.2 1 6-5.4-2.8-5.4 2.8 1-6-4.3-4.2 6-.9L12 3Z" />
    <template v-else-if="name === 'medal'"><circle cx="12" cy="12" r="7" /><path d="m9.5 12 1.7 1.7 3.5-3.5M8 5 6 2m10 3 2-3" /></template>
    <template v-else-if="name === 'clock'"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></template>
    <template v-else-if="name === 'check-circle'"><circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" /></template>
    <path v-else-if="name === 'refresh'" d="M20 11a8 8 0 0 0-14.5-4L3 10m0-4v4h4M4 13a8 8 0 0 0 14.5 4L21 14m0 4v-4h-4" />
    <path v-else-if="name === 'gift'" d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M12 7H8.5a2.5 2.5 0 1 1 2.5-2.5V7Zm0 0h3.5A2.5 2.5 0 1 0 13 4.5V7Z" />
    <template v-else-if="name === 'user-check'"><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0M16 11l2 2 4-4" /></template>
    <path v-else-if="name === 'truck'" d="M3 6h11v10H3zM14 10h4l3 3v3h-7M7 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm11 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z" />
    <path v-else-if="name === 'history'" d="M4 5v5h5M4.5 10A8 8 0 1 1 6 17M12 8v4l2.5 1.5" />
    <path v-else-if="name === 'headset'" d="M4 13v-1a8 8 0 0 1 16 0v1M4 13h3v5H5a1 1 0 0 1-1-1v-4Zm16 0h-3v5h2a1 1 0 0 0 1-1v-4ZM9 21h4" />
    <path v-else-if="name === 'chevron-right'" d="m9 5 7 7-7 7" />
    <path v-else-if="name === 'alert'" d="M12 3 2.8 20h18.4L12 3Zm0 6v5m0 3h.01" />
    <path v-else-if="name === 'hourglass'" d="M6 3h12M6 21h12M7 3c0 4 5 5 5 9s-5 5-5 9m10-18c0 4-5 5-5 9s5 5 5 9" />
    <path v-else-if="name === 'layout-dashboard'" d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z" />
    <template v-else-if="name === 'clipboard-check'"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V3h6v1M8 12l2 2 5-5" /></template>
    <path v-else-if="name === 'building'" d="M4 20V6l8-3 8 3v14M8 20v-4h8v4M8 8h1M12 8h1M16 8h1M8 11h1M12 11h1M16 11h1" />
    <template v-else-if="name === 'users'"><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0M16 11a3 3 0 0 1 5 2M18 20a5 5 0 0 0-2-4" /></template>
    <path v-else-if="name === 'receipt'" d="M6 3h12v18l-3-2-3 2-3-2-3 2V3Zm3 5h6M9 12h6M9 15h4" />
    <path v-else-if="name === 'shield-check'" d="M12 3 19 6v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3Z M8.5 12l2.2 2.2 4.8-4.8" />
    <path v-else-if="name === 'chart'" d="M4 19V5M4 19h17M8 15l3-4 3 2 5-7" />
    <path v-else-if="name === 'plus'" d="M12 5v14M5 12h14" />
    <path v-else-if="name === 'minus'" d="M5 12h14" />
    <path v-else-if="name === 'close'" d="m6 6 12 12M6 18 18 6" />
    <path v-else-if="name === 'check'" d="m5 12 4 4L19 6" />
    <path v-else-if="name === 'arrow-right'" d="M4 12h15m-6-6 6 6-6 6" />
    <template v-else-if="name === 'wechat'"><path d="M20.5 11.5c0-3.6-3.7-6.5-8.2-6.5S4 7.9 4 11.5c0 2 1.2 3.8 3.2 5l-.7 2.4 2.8-1.4c.9.3 1.9.5 3 .5 4.5 0 8.2-2.9 8.2-6.5Z" fill="currentColor" stroke="none" /><circle cx="9.5" cy="11" r="1.1" fill="#fff" stroke="none" /><circle cx="14.5" cy="11" r="1.1" fill="#fff" stroke="none" /></template>
  </svg>
</template>
<script setup>
defineProps({ name: { type: String, required: true }, size: { type: [String, Number], default: 22 } })
</script>
<style scoped>.app-icon{display:inline-block;flex:none;vertical-align:middle}</style>
