<template>
  <main class="page favorites-page">
    <header class="topbar favorites-topbar"><button class="icon-button" type="button" aria-label="返回" @click="router.back"><AppIcon name="arrow-left" /></button><h1>我的收藏</h1><span class="favorite-count">{{ favorites.items.length }} 场</span></header>
    <div class="container favorites-container">
      <section v-if="favorites.items.length" class="favorite-intro"><b>收藏的活动</b><span>关注活动动态，方便随时购票</span></section>
      <section v-if="favorites.items.length" class="favorite-list">
        <article v-for="activity in displayItems" :key="activity.id" class="card favorite-card">
          <component :is="['AVAILABLE', 'UPCOMING', 'SOLD_OUT'].includes(activity.availability) ? RouterLink : 'div'" :to="['AVAILABLE', 'UPCOMING', 'SOLD_OUT'].includes(activity.availability) ? `/activities/${activity.id}` : undefined" class="favorite-main">
            <div class="favorite-cover"><ActivityCover :src="activity.coverFileUrl || ''" :title="activity.title" /><em>{{ categoryText(activity) }}</em></div>
            <div class="favorite-copy"><h2>{{ activity.title || '未命名活动' }}</h2><p><AppIcon name="calendar" size="14" /> {{ formatRange(activity) }}</p><p><AppIcon name="pin" size="14" /> {{ activity.venueAddress || activity.venueName || '地址待定' }}</p><strong v-if="activity.minPrice != null">¥{{ activity.minPrice }}<small> 起</small></strong><span v-else class="price-muted">票价待公布</span></div>
          </component>
          <div class="favorite-actions"><button v-if="activity.availability === 'UPCOMING'" type="button" class="remove-button opening-subscribe" @click="toggleSubscription(activity)"><AppIcon name="bell" size="15" /> {{ reminders.subscriptions[String(activity.id)] && !reminders.subscriptions[String(activity.id)].delivered ? '已订阅 · 取消' : '提醒我开售' }}</button><button v-if="activity.availability === 'ERROR'" type="button" class="remove-button" @click="refresh">重试</button><span :class="['status-label', statusClass(activity)]">{{ statusText(activity) }}</span><button type="button" class="remove-button" @click="remove(activity.id)"><AppIcon name="heart" size="17" /> 取消收藏</button></div>
        </article>
      </section>
      <section v-else class="empty-favorites card"><span><AppIcon name="heart" size="30" /></span><b>还没有收藏活动</b><p>在活动详情页点击爱心，收藏感兴趣的演出、赛事和展览。</p><RouterLink to="/home" class="primary-button">去发现活动</RouterLink></section>
    </div>
  </main>
