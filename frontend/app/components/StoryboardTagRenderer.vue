<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text: string
  characters?: Array<{ id: number; name: string; image_url?: string }>
  scenes?: Array<{ id: number; location: string; image_url?: string }>
  propsList?: Array<{ id: number; name: string; image_url?: string }>
}>()

interface Segment {
  type: 'text' | 'role' | 'location' | 'prop'
  content: string
  name?: string
  imageUrl?: string
}

const segments = computed<Segment[]>(() => {
  if (!props.text) return []
  const result: Segment[] = []
  const regex = /<(role|location|prop)>(.*?)<\/\1>/g
  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(props.text)) !== null) {
    if (match.index > lastIndex) {
      result.push({ type: 'text', content: props.text.slice(lastIndex, match.index) })
    }
    const tagType = match[1] as 'role' | 'location' | 'prop'
    const tagContent = match[2]
    let name = tagContent
    let imageUrl: string | undefined

    if (tagType === 'role') {
      const cleanName = tagContent.includes('--') ? tagContent.split('--')[0] : tagContent
      name = cleanName
      const char = props.characters?.find(c => c.name === cleanName)
      imageUrl = char?.image_url || undefined
    } else if (tagType === 'location') {
      const scene = props.scenes?.find(s => s.location === tagContent)
      imageUrl = scene?.image_url || undefined
    } else if (tagType === 'prop') {
      const prop = props.propsList?.find(p => p.name === tagContent)
      imageUrl = prop?.image_url || undefined
    }

    result.push({ type: tagType, content: tagContent, name, imageUrl })
    lastIndex = match.index + match[0].length
  }

  if (lastIndex < props.text.length) {
    result.push({ type: 'text', content: props.text.slice(lastIndex) })
  }
  return result
})
</script>

<template>
  <span class="tag-renderer">
    <template v-for="(seg, i) in segments" :key="i">
      <span v-if="seg.type === 'text'">{{ seg.content }}</span>
      <span v-else-if="seg.type === 'role'" class="tag tag-role" :title="seg.content">
        <img v-if="seg.imageUrl" :src="seg.imageUrl" class="tag-thumb" />
        <span v-else class="tag-icon">👤</span>
        <span class="tag-name">{{ seg.name }}</span>
      </span>
      <span v-else-if="seg.type === 'location'" class="tag tag-location" :title="seg.content">
        <img v-if="seg.imageUrl" :src="seg.imageUrl" class="tag-thumb" />
        <span v-else class="tag-icon">🏠</span>
        <span class="tag-name">{{ seg.name }}</span>
      </span>
      <span v-else-if="seg.type === 'prop'" class="tag tag-prop" :title="seg.content">
        <img v-if="seg.imageUrl" :src="seg.imageUrl" class="tag-thumb" />
        <span v-else class="tag-icon">📦</span>
        <span class="tag-name">{{ seg.name }}</span>
      </span>
    </template>
  </span>
</template>

<style scoped>
.tag-renderer {
  line-height: 2.2;
}
.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  vertical-align: middle;
  cursor: pointer;
  font-size: 0.85em;
}
.tag-role {
  background: rgba(124, 58, 237, 0.15);
  border: 1px solid rgba(124, 58, 237, 0.4);
  color: #c4b5fd;
}
.tag-location {
  background: rgba(37, 99, 235, 0.15);
  border: 1px solid rgba(37, 99, 235, 0.4);
  color: #93c5fd;
}
.tag-prop {
  background: rgba(245, 158, 11, 0.15);
  border: 1px solid rgba(245, 158, 11, 0.4);
  color: #fcd34d;
}
.tag-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}
.tag-icon {
  font-size: 0.75em;
}
.tag-name {
  font-weight: 500;
}
</style>
