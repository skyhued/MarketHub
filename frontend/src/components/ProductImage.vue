<template>
  <div class="pimg" :class="[`pimg--${tint}`, { 'pimg--compact': compact }]">
    <img
      v-if="src && !failed"
      :src="productImageUrl(src)"
      :alt="name"
      loading="lazy"
      @error="failed = true"
    />
    <div v-else class="pimg-sign" aria-hidden="true">
      <span>{{ compact ? (name || '物').charAt(0) : (name || '闲置好物') }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { productImageUrl, tintFor } from '@/utils/format'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})

const failed = ref(false)
watch(() => props.src, () => { failed.value = false })

const tint = computed(() => tintFor(props.name))
</script>

<style scoped>
.pimg {
  --tint-bg: #dcddfb;
  --tint-ink: #2b2bd9;
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--tint-bg);
  container-type: inline-size;
}
.pimg--blue { --tint-bg: #dcddfb; --tint-ink: #2b2bd9; }
.pimg--orange { --tint-bg: #ffe0d1; --tint-ink: #c93a0a; }
.pimg--yellow { --tint-bg: #fff1a8; --tint-ink: #7a5d00; }
.pimg--green { --tint-bg: #d4f2e1; --tint-ink: #0f6b3f; }
.pimg--pink { --tint-bg: #ffd9e8; --tint-ink: #b01a58; }
.pimg--lilac { --tint-bg: #e6dcfb; --tint-ink: #5f28b3; }

img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pimg-sign {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10%;
  text-align: center;
  color: var(--tint-ink);
  background-image:
    repeating-linear-gradient(-45deg, transparent 0 14px, rgba(255, 255, 255, 0.35) 14px 15px);
}

.pimg-sign span {
  font-family: var(--font-display);
  font-size: clamp(18px, 15cqi, 44px);
  line-height: 1.12;
  letter-spacing: 0.04em;
  display: block;
  max-height: 3.4em;
  overflow: hidden;
  text-wrap: balance;
  overflow-wrap: anywhere;
  transform: rotate(-4deg);
}

.pimg--compact .pimg-sign {
  padding: 0;
  background-image: none;
}
.pimg--compact .pimg-sign span {
  font-size: 56cqi;
  transform: none;
}
</style>
