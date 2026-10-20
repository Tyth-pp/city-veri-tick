<template><div :inert="sessionFeedback || actionDialog ? true : undefined"><RouterView /></div><ActionDialogHost /><SessionFeedbackDialog /></template>
<script setup>
import SessionFeedbackDialog from './components/SessionFeedbackDialog.vue'
import { sessionFeedback } from './utils/sessionFeedback'
import ActionDialogHost from './components/ActionDialogHost.vue'
import { actionDialog,finishAction } from './utils/actionDialog'
import {watch} from 'vue'
watch(sessionFeedback,value=>{if(value)finishAction()})
</script>
