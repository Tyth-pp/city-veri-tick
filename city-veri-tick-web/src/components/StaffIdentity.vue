<template><span class="staff-identity"><span class="staff-name-avatar" aria-hidden="true"><template v-if="initial">{{ initial }}</template><AppIcon v-else name="user" size="18" /></span><span class="staff-identity-text"><strong>{{ displayName }}</strong><small v-if="subtitle">{{ subtitle }}</small></span></span></template>
<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { surname,staffLabel } from '../utils/staffIdentity'
const props=defineProps({account:{type:Object,default:()=>({})},subtitle:String})
const initial=computed(()=>surname(props.account?.name))
const displayName=computed(()=>staffLabel(props.account))
</script>
