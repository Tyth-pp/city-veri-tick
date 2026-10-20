import {mount,flushPromises} from '@vue/test-utils'
import {expect,it,vi} from 'vitest'
import Host from './ActionDialogHost.vue'
import {askAction,actionDialog} from '../utils/actionDialog'
vi.mock('vue-router',()=>({useRoute:()=>({fullPath:'/test'})}))
it('requires real reason, trims submitted input, and cancellation does not approve destructive action',async()=>{
 const w=mount(Host,{global:{stubs:{Teleport:true}}})
 const response=askAction({title:'驳回活动',input:true,required:true});await flushPromises()
 await w.get('form').trigger('submit');expect(w.get('[role="alert"]').text()).toContain('请填写操作原因');expect(actionDialog.value).not.toBeNull()
 await w.get('textarea').setValue('  请补充场地  ');await w.get('form').trigger('submit');expect(await response).toEqual({reason:'请补充场地'})
 const cancelled=askAction('确认删除？');await flushPromises();await w.get('button[aria-label="关闭确认弹窗"]').trigger('click');expect(await cancelled).toBeNull();w.unmount()
})
