<template>
  <div class="studio" v-if="drama">
    <header class="studio-topbar">
      <div class="studio-topbar-main">
        <button class="back-btn topbar-back" @click="navigateTo(`/drama/${dramaId}`)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>
          </svg>
          返回项目
        </button>
        <div class="studio-identity">
          <h1 class="studio-title">{{ drama.title }}</h1>
          <span class="studio-episode-chip">第 {{ episodeNumber }} 集</span>
          <div class="studio-meta-row">
            <span class="studio-meta-inline">{{ chars.length }} 角色 · {{ scenes.length }} 场景 · {{ sbs.length }} 镜头</span>
          </div>
        </div>
      </div>
      <div class="studio-topbar-side">
        <div class="studio-actions">
          <button class="btn" @click="refresh">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            刷新
          </button>
        </div>
      </div>
    </header>

    <div class="studio-body">
      <aside class="sidebar">
        <nav class="pipeline">
          <button
            v-for="s in mainSteps"
            :key="s.id"
            :class="['pipe-step', { active: step === s.id, done: s.done }]"
            @click="step = s.id"
          >
            <span class="pipe-step-num">
              <svg v-if="s.done" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
              <span v-else>{{ s.num }}</span>
            </span>
            <span class="pipe-step-copy">
              <span class="pipe-step-label">{{ s.label }}</span>
              <span class="pipe-step-desc">{{ s.desc }}</span>
            </span>
          </button>
        </nav>
        <div class="sidebar-bottom">
          <button class="refresh-btn" @click="refresh">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            刷新数据
          </button>
        </div>
      </aside>

      <main class="main">
        <component :is="currentStepComponent" />

        <div v-if="imageViewer.open && imageViewer.src" class="overlay image-viewer-overlay" @click.self="closeImageViewer">
          <div class="card image-viewer-dialog">
            <div class="image-viewer-head">
              <div class="image-viewer-title">{{ imageViewer.title || '图片预览' }}</div>
              <button class="btn btn-ghost btn-icon" @click="closeImageViewer">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="image-viewer-body">
              <img :src="imageViewer.src" :alt="imageViewer.title || '图片预览'" class="image-viewer-img" />
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { useEpisodeStudio } from '~/composables/useEpisodeStudio'
import StepScript from '~/components/studio/StepScript.vue'
import StepAssets from '~/components/studio/StepAssets.vue'
import StepVideo from '~/components/studio/StepVideo.vue'

definePageMeta({ layout: 'studio' })

const route = useRoute()
const dramaId = Number(route.params.id)
const episodeNumber = Number(route.params.episodeNumber)

const {
  drama, chars, scenes, sbs,
  step, mainSteps,
  imageViewer, closeImageViewer,
  refresh,
} = useEpisodeStudio(dramaId, episodeNumber)

const stepComponents = { 1: StepScript, 2: StepAssets, 3: StepVideo }
const currentStepComponent = computed(() => stepComponents[step.value] || StepScript)
</script>

