<template>
  <div class="content-panel">
    <!-- Sub: Raw Content -->
    <div v-if="subTab === 'raw'" class="step-editor">
      <div class="step-toolbar">
        <div class="toolbar-left">
          <div class="step-indicator">
            <span class="step-num">01</span>
            <span class="step-name">原始内容</span>
          </div>
        </div>
        <div class="toolbar-right">
          <span v-if="rawLen" class="char-count">{{ rawLen }} 字</span>
          <button class="btn btn-sm" @click="saveRaw(); toast.success('已保存')">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            保存
          </button>
        </div>
      </div>
      <textarea
        class="fill-textarea"
        v-model="localRaw"
        placeholder="粘贴小说原文、故事大纲或分镜描述..."
      />
    </div>

    <!-- Sub: Rewrite -->
    <div v-else-if="subTab === 'rewrite'" class="step-editor">
      <div class="step-toolbar">
        <div class="toolbar-left">
          <div class="step-indicator">
            <span class="step-num">02</span>
            <span class="step-name">AI 改写</span>
          </div>
        </div>
        <div class="toolbar-right">
          <span v-if="scriptLen" class="char-count">{{ scriptLen }} 字</span>
          <button v-if="rawContent" class="btn btn-sm" @click="skipRewrite">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M5 12h14"/><path d="M13 18l6-6-6-6"/></svg>
            跳过改写
          </button>
          <button v-if="scriptContent" class="btn btn-sm" @click="doRewrite" :disabled="rn">
            <Loader2 v-if="rn && rt === 'script_rewriter'" :size="11" class="animate-spin" />
            <svg v-else width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
            重新改写
          </button>
        </div>
      </div>
      <div v-if="!scriptContent && !rn" class="step-empty">
        <div class="empty-visual">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/>
          </svg>
        </div>
        <div class="empty-title">AI 改写为格式化剧本</div>
        <div class="empty-desc">你可以先用 AI 把原始内容整理成格式化剧本，也可以跳过这一步，直接使用原始内容继续提取角色与场景。</div>
        <div class="step-empty-actions">
          <button class="btn btn-primary" @click="doRewrite">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            开始改写
          </button>
          <button class="btn" @click="skipRewrite">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M5 12h14"/><path d="M13 18l6-6-6-6"/></svg>
            跳过改写
          </button>
        </div>
      </div>
      <div v-else-if="rn && rt === 'script_rewriter'" class="step-loading">
        <Loader2 :size="24" class="animate-spin" style="color:var(--accent)" />
        <div class="loading-text">正在改写剧本...</div>
      </div>
      <textarea v-else class="fill-textarea" v-model="localScript" placeholder="格式化剧本内容..." />
    </div>

    <!-- Sub: Storyboard Breakdown -->
    <div v-else-if="subTab === 'storyboard'" class="step-editor">
      <div class="step-toolbar">
        <div class="toolbar-left">
          <div class="step-indicator">
            <span class="step-num">05</span>
            <span class="step-name">分镜列表</span>
          </div>
        </div>
        <div class="toolbar-right">
          <span v-if="sbs.length" class="char-count">{{ sbs.length }} 镜头 · {{ totalDuration }}s</span>
          <button v-if="sbs.length" class="btn btn-sm" @click="addShot">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            添加
          </button>
          <template v-if="!sbs.length">
            <span class="locked-config">视频模型 · {{ lockedVideoConfigLabel }}</span>
          </template>
          <button class="btn btn-sm" :disabled="rn" @click="doBreakdown">
            <Loader2 v-if="rt === 'storyboard_breaker'" :size="11" class="animate-spin" />
            <svg v-else width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            {{ sbs.length ? '重新拆解' : 'AI 拆解分镜' }}
          </button>
        </div>
      </div>
      <div v-if="sbs.length" class="split-layout">
        <!-- Shot List -->
        <div class="shot-list">
          <div class="shot-list-head">
            <div>
              <div class="shot-list-title">镜头序列</div>
              <div class="shot-list-sub">按镜头顺序检查内容与素材状态</div>
            </div>
            <span class="tag mono">{{ totalDuration }}s</span>
          </div>
          <div class="shot-list-body">
            <div
              v-for="(sb, i) in sbs"
              :key="sb.id"
              :class="['shot-item', { active: selectedSb?.id === sb.id }]"
              @click="selectedSb = sb"
            >
              <div class="shot-item-header">
                <div class="shot-num">#{{ String(i+1).padStart(2,'0') }}</div>
                <span class="tag" style="font-size:10px">{{ sb.shot_type || sb.shotType || '—' }}</span>
                <span v-if="getStoryboardCharacterIds(sb).length" class="tag" style="font-size:10px">{{ getStoryboardCharacterIds(sb).length }} 角色</span>
                <div class="shot-status">
                  <div v-if="sb.imageUrl || sb.composedImage || sb.firstFrameImage" class="shot-dot has-img" title="已生成图片"></div>
                  <div v-if="sb.videoUrl || sb.composedVideoUrl" class="shot-dot has-video" title="已生成视频"></div>
                  <div v-if="sb.dialogue" class="shot-dot has-dialogue" title="有对白"></div>
                </div>
              </div>
              <div class="shot-body">
                <div class="shot-desc">{{ sb.description || sb.title || '无描述' }}</div>
              </div>
              <div class="shot-meta">
                <span class="mono dim" style="font-size:10px">{{ sb.duration || 10 }}s</span>
                <span v-if="sb.location" class="shot-location">{{ sb.location }}</span>
                <span v-if="getStoryboardCharacterNames(sb).length" class="shot-location">{{ getStoryboardCharacterNames(sb).join(' / ') }}</span>
                <span v-if="sb.dialogue" class="shot-dialogue">{{ sb.dialogue }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Detail Panel -->
        <div class="detail-panel" v-if="selectedSb">
            <div class="detail-head">
              <div class="detail-head-copy">
                <span class="detail-head-title">镜头 #{{ sbs.indexOf(selectedSb) + 1 }}</span>
              <span class="detail-head-sub">{{ selectedSb.title || `镜头 ${sbs.indexOf(selectedSb) + 1}` }} · {{ selectedSb.shot_type || selectedSb.shotType || '未设置景别' }}</span>
              </div>
              <span class="tag mono">{{ (selectedSb.duration || 10) }}s</span>
              <button class="btn btn-ghost btn-icon ml-auto" style="color:var(--error)" @click="deleteShot(selectedSb)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>
              </button>
          </div>
          <div class="detail-body">
            <div class="detail-hero">
              <div class="detail-hero-copy">
                <div class="detail-hero-label">镜头概览</div>
                <div class="detail-hero-text">{{ selectedSb.description || selectedSb.title || '当前镜头还没有画面描述，建议先补充核心动作和构图。' }}</div>
                <div class="detail-status-row">
                  <span class="tag">{{ getSceneName(selectedSb) }}</span>
                  <span class="tag">{{ selectedSb.angle || '未设角度' }}</span>
                  <span class="tag">{{ selectedSb.movement || '未设运镜' }}</span>
                  <span class="tag" :class="getFirstFrame(selectedSb) ? 'tag-success' : ''">首帧 {{ getFirstFrame(selectedSb) ? '已生成' : '待生成' }}</span>
                  <span class="tag" :class="getLastFrame(selectedSb) ? 'tag-success' : ''">尾帧 {{ getLastFrame(selectedSb) ? '已生成' : '待生成' }}</span>
                  <span class="tag" :class="hasVid(selectedSb) ? 'tag-success' : ''">视频 {{ hasVid(selectedSb) ? '已生成' : '待生成' }}</span>
                </div>
              </div>
              <div class="detail-preview-grid">
                <div class="detail-preview-card">
                  <div class="detail-preview-title">首帧</div>
                  <div class="detail-preview-media">
                    <img
                      v-if="getFirstFrame(selectedSb)"
                      :src="'/' + getFirstFrame(selectedSb)"
                      class="previewable-image"
                      @click.stop="openImageViewer('/' + getFirstFrame(selectedSb), `镜头 #${sbs.indexOf(selectedSb) + 1} 首帧`)"
                    />
                    <div v-else class="detail-preview-empty">待生成</div>
                  </div>
                </div>
                <div class="detail-preview-card">
                  <div class="detail-preview-title">尾帧</div>
                  <div class="detail-preview-media">
                    <img
                      v-if="getLastFrame(selectedSb)"
                      :src="'/' + getLastFrame(selectedSb)"
                      class="previewable-image"
                      @click.stop="openImageViewer('/' + getLastFrame(selectedSb), `镜头 #${sbs.indexOf(selectedSb) + 1} 尾帧`)"
                    />
                    <div v-else class="detail-preview-empty">待生成</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="detail-section">
              <div class="detail-section-head">
                <span class="detail-section-title">镜头结构</span>
                <span class="detail-section-copy">景别、角度、运镜、场景绑定和时长</span>
              </div>
              <div class="field-grid field-grid-4">
                <label class="field">
                  <span class="field-label">标题</span>
                  <input :value="selectedSb.title || ''" class="input"
                    @blur="updateField(selectedSb, 'title', $event.target.value)" placeholder="如：雪地逼近" />
                </label>
                <label class="field">
                  <span class="field-label">景别</span>
                  <input
                    list="shot-type-list"
                    :value="selectedSb.shot_type || selectedSb.shotType || ''"
                    class="input"
                    placeholder="选择或输入景别"
                    @change="updateField(selectedSb, 'shot_type', $event.target.value)"
                  />
                  <datalist id="shot-type-list">
                    <option v-for="t in shotTypes" :key="t" :value="t" />
                  </datalist>
                </label>
                <label class="field">
                  <span class="field-label">角度</span>
                  <input
                    list="shot-angle-list"
                    :value="selectedSb.angle || ''"
                    class="input"
                    placeholder="选择或输入角度"
                    @change="updateField(selectedSb, 'angle', $event.target.value)"
                  />
                  <datalist id="shot-angle-list">
                    <option v-for="t in shotAngles" :key="t" :value="t" />
                  </datalist>
                </label>
                <label class="field">
                  <span class="field-label">运镜</span>
                  <input
                    list="shot-movement-list"
                    :value="selectedSb.movement || ''"
                    class="input"
                    placeholder="选择或输入运镜"
                    @change="updateField(selectedSb, 'movement', $event.target.value)"
                  />
                  <datalist id="shot-movement-list">
                    <option v-for="t in shotMovements" :key="t" :value="t" />
                  </datalist>
                </label>
              </div>
              <div class="field-grid field-grid-4">
                <label class="field">
                  <span class="field-label">绑定角色</span>
                  <div class="role-pills">
                    <button
                      v-for="char in chars"
                      :key="char.id"
                      type="button"
                      :class="['role-pill', { active: isStoryboardCharacterSelected(selectedSb, char.id) }]"
                      @click="toggleStoryboardCharacter(selectedSb, char.id)"
                    >
                      {{ char.name }}
                    </button>
                    <span v-if="!chars.length" class="dim" style="font-size:12px">当前集还没有角色</span>
                  </div>
                </label>
                <label class="field">
                  <span class="field-label">绑定场景</span>
                  <select class="input" :value="selectedSb.scene_id || selectedSb.sceneId || ''"
                    @change="updateField(selectedSb, 'scene_id', $event.target.value ? Number($event.target.value) : null)">
                    <option value="">未绑定场景</option>
                    <option v-for="scene in scenes" :key="scene.id" :value="scene.id">
                      {{ scene.location }} · {{ scene.time || '未设时间' }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span class="field-label">地点</span>
                  <input :value="selectedSb.location || ''" class="input"
                    @blur="updateField(selectedSb, 'location', $event.target.value)" placeholder="场景地点" />
                </label>
                <label class="field">
                  <span class="field-label">时间</span>
                  <input :value="selectedSb.time || ''" class="input"
                    @blur="updateField(selectedSb, 'time', $event.target.value)" placeholder="如：深夜 / 清晨" />
                </label>
                <label class="field">
                  <span class="field-label">时长</span>
                  <input :value="selectedSb.duration || 10" class="input" type="number" min="1" max="60"
                    @blur="updateField(selectedSb, 'duration', Number($event.target.value))" />
                </label>
              </div>
            </div>
            <div class="detail-section">
              <div class="detail-section-head">
                <span class="detail-section-title">画面语义</span>
                <span class="detail-section-copy">动作、结果、氛围和对白</span>
              </div>
              <div class="field-grid field-grid-2">
                <label class="field">
                  <span class="field-label">动作</span>
                  <textarea :value="selectedSb.action || ''" class="textarea" rows="3"
                    @blur="updateField(selectedSb, 'action', $event.target.value)" placeholder="谁在做什么，表情和动作细节是什么" />
                </label>
                <label class="field">
                  <span class="field-label">结果</span>
                  <textarea :value="selectedSb.result || ''" class="textarea" rows="3"
                    @blur="updateField(selectedSb, 'result', $event.target.value)" placeholder="镜头结束时的状态变化或画面结果" />
                </label>
              </div>
              <div class="field-grid field-grid-2">
                <label class="field">
                  <span class="field-label">画面描述</span>
                  <textarea :value="selectedSb.description || ''" class="textarea" rows="4"
                    @blur="updateField(selectedSb, 'description', $event.target.value)" placeholder="描述画面内容..." />
                </label>
                <label class="field">
                  <span class="field-label">氛围</span>
                  <textarea :value="selectedSb.atmosphere || ''" class="textarea" rows="4"
                    @blur="updateField(selectedSb, 'atmosphere', $event.target.value)" placeholder="光线、色调、空气感、环境氛围" />
                </label>
              </div>
              <label class="field">
                <span class="field-label">对白 / 旁白</span>
                <textarea :value="selectedSb.dialogue || ''" class="textarea" rows="3"
                  @blur="updateField(selectedSb, 'dialogue', $event.target.value)" placeholder="角色名：台词内容 或 旁白：内容" />
              </label>
            </div>
            <div class="detail-section">
              <div class="detail-section-head">
                <span class="detail-section-title">生成提示</span>
                <span class="detail-section-copy">分别服务图片、视频、配乐和音效生成</span>
              </div>
              <label class="field">
                <span class="field-label">静态画面提示词</span>
                <textarea :value="selectedSb.image_prompt || selectedSb.imagePrompt || ''" class="textarea" rows="4"
                  @blur="updateField(selectedSb, 'image_prompt', $event.target.value)" placeholder="用于首帧、尾帧和镜头图片的单帧画面提示词" />
              </label>
              <label class="field">
                <span class="field-label">视频提示词</span>
                <textarea :value="selectedSb.video_prompt || selectedSb.videoPrompt || ''" class="textarea" rows="5"
                  @blur="updateField(selectedSb, 'video_prompt', $event.target.value)" placeholder="按 3 秒分段的视频提示词..." />
              </label>
              <div class="field-grid field-grid-2">
                <label class="field">
                  <span class="field-label">配乐提示词</span>
                  <textarea :value="selectedSb.bgm_prompt || selectedSb.bgmPrompt || ''" class="textarea" rows="3"
                    @blur="updateField(selectedSb, 'bgm_prompt', $event.target.value)" placeholder="如：压抑低频弦乐，缓慢推进" />
                </label>
                <label class="field">
                  <span class="field-label">音效提示词</span>
                  <textarea :value="selectedSb.sound_effect || selectedSb.soundEffect || ''" class="textarea" rows="3"
                    @blur="updateField(selectedSb, 'sound_effect', $event.target.value)" placeholder="如：风雪声、脚踩积雪、衣料摩擦声" />
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="rn && rt === 'storyboard_breaker'" class="step-loading">
        <Loader2 :size="24" class="animate-spin" style="color:var(--accent)" />
        <div class="loading-text">正在拆解分镜并生成提示词...</div>
      </div>

      <div v-else class="step-empty">
        <div class="empty-visual">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round">
            <rect x="2" y="2" width="20" height="20" rx="2.5"/><line x1="7" y1="8" x2="7" y2="16"/><line x1="10" y1="8" x2="10" y2="16"/><line x1="13" y1="8" x2="13" y2="16"/>
          </svg>
        </div>
        <div class="empty-title">将剧本拆解为分镜序列</div>
        <div class="empty-desc">AI 自动分析剧本，生成镜头列表和视频提示词</div>
        <div class="locked-config-banner">当前集视频模型：{{ lockedVideoConfigLabel }}</div>
        <button class="btn btn-primary" @click="doBreakdown">
          <Loader2 v-if="rt === 'storyboard_breaker'" :size="13" class="animate-spin" />
          <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          AI 拆解分镜
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { toast } from 'vue-sonner'
import { Loader2 } from 'lucide-vue-next'
import { episodeAPI, storyboardAPI } from '~/composables/useApi'
import { useEpisodeStudio } from '~/composables/useEpisodeStudio'
import { useAgent } from '~/composables/useAgent'

const route = useRoute()
const dramaId = Number(route.params.id)
const episodeNumber = Number(route.params.episodeNumber)
const {
  episode, chars, scenes, sbs, selectedSb, subTab,
  epId, rawContent, scriptContent, totalDuration,
  lockedVideoConfigId, lockedVideoConfigLabel,
  videoConfigs,
  refresh,
  openImageViewer,
  getFirstFrame, getLastFrame, hasVid,
  getStoryboardCharacterIds, getStoryboardCharacterNames,
  getSceneName,
} = useEpisodeStudio(dramaId, episodeNumber)

const { running: rn, runningType: rt, run: runAgent } = useAgent()

// ── local state ──
const localRaw = ref('')
const localScript = ref('')
const rawLen = computed(() => localRaw.value.replace(/\s/g, '').length || 0)
const scriptLen = computed(() => localScript.value.replace(/\s/g, '').length || 0)

// ── local constants ──
const shotTypes = ['特写', '近景', '中景', '全景', '远景', '大远景']
const shotAngles = ['平视', '仰视', '俯视', '侧拍', '背拍', '斜侧', '主观视角', '过肩']
const shotMovements = ['固定', '推镜', '拉镜', '摇镜', '移镜', '跟拍', '升降', '手持', '环绕']

// ── watchers ──
watch(rawContent, v => { localRaw.value = v }, { immediate: true })
watch(scriptContent, v => { localScript.value = v }, { immediate: true })

// ── local functions ──
function saveRaw() {
  episodeAPI.update(epId.value, { content: localRaw.value })
  episode.value.content = localRaw.value
}

function saveScr() {
  episodeAPI.update(epId.value, { script_content: localScript.value })
  episode.value.script_content = localScript.value
}

function doRewrite() {
  saveRaw()
  runAgent('script_rewriter', '请读取剧本并改写为格式化剧本，然后保存', dramaId, epId.value, refresh)
}
function skipRewrite() {
  const raw = (localRaw.value || rawContent.value || '').trim()
  if (!raw) {
    toast.warning('请先填写原始内容')
    return
  }
  localScript.value = raw
  saveScr()
  toast.success('已跳过 AI 改写，当前将直接使用原始内容')
  subTab.value = 'storyboard'
}

function doBreakdown() {
  const cfg = videoConfigs.value.find(c => c.id === lockedVideoConfigId.value)
  const label = cfg ? `${cfg.name} (${cfg.provider})` : '默认'
  runAgent('storyboard_breaker', `请拆解分镜并生成视频提示词。视频模型：${label}，请根据该模型的特性和时长限制生成合适的视频提示词。`, dramaId, epId.value, refresh)
}

async function addShot() {
  await storyboardAPI.create({ episode_id: epId.value, storyboard_number: sbs.value.length + 1, title: `镜头${sbs.value.length + 1}`, duration: 10 })
  refresh()
}

async function deleteShot(sb) {
  if (!confirm('确定删除此镜头？')) return
  const idx = sbs.value.indexOf(sb)
  await storyboardAPI.del(sb.id)
  await refresh()
  if (sbs.value.length) selectedSb.value = sbs.value[Math.min(idx, sbs.value.length - 1)]
  else selectedSb.value = null
}

function toCamel(field) {
  return field.replace(/_([a-z])/g, (_, c) => c.toUpperCase())
}

function updateField(sb, field, value) {
  const current = sb[field] ?? sb[toCamel(field)]
  if (current === value) return
  sb[field] = value
  const camelField = toCamel(field)
  if (camelField !== field) sb[camelField] = value
  storyboardAPI.update(sb.id, { [field]: value })
}

function isStoryboardCharacterSelected(sb, charId) {
  return getStoryboardCharacterIds(sb).includes(charId)
}

function toggleStoryboardCharacter(sb, charId) {
  const currentIds = getStoryboardCharacterIds(sb)
  const nextIds = currentIds.includes(charId)
    ? currentIds.filter(id => id !== charId)
    : [...currentIds, charId]
  updateField(sb, 'character_ids', nextIds)
}
</script>
<style scoped>
/* Container */
.content-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; position: relative; min-height: 0; height: 100%; }

/* Toolbar */
.step-toolbar {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px; border-bottom: 1px solid rgba(27, 41, 64, 0.08);
  background: linear-gradient(180deg, rgba(255,255,255,0.8), rgba(255,255,255,0.42)); flex-shrink: 0;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; flex: 1; }
.toolbar-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.step-indicator { display: flex; align-items: center; gap: 8px; }
.step-num {
  width: 26px; height: 26px; border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
  background: rgba(19, 51, 121, 0.08);
  font-family: var(--font-mono); font-size: 10px; font-weight: 800; color: var(--accent-text); letter-spacing: 0.05em;
}
.step-name { font-size: 13px; font-weight: 700; color: var(--text-1); font-family: var(--font-display); }
.char-count { font-size: 11px; color: var(--text-3); font-family: var(--font-mono); }

/* Editor Area */
.step-editor { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.fill-textarea {
  flex: 1; border: none; border-radius: 0; padding: 26px 28px;
  font-size: 13.5px; line-height: 1.9; resize: none; outline: none;
  font-family: var(--font-body); background: linear-gradient(180deg, rgba(255,255,255,0.28), rgba(255,255,255,0.12)); color: var(--text-0);
}
.fill-textarea:focus { box-shadow: none; }

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
.step-empty-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: center; }

/* Step Loading */
.step-loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  flex: 1; gap: 12px;
}
.loading-text { font-size: 13px; color: var(--text-2); }
/* Split layout (storyboard) */
.split-layout { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.shot-list { width: 296px; flex-shrink: 0; overflow-y: auto; border-right: 1px solid var(--border); background: var(--bg-0); }
.shot-list-head {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 11px 12px 10px;
  border-bottom: 1px solid rgba(27, 41, 64, 0.06);
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(10px);
}
.shot-list-title { font-size: 13px; font-weight: 700; color: var(--text-0); }
.shot-list-sub { margin-top: 3px; font-size: 11px; color: var(--text-3); line-height: 1.45; }
.shot-list-body { padding: 6px; }
.shot-item {
  position: relative; padding: 10px 11px; cursor: pointer;
  border: 1px solid transparent; border-left: 3px solid transparent;
  transition: all 0.15s;
  display: flex; flex-direction: column; gap: 5px;
  border-radius: 14px;
}
.shot-item + .shot-item { margin-top: 6px; }
.shot-item:hover { background: var(--bg-hover); border-color: rgba(27, 41, 64, 0.06); }
.shot-item.active {
  background: var(--bg-0);
  border-left-color: var(--accent);
  box-shadow: inset 0 0 0 1px var(--accent-glow);
  z-index: 1;
}
.shot-item-header { display: flex; align-items: center; gap: 8px; }
.shot-num {
  font-size: 11px; font-family: var(--font-mono); font-weight: 700;
  color: var(--accent); background: var(--accent-bg);
  padding: 2px 6px; border-radius: 4px; flex-shrink: 0;
  letter-spacing: 0.03em;
}
.shot-item.active .shot-num { background: var(--accent); color: #fff; }
.shot-status { display: flex; gap: 4px; margin-left: auto; flex-shrink: 0; }
.shot-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--bg-3); flex-shrink: 0; }
.shot-dot.has-img { background: var(--success); }
.shot-dot.has-video { background: var(--info); }
.shot-dot.has-dialogue { background: var(--warning); }
.shot-body { }
.shot-desc { font-size: 12px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; color: var(--text-1); }
.shot-item.active .shot-desc { color: var(--text-0); }
.shot-meta { display: flex; align-items: center; gap: 6px; }
.shot-location {
  font-size: 10px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.shot-dialogue {
  font-size: 10px; color: var(--text-3); margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  padding-left: 6px; border-left: 2px solid var(--border);
}

.detail-panel { flex: 1; display: flex; flex-direction: column; overflow-y: auto; min-width: 0; }
.detail-head { display: flex; align-items: center; gap: 8px; padding: 9px 14px; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.detail-head-copy { display: flex; flex-direction: column; gap: 2px; }
.detail-head-title { font-size: 14px; font-weight: 700; color: var(--text-0); }
.detail-head-sub { font-size: 11px; color: var(--text-3); }
.detail-body { padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; }
.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(220px, 0.9fr);
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(20,39,82,0.08), rgba(255,255,255,0.68));
  border: 1px solid rgba(27, 41, 64, 0.08);
}
.detail-hero-copy { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.detail-hero-label {
  font-size: 10px; font-weight: 700; letter-spacing: 0.12em;
  text-transform: uppercase; color: var(--text-3);
}
.detail-hero-text { font-size: 13px; color: var(--text-1); line-height: 1.7; }
.detail-status-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.detail-preview-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.detail-preview-card { display: flex; flex-direction: column; gap: 6px; }
.detail-preview-title { font-size: 11px; font-weight: 700; color: var(--text-2); }
.detail-preview-media {
  position: relative; aspect-ratio: 16/9; overflow: hidden;
  border-radius: 14px; background: rgba(18,25,42,0.08);
  border: 1px solid rgba(27, 41, 64, 0.08);
}
.detail-preview-media img { width: 100%; height: 100%; object-fit: cover; display: block; }
.detail-preview-empty {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  color: var(--text-3); font-size: 12px;
}
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.72);
  border: 1px solid rgba(27, 41, 64, 0.08);
}
.detail-section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-section-title { font-size: 12px; font-weight: 700; color: var(--text-0); }
.detail-section-copy { font-size: 11px; color: var(--text-3); }

/* Field */
.field { display: flex; flex-direction: column; gap: 5px; }
.field-label { font-size: 12px; font-weight: 500; color: var(--text-1); }
.field-grid { display: grid; gap: 12px; }
.field-grid-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.field-grid-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.locked-config {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(19, 51, 121, 0.08);
  border: 1px solid rgba(19, 51, 121, 0.12);
  color: var(--text-1);
  font-size: 11px;
  font-weight: 600;
}
.locked-config-banner {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--text-2);
}
.role-pills { display: flex; flex-wrap: wrap; gap: 8px; }
.role-pill {
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(27, 41, 64, 0.12);
  background: rgba(255,255,255,0.86);
  color: var(--text-2);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.role-pill:hover { border-color: var(--accent); color: var(--text-0); }
.role-pill.active {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
  box-shadow: 0 8px 18px rgba(29, 77, 176, 0.18);
}
</style>
