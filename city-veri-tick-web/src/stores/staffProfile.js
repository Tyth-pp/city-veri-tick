import { ref,watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { staffProfileApi } from '../api/staffProfile'
export const useStaffProfileStore=defineStore('staff-profile',()=>{
 const auth=useAuthStore(),profile=ref(null),error=ref(''),loading=ref(false)
 let request=0
 watch(()=>auth.accessToken,()=>{request++;profile.value=null;error.value='';loading.value=false})
 async function load(){
  if(!auth.accessToken||!['PLATFORM_ADMIN','ORGANIZER_ADMIN','VERIFIER'].includes(auth.role))return
  const version=++request,token=auth.accessToken;loading.value=true;error.value=''
  try{const data=await staffProfileApi.self();if(version===request&&token===auth.accessToken)profile.value=data}
  catch(e){if(version===request)error.value=e.message||'账号资料加载失败'}
  finally{if(version===request)loading.value=false}
 }
 async function save(payload){
  const token=auth.accessToken,data=await staffProfileApi.saveSelf(payload)
  if(token!==auth.accessToken)throw new Error('登录账号已变化，请重新打开资料')
  request++;profile.value=data;error.value='';return data
 }
 return {profile,error,loading,load,save}
})
