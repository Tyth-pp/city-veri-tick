<template>
  <RouterLink class="activity-card card" :to="`/activities/${activity.id}`">
    <div class="activity-image" role="img" :aria-label="activity.title || '活动封面'">
      <img v-if="coverUrl && !coverBroken" :src="coverUrl" :alt="activity.title || '活动封面'" @error="coverBroken = true" />
      <div v-if="!coverUrl || coverBroken" class="cover-fallback"><span><AppIcon name="ticket" size="42" /></span><b>{{ (activity.title || '活动').slice(0, 6) }}</b></div>
      <span class="category-chip">{{ categoryText }}</span>
      <span class="status-chip" :class="{ upcoming: activity.availability === 'UPCOMING' }"><i></i>{{ statusText }}</span>
      <span v-if="discountText" class="discount-chip">{{ discountText }}</span>
      <button class="favorite-button" type="button" :class="{ active: favoriteStore.has(activity.id) }" :aria-label="favoriteStore.has(activity.id) ? '取消收藏' : '收藏活动'" @click.prevent.stop="toggleFavorite"><AppIcon name="heart" size="17" /></button>
    </div>
    <div class="activity-body">
      <h3>{{ activity.title || '未命名活动' }}</h3>
      <p class="subtitle">{{ activity.description || '城市热门活动，现场体验更精彩' }}</p>
      <p><span class="meta-icon"><AppIcon name="calendar" size="16" /></span>{{ sessionTime }}</p>
      <p><span class="meta-icon"><AppIcon name="pin" size="16" /></span>{{ activity.venueAddress || activity.venueName || '地址待定' }}</p>
      <p v-if="activity.availability === 'UPCOMING' && activity.saleStartTime" class="opening-time"><span class="meta-icon"><AppIcon name="bell" size="16" /></span>{{ formatOpening(activity.saleStartTime) }} 开售</p>
      <div class="price-row"><template v-if="activity.minPrice != null"><strong>¥{{ Number(activity.minPrice) }}</strong><em>起</em></template><em v-else>票价待公布</em><span class="buy-link">{{ activity.availability === 'UPCOMING' ? '查看开售' : activity.availability === 'SOLD_OUT' ? '查看详情' : '立即购票' }}</span></div>
    </div>
  </RouterLink>
</template>
<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import { useAuthStore } from '../stores/auth'
import { useFavoritesStore } from '../stores/favorites'
import { formatOpening } from '../utils/ticketSale'
import guitarCover from '../assets/covers/guitar-beginner.svg'
import basketballCover from '../assets/covers/campus-basketball.svg'
import potteryCover from '../assets/covers/pottery-workshop.svg'
const props = defineProps({ activity: { type: Object, required: true } })
const router = useRouter(), auth = useAuthStore(), favoriteStore = useFavoritesStore()
const coverBroken = ref(false)
const fallbackCover = computed(() => { const t = props.activity.title || ''; if (t.includes('吉他')) return guitarCover; if (t.includes('篮球')) return basketballCover; if (t.includes('陶艺')) return potteryCover; return null })
const coverUrl = computed(() => props.activity.coverFileUrl || fallbackCover.value)
watch(coverUrl, () => { coverBroken.value = false }, { flush: 'sync' })
const statusText = computed(() => ({ UPCOMING: '即将开售', AVAILABLE: '售票中', SOLD_OUT: '已售罄' }[props.activity.availability] || props.activity.statusText || '售票中'))
const categoryLabels = { MOVIE: '电影', CONCERT: '演唱会', PERFORMANCE: '演出', STAND_UP: '脱口秀', EXHIBITION_ART: '展览艺术', MUSIC_FESTIVAL: '音乐节', OPERA: '戏曲艺术', SPORTS: '体育运动', THEATRE_OPERA: '话剧歌剧' }
const categoryText = computed(() => categoryLabels[props.activity.category] || props.activity.category || props.activity.type || '演出')
const sessionTime = computed(() => {
  if (!props.activity.startTime) return '场次时间待定'
  const start = format(props.activity.startTime)
  const end = props.activity.endTime ? format(props.activity.endTime) : ''
  return end ? `${start} - ${end}` : start
})
const discountText = computed(() => Number(props.activity.availableStock ?? 100) < 20 ? `余票仅剩${props.activity.availableStock}张` : '')
function toggleFavorite() { if (auth.isGuest) { router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } }); return } favoriteStore.toggle(props.activity) }
function format(value) { if (!value) return '时间待定'; return String(value).replace('T', ' ').replace(/([+-]\d\d:\d\d|\.\d+Z?)$/, '').replace(/(\d{2}:\d{2}):\d{2}$/, '$1') }
</script>
<style scoped>
.status-chip.upcoming{background:#2563eb}.opening-time{color:#2563eb!important;font-size:12px!important}
.activity-card{overflow:hidden;display:block;transition:transform .18s ease,box-shadow .18s ease}.activity-card:hover{transform:translateY(-2px);box-shadow:0 10px 24px rgba(30,55,90,.12)}.activity-image{aspect-ratio:16/9;background:#dfe8fb;position:relative;overflow:hidden}.activity-image>img{display:block;width:100%;height:100%;object-fit:cover;object-position:center}.cover-fallback{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;color:var(--primary);background:linear-gradient(145deg,#eef4ff,#dce7ff);font-size:17px;gap:5px}.cover-fallback span{font-size:44px}.category-chip,.status-chip,.discount-chip{position:absolute;color:#fff;padding:6px 10px;border-radius:999px;font-size:11px;font-weight:700;line-height:1}.category-chip{left:12px;top:12px;background:rgba(15,23,42,.82)}.status-chip{right:12px;top:12px;background:#10b981}.status-chip i{display:inline-block;width:6px;height:6px;border-radius:50%;background:#d1fae5;margin-right:5px}.discount-chip{right:12px;bottom:12px;background:#f97316}.favorite-button{position:absolute;right:12px;bottom:12px;display:grid;place-items:center;width:32px;height:32px;border:0;border-radius:50%;background:rgba(255,255,255,.94);color:#7b8aa2;box-shadow:0 3px 9px rgba(15,23,42,.15)}.favorite-button.active{color:#ef476f}.favorite-button.active .app-icon{fill:currentColor}.activity-body{padding:18px 15px 16px}.activity-body h3{margin:0 0 5px;font-size:18px;line-height:1.35;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;color:#0f172a}.activity-body p{margin:7px 0;color:var(--muted);font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.activity-body .subtitle{margin:0 0 10px;color:#718096;font-size:13px}.meta-icon{display:inline-block;width:21px;color:#64748b;font-weight:700}.price-row{display:flex;align-items:baseline;gap:3px;margin-top:18px;color:var(--primary)}.price-row strong{font-size:23px}.price-row em{font-style:normal;font-size:12px;color:var(--muted)}.buy-link{margin-left:auto;color:#fff;background:var(--primary);border-radius:12px;padding:12px 20px;font-size:14px;font-weight:700}
.activity-body p{display:flex;align-items:center;gap:5px}.activity-body p .meta-icon{display:inline-flex;align-items:center;justify-content:center;flex:none}</style>