<style scoped>
.studio {
  display: flex; flex-direction: column; height: 100vh; overflow: hidden;
  padding: 14px; gap: 12px;
  background: radial-gradient(circle at top left, rgba(255,255,255,0.7), transparent 28%),
    linear-gradient(180deg, rgba(255,255,255,0.22), rgba(255,255,255,0)), var(--bg-base);
}
.studio-topbar {
  display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-shrink: 0;
  padding: 8px 12px; border-radius: 18px;
  background: rgba(252, 253, 255, 0.84); border: 1px solid rgba(27, 41, 64, 0.08);
  box-shadow: 0 14px 36px rgba(20, 32, 54, 0.07), 0 3px 10px rgba(20, 32, 54, 0.04);
  backdrop-filter: blur(16px);
}
.studio-topbar-main { display: flex; align-items: center; gap: 8px; min-width: 0; }
.topbar-back { width: auto; min-width: 76px; padding: 0 8px; height: 28px; border-radius: 999px; white-space: nowrap; font-size: 11px; }
.studio-identity { min-width: 0; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.studio-title { font-size: 14px; line-height: 1; letter-spacing: -0.04em; white-space: nowrap; }
.studio-episode-chip {
  display: inline-flex; align-items: center; height: 20px; padding: 0 7px; border-radius: 999px;
  background: rgba(19, 51, 121, 0.08); color: var(--accent-text); font-size: 9px; font-weight: 700;
}
.studio-meta-row { display: flex; align-items: center; gap: 4px; flex-wrap: nowrap; min-width: 0; }
.studio-meta-inline { font-size: 9px; color: var(--text-3); font-weight: 600; white-space: nowrap; }
.studio-topbar-side { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.studio-actions { display: flex; gap: 6px; }
.studio-topbar .btn { height: 28px; padding: 0 10px; font-size: 11px; white-space: nowrap; }
.studio-body { display: grid; grid-template-columns: 244px minmax(0, 1fr); gap: 10px; min-height: 0; flex: 1; }

.sidebar, .main {
  background: rgba(252, 253, 255, 0.84); border: 1px solid rgba(27, 41, 64, 0.08);
  box-shadow: 0 18px 48px rgba(20, 32, 54, 0.08), 0 4px 14px rgba(20, 32, 54, 0.05);
  backdrop-filter: blur(16px);
}
.sidebar { width: auto; flex-shrink: 0; display: flex; flex-direction: column; overflow: hidden; min-height: 0; border-radius: 28px; }
.back-btn {
  width: 40px; height: 40px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
  border: 1px solid rgba(27, 41, 64, 0.1); border-radius: 14px; background: rgba(255,255,255,0.8);
  color: var(--text-2); cursor: pointer; transition: all 0.15s; box-shadow: var(--shadow-xs);
}
.back-btn:hover { background: #fff; color: var(--text-0); }
.pipeline { flex: 1; overflow-y: auto; padding: 16px 14px 12px; display: flex; flex-direction: column; gap: 12px; }
.pipe-step {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px;
  background: transparent; border: 1px solid transparent; cursor: pointer; transition: all 0.15s;
  text-align: left; width: 100%; color: var(--text-2);
}
.pipe-step:hover { background: rgba(255,255,255,0.3); color: var(--text-0); }
.pipe-step.active { background: rgba(255,255,255,0.7); border-color: rgba(19, 51, 121, 0.1); color: var(--text-0); box-shadow: 0 2px 8px rgba(20,32,54,0.06); }
.pipe-step.done { color: var(--success); }
.pipe-step-num {
  width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; font-family: var(--font-mono);
  background: var(--bg-2); border: 1px solid var(--border); color: var(--text-3); flex-shrink: 0;
}
.pipe-step.active .pipe-step-num { background: var(--accent-bg); border-color: rgba(19,51,121,0.15); color: var(--accent-text); }
.pipe-step.done .pipe-step-num { background: rgba(45,122,69,0.96); border-color: rgba(45,122,69,0.18); color: #fff; }
.pipe-step-copy { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.pipe-step-label { font-size: 12px; font-weight: 600; }
.pipe-step-desc { font-size: 10px; color: var(--text-3); }
.sidebar-bottom {
  padding: 12px 14px 14px; border-top: 1px solid rgba(27, 41, 64, 0.08);
  display: flex; flex-direction: column; gap: 8px; flex-shrink: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.12), rgba(255,255,255,0.72));
}
.refresh-btn {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 7px 0; border-radius: 12px; font-size: 11px; font-weight: 600;
  background: rgba(255,255,255,0.7); border: 1px solid rgba(27, 41, 64, 0.08);
  color: var(--text-2); cursor: pointer; transition: all 0.15s;
}
.refresh-btn:hover { background: #fff; color: var(--text-0); }
.main { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; min-height: 0; border-radius: 30px; }

.image-viewer-overlay { z-index: 120; padding: 28px; background: rgba(18, 24, 34, 0.68); backdrop-filter: blur(10px); }
.image-viewer-dialog {
  width: min(1100px, calc(100vw - 56px)); max-height: calc(100vh - 56px);
  display: flex; flex-direction: column; overflow: hidden; border-radius: 24px;
  background: linear-gradient(180deg, rgba(255,255,255,0.96), rgba(248,251,255,0.92));
}
.image-viewer-head { display: flex; align-items: center; gap: 12px; padding: 16px 18px; border-bottom: 1px solid rgba(27, 41, 64, 0.08); }
.image-viewer-title { font-size: 14px; font-weight: 700; color: var(--text-1); font-family: var(--font-display); }
.image-viewer-body { display: flex; align-items: center; justify-content: center; padding: 20px; overflow: auto; min-height: 0; }
.image-viewer-img { display: block; max-width: 100%; max-height: calc(100vh - 140px); border-radius: 18px; box-shadow: 0 18px 48px rgba(8, 14, 24, 0.22); background: rgba(255,255,255,0.9); }

@media (max-width: 860px) {
  .studio-body { grid-template-columns: 1fr; }
  .sidebar { display: none; }
}
</style>