</template>
<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import ActivityCover from '../../components/ActivityCover.vue'
import { activityApi } from '../../api/activity' 
import { useRouter } from 'vue-router'
import AppIcon from '../../components/AppIcon.vue'
import { useFavoritesStore } from '../../stores/favorites'
import { useRemindersStore } from '../../stores/reminders'
const router = useRouter(), favorites = useFavoritesStore(), reminders = useRemindersStore()
const previews = ref({})
let sequence = 0, timer
const displayItems = computed(() => favorites.items.map(snapshot => {
  const preview = previews.value[String(snapshot.id)]
  const activity = { ...snapshot, coverFileUrl: '', ...preview?.activity, availability: preview?.availability || 'LOADING' }
  return activity
}))
async function refresh() {
  const request = ++sequence
  const ids = favorites.items.map(item => String(item.id))
  const account = favorites.accountId
  const entries = await Promise.all(ids.map(async id => {
    try { return [id, await activityApi.preview(id)] }
    catch (error) { return [id, { availability: error.status === 404 || error.code === 404 ? 'UNAVAILABLE' : 'ERROR' }] }
  }))
  if (request === sequence && account === favorites.accountId) previews.value = Object.fromEntries(entries)
}
watch(() => `${favorites.accountId}:${favorites.items.map(item => item.id).join(',')}`, () => { previews.value = {}; refresh() }, { immediate: true })
function onVisibility() { if (document.visibilityState === 'visible') { refresh() } }
onMounted(() => { timer = setInterval(() => { refresh() }, 5 * 60 * 1000); document.addEventListener('visibilitychange', onVisibility) })
onUnmounted(() => { sequence++; clearInterval(timer); document.removeEventListener('visibilitychange', onVisibility) })
const categoryLabels = { MOVIE: '电影', CONCERT: '演唱会', PERFORMANCE: '演出', STAND_UP: '脱口秀', EXHIBITION_ART: '展览艺术', MUSIC_FESTIVAL: '音乐节', OPERA: '戏曲艺术', SPORTS: '体育运动', THEATRE_OPERA: '话剧歌剧' }
function categoryText(activity) { return categoryLabels[activity.category] || activity.category || '活动' }
function formatRange(activity) { if (!activity.startTime) return '场次时间待公布'; const format = value => { const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}:\d{2})/); return match ? `${match[1]}/${match[2]}/${match[3]} ${match[4]}` : '时间待公布' }; return activity.endTime ? `${format(activity.startTime)} - ${format(activity.endTime)}` : format(activity.startTime) }
function statusText(activity) {
  return { AVAILABLE: '正在售票', ENDED: '活动已结束', UPCOMING: '即将开售', SOLD_OUT: '已售罄', UNAVAILABLE: '活动已下架或不可访问', UNAVAILABLE_SALE: '暂不可购买', ERROR: '状态暂未更新', LOADING: '正在更新活动信息' }[activity.availability] || '暂不可购买'
}
function statusClass(activity) { return activity.availability === 'AVAILABLE' ? 'available' : 'muted' }
function toggleSubscription(activity) { const subscription = reminders.subscriptions[String(activity.id)]; if (subscription && !subscription.delivered) reminders.unsubscribe(activity.id); else reminders.subscribe(activity) }
function remove(id) { reminders.unsubscribe(id); favorites.remove(id) }
</script>
<style scoped>
.favorites-page{background:#f8fafc;min-height:100vh}.favorites-topbar{height:58px;position:sticky;top:0;z-index:4;background:rgba(255,255,255,.96);backdrop-filter:blur(10px)}.favorites-topbar h1{margin:0;font-size:18px}.favorite-count{width:36px;color:var(--muted);font-size:12px;text-align:right}.favorites-container{padding-top:16px;padding-bottom:92px}.favorite-intro{display:flex;align-items:baseline;gap:9px;margin:0 0 12px}.favorite-intro b{font-size:18px}.favorite-intro span{color:var(--muted);font-size:12px}.favorite-list{display:flex;flex-direction:column;gap:12px}.favorite-card{overflow:hidden}.favorite-main{display:flex;gap:12px;padding:13px;color:inherit}.favorite-cover{position:relative;width:108px;height:82px;flex:none;overflow:hidden;border-radius:10px;background:#eaf0ff;display:grid;place-items:center;color:var(--primary)}.favorite-cover img{width:100%;height:100%;object-fit:cover}.favorite-cover em{position:absolute;left:6px;top:6px;padding:4px 7px;border-radius:9px;background:rgba(15,23,42,.75);color:#fff;font-size:10px;font-style:normal}.favorite-copy{min-width:0;flex:1}.favorite-copy h2{margin:1px 0 7px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:16px}.favorite-copy p{display:flex;align-items:center;gap:4px;margin:4px 0;color:var(--muted);font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.favorite-copy strong{display:block;margin-top:7px;color:var(--primary);font-size:17px}.favorite-copy strong small{font-size:10px;font-weight:400}.price-muted{display:block;margin-top:7px;color:var(--muted);font-size:11px}.favorite-actions{display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--line);padding:9px 13px}.status-label{font-size:11px}.status-label.available{color:#07865e}.status-label.muted{color:var(--muted)}.remove-button{display:inline-flex;align-items:center;gap:4px;border:0;background:transparent;color:#71809a;font-size:11px}.remove-button:hover{color:#ef4444}.empty-favorites{display:flex;flex-direction:column;align-items:center;text-align:center;padding:48px 24px}.empty-favorites>span{display:grid;place-items:center;width:62px;height:62px;border-radius:20px;background:#edf2ff;color:var(--primary);margin-bottom:16px}.empty-favorites b{font-size:17px}.empty-favorites p{max-width:270px;margin:8px 0 20px;color:var(--muted);font-size:12px;line-height:1.6}.empty-favorites .primary-button{display:inline-flex;align-items:center;justify-content:center;min-width:130px;text-decoration:none}@media(max-width:350px){.favorite-cover{width:94px}.favorite-main{gap:9px;padding:11px}.favorite-actions{padding:8px 11px}}
</style>
