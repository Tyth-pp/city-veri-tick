<template>
  <div class="activity-cover">
    <img v-if="src && !failed" :src="src" :alt="title ? `${title}封面` : '活动封面'" @error="failed = true" />
    <div v-else class="cover-placeholder" role="img" :aria-label="`${title || '活动'}暂无封面`"><AppIcon name="ticket" size="28" /><small>暂无封面</small></div>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
const props = defineProps({ src: { type: String, default: '' }, title: { type: String, default: '' } })
const failed = ref(false)
watch(() => props.src, () => { failed.value = false }, { flush: 'sync' })
</script>
<style scoped>
.activity-cover{width:100%;height:100%;overflow:hidden;background:linear-gradient(145deg,#edf3ff,#dce7f8);color:#6685ba}
.activity-cover img{display:block;width:100%;height:100%;object-fit:cover;object-position:center}
.cover-placeholder{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px}
.cover-placeholder small{font-size:10px}
</style>
