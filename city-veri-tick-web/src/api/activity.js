import { http } from './http'
const categoryCodes = { '电影': 'MOVIE', '演唱会': 'CONCERT', '演出': 'PERFORMANCE', '脱口秀': 'STAND_UP', '展览艺术': 'EXHIBITION_ART', '音乐节': 'MUSIC_FESTIVAL', '戏曲艺术': 'OPERA', '体育运动': 'SPORTS', '话剧歌剧': 'THEATRE_OPERA' }
export const activityApi = {
  list: (keyword, category) => http.get('/controller/user/activities', { params: { ...(keyword ? { keyword } : {}), ...(category && category !== '全部' ? { category: categoryCodes[category] || category } : {}) } }),
  detail: (id) => http.get(`/controller/user/activities/${id}`),
  preview: (id) => http.get(`/controller/user/activities/${id}/preview`)
}
