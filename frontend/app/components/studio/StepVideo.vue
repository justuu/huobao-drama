<template>
  <div class="content-panel">
    <div class="step-toolbar prod-toolbar">
      <div class="toolbar-left">
        <div class="step-indicator">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
          <span class="step-name">分集视频</span>
        </div>
      </div>
      <div class="prod-tabs">
        <button v-for="t in step3Tabs" :key="t.id"
          :class="['prod-tab', { active: subTab === t.id }]"
          @click="subTab = t.id">
          {{ t.label }}
          <span v-if="t.badge" class="prod-tab-badge">{{ t.badge }}</span>
        </button>
      </div>
    </div>

    <!-- Videos sub-tab -->
    <div v-if="subTab === 'videos'" class="prod-content">
      <div class="prod-section-bar">
        <span class="dim" style="font-size:12px">{{ sbs.length }} 个镜头</span>
        <span class="tag mono">{{ shotVidCount }}/{{ sbs.length }} 已生成</span>
        <div class="ml-auto flex gap-1">
          <button class="btn btn-sm" @click="batchVideos">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
            批量视频
          </button>
        </div>
      </div>
      <div v-if="!sbs.length" class="step-empty" style="min-height:260px">
        <div class="empty-visual"><svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg></div>
        <div class="empty-title">尚未拆解分镜</div>
        <div class="empty-desc">请先在"剧本导入"步骤中完成分镜拆解</div>
        <button class="btn btn-primary" @click="step = 1; subTab = 'storyboard'">前往分镜拆解</button>
      </div>
      <div v-else class="prod-grid">
        <div v-for="(sb, i) in sbs" :key="sb.id" class="card prod-card">
          <div class="prod-cover">
            <video v-if="hasVid(sb)" :src="'/' + getVideoUrl(sb)" class="prod-video" controls preload="metadata" playsinline />
            <img v-else-if="hasImg(sb)" :src="'/' + getStoryboardCover(sb)" class="previewable-image" @click.stop="openImageViewer('/' + getStoryboardCover(sb), `镜头 #${String(i + 1).padStart(2, '0')} 参考图`)" />
            <div v-else class="prod-cover-empty"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg></div>
            <span class="prod-idx">#{{ String(i+1).padStart(2,'0') }}</span>
          </div>
          <div class="prod-info">
                <div class="prod-desc truncate">{{ sb.description || sb.title || '—' }}</div>
                <div class="prod-meta-line">{{ sb.shot_type || sb.shotType || '未设景别' }} · {{ sb.duration || 10 }}s</div>
                <div class="prod-dots">
                  <span :class="['dot', hasImg(sb) && 'ok']" /><span style="font-size:10px">图</span>
                  <span :class="['dot', hasVid(sb) && 'ok', isPendingVideo(sb.id) && 'pending']" /><span style="font-size:10px">{{ isPendingVideo(sb.id) ? '视频生成中' : '视频' }}</span>
                </div>
                <div v-if="videoFailMessage(sb.id)" class="prod-error">{{ videoFailMessage(sb.id) }}</div>
          </div>
          <div class="prod-actions">
            <button class="btn btn-sm" :disabled="isPendingVideo(sb.id)" @click="genVid(sb)">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
              {{ isPendingVideo(sb.id) ? '生成中' : '生成视频' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Compose sub-tab -->
    <div v-else-if="subTab === 'compose'" class="prod-content">
      <div class="prod-section-bar">
        <span class="dim" style="font-size:12px">{{ sbs.length }} 个镜头</span>
        <span class="tag mono">{{ composedCount }}/{{ sbs.length }} 已合成</span>
        <div class="ml-auto flex gap-1">
          <button class="btn btn-sm" @click="batchCompose">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
            批量合成
          </button>
        </div>
      </div>
      <div class="prod-grid">
        <div v-for="(sb, i) in sbs" :key="sb.id" class="card prod-card">
          <div class="prod-cover">
            <video v-if="hasComposed(sb)" :src="'/' + getComposedVideoUrl(sb)" class="prod-video" controls preload="metadata" playsinline />
            <video v-else-if="hasVid(sb)" :src="'/' + getVideoUrl(sb)" class="prod-video" controls preload="metadata" playsinline />
            <img v-else-if="hasImg(sb)" :src="'/' + getStoryboardCover(sb)" class="previewable-image" @click.stop="openImageViewer('/' + getStoryboardCover(sb), `镜头 #${String(i + 1).padStart(2, '0')} 参考图`)" />
            <div v-else class="prod-cover-empty"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg></div>
            <span class="prod-idx">#{{ String(i+1).padStart(2,'0') }}</span>
            <span v-if="hasComposed(sb)" class="prod-overlay-badge">已合成</span>
          </div>
          <div class="prod-info">
                <div class="prod-desc truncate">{{ sb.description || sb.title || '—' }}</div>
                <div class="prod-meta-line">{{ sb.shot_type || sb.shotType || '未设景别' }} · {{ sb.duration || 10 }}s</div>
                <div class="prod-dots">
                  <span :class="['dot', hasVid(sb) && 'ok']" /><span style="font-size:10px">视频</span>
                  <span :class="['dot', hasTTS(sb) && 'ok']" /><span style="font-size:10px">配音</span>
                  <span :class="['dot', hasComposed(sb) && 'ok', isPendingCompose(sb.id) && 'pending']" /><span style="font-size:10px">{{ isPendingCompose(sb.id) ? '合成中' : '合成' }}</span>
                </div>
                <div v-if="composeFailMessage(sb.id)" class="prod-error">{{ composeFailMessage(sb.id) }}</div>
          </div>
          <div class="prod-actions">
            <button class="btn btn-sm" :disabled="!hasVid(sb) || isPendingCompose(sb.id)" @click="doCompose(sb)">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
              {{ isPendingCompose(sb.id) ? '合成中' : (hasComposed(sb) ? '重新合成' : '开始合成') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Export sub-tab -->
    <div v-else-if="subTab === 'export'" class="prod-content">
      <div class="export-split">
        <div class="export-main">
          <template v-if="mergeUrl">
            <video :src="'/' + mergeUrl" controls class="export-video" />
            <div class="export-bar">
              <span class="tag tag-success">拼接完成</span>
              <span class="dim" style="font-size:12px">{{ sbs.length }} 镜头 · {{ totalDuration }}s</span>
              <a :href="'/' + mergeUrl" download class="btn btn-primary ml-auto">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                下载视频
              </a>
            </div>
          </template>
          <template v-else>
            <div class="step-empty">
              <div class="empty-visual"><svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg></div>
              <div class="empty-title">拼接全集视频</div>
              <div class="empty-desc">将 {{ composedCount }} 个已合成镜头拼接为完整视频</div>
              <button class="btn btn-primary" :disabled="composedCount === 0" @click="doMerge" style="margin-top:12px">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
                开始拼接
              </button>
            </div>
          </template>
        </div>
        <div class="export-list">
          <div class="export-list-head">镜头概览</div>
          <div class="export-list-body">
            <div v-for="(sb, i) in sbs" :key="sb.id" class="exp-row">
              <span class="mono dim" style="font-size:10px">#{{ String(i+1).padStart(2,'0') }}</span>
              <span class="truncate" style="flex:1;font-size:11px">{{ sb.description || sb.title || '—' }}</span>
              <span :class="['dot', hasComposed(sb) && 'ok']" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { toast } from 'vue-sonner'
import { videoAPI, composeAPI, mergeAPI } from '~/composables/useApi'
import { useEpisodeStudio } from '~/composables/useEpisodeStudio'

const route = useRoute()
const dramaId = Number(route.params.id)
const episodeNumber = Number(route.params.episodeNumber)

const {
  drama, episode, chars, scenes, sbs, subTab, step3Tabs, step, epId,
  composedCount, mergeUrl, totalDuration, shotVidCount,
  openImageViewer, hasImg, hasVid, hasComposed, hasTTS,
  getStoryboardCover, getVideoUrl, getComposedVideoUrl,
  refresh, sleep, mergeData,
} = useEpisodeStudio(dramaId, episodeNumber)

// ─── local state ───
const pendingVideoIds = ref([])
const pendingComposeIds = ref([])
const failedVideoMessages = ref({})
const failedComposeMessages = ref({})

// ─── helpers ───
function isPendingVideo(id) {
  return pendingVideoIds.value.includes(id)
}

function videoFailMessage(id) {
  return failedVideoMessages.value[id] || ''
}

function isPendingCompose(id) {
  return pendingComposeIds.value.includes(id)
}

function composeFailMessage(id) {
  return failedComposeMessages.value[id] || ''
}

function getFirstFrame(s) { return s?.first_frame_image || s?.firstFrameImage || null }
function getLastFrame(s) { return s?.last_frame_image || s?.lastFrameImage || null }
function getRefs(sb) {
  const raw = sb.reference_images || sb.referenceImages
  if (!raw) return []
  try { return JSON.parse(raw) } catch { return [] }
}

function watchAsyncResult(check, attempts = 24, delay = 2500) {
  void (async () => {
    for (let i = 0; i < attempts; i++) {
      await sleep(delay)
      await refresh()
      if (check()) return
    }
  })()
}

// ─── video generation ───
async function genVid(sb) {
  const params = {
    storyboard_id: sb.id,
    drama_id: dramaId,
    prompt: sb.video_prompt || sb.videoPrompt || '',
    duration: Number(sb.duration || 5),
  }
  const first = getFirstFrame(sb)
  const last = getLastFrame(sb)
  const refs = getRefs(sb)
  if (first && last) { Object.assign(params, { reference_mode: 'first_last', first_frame_url: first, last_frame_url: last }) }
  else if (refs.length) { Object.assign(params, { reference_mode: 'multiple', reference_image_urls: [first, ...refs].filter(Boolean) }) }
  else if (first) { Object.assign(params, { reference_mode: 'single', image_url: first }) }
  try {
    delete failedVideoMessages.value[sb.id]
    if (!isPendingVideo(sb.id)) pendingVideoIds.value.push(sb.id)
    const generation = await videoAPI.generate(params)
    toast.success('视频生成中')
    await refresh()
    pollVideoGeneration(generation?.id, sb.id)
  } catch (e) {
    pendingVideoIds.value = pendingVideoIds.value.filter(item => item !== sb.id)
    toast.error(e.message)
  }
}

async function pollVideoGeneration(generationId, storyboardId) {
  if (!generationId) {
    watchAsyncResult(() => {
      const target = sbs.value.find(s => s.id === storyboardId)
      const done = !!(target?.video_url || target?.videoUrl)
      if (done) pendingVideoIds.value = pendingVideoIds.value.filter(item => item !== storyboardId)
      return done
    }, 60, 4000)
    return
  }
  for (let i = 0; i < 120; i++) {
    await sleep(4000)
    try {
      const res = await videoAPI.get(generationId)
      await refresh()
      if (res?.status === 'completed') {
        pendingVideoIds.value = pendingVideoIds.value.filter(item => item !== storyboardId)
        delete failedVideoMessages.value[storyboardId]
        toast.success('视频生成完成')
        return
      }
      if (res?.status === 'failed') {
        pendingVideoIds.value = pendingVideoIds.value.filter(item => item !== storyboardId)
        failedVideoMessages.value = {
          ...failedVideoMessages.value,
          [storyboardId]: res?.error_msg || res?.errorMsg || '视频生成失败',
        }
        toast.error(failedVideoMessages.value[storyboardId])
        return
      }
    } catch {}
  }
  pendingVideoIds.value = pendingVideoIds.value.filter(item => item !== storyboardId)
  failedVideoMessages.value = {
    ...failedVideoMessages.value,
    [storyboardId]: '视频生成超时',
  }
  toast.error('视频生成超时')
}

// ─── batch videos ───
function batchVideos() {
  const pendingIds = sbs.value.filter(s => !hasVid(s)).map(s => s.id)
  pendingIds.forEach(id => {
    const sb = sbs.value.find(item => item.id === id)
    if (sb) genVid(sb)
  })
  if (pendingIds.length) {
    pendingVideoIds.value = [...new Set([...pendingVideoIds.value, ...pendingIds])]
    watchAsyncResult(() => pendingIds.every(id => {
      const target = sbs.value.find(s => s.id === id)
      const done = !!(target?.video_url || target?.videoUrl)
      if (done) pendingVideoIds.value = pendingVideoIds.value.filter(item => item !== id)
      return done
    }), 80, 4000)
  }
}

// ─── compose ───
async function doCompose(sb) {
  try {
    delete failedComposeMessages.value[sb.id]
    if (!isPendingCompose(sb.id)) pendingComposeIds.value.push(sb.id)
    await composeAPI.shot(sb.id)
    toast.success('合成完成')
    pendingComposeIds.value = pendingComposeIds.value.filter(item => item !== sb.id)
    refresh()
  } catch (e) {
    pendingComposeIds.value = pendingComposeIds.value.filter(item => item !== sb.id)
    failedComposeMessages.value = {
      ...failedComposeMessages.value,
      [sb.id]: e.message,
    }
    toast.error(e.message)
  }
}

async function batchCompose() {
  await composeAPI.all(epId.value)
  pendingComposeIds.value = [...new Set(sbs.value.filter(sb => !!sb.video_url || !!sb.videoUrl).map(sb => sb.id))]
  toast.success('批量合成已开始')
  pollComposeStatus()
}

async function pollComposeStatus() {
  for (let i = 0; i < 120; i++) {
    await sleep(3000)
    try {
      const res = await composeAPI.status(epId.value)
      await refresh()
      const items = Array.isArray(res?.items) ? res.items : []
      const processingIds = items.filter(item => item.status === 'compose_processing').map(item => item.id)
      pendingComposeIds.value = processingIds

      const failedItems = items.filter(item => item.status === 'compose_failed')
      if (failedItems.length) {
        const next = { ...failedComposeMessages.value }
        failedItems.forEach((item) => {
          next[item.id] = item.error_msg || item.errorMsg || '视频合成失败'
        })
        failedComposeMessages.value = next
      }

      if (!processingIds.length) {
        if (failedItems.length) toast.error(`有 ${failedItems.length} 个镜头合成失败`)
        else toast.success('批量合成完成')
        return
      }
    } catch {}
  }
}

// ─── merge ───
async function doMerge() {
  await mergeAPI.merge(epId.value); toast.success('拼接中...')
  const poll = setInterval(async () => {
    try { mergeData.value = await mergeAPI.status(epId.value) } catch {}
    if (mergeData.value?.status === 'completed' || mergeData.value?.status === 'failed') {
      clearInterval(poll)
      mergeData.value.status === 'completed' ? toast.success('拼接完成') : toast.error('拼接失败')
    }
  }, 3000)
}
</script>
<style scoped>
.content-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; position: relative; min-height: 0; }

/* Toolbar */
.step-toolbar {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px; border-bottom: 1px solid rgba(27, 41, 64, 0.08);
  background: linear-gradient(180deg, rgba(255,255,255,0.8), rgba(255,255,255,0.42)); flex-shrink: 0;
}
.prod-toolbar { background: linear-gradient(180deg, rgba(255,255,255,0.8), rgba(255,255,255,0.42)); }
.toolbar-left { display: flex; align-items: center; gap: 8px; flex: 1; }
.toolbar-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.step-indicator { display: flex; align-items: center; gap: 8px; }
.step-name { font-size: 13px; font-weight: 700; color: var(--text-1); font-family: var(--font-display); }

/* Tabs */
.prod-tabs { display: flex; gap: 0; background: var(--bg-2); border-radius: var(--radius); padding: 2px; }
.prod-tab {
  display: flex; align-items: center; gap: 4px; padding: 6px 12px; font-size: 12px;
  border: none; background: transparent; color: var(--text-2); cursor: pointer;
  border-radius: calc(var(--radius) - 2px); transition: all 0.15s; font-weight: 500;
}
.prod-tab:hover { color: var(--text-0); }
.prod-tab.active { background: var(--bg-0); color: var(--text-0); font-weight: 600; box-shadow: var(--shadow-xs); }
.prod-tab-badge { font-size: 10px; font-family: var(--font-mono); padding: 0 4px; background: var(--bg-3); border-radius: 99px; }
.prod-tab.active .prod-tab-badge { background: var(--accent-bg); color: var(--accent-text); }

/* Production content */
.prod-content { flex: 1; overflow-y: auto; padding: 12px 16px; display: flex; flex-direction: column; gap: 12px; }
.prod-section-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* Prod grid */
.prod-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.prod-card {
  display: flex; flex-direction: column; overflow: hidden;
  transition: transform 0.18s var(--ease-out), box-shadow 0.18s var(--ease-out), border-color 0.18s var(--ease-out);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255,255,255,0.74), rgba(248,251,255,0.58));
}
.prod-card:hover { transform: translateY(-2px); box-shadow: 0 16px 30px rgba(20, 32, 54, 0.08); }
.prod-cover { position: relative; aspect-ratio: 16/9; background: var(--bg-2); overflow: hidden; }
.prod-cover img { width: 100%; height: 100%; object-fit: cover; }
.prod-video { width: 100%; height: 100%; object-fit: cover; background: #000; display: block; }
.prod-cover-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--text-3); }
.prod-idx {
  position: absolute; top: 5px; left: 5px; font-size: 10px; font-weight: 700;
  font-family: var(--font-mono); background: rgba(0,0,0,0.5); color: #fff; padding: 1px 5px; border-radius: 3px;
}
.prod-overlay-badge {
  position: absolute; bottom: 5px; right: 5px; font-size: 10px; font-weight: 600;
  background: var(--success); color: #fff; padding: 1px 5px; border-radius: 3px;
}
.prod-info { padding: 10px 12px 8px; }
.prod-desc { font-size: 12px; line-height: 1.4; }
.prod-meta-line { margin-top: 5px; font-size: 10px; color: var(--text-3); }
.prod-dots { display: flex; align-items: center; gap: 4px; margin-top: 5px; color: var(--text-3); }
.prod-error {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.45;
  color: var(--error);
}
.prod-actions { display: flex; gap: 6px; padding: 8px 10px 10px; border-top: 1px solid rgba(27, 41, 64, 0.08); }
.prod-actions .btn { flex: 1; justify-content: center; }

