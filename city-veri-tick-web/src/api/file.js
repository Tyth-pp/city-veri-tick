import { http } from './http'

export function uploadImage(file, onProgress) {
  if (!file) return Promise.reject(new Error('请选择图片文件'))
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
    return Promise.reject(new Error('仅支持 JPG、PNG、WEBP、GIF 图片'))
  }
  if (file.size > 10 * 1024 * 1024) {
    return Promise.reject(new Error('图片不能超过 10MB，请压缩后再上传'))
  }
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/controller/files/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (event) => onProgress?.(event.total ? Math.round(event.loaded * 100 / event.total) : 0)
  })
}
