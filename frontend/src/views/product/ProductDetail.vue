<template>
  <div class="page">
    <router-link to="/" class="link-back">
      <el-icon><ArrowLeft /></el-icon>返回列表
    </router-link>

    <TagLoader v-if="loading" />

    <template v-else-if="product">
      <div class="detail">
        <div class="big">
          <span class="big-pin" aria-hidden="true"></span>
          <div class="big-tag">
            <span class="big-hole" aria-hidden="true"></span>
            <div class="big-photo">
              <ProductImage :src="product.imageUrl" :name="product.name" />
              <span v-if="stampText" class="big-stamp stamp" :class="stampTone">{{ stampText }}</span>
            </div>
          </div>
        </div>

        <div class="info">
          <p class="info-kicker">
            <span class="info-cat">{{ product.categoryName || '未分类' }}</span>
            <span>{{ dateOnly(product.createTime) }} 上架</span>
          </p>
          <h1 class="info-name">{{ product.name }}</h1>

          <PriceSticker class="info-price" :price="product.price" size="lg" />

          <dl class="facts">
            <div>
              <dt>库存</dt>
              <dd :class="{ 'is-low': product.stock <= 3 }">{{ product.stock }}<small>件</small></dd>
            </div>
            <div>
              <dt>已售</dt>
              <dd>{{ product.salesCount ?? 0 }}<small>件</small></dd>
            </div>
            <div>
              <dt>浏览</dt>
              <dd>{{ product.viewCount ?? 0 }}<small>次</small></dd>
            </div>
            <div v-if="product.sellerName">
              <dt>卖家</dt>
              <dd class="facts-seller">{{ product.sellerName }}</dd>
            </div>
          </dl>

          <div class="buy">
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="Math.max(1, product.stock)"
              :disabled="!purchasable"
              size="large"
              class="buy-qty"
              aria-label="购买数量"
            />
            <el-button type="primary" size="large" class="buy-cart" :disabled="!purchasable" @click="handleAddToCart">
              <el-icon><ShoppingCart /></el-icon>
              <span>加入购物车</span>
            </el-button>
            <button
              type="button"
              class="fav"
              :class="{ on: isFavorited }"
              :aria-pressed="isFavorited"
              @click="handleToggleFavorite"
            >
              <el-icon><StarFilled v-if="isFavorited" /><Star v-else /></el-icon>
              {{ isFavorited ? '已收藏' : '收藏' }}
            </button>
          </div>
          <p v-if="blockedReason" class="buy-hint">{{ blockedReason }}</p>

          <router-link v-if="canEdit" :to="`/product/edit/${product.id}`" class="edit-link">
            <el-icon><EditPen /></el-icon>编辑商品 / 补货
          </router-link>
        </div>
      </div>

      <section class="note">
        <span class="note-tape" aria-hidden="true"></span>
        <h2 class="section-title">卖家说</h2>
        <p class="note-body">{{ product.description || '卖家没有留下描述。' }}</p>
      </section>

      <ReviewSection :product-id="product.id" />

      <section v-if="similarProducts.length" class="related">
        <h2 class="section-title">相关商品</h2>
        <div class="tag-grid related-grid">
          <ProductCard v-for="(p, i) in similarProducts" :key="'sim-' + p.id" :product="p" :index="i" compact />
        </div>
      </section>
    </template>

    <EmptyState v-else title="商品不存在" hint="可能已下架，或链接有误">
      <el-button type="primary" @click="router.push('/')">返回列表</el-button>
    </EmptyState>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Star, StarFilled, ShoppingCart, EditPen } from '@element-plus/icons-vue'
