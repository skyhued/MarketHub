<template>
  <div class="home">
    <section class="hero">
      <div class="hero-inner">
        <form class="search" role="search" @submit.prevent="applyKeyword">
          <el-icon class="search-icon" aria-hidden="true"><Search /></el-icon>
          <input
            v-model="keywordInput"
            type="search"
            placeholder="搜索商品，如键盘、教材"
            aria-label="搜索商品名称或描述"
          />
          <button type="submit">搜索</button>
        </form>

        <p class="hero-sub">
          <template v-if="heroProducts.length">当前 <b>{{ heroProducts.length }}</b> 件在售</template>
          <template v-else>暂无在售商品</template>
        </p>
      </div>

      <div class="hero-line">
        <HangingTags :products="heroProducts" />
      </div>
    </section>

    <div class="page">
      <div class="filters">
        <div class="cats" role="group" aria-label="按分类筛选">
          <button
            type="button"
            class="cat"
            :class="{ active: filters.categoryId === null }"
            :aria-pressed="filters.categoryId === null"
            @click="pickCategory(null)"
          >全部</button>
          <button
            v-for="c in categories"
            :key="c.id"
            type="button"
            class="cat"
            :class="{ active: filters.categoryId === c.id }"
            :aria-pressed="filters.categoryId === c.id"
            @click="pickCategory(c.id)"
          >{{ c.name }}</button>
        </div>

        <div class="tools">
          <div class="price-range">
            <span class="tools-label">价格</span>
            <input v-model="filters.minPrice" type="number" min="0" placeholder="最低" aria-label="最低价格" />
            <span class="dash">–</span>
            <input v-model="filters.maxPrice" type="number" min="0" placeholder="最高" aria-label="最高价格" />
          </div>
          <el-select v-model="filters.sortBy" class="sort" aria-label="排序方式">
            <el-option label="最新上架" value="time_desc" />
            <el-option label="最早上架" value="time_asc" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
          </el-select>
        </div>
      </div>

      <div class="result-line">
        <span>
          <template v-if="keyword">“{{ keyword }}”</template>
          <template v-else>{{ activeCategoryName }}</template>
          <span class="result-count"> · {{ shown.length }} 件</span>
        </span>
        <button v-if="hasFilters" type="button" class="clear" @click="handleReset">清除筛选</button>
      </div>

      <TagLoader v-if="loading" text="正在理货" />

      <div v-else-if="shown.length" class="tag-grid">
        <ProductCard v-for="(product, i) in shown" :key="product.id" :product="product" :index="i" />
      </div>

      <EmptyState
        v-else
        :title="hasFilters ? '没有符合条件的商品' : '暂无在售商品'"
        :hint="hasFilters ? '换个关键词，或放宽价格范围' : ''"
      >
        <el-button v-if="hasFilters" @click="handleReset">清除筛选</el-button>
      </EmptyState>

      <section v-if="userStore.isLoggedIn && recommendList.length" class="recommend">
        <h2 class="section-title">猜你喜欢</h2>
        <p class="page-sub recommend-sub">根据你的浏览、收藏和购买记录推荐</p>
        <div class="tag-grid">
          <ProductCard v-for="(product, i) in recommendList" :key="'rec-' + product.id" :product="product" :index="i" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { searchProductsAPI, getCategoriesAPI, getRecommendAPI } from '@/api'
import ProductCard from '@/components/ProductCard.vue'
import HangingTags from '@/components/HangingTags.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const products = ref([])
const heroProducts = ref([])
const categories = ref([])
const recommendList = ref([])
const loading = ref(false)

const keywordInput = ref('')
const keyword = ref('')

const filters = reactive({
  categoryId: null,
  minPrice: '',
  maxPrice: '',
  sortBy: 'time_desc'
})

const activeCategoryName = computed(() =>
  categories.value.find(c => c.id === filters.categoryId)?.name || '全部在售'
)

const hasFilters = computed(() =>
  !!keyword.value || filters.categoryId !== null || filters.minPrice !== '' || filters.maxPrice !== ''
)

function toPrice(v) {
  if (v === '' || v === null) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

/** 价格区间和排序在前端做，改了立刻生效，不用再点搜索。 */
const shown = computed(() => {
  const min = toPrice(filters.minPrice)
  const max = toPrice(filters.maxPrice)
  const list = products.value.filter(p => {
    const price = Number(p.price)
    return (min === null || price >= min) && (max === null || price <= max)
  })
  const byTime = (a, b) => String(a.createTime).localeCompare(String(b.createTime))
  const sorters = {
    time_desc: (a, b) => byTime(b, a),
    time_asc: byTime,
    price_asc: (a, b) => a.price - b.price,
    price_desc: (a, b) => b.price - a.price
  }
  return list.sort(sorters[filters.sortBy])
})

async function loadCategories() {
  try {
    const res = await getCategoriesAPI()
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    if (filters.categoryId !== null) params.categoryId = filters.categoryId
    const res = await searchProductsAPI(params)
    products.value = res.data || []
    // 首屏的晾绳只挂不带筛选条件的最新商品
    if (!params.keyword && params.categoryId === undefined) {
      heroProducts.value = products.value
    }
  } catch (e) {
    products.value = []
  } finally {
    loading.value = false
  }
}

async function loadRecommend() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getRecommendAPI()
    recommendList.value = res.data || []
  } catch (e) {
    recommendList.value = []
  }
}

