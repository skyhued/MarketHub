<template>
  <div class="tag" :style="{ '--i': index }">
    <div
      class="tag-swing"
      :class="{ 'is-swinging': swinging }"
      @mouseenter="swinging = true"
      @animationend="swinging = false"
    >
      <span class="tag-pin" aria-hidden="true"></span>
      <router-link :to="`/product/${product.id}`" class="tag-paper">
        <span class="tag-bg" aria-hidden="true"></span>
        <span class="tag-hole" aria-hidden="true"></span>

        <div class="tag-photo-wrap">
          <div class="tag-photo">
            <ProductImage :src="product.imageUrl" :name="product.name" />
            <span v-if="stamp" class="tag-stamp stamp" :class="stamp.tone">{{ stamp.text }}</span>
          </div>
          <PriceSticker class="tag-price" :price="product.price" :size="compact ? 'sm' : 'md'" />
        </div>

        <div class="tag-text">
          <h3 class="tag-name">{{ product.name }}</h3>
          <p class="tag-meta">
            <span v-if="product.categoryName">{{ product.categoryName }}</span>
            <span v-if="stockText" :class="{ 'is-low': product.stock <= 3 }">{{ stockText }}</span>
          </p>
        </div>
      </router-link>
    </div>

    <div v-if="$slots.actions" class="tag-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ProductImage from './ProductImage.vue'
import PriceSticker from './PriceSticker.vue'

const props = defineProps({
  product: { type: Object, required: true },
  index: { type: Number, default: 0 },
  compact: { type: Boolean, default: false }
})

const swinging = ref(false)

/** 售罄、下架、审核中的商品盖一个章；列表里大多是在售商品，不盖 */
const stamp = computed(() => {
  const p = props.product
  if (p.auditStatus === 0) return { text: '待审核', tone: 'stamp--blue' }
  if (p.auditStatus === 2) return { text: '未通过', tone: '' }
  if (p.status === 0) return { text: '已下架', tone: 'stamp--grey' }
  if (p.stock != null && p.stock <= 0) return { text: '已售罄', tone: '' }
  return null
})

const stockText = computed(() => {
  const stock = props.product.stock
  if (stock == null || stock <= 0) return ''
  return stock <= 3 ? `仅剩 ${stock} 件` : `库存 ${stock}`
})
</script>

<style scoped>
.tag {
  position: relative;
  animation: rise 0.5s ease both;
  animation-delay: calc(min(var(--i), 12) * 45ms);
}

.tag-swing {
  position: relative;
  transform-origin: 50% -14px;
}
.tag-swing.is-swinging {
  animation: swing 1.1s ease-out;
}

@keyframes swing {
  0% { rotate: 0deg; }
  18% { rotate: 4.5deg; }
  38% { rotate: -3deg; }
  58% { rotate: 1.6deg; }
  78% { rotate: -0.6deg; }
  100% { rotate: 0deg; }
}

.tag-pin {
  position: absolute;
  left: 50%;
  top: -18px;
  width: 2px;
  height: 36px;
  margin-left: -1px;
  background: linear-gradient(#6d7092, #9a9cb8);
  z-index: 2;
}
.tag-pin::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 50%;
  width: 10px;
  height: 10px;
  margin-left: -5px;
  border-radius: 50%;
  background: var(--ink-soft);
  box-shadow: inset -2px -2px 0 rgba(0, 0, 0, 0.25);
}

.tag-paper {
  position: relative;
  display: block;
  padding: 34px 12px 14px;
  color: var(--ink);
  border-radius: 12px;
}
.tag-paper:focus-visible {
  outline-offset: 4px;
}

.tag-bg {
  position: absolute;
  inset: 0;
  filter: drop-shadow(0 1px 0 rgba(21, 21, 46, 0.08)) drop-shadow(0 12px 14px rgba(21, 21, 46, 0.16));
  transition: filter 0.3s;
}
.tag-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--paper);
  border-radius: 0 0 12px 12px;
  clip-path: polygon(26px 0, calc(100% - 26px) 0, 100% 26px, 100% 100%, 0 100%, 0 26px);
}
.tag:hover .tag-bg {
  filter: drop-shadow(0 1px 0 rgba(21, 21, 46, 0.08)) drop-shadow(0 18px 18px rgba(21, 21, 46, 0.22));
}

.tag-hole {
  position: absolute;
  top: 10px;
  left: 50%;
  width: 14px;
  height: 14px;
  margin-left: -7px;
  border-radius: 50%;
  background: var(--board);
  box-shadow: 0 0 0 3px #c9ccdf, inset 0 1px 2px rgba(0, 0, 0, 0.2);
  z-index: 1;
}

.tag-photo {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  overflow: hidden;
}

.tag-photo :deep(img) {
  transition: transform 0.5s ease;
}
.tag:hover .tag-photo :deep(img) {
  transform: scale(1.05);
}

.tag-stamp {
  position: absolute;
  top: 50%;
  left: 50%;
  translate: -50% -50%;
  font-size: 22px;
}

.tag-text {
  position: relative;
  padding: 20px 4px 0;
}

.tag-name {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
  min-height: 2.8em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  padding-right: 8px;
}

.tag-meta {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--muted);
}
.tag-meta .is-low {
  color: var(--stamp);
  font-weight: 600;
}

.tag-photo-wrap {
  position: relative;
}

.tag-price {
  position: absolute;
  right: -16px;
  bottom: -20px;
  z-index: 3;
  transition: rotate 0.4s var(--ease-swing);
}
.tag:hover .tag-price {
  rotate: 6deg;
}

.tag-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  justify-content: center;
  margin-top: 12px;
}
</style>