import { getProductByIdAPI, addToCartAPI, addFavoriteAPI, removeFavoriteAPI, checkFavoriteAPI, getRelatedProductsAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { dateOnly } from '@/utils/format'
import ReviewSection from '@/components/ReviewSection.vue'
import ProductCard from '@/components/ProductCard.vue'
import ProductImage from '@/components/ProductImage.vue'
import PriceSticker from '@/components/PriceSticker.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const product = ref(null)
const loading = ref(true)
const quantity = ref(1)
const isFavorited = ref(false)
const similarProducts = ref([])

/** 卖光了的商品照常展示（评价入口、相关推荐都还在），只是不能下单 */
const soldOut = computed(() => product.value?.stock <= 0)
const purchasable = computed(() =>
  product.value?.status === 1 && product.value?.auditStatus === 1 && !soldOut.value
)

/** 不能购买时提示原因；未上架商品只有主人和管理员能看到。 */
const blockedReason = computed(() => {
  const p = product.value
  if (!p) return ''
  if (p.auditStatus === 0) return '商品还在审核，通过后才会上架。'
  if (p.auditStatus === 2) return '商品没有通过审核，修改后可以重新提交。'
  if (p.status === 0) return '商品已下架，暂时不能购买。'
  if (soldOut.value) return '这件已经卖完了。'
  return ''
})

const stampText = computed(() => {
  const p = product.value
  if (p.auditStatus === 0) return '待审核'
  if (p.auditStatus === 2) return '未通过'
  if (p.status === 0) return '已下架'
  if (soldOut.value) return '已售罄'
  return ''
})
const stampTone = computed(() => {
  const p = product.value
  if (p.auditStatus === 0) return 'stamp--blue'
  if (p.status === 0 && p.auditStatus === 1) return 'stamp--grey'
  return ''
})

const canEdit = computed(() =>
  userStore.isAdmin || (userStore.hasPerm('product:update') && userStore.userId == product.value?.userId)
)

async function loadProduct() {
  loading.value = true
  try {
    const res = await getProductByIdAPI(route.params.id)
    product.value = res.data
  } catch (e) {
    product.value = null
  } finally {
    loading.value = false
  }
}

async function loadFavoriteStatus() {
  if (!userStore.isLoggedIn) {
    isFavorited.value = false
    return
  }
  try {
    const res = await checkFavoriteAPI(route.params.id)
    isFavorited.value = res.data.favorited
  } catch (e) {
    isFavorited.value = false
  }
}

function requireLogin() {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function handleAddToCart() {
  if (!requireLogin()) return
  try {
    await addToCartAPI({ productId: product.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch (e) {
  }
}

async function handleToggleFavorite() {
  if (!requireLogin()) return
  try {
    if (isFavorited.value) {
      await removeFavoriteAPI(product.value.id)
      ElMessage.success('已取消收藏')
      isFavorited.value = false
    } else {
      await addFavoriteAPI(product.value.id)
      ElMessage.success('已收藏')
      isFavorited.value = true
    }
  } catch (e) {
  }
}

async function loadSimilar() {
  try {
    const res = await getRelatedProductsAPI(route.params.id)
    similarProducts.value = res.data || []
  } catch (e) {
    similarProducts.value = []
  }
}

onMounted(() => {
  loadProduct()
  loadFavoriteStatus()
  loadSimilar()
})
</script>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 6fr);
  gap: 56px;
  align-items: start;
  padding-top: 20px;
}

.big {
  position: relative;
  padding-top: 28px;
  animation: rise 0.5s ease both;
}
.big-pin {
  position: absolute;
  top: 0;
  left: 50%;
  width: 2px;
  height: 52px;
  margin-left: -1px;
  background: #8b8fb0;
  z-index: 2;
}
.big-pin::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 50%;
  width: 14px;
  height: 14px;
  margin-left: -7px;
  border-radius: 50%;
  background: var(--ink-soft);
  box-shadow: inset -3px -3px 0 rgba(0, 0, 0, 0.25);
}

.big-tag {
  position: relative;
  padding: 48px 20px 20px;
  background: var(--paper);
  border-radius: 16px;
  clip-path: polygon(40px 0, calc(100% - 40px) 0, 100% 40px, 100% 100%, 0 100%, 0 40px);
  rotate: -1.5deg;
  transform-origin: 50% -28px;
  filter: drop-shadow(0 20px 24px rgba(21, 21, 46, 0.18));
}
.big-hole {
  position: absolute;
  top: 16px;
  left: 50%;
  width: 18px;
  height: 18px;
  margin-left: -9px;
  border-radius: 50%;
  background: var(--board);
  box-shadow: 0 0 0 4px #c9ccdf, inset 0 1px 2px rgba(0, 0, 0, 0.2);
}
.big-photo {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 10px;
  overflow: hidden;
}
.big-stamp {
  position: absolute;
  top: 50%;
  left: 50%;
  translate: -50% -50%;
  font-size: 40px;
  padding: 10px 18px 8px 22px;
}

.info {
  padding-top: 24px;
  animation: rise 0.5s ease 0.08s both;
}

.info-kicker {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--muted);
}
.info-cat {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--ultra-mist);
  color: var(--ultra);
  font-weight: 700;
}