/* Dots */
.dot { width: 7px; height: 7px; border-radius: 50%; background: var(--bg-3); flex-shrink: 0; }
.dot.ok { background: var(--success); }
.dot.pending {
  background: var(--accent-dark);
  box-shadow: 0 0 0 3px rgba(76, 125, 255, 0.14);
}

/* Previewable image */
.previewable-image { cursor: zoom-in; transition: transform 0.18s var(--ease-out), filter 0.18s var(--ease-out); }
.previewable-image:hover { transform: scale(1.015); filter: saturate(1.04); }

/* Export */
.export-split { flex: 1; display: flex; min-height: 0; }
.export-main { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px; }
.export-video { max-width: 720px; width: 100%; border-radius: var(--radius-lg); background: #000; }
.export-bar { display: flex; align-items: center; gap: 12px; margin-top: 16px; width: 100%; max-width: 720px; }
.export-list { width: 240px; flex-shrink: 0; border-left: 1px solid var(--border); display: flex; flex-direction: column; overflow: hidden; }
.export-list-head { padding: 11px 14px; font-size: 11px; font-weight: 700; color: var(--text-3); border-bottom: 1px solid var(--border); text-transform: uppercase; letter-spacing: 0.06em; }
.export-list-body { flex: 1; overflow-y: auto; padding: 6px; }
.exp-row { display: flex; align-items: center; gap: 8px; padding: 5px 8px; border-radius: var(--radius); }
.exp-row:hover { background: var(--bg-hover); }

/* Step Empty State */
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

/* Shared */
.dim { color: var(--text-3); }

@media (max-width: 1240px) {
  .export-split {
    flex-direction: column;
  }
  .export-list {
    width: 100%;
  }
}

@media (max-width: 860px) {
  .toolbar-right,
  .export-bar {
    flex-wrap: wrap;
  }
  .prod-grid {
    grid-template-columns: 1fr;
  }
}
</style>