function applyKeyword() {
  keyword.value = keywordInput.value.trim()
  loadProducts()
}

function pickCategory(id) {
  if (filters.categoryId === id) return
  filters.categoryId = id
  loadProducts()
}

function handleReset() {
  keyword.value = ''
  keywordInput.value = ''
  filters.categoryId = null
  filters.minPrice = ''
  filters.maxPrice = ''
  filters.sortBy = 'time_desc'
  loadProducts()
}

onMounted(() => {
  loadCategories()
  loadProducts()
  loadRecommend()
})
</script>

<style scoped>
.hero {
  position: relative;
  display: flow-root;
  background-color: var(--ultra);
  background-image: radial-gradient(circle at center, rgba(255, 255, 255, 0.09) 1.6px, transparent 2px);
  background-size: 26px 26px;
  color: #fff;
  padding-top: 48px;
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.hero-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
  animation: rise 0.6s ease 0.1s both;
}
.hero-sub b {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 18px;
  color: var(--highlight);
  padding: 0 2px;
}

.search {
  display: flex;
  align-items: center;
  width: min(720px, 100%);
  height: 60px;
  padding: 0 6px 0 20px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 0 var(--ultra-deep);
  animation: rise 0.6s ease both;
}
.search:focus-within {
  outline: 3px solid var(--highlight);
  outline-offset: 3px;
}
.search-icon {
  font-size: 20px;
  color: var(--muted);
  flex-shrink: 0;
}
.search input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 12px;
  border: 0;
  outline: 0;
  background: transparent;
  font: inherit;
  font-size: 16px;
  color: var(--ink);
}
.search input::placeholder {
  color: var(--faint);
}
.search button {
  height: 44px;
  padding: 0 20px;
  border: 0;
  border-radius: 10px;
  background: var(--ink);
  color: #fff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}
.search button:hover {
  background: var(--ultra-deep);
}

.hero-line {
  max-width: 1200px;
  margin: 8px auto -96px;
  padding: 0 24px;
  position: relative;
  z-index: 2;
}

.page {
  padding-top: 112px;
}

.filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px 24px;
  flex-wrap: wrap;
  padding: 14px;
  background: var(--paper);
  border-radius: 16px;
  box-shadow: var(--shadow-tag);
}

.cats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cat {
  height: 36px;
  padding: 0 16px;
  border: 1.5px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: var(--ink-soft);
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, transform 0.2s var(--ease-swing);
}
.cat:hover {
  border-color: var(--ultra);
  color: var(--ultra);
}
.cat.active {
  background: var(--highlight);
  border-color: var(--ink);
  color: var(--ink);
  transform: rotate(-2deg);
}

.tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.tools-label {
  font-size: 13px;
  color: var(--muted);
  margin-right: 4px;
}
.price-range {
  display: flex;
  align-items: center;
  gap: 6px;
}
.price-range input {
  width: 78px;
  height: 36px;
  padding: 0 10px;
  border: 1.5px solid var(--line);
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
  color: var(--ink);
  background: #fff;
  -moz-appearance: textfield;
}
.price-range input::-webkit-outer-spin-button,
.price-range input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}
.price-range input:focus {
  border-color: var(--ultra);
  outline: 0;
}
.dash {
  color: var(--faint);
}
.sort {
  width: 150px;
}
.sort :deep(.el-select__wrapper) {
  min-height: 36px;
}

.result-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 28px 4px 4px;
  font-family: var(--font-display);
  font-size: 24px;
}
.result-count {
  font-family: var(--font-body);
  font-size: 15px;
  color: var(--muted);
}
.clear {
  border: 0;
  background: none;
  font: inherit;
  font-size: 14px;
  color: var(--ultra);
  cursor: pointer;
}
.clear:hover {
  text-decoration: underline;
}

.recommend {
  margin-top: 72px;
}
.recommend .section-title {
  margin-bottom: 0;
}
.recommend-sub {
  margin-bottom: 8px;
}

@media (max-width: 720px) {
  .hero {
    padding-top: 32px;
  }
  .hero-inner {
    padding: 0 16px;
    gap: 12px;
  }
  .hero-line {
    padding: 0 16px;
  }
  .filters {
    padding: 12px;
  }
  .cats {
    flex-wrap: nowrap;
    overflow-x: auto;
    width: 100%;
    padding-bottom: 2px;
  }
  .cat {
    flex-shrink: 0;
  }
}
</style>
