<template>
  <div class="auth">
    <section class="auth-side">
      <div class="auth-copy">
        <h1 class="auth-title"><slot name="title" /></h1>
        <p class="auth-sub"><slot name="sub" /></p>
      </div>
      <div class="auth-line">
        <HangingTags :products="products" :max="3" />
      </div>
    </section>

    <section class="auth-main">
      <div class="auth-card">
        <slot />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchProductsAPI } from '@/api'
import HangingTags from './HangingTags.vue'

const products = ref([])

onMounted(async () => {
  try {
    const res = await searchProductsAPI({})
    products.value = res.data || []
  } catch (e) {
    products.value = []
  }
})
</script>

<style scoped>
.auth {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  min-height: calc(100vh - 64px);
}

.auth-side {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 56px 48px 0;
  background-color: var(--ultra);
  background-image: radial-gradient(circle at center, rgba(255, 255, 255, 0.09) 1.6px, transparent 2px);
  background-size: 26px 26px;
  color: #fff;
  overflow: hidden;
}

.auth-title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(48px, 5.4vw, 80px);
  line-height: 1;
  animation: rise 0.6s ease both;
}
.auth-title :deep(em) {
  font-style: normal;
  color: var(--highlight);
}

.auth-sub {
  margin-top: 18px;
  max-width: 26em;
  font-size: 16px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.78);
  animation: rise 0.6s ease 0.1s both;
}

.auth-line {
  margin: 24px -24px -40px;
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.auth-card {
  width: min(420px, 100%);
  animation: rise 0.5s ease 0.1s both;
}

@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
    min-height: 0;
  }
  .auth-side {
    padding: 32px 20px 28px;
  }
  .auth-line {
    display: none;
  }
  .auth-main {
    padding: 32px 16px 56px;
  }
}
</style>
