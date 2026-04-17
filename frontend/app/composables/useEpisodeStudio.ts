import { toast } from 'vue-sonner'
import {
  dramaAPI, episodeAPI, storyboardAPI, characterAPI, sceneAPI, propAPI,
  imageAPI, videoAPI, composeAPI, mergeAPI, gridAPI, aiConfigAPI, voicesAPI,
} from '~/composables/useApi'
import { useAgent } from '~/composables/useAgent'

export { useAgent }

// ── singleton store keyed by "dramaId:episodeNumber" ──
const storeMap = new Map<string, ReturnType<typeof createEpisodeStudio>>()

export function useEpisodeStudio(dramaId: number, episodeNumber: number) {
  const key = `${dramaId}:${episodeNumber}`
  if (!storeMap.has(key)) {
    storeMap.set(key, createEpisodeStudio(dramaId, episodeNumber))
  }
  const store = storeMap.get(key)!

  // lifecycle hooks — only the first component to mount triggers data loading
  onMounted(() => {
    if (!store._initialized) {
      store._initialized = true
      store.refresh()
      store.loadConfigs()
      store.loadVoices()
    }
    window.addEventListener('keydown', store._handleKeydown)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', store._handleKeydown)
  })

  return store
}

// ── factory ──
function createEpisodeStudio(dramaId: number, episodeNumber: number) {
  // ─── state refs ───
  const drama = ref<any>(null)
  const episode = ref<any>(null)
  const chars = ref<any[]>([])
  const scenes = ref<any[]>([])
  const sbs = ref<any[]>([])
  const mergeData = ref<any>(null)
  const dramaProps = ref<any[]>([])
  const step = ref(1)
  const subTab = ref('raw')
  const selectedSb = ref<any>(null)
  const imageViewer = ref({ open: false, src: '', title: '' })
  const imageConfigs = ref<any[]>([])
  const videoConfigs = ref<any[]>([])
  const audioConfigs = ref<any[]>([])

  const fallbackVoiceProfiles = [
    { id: 'alloy', label: 'Alloy', gender: '中性', traits: '平衡、自然、克制', suitable: '通用叙述、旁白、需要稳定输出的角色' },
    { id: 'echo', label: 'Echo', gender: '男声', traits: '低沉、稳重、冷静', suitable: '成熟男性、父辈、旁白、压迫感角色' },
    { id: 'fable', label: 'Fable', gender: '男声', traits: '温暖、讲述感、表现力强', suitable: '男主、成长型角色、叙事担当' },
    { id: 'onyx', label: 'Onyx', gender: '男声', traits: '深沉、有力、权威', suitable: '反派、强势角色、掌控型人物' },
    { id: 'nova', label: 'Nova', gender: '女声', traits: '温柔、甜润、亲和', suitable: '女主、母亲、柔和配角' },
    { id: 'shimmer', label: 'Shimmer', gender: '女声', traits: '明亮、活泼、年轻', suitable: '少女、轻快角色、跳脱配角' },
  ]
  const voiceProfiles = ref<any[]>(fallbackVoiceProfiles)

  // ─── computed ───
  const epId = computed(() => episode.value?.id || 0)
  const rawContent = computed(() => episode.value?.content || '')
  const scriptContent = computed(() => episode.value?.script_content || episode.value?.scriptContent || '')
  const charsVoiced = computed(() => chars.value.filter(c => c.voice_style || c.voiceStyle).length)
  const voiceSampleCount = computed(() => chars.value.filter(c => c.voice_sample_url || c.voiceSampleUrl).length)
  const composedCount = computed(() => sbs.value.filter(s => s.composed_video_url || s.composedVideoUrl).length)
  const mergeUrl = computed(() => mergeData.value?.merged_url || mergeData.value?.mergedUrl || null)

  function isNarratorCharacter(char: any) {
    const text = `${char?.name || ''} ${char?.role || ''}`.toLowerCase()
    return text.includes('旁白') || text.includes('narrator') || text.includes('画外音')
  }

  const visualChars = computed(() => chars.value.filter(c => !isNarratorCharacter(c)))
  const visualCharTotal = computed(() => visualChars.value.length)
  const charImgCount = computed(() => visualChars.value.filter(c => c.image_url || c.imageUrl).length)
  const sceneImgCount = computed(() => scenes.value.filter(s => s.image_url || s.imageUrl).length)
  const ttsEligibleCount = computed(() => sbs.value.filter(s => hasDialogue(s)).length)
  const ttsGeneratedCount = computed(() => sbs.value.filter(s => hasDialogue(s) && hasTTS(s)).length)
  const shotImgCount = computed(() => sbs.value.filter(s => s.first_frame_image || s.firstFrameImage || s.last_frame_image || s.lastFrameImage || s.composed_image || s.composedImage).length)
  const shotVidCount = computed(() => sbs.value.filter(s => s.video_url || s.videoUrl).length)
  const totalDuration = computed(() => sbs.value.reduce((s, sb) => s + (sb.duration || 10), 0))
  const lockedImageConfigId = computed(() => episode.value?.image_config_id || episode.value?.imageConfigId || null)
  const lockedVideoConfigId = computed(() => episode.value?.video_config_id || episode.value?.videoConfigId || null)
  const lockedAudioConfigId = computed(() => episode.value?.audio_config_id || episode.value?.audioConfigId || null)
  const lockedAudioProvider = computed(() => audioConfigs.value.find(c => c.id === lockedAudioConfigId.value)?.provider || '')
  const lockedImageConfigLabel = computed(() => configLabel(imageConfigs.value.find(c => c.id === lockedImageConfigId.value)))
  const lockedVideoConfigLabel = computed(() => configLabel(videoConfigs.value.find(c => c.id === lockedVideoConfigId.value)))
  const lockedAudioConfigLabel = computed(() => configLabel(audioConfigs.value.find(c => c.id === lockedAudioConfigId.value)))

  const voiceSelectOptions = computed(() => voiceProfiles.value.map(v => ({ label: `${v.label} · ${v.traits}`, value: v.id })))
  const videoConfigSelectOptions = computed(() => videoConfigs.value.map(c => {
    let modelName = ''
    try { const m = JSON.parse(c.model || '[]'); modelName = Array.isArray(m) ? (m[0] || '') : (m || '') } catch { modelName = c.model || '' }
    const label = modelName ? `${modelName} (${c.provider})` : `${c.name} (${c.provider})`
    return { label, value: c.id }
  }))

  const mainSteps = computed(() => [
    { id: 1, num: '1', label: '剧本导入', desc: '内容改写与分镜拆解', done: !!scriptContent.value && !!sbs.value.length },
    { id: 2, num: '2', label: '角色+场景+道具', desc: '提取并生成素材图', done: !!chars.value.length && charImgCount.value === visualCharTotal.value && sceneImgCount.value === scenes.value.length },
    { id: 3, num: '3', label: '分集视频', desc: '视频生成与导出', done: !!mergeUrl.value },
  ])

  const step1Tabs = computed(() => [
    { key: 'raw', label: '原始内容', done: !!rawContent.value },
    { key: 'rewrite', label: 'AI 改写', done: !!scriptContent.value },
    { key: 'storyboard', label: '分镜拆解', done: !!sbs.value.length },
  ])

  const step3Tabs = computed(() => [
    { id: 'videos', label: '视频生成', badge: shotVidCount.value ? `${shotVidCount.value}/${sbs.value.length}` : '' },
    { id: 'compose', label: '视频合成', badge: composedCount.value ? `${composedCount.value}/${sbs.value.length}` : '' },
    { id: 'export', label: '导出', badge: mergeUrl.value ? '完成' : '' },
  ])
  // ─── functions ───

  function configLabel(config: any) {
    if (!config) return '未配置'
    let modelName = ''
    try { const m = JSON.parse(config.model || '[]'); modelName = Array.isArray(m) ? (m[0] || '') : (m || '') } catch { modelName = config.model || '' }
    return modelName ? `${config.name} · ${modelName} (${config.provider})` : `${config.name} (${config.provider})`
  }

  function openImageViewer(src: string, title = '') {
    if (!src) return
    imageViewer.value = { open: true, src, title }
  }

  function closeImageViewer() {
    imageViewer.value = { open: false, src: '', title: '' }
  }

  function _handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape' && imageViewer.value.open) closeImageViewer()
  }

  function getFirstFrame(s: any) { return s?.first_frame_image || s?.firstFrameImage || null }
  function getLastFrame(s: any) { return s?.last_frame_image || s?.lastFrameImage || null }
  function getStoryboardCover(s: any) { return s?.composed_image || s?.composedImage || getFirstFrame(s) || getLastFrame(s) || null }
  function getVideoUrl(s: any) { return s?.video_url || s?.videoUrl || null }
  function getComposedVideoUrl(s: any) { return s?.composed_video_url || s?.composedVideoUrl || null }
  function hasImg(s: any) { return !!getStoryboardCover(s) }
  function hasVid(s: any) { return !!getVideoUrl(s) }
  function hasComposed(s: any) { return !!getComposedVideoUrl(s) }
  const IGNORE_TTS_SPEAKERS = /^(环境音|环境声|音效|效果音|sfx|sound ?effect|bgm|背景音|背景音乐|ambient)$/i
  const IGNORE_TTS_TEXT = /^(无|无对白|无台词|无旁白|无需配音|无需对白|none|null|n\/a|na|环境音|环境声|音效|效果音|纯音效|纯环境音|只有环境音|仅环境音|背景音|背景音乐|bgm|sfx|ambient)$/i

  function getDialogueSpeakerRaw(sb: any) {
    const dialogue = sb?.dialogue?.trim() || ''
    const match = dialogue.match(/^(.+?)[:：]/)
    return match ? match[1].replace(/[（(].+?[)）]/g, '').trim() : ''
  }

  function getDialogueText(sb: any) {
    const dialogue = sb?.dialogue?.trim() || ''
    return dialogue ? dialogue.replace(/^.+?[:：]\s*/, '').trim() : ''
  }

  function isTTSIgnorable(sb: any) {
    const speaker = getDialogueSpeakerRaw(sb)
    const text = getDialogueText(sb)
    if (!sb?.dialogue?.trim()) return true
    if (speaker && IGNORE_TTS_SPEAKERS.test(speaker)) return true
    if (!text) return true
    if (IGNORE_TTS_TEXT.test(text)) return true
    return false
  }

  function hasDialogue(sb: any) { return !isTTSIgnorable(sb) }
  function hasTTS(sb: any) { return !!(sb?.tts_audio_url || sb?.ttsAudioUrl) }
  function getTTSUrl(sb: any) { return sb?.tts_audio_url || sb?.ttsAudioUrl || '' }

  function getDialogueSpeaker(sb: any) {
    const speaker = getDialogueSpeakerRaw(sb)
    if (!speaker) return '旁白'
    return speaker
  }

  function getStoryboardCharacterIds(sb: any) {
    return sb?.character_ids || sb?.characterIds || []
  }

  function getStoryboardCharacterNames(sb: any) {
    const ids = getStoryboardCharacterIds(sb)
    return chars.value.filter(char => ids.includes(char.id)).map(char => char.name)
  }

  function getSceneName(sb: any) {
    const sceneId = sb?.scene_id || sb?.sceneId
    if (!sceneId) return '未绑定场景'
    const scene = scenes.value.find(s => s.id === sceneId)
    return scene ? `${scene.location} · ${scene.time || '未设时间'}` : `场景 #${sceneId}`
  }

  function sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
  // ─── data loading ───

  async function refresh() {
    if (!dramaId || isNaN(dramaId)) { console.error('useEpisodeStudio: invalid dramaId', dramaId); return }
    try {
      drama.value = await dramaAPI.get(dramaId)
      const ep = drama.value.episodes?.find((e: any) => (e.episode_number || e.episodeNumber) === episodeNumber)
      if (ep) {
        episode.value = ep
        try { chars.value = await episodeAPI.characters(ep.id) } catch { chars.value = [] }
        try { scenes.value = await episodeAPI.scenes(ep.id) } catch { scenes.value = [] }
        try { dramaProps.value = await propAPI.list(dramaId) } catch { dramaProps.value = [] }
        sbs.value = await episodeAPI.storyboards(ep.id)
        if (sbs.value.length && !selectedSb.value) selectedSb.value = sbs.value[0]

        const epHasContent = !!(episode.value?.content)
        const epHasScript = !!(episode.value?.script_content || episode.value?.scriptContent)
        const epHasSbs = sbs.value.length > 0

        if (epHasSbs) { step.value = 1; subTab.value = 'storyboard' }
        else if (epHasScript && chars.value.length) { step.value = 2; subTab.value = 'extract' }
        else if (epHasScript || epHasContent) { step.value = 1; subTab.value = 'rewrite' }
        else { step.value = 1; subTab.value = 'raw' }
        await loadLatestGridImage()
      }
    } catch (e: any) {
      toast.error(e.message)
    }
    try { mergeData.value = await mergeAPI.status(epId.value) } catch {}
  }

  async function loadLatestGridImage() {
    try {
      const rows = await imageAPI.list({ drama_id: dramaId })
      const list = Array.isArray(rows) ? rows : []
      const grids = list
        .filter((row: any) => row?.status === 'completed' && String(row?.frame_type || row?.frameType || '').startsWith('grid_') && (row?.local_path || row?.localPath))
        .sort((a: any, b: any) => Number(b?.id || 0) - Number(a?.id || 0))
      if (grids.length) {
        // just keep the latest grid image path available for reference
      }
    } catch {}
  }
  async function loadConfigs() {
    try {
      const [imgCfgs, vidCfgs, audCfgs] = await Promise.all([
        aiConfigAPI.list('image'),
        aiConfigAPI.list('video'),
        aiConfigAPI.list('audio'),
      ])
      imageConfigs.value = imgCfgs || []
      videoConfigs.value = vidCfgs || []
      audioConfigs.value = audCfgs || []
    } catch (e) { console.error('Failed to load AI configs', e) }
  }

  function inferVoiceGender(name: string, desc: string[] = []) {
    const text = `${name} ${Array.isArray(desc) ? desc.join(' ') : ''}`
    if (/[男|青年|大爷|学长|boy|man|male]/i.test(text)) return '男声'
    if (/[女|少女|御姐|奶奶|girl|woman|female]/i.test(text)) return '女声'
    return '中性'
  }

  function mapVoiceProfile(v: any) {
    const desc = Array.isArray(v.description) ? v.description : []
    return {
      id: v.voice_id,
      label: v.voice_name || v.voice_id,
      gender: inferVoiceGender(v.voice_name || v.voice_id, desc),
      traits: desc.length ? desc.slice(0, 2).join('、') : `${v.language || '多语言'}音色`,
      suitable: desc.length > 2 ? desc.slice(2).join('、') : `${v.language || '通用'}角色`,
    }
  }

  async function loadVoices() {
    try {
      const provider = lockedAudioProvider.value || 'minimax'
      const rows = await voicesAPI.list(provider)
      voiceProfiles.value = rows?.length ? rows.map(mapVoiceProfile) : fallbackVoiceProfiles
    } catch (e) {
      console.error('Failed to load voices', e)
      voiceProfiles.value = fallbackVoiceProfiles
    }
  }
  // ─── watchers ───

  watch(step, (val) => {
    if (val === 1) subTab.value = scriptContent.value ? (sbs.value.length ? 'storyboard' : 'rewrite') : 'raw'
    else if (val === 3) subTab.value = 'videos'
  })

  watch([lockedAudioConfigId, audioConfigs], () => { loadVoices() }, { deep: true })

  // ─── return ───
  return {
    // state
    drama, episode, chars, scenes, sbs, mergeData, dramaProps,
    step, subTab, selectedSb,
    imageViewer,
    imageConfigs, videoConfigs, audioConfigs,
    voiceProfiles, fallbackVoiceProfiles,

    // computed
    epId, rawContent, scriptContent,
    charsVoiced, voiceSampleCount, composedCount, mergeUrl,
    visualChars, visualCharTotal,
    charImgCount, sceneImgCount,
    ttsEligibleCount, ttsGeneratedCount,
    shotImgCount, shotVidCount,
    totalDuration,
    lockedImageConfigId, lockedVideoConfigId, lockedAudioConfigId,
    lockedAudioProvider,
    lockedImageConfigLabel, lockedVideoConfigLabel, lockedAudioConfigLabel,
    voiceSelectOptions, videoConfigSelectOptions,
    mainSteps, step1Tabs, step3Tabs,

    // functions
    refresh, loadConfigs, loadVoices,
    configLabel, isNarratorCharacter,
    openImageViewer, closeImageViewer,
    getFirstFrame, getLastFrame, getStoryboardCover,
    getVideoUrl, getComposedVideoUrl,
    hasImg, hasVid, hasComposed,
    hasDialogue, hasTTS, getTTSUrl,
    getDialogueSpeaker, getDialogueText,
    getStoryboardCharacterIds, getStoryboardCharacterNames,
    getSceneName,
    sleep,

    // internal (for lifecycle)
    _handleKeydown,
    _initialized: false,
  }
}