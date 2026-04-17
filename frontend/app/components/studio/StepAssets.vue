<template>
  <div class="content-panel">
    <!-- Toolbar -->
    <div class="step-toolbar">
      <div class="toolbar-left">
        <div class="step-tabs">
          <button class="step-tab" :class="{ active: activeTab === 'chars' }" @click="activeTab = 'chars'">
            角色 <span class="tab-badge">{{ chars.length }}</span>
          </button>
          <button class="step-tab" :class="{ active: activeTab === 'scenes' }" @click="activeTab = 'scenes'">
            场景 <span class="tab-badge">{{ scenes.length }}</span>
          </button>
          <button v-if="dramaProps.length" class="step-tab" :class="{ active: activeTab === 'props' }" @click="activeTab = 'props'">
            道具 <span class="tab-badge">{{ dramaProps.length }}</span>
          </button>
        </div>
      </div>
      <div class="toolbar-right">
        <button class="btn btn-sm" @click="doExtract" :disabled="rn">
          <Loader2 v-if="rn && rt === 'extractor'" :size="11" class="animate-spin" />
          <svg v-else width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          {{ chars.length ? '重新提取' : 'AI 提取' }}
        </button>
        <button v-if="visualChars.length && activeTab === 'chars'" class="btn btn-sm" @click="batchCharImages">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          批量生成角色图
        </button>
        <button v-if="scenes.length && activeTab === 'scenes'" class="btn btn-sm" @click="batchSceneImages">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          批量生成场景图
        </button>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!chars.length && !rn" class="step-empty">
      <div class="empty-visual"><svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></div>
      <div class="empty-title">从剧本提取角色、场景与道具</div>
      <div class="empty-desc">AI 自动分析剧本，提取角色信息、场景列表和关键道具</div>
      <button class="btn btn-primary" @click="doExtract">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
        开始提取
      </button>
    </div>

    <!-- Loading state -->
    <div v-else-if="rn && rt === 'extractor'" class="step-loading">
      <Loader2 :size="24" class="animate-spin" style="color:var(--accent)" />
      <div class="loading-text">正在提取角色、场景和道具...</div>
    </div>

    <!-- Tabbed layout: Characters, Scenes, Props -->
    <div v-else class="s2-layout">
      <!-- Characters Tab -->
      <div v-show="activeTab === 'chars'" class="s2-col">
        <div class="s2-col-head">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <span>角色</span>
          <span class="tag tag-accent">{{ visualChars.length }}</span>
          <span v-if="chars.length > visualChars.length" class="tag">+{{ chars.length - visualChars.length }} 旁白</span>
          <span class="tag">{{ charImgCount }}/{{ visualCharTotal }} 已生成</span>
        </div>
        <div class="s2-cards">
          <div v-for="c in visualChars" :key="c.id" class="card s2-card">
            <div class="s2-card-img" @click="(c.image_url || c.imageUrl) ? openImageViewer('/' + (c.image_url || c.imageUrl), c.name + ' 角色形象') : genCharImg(c.id)">
              <img v-if="c.image_url || c.imageUrl" :src="'/' + (c.image_url || c.imageUrl)" />
              <div v-else class="s2-card-img-empty">
                <Loader2 v-if="isPendingCharImage(c.id)" :size="16" class="animate-spin" />
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </div>
              <span class="s2-card-badge" :class="(c.image_url || c.imageUrl) ? 'is-ready' : isPendingCharImage(c.id) ? 'is-pending' : ''">{{ (c.image_url || c.imageUrl) ? '已生成' : isPendingCharImage(c.id) ? '生成中' : '点击生成' }}</span>
            </div>
            <div class="s2-card-body">
              <div class="s2-card-name">{{ c.name }}</div>
              <span class="tag">{{ c.role || '角色' }}</span>
            </div>
            <div class="s2-card-desc">{{ c.description || c.appearance || c.personality || '暂无描述' }}</div>
            <div class="s2-card-foot">
              <button class="btn btn-sm" :disabled="isPendingCharImage(c.id)" @click="genCharImg(c.id)">
                {{ isPendingCharImage(c.id) ? '生成中' : (c.image_url || c.imageUrl) ? '重新生成' : '生成形象' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Scenes Tab -->
      <div v-show="activeTab === 'scenes'" class="s2-col">
        <div class="s2-col-head">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
          <span>场景</span>
          <span class="tag tag-accent">{{ scenes.length }}</span>
          <span class="tag">{{ sceneImgCount }}/{{ scenes.length }} 已生成</span>
        </div>
        <div class="s2-cards s2-cards-wide">
          <div v-for="s in scenes" :key="s.id" class="card s2-card s2-card-wide">
            <div class="s2-card-img s2-card-img-wide" @click="(s.image_url || s.imageUrl) ? openImageViewer('/' + (s.image_url || s.imageUrl), s.location + ' 场景图') : genSceneImg(s.id)">
              <img v-if="s.image_url || s.imageUrl" :src="'/' + (s.image_url || s.imageUrl)" />
              <div v-else class="s2-card-img-empty">
                <Loader2 v-if="isPendingSceneImage(s.id)" :size="16" class="animate-spin" />
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              </div>
              <span class="s2-card-badge" :class="(s.image_url || s.imageUrl) ? 'is-ready' : isPendingSceneImage(s.id) ? 'is-pending' : ''">{{ (s.image_url || s.imageUrl) ? '已生成' : isPendingSceneImage(s.id) ? '生成中' : '点击生成' }}</span>
            </div>
            <div class="s2-card-body">
              <div class="s2-card-name">{{ s.location }}</div>
              <span v-if="s.time" class="tag">{{ s.time }}</span>
            </div>
            <div class="s2-card-desc">{{ s.description || s.time || '等待补充场景描述' }}</div>
            <div class="s2-card-foot">
              <button class="btn btn-sm" :disabled="isPendingSceneImage(s.id)" @click="genSceneImg(s.id)">
                {{ isPendingSceneImage(s.id) ? '生成中' : (s.image_url || s.imageUrl) ? '重新生成' : '生成场景图' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Props Tab -->
      <div v-show="activeTab === 'props'" class="s2-col">
        <div class="s2-col-head">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
          <span>道具</span>
          <span class="tag tag-accent">{{ dramaProps.length }}</span>
        </div>
        <div class="s2-cards">
          <div v-for="p in dramaProps" :key="p.id" class="card s2-card">
            <div class="s2-card-body">
              <div class="s2-card-name">{{ p.name }}</div>
              <span class="tag">{{ p.type || '道具' }}</span>
            </div>
            <div class="s2-card-desc">{{ p.description || '暂无描述' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { Loader2 } from 'lucide-vue-next'
import { useEpisodeStudio } from '~/composables/useEpisodeStudio'
import { useAgent } from '~/composables/useAgent'
import { characterAPI, sceneAPI } from '~/composables/useApi'

const route = useRoute()
const dramaId = Number(route.params.id)
const episodeNumber = Number(route.params.episodeNumber)

const {
  drama, chars, scenes, dramaProps, sbs, epId,
  visualChars, visualCharTotal, charImgCount, sceneImgCount,
  lockedImageConfigLabel, openImageViewer, refresh, sleep,
} = useEpisodeStudio(dramaId, episodeNumber)

const { running: rn, runningType: rt, run: runAgent } = useAgent()

const pendingCharImageIds = ref<number[]>([])
const pendingSceneImageIds = ref<number[]>([])
const activeTab = ref('chars')

function isPendingCharImage(id: number) {
  return pendingCharImageIds.value.includes(id)
}

function isPendingSceneImage(id: number) {
  return pendingSceneImageIds.value.includes(id)
}

function watchAsyncResult(check: () => boolean, attempts = 24, delay = 2500) {
  void (async () => {
    for (let i = 0; i < attempts; i++) {
      await sleep(delay)
      await refresh()
      if (check()) return
    }
  })()
}

function doExtract() {
  runAgent('extractor', '请从剧本中提取所有角色和场景信息，提取时自动与项目已有数据进行去重合并', dramaId, epId.value, refresh)
}

async function genCharImg(id: number) {
  try {
    if (!isPendingCharImage(id)) pendingCharImageIds.value.push(id)
    await characterAPI.generateImage(id, epId.value)
    toast.success('角色图片生成中')
    await refresh()
    watchAsyncResult(() => {
      const char = chars.value.find((c: any) => c.id === id)
      const done = !!(char?.image_url || char?.imageUrl)
      if (done) pendingCharImageIds.value = pendingCharImageIds.value.filter(item => item !== id)
      return done
    })
  } catch (e: any) {
    pendingCharImageIds.value = pendingCharImageIds.value.filter(item => item !== id)
    toast.error(e.message)
  }
}

function batchCharImages() {
  const ids = visualChars.value.filter((c: any) => !(c.image_url || c.imageUrl)).map((c: any) => c.id)
  if (!ids.length) { toast.info('所有角色图片已生成'); return }
  pendingCharImageIds.value = [...new Set([...pendingCharImageIds.value, ...ids])]
  characterAPI.batchImages(ids, epId.value).then(async () => {
    toast.success('角色图片批量生成中')
    await refresh()
    watchAsyncResult(() => ids.every(id => {
      const char = chars.value.find((c: any) => c.id === id)
      const done = !!(char?.image_url || char?.imageUrl)
      if (done) pendingCharImageIds.value = pendingCharImageIds.value.filter(item => item !== id)
      return done
    }), 36)
  }).catch((e: any) => {
    pendingCharImageIds.value = pendingCharImageIds.value.filter(item => !ids.includes(item))
    toast.error(e.message)
  })
}

async function genSceneImg(id: number) {
  try {
    if (!isPendingSceneImage(id)) pendingSceneImageIds.value.push(id)
    await sceneAPI.generateImage(id, epId.value)
    toast.success('场景图片生成中')
    await refresh()
    watchAsyncResult(() => {
      const scene = scenes.value.find((s: any) => s.id === id)
      const done = !!(scene?.image_url || scene?.imageUrl)
      if (done) pendingSceneImageIds.value = pendingSceneImageIds.value.filter(item => item !== id)
      return done
    })
  } catch (e: any) {
    pendingSceneImageIds.value = pendingSceneImageIds.value.filter(item => item !== id)
    toast.error(e.message)
  }
}

function batchSceneImages() {
  const ids = scenes.value.filter((s: any) => !(s.image_url || s.imageUrl)).map((s: any) => s.id)
  if (!ids.length) { toast.info('所有场景图片已生成'); return }
  pendingSceneImageIds.value = [...new Set([...pendingSceneImageIds.value, ...ids])]
  ids.forEach(id => { sceneAPI.generateImage(id, epId.value).then(() => refresh()).catch((e: any) => toast.error(e.message)) })
  toast.success('场景图片批量生成中')
  watchAsyncResult(() => ids.every(id => {
    const scene = scenes.value.find((s: any) => s.id === id)
    const done = !!(scene?.image_url || scene?.imageUrl)
    if (done) pendingSceneImageIds.value = pendingSceneImageIds.value.filter(item => item !== id)
    return done
  }), 36)
}
</script>

<style scoped>
/* ===== Shared toolbar & states ===== */
.content-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; position: relative; min-height: 0; }

.step-toolbar {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px; border-bottom: 1px solid rgba(27, 41, 64, 0.08);
  background: linear-gradient(180deg, rgba(255,255,255,0.8), rgba(255,255,255,0.42)); flex-shrink: 0;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; flex: 1; }
.toolbar-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.step-tabs {
  display: flex;
  background: rgba(27, 41, 64, 0.05);
  padding: 3px;
  border-radius: 8px;
  gap: 4px;
}
.step-tab {
  border: none;
  background: transparent;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}
.step-tab:hover {
  background: rgba(27, 41, 64, 0.03);
}
.step-tab.active {
  background: #fff;
  color: var(--text-0);
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.tab-badge {
  background: rgba(27, 41, 64, 0.1);
  color: var(--text-1);
  padding: 0 6px;
  border-radius: 10px;
  font-size: 11px;
  line-height: 18px;
  font-family: var(--font-mono);
}
.step-tab.active .tab-badge {
  background: var(--accent);
  color: #fff;
}

.step-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  flex: 1; min-height: 300px; gap: 10px; padding: 46px;
  animation: fadeIn 0.3s var(--ease-out);
}
.empty-visual {
  width: 72px; height: 72px; border-radius: 22px;
  background: rgba(255,255,255,0.8); color: var(--accent);
  border: 1px solid rgba(27, 41, 64, 0.08);
  box-shadow: var(--shadow-sm);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 8px;
}
.empty-title { font-size: 22px; font-weight: 700; font-family: var(--font-display); color: var(--text-0); }
.empty-desc { font-size: 13px; color: var(--text-2); max-width: 420px; text-align: center; line-height: 1.8; }

.step-loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  flex: 1; gap: 12px;
}
.loading-text { font-size: 13px; color: var(--text-2); }

/* ===== S2 Layout ===== */
.s2-layout {
  padding: 16px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.s2-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.s2-col-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
  padding: 4px 0;
}

.s2-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.s2-cards.s2-cards-wide {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.s2-card {
  border-radius: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--border);
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.s2-card:hover {
  border-color: rgba(19, 51, 121, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.s2-card-wide {
  /* scene cards use wider layout */
}

.s2-card-img {
  position: relative;
  aspect-ratio: 3 / 4;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.02);
  cursor: pointer;
  margin-bottom: 8px;
}

.s2-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.s2-card-img-wide {
  aspect-ratio: 16 / 9;
}

.s2-card-img-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  background: linear-gradient(135deg, rgba(255,255,255,0.6), rgba(240,242,248,0.4));
  border: 1px dashed var(--border);
  border-radius: 8px;
}

.s2-card-badge {
  position: absolute;
  bottom: 6px;
  left: 6px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  backdrop-filter: blur(4px);
}

.s2-card-badge.is-ready {
  background: rgba(45, 122, 69, 0.75);
}

.s2-card-badge.is-pending {
  background: rgba(180, 130, 20, 0.75);
}

.s2-card-body {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.s2-card-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-0);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.s2-card-desc {
  font-size: 11px;
  color: var(--text-3);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}

.s2-card-foot {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .s2-cards,
  .s2-cards.s2-cards-wide {
    grid-template-columns: 1fr;
  }
}
</style>
