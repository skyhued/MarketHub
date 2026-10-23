<template>
  <span class="sticker" :class="`sticker--${size}`" :aria-label="`价格 ${money(price)} 元`">
    <span class="sticker-yen" aria-hidden="true">¥</span>
    <span class="sticker-num" aria-hidden="true">{{ stickerPrice(price) }}</span>
  </span>
</template>

<script setup>
import { money, stickerPrice } from '@/utils/format'

defineProps({
  price: { type: [Number, String], required: true },
  size: { type: String, default: 'md' }
})
</script>

<style scoped>
.sticker {
  --d: 68px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 1px;
  min-width: var(--d);
  height: var(--d);
  padding: 0 10px;
  border-radius: calc(var(--d) / 2);
  background:
    radial-gradient(circle at 32% 28%, rgba(255, 255, 255, 0.35), transparent 42%),
    var(--sticker);
  color: var(--ink);
  box-shadow: 0 2px 0 rgba(21, 21, 46, 0.18), 0 6px 14px -6px rgba(201, 58, 10, 0.6);
  transform: rotate(-9deg);
  line-height: 1;
  white-space: nowrap;
}

.sticker-yen {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: calc(var(--d) * 0.2);
  margin-top: calc(var(--d) * -0.12);
}

.sticker-num {
  font-family: var(--font-marker);
  font-size: calc(var(--d) * 0.34);
  letter-spacing: -0.02em;
}

.sticker--sm { --d: 52px; padding: 0 8px; }
.sticker--lg { --d: 104px; padding: 0 18px; }
</style>
