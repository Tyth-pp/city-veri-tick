import {shallowRef} from 'vue'
export const actionDialog=shallowRef(null)
let resolvePending
export function finishAction(value=null){const resolve=resolvePending;resolvePending=null;actionDialog.value=null;resolve?.(value)}
export function askAction(options){finishAction();return new Promise(resolve=>{resolvePending=resolve;actionDialog.value=typeof options==='string'?{title:'确认操作',message:options}:options})}
