<template>
  <div class="line" :style="{ '--sag': sag + 'px' }">
    <svg class="line-rope" viewBox="0 0 1000 140" preserveAspectRatio="none" aria-hidden="true">
      <path :d="`M-10 ${TOP} Q500 ${TOP + sag * 2} 1010 ${TOP}`" />
    </svg>

    <router-link
      v-for="(p, i) in visible"
      :key="p.id"
      :to="`/product/${p.id}`"
      class="mini"
      :style="tagStyle(i)"
    >
      <span class="mini-peg" aria-hidden="true"></span>
      <span class="mini-card">
        <span class="mini-hole" aria-hidden="true"></span>
        <span class="mini-photo-wrap">
          <span class="mini-photo">
            <ProductImage :src="p.imageUrl" :name="p.name" />
          </span>
          <PriceSticker class="mini-price" :price="p.price" size="sm" />
        </span>
        <span class="mini-name">{{ p.name }}</span>
      </span>
    </router-link>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import ProductImage from './ProductImage.vue'
import PriceSticker from './PriceSticker.vue'

const props = defineProps({
  products: { type: Array, default: () => [] },
  max: { type: Number, default: 5 }
})

const TOP = 16
const sag = 44

const slots = ref(props.max)
let mq = []

function updateSlots() {
  if (window.matchMedia('(max-width: 560px)').matches) slots.value = Math.min(2, props.max)
  else if (window.matchMedia('(max-width: 900px)').matches) slots.value = Math.min(3, props.max)
  else slots.value = props.max
}

onMounted(() => {
  mq = ['(max-width: 560px)', '(max-width: 900px)'].map(q => window.matchMedia(q))
  mq.forEach(m => m.addEventListener('change', updateSlots))
  updateSlots()
})
onBeforeUnmount(() => mq.forEach(m => m.removeEventListener('change', updateSlots)))

const visible = computed(() => props.products.slice(0, slots.value))

function tagStyle(i) {
  const n = visible.value.length
  const t = 0.1 + 0.8 * ((i + 0.5) / n)
  const y = TOP + 4 * sag * t * (1 - t)
  return {
    left: `${t * 100}%`,
    top: `${y}px`,
    '--delay': `${150 + i * 110}ms`,
    '--sway': `${3.4 + (i % 3) * 0.6}s`,
    '--tilt': `${[-3, 2, -1.5, 3, -2][i % 5]}deg`
  }
}
</script>

<style scoped>
.line {
  position: relative;
  height: 330px;
}

.line-rope {
  position: absolute;
  inset: 0 0 auto 0;
  width: 100%;
  height: 140px;
  overflow: visible;
}
.line-rope path {
  fill: none;
  stroke: rgba(255, 255, 255, 0.75);
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
  stroke-dasharray: 1400;
  stroke-dashoffset: 1400;
  animation: rope 0.9s ease-out forwards;
}

@keyframes rope {
  to { stroke-dashoffset: 0; }
}

.mini {
  position: absolute;
  width: 156px;
  margin-left: -78px;
  transform-origin: 50% 0;
  rotate: var(--tilt);
  animation:
    drop 0.9s var(--ease-swing) var(--delay) both,
    sway var(--sway) ease-in-out calc(var(--delay) + 0.9s) infinite alternate;
  color: var(--ink);
}

@keyframes drop {
  from { translate: 0 -70px; opacity: 0; }
  to { translate: 0 0; opacity: 1; }
}

@keyframes sway {
  from { rotate: var(--tilt); }
  to { rotate: calc(var(--tilt) * -1); }
}

.mini:hover,
.mini:focus-visible {
  animation-play-state: running, paused;
}
.mini:focus-visible {
  outline-color: var(--highlight);
}

.mini-peg {
  position: absolute;
  top: -10px;
  left: 50%;
  width: 12px;
  height: 26px;
  margin-left: -6px;
  border-radius: 3px;
  background: var(--highlight);
  box-shadow: inset -3px 0 0 rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.25);
  z-index: 2;
}

.mini-card {
  position: relative;
  display: block;
  margin-top: 8px;
  padding: 24px 10px 12px;
  background: var(--paper);
  border-radius: 10px;
  clip-path: polygon(22px 0, calc(100% - 22px) 0, 100% 22px, 100% 100%, 0 100%, 0 22px);
  transition: translate 0.3s var(--ease-swing);
}
.mini:hover .mini-card {
  translate: 0 -4px;
}

.mini-hole {
  position: absolute;
  top: 7px;
  left: 50%;
  width: 10px;
  height: 10px;
  margin-left: -5px;
  border-radius: 50%;
  background: var(--ultra);
  box-shadow: 0 0 0 2.5px #c9ccdf;
}

.mini-photo-wrap {
  position: relative;
  display: block;
}

.mini-photo {
  display: block;
  aspect-ratio: 1 / 1;
  border-radius: 6px;
  overflow: hidden;
}

.mini-name {
  display: block;
  margin-top: 14px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-price {
  position: absolute;
  right: -2px;
  bottom: -12px;
}

@media (max-width: 560px) {
  .line {
    height: 290px;
  }
  .mini {
    width: 136px;
    margin-left: -68px;
  }
}
</style>
