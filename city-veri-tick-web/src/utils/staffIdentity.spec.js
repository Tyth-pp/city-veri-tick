import { expect,it } from 'vitest'
import { surname,staffLabel } from './staffIdentity'
it('uses provided staff names only, supports compound surnames and preserves account fallback',()=>{
 expect(surname('张明')).toBe('张');expect(surname(' 欧阳明 ')).toBe('欧阳')
 expect(surname('')).toBe('');expect(surname('Development organizer')).toBe('D')
 expect(staffLabel({name:null,username:'admin_root',nickname:'演示昵称'})).toBe('admin_root')
})
