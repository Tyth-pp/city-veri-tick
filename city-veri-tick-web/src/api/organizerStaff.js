import { http } from './http'
const base='/controller/admin/organizer/verifiers'
export const organizerStaffApi={
 list:()=>http.get(base),
 activities:()=>http.get(base+'/activities'),
 create:payload=>http.post(base,payload),
 enabled:(id,enabled,reason)=>http.patch(base+'/'+id+'/enabled',{reason},{params:{enabled}}),
 assign:(id,activityIds)=>http.put(base+'/'+id+'/activities',{activityIds})
}

