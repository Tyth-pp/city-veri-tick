import { http } from './http'
export const staffProfileApi={
 self:()=>http.get('/controller/account/profile'),
 saveSelf:payload=>http.put('/controller/account/profile',payload),
 saveManaged:(id,payload)=>http.put(`/controller/admin/platform/users/${id}/profile`,payload),
 saveVerifier:(id,payload)=>http.put(`/controller/admin/organizer/verifiers/${id}/profile`,payload)
}