.info-name {
  margin-top: 14px;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.12;
  text-wrap: balance;
}

.info-price {
  margin: 24px 0 8px -6px;
}

.facts {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  margin: 24px 0 28px;
  border-top: 2px dashed var(--line);
  border-bottom: 2px dashed var(--line);
}
.facts > div {
  padding: 14px 0 14px 16px;
  border-left: 2px dashed var(--line);
}
.facts > div:first-child {
  padding-left: 0;
  border-left: 0;
}
.facts dt {
  font-size: 12px;
  color: var(--muted);
}
.facts dd {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.1;
}
.facts dd small {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--muted);
  margin-left: 3px;
}
.facts dd.is-low {
  color: var(--stamp);
}
.facts .facts-seller {
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.buy {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.buy-qty {
  width: 140px;
}
.buy-cart {
  min-width: 180px;
}
.buy-cart .el-icon {
  margin-right: 6px;
  font-size: 18px;
}

.fav {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 46px;
  padding: 0 18px;
  border: 2px solid var(--line);
  border-radius: 10px;
  background: #fff;
  color: var(--ink-soft);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, transform 0.3s var(--ease-swing);
}
.fav .el-icon {
  font-size: 18px;
}
.fav:hover {
  border-color: var(--ink);
}
.fav.on {
  background: var(--highlight);
  border-color: var(--ink);
  color: var(--ink);
}
.fav.on .el-icon {
  animation: pop 0.4s var(--ease-swing);
}
@keyframes pop {
  0% { scale: 0.4; }
  100% { scale: 1; }
}

.buy-hint {
  margin-top: 12px;
  font-size: 14px;
  color: var(--stamp);
  font-weight: 600;
}

.edit-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 20px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ultra);
}
.edit-link:hover {
  text-decoration: underline;
}

.note {
  position: relative;
  margin-top: 64px;
  padding: 32px 32px 28px;
  background: var(--paper);
  border-radius: 6px;
  box-shadow: var(--shadow-tag);
}
.note-tape {
  position: absolute;
  top: -12px;
  left: 40px;
  width: 110px;
  height: 26px;
  background: rgba(255, 225, 77, 0.85);
  rotate: -3deg;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}
.note .section-title {
  margin-bottom: 10px;
}
.note-body {
  font-size: 16px;
  line-height: 1.9;
  white-space: pre-wrap;
  color: var(--ink-soft);
}

.related {
  margin-top: 64px;
}
.related-grid {
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
}

@media (max-width: 860px) {
  .detail {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .big {
    max-width: 440px;
    margin: 0 auto;
    width: 100%;
  }
  .facts {
    grid-auto-flow: row;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .facts > div:nth-child(3) {
    padding-left: 0;
    border-left: 0;
    border-top: 2px dashed var(--line);
  }
  .facts > div:nth-child(4) {
    border-top: 2px dashed var(--line);
  }
  .buy-cart {
    flex: 1;
  }
  .note {
    padding: 28px 20px 22px;
  }
}
</style>
