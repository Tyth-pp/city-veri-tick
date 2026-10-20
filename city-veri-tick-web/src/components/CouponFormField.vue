<template>
  <div class="coupon-field" :class="{invalid:!!error}">
    <label :for="id">{{ label }}<span v-if="required" aria-hidden="true"> *</span></label>
    <slot />
    <small v-if="error" class="field-error" role="alert">{{ error }}</small>
    <small v-else-if="hint" class="field-hint">{{ hint }}</small>
  </div>
</template>
<script setup>defineProps({id:String,label:String,error:String,hint:String,required:Boolean})</script>
<style scoped>
.coupon-field{display:flex;flex-direction:column;gap:8px;min-width:0}.coupon-field>label{font-size:14px;font-weight:500;color:#334563}.coupon-field>label span{color:#e25160}.coupon-field :deep(input),.coupon-field :deep(select),.coupon-field :deep(textarea){width:100%;min-height:42px;box-sizing:border-box;border:1px solid #dce4f2;border-radius:8px;background:#fff;padding:10px 12px;color:#172d4d;font:inherit;font-size:14px;outline:none}.coupon-field :deep(input:focus),.coupon-field :deep(select:focus),.coupon-field :deep(textarea:focus){border-color:#2463eb;box-shadow:0 0 0 3px #2463eb0d}.coupon-field.invalid :deep(input),.coupon-field.invalid :deep(select),.coupon-field.invalid :deep(textarea){border-color:#df4b5d}.coupon-field :deep(input:disabled){background:#f2f5fa;color:#96a0b0;cursor:not-allowed}.field-error{color:#d33c50;font-size:12px;line-height:1.4}.field-hint{color:#8391a8;font-size:12px;line-height:1.4}
</style>
