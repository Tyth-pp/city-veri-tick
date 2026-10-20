const compoundSurnames=['欧阳','司马','上官','诸葛','东方','皇甫','尉迟','公孙','慕容','司徒','司空','夏侯','令狐','独孤','南宫','端木','宇文','长孙']
export function surname(name) {
 const value=String(name||'').trim()
 if(!value)return ''
 return compoundSurnames.find(prefix=>value.startsWith(prefix))||Array.from(value)[0].toUpperCase()
}
export function staffLabel(account) {return account?.name?.trim()||account?.username||'账号资料'}
export function roleLabel(role){return String(role||'').split(',').filter(Boolean).map(r=>({PLATFORM_ADMIN:'平台管理员',ORGANIZER_ADMIN:'主办方管理员',VERIFIER:'核销员',USER:'普通用户'}[r]||r)).join('、')||'工作人员'}
