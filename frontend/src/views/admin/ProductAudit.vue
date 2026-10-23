<template>
  <AdminLayout title="商品审核" :sub="loading ? '' : `${products.length} 件商品等待审核`">
    <TagLoader v-if="loading" />

    <div v-else-if="products.length" class="queue">
      <article v-for="(p, i) in products" :key="p.id" class="item" :style="{ '--i': i }">
        <router-link :to="`/product/${p.id}`" class="item-photo" target="_blank" :aria-label="`在新窗口查看 ${p.name}`">
          <ProductImage :src="p.imageUrl" :name="p.name" />
          <span v-if="stamped[p.id]" class="item-stamp stamp" :class="stamped[p.id] === 1 ? 'stamp--green' : ''">
            {{ stamped[p.id] === 1 ? '通过' : '驳回' }}
          </span>
        </router-link>

        <div class="item-info">
          <div class="item-row">
            <h3 class="item-name">{{ p.name }}</h3>
            <span class="item-price mono">¥{{ money(p.price) }}</span>
          </div>
          <p class="item-meta">
            <span v-if="p.categoryName">{{ p.categoryName }}</span>
            <span v-if="p.sellerName">{{ p.sellerName }}</span>
            <span>{{ dateTime(p.createTime) }} 提交</span>
          </p>
          <p class="item-desc">{{ p.description || '卖家没有写描述' }}</p>
        </div>

        <div class="item-actions">
          <el-button :disabled="!!stamped[p.id]" @click="handleAudit(p, 2)">驳回</el-button>
          <el-button type="primary" :disabled="!!stamped[p.id]" @click="handleAudit(p, 1)">通过</el-button>
        </div>
      </article>
    </div>

    <EmptyState v-else title="暂无待审核商品" hint="商家发布的新商品会显示在这里" />
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminProductsAPI, auditProductAPI } from '@/api'
import { money, dateTime } from '@/utils/format'
import AdminLayout from '@/components/AdminLayout.vue'
import ProductImage from '@/components/ProductImage.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

const products = ref([])
const loading = ref(false)
/** 刚审核完的商品 id → 结果（1 通过 / 2 驳回），先盖章，稍后从队列里移除 */
const stamped = reactive({})

async function loadProducts() {
  loading.value = true
  try {
    const res = await getAdminProductsAPI({ auditStatus: 0 })
    products.value = res.data || []
  } catch (e) {
    products.value = []
  } finally {
    loading.value = false
  }
}

async function handleAudit(product, status) {
  try {
    await auditProductAPI(product.id, status)
    stamped[product.id] = status
    ElMessage.success(status === 1 ? '已通过，商品已上架' : '已驳回')
    setTimeout(() => {
      products.value = products.value.filter(p => p.id !== product.id)
      delete stamped[product.id]
    }, 900)
  } catch (e) {
  }
}

onMounted(loadProducts)
</script>

<style scoped>
.queue {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.item {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  padding: 14px 20px 14px 14px;
  background: var(--paper);
  border-radius: 14px;
  box-shadow: var(--shadow-tag);
  animation: rise 0.4s ease both;
  animation-delay: calc(min(var(--i), 10) * 40ms);
}

.item-photo {
  position: relative;
  width: 112px;
  height: 112px;
  border-radius: 10px;
  overflow: hidden;
}
.item-stamp {
  position: absolute;
  top: 50%;
  left: 50%;
  translate: -50% -50%;
  font-size: 24px;
  animation: stamp-down 0.35s cubic-bezier(0.2, 0.9, 0.3, 1.4) both;
}
@keyframes stamp-down {
  from { scale: 2.4; opacity: 0; }
  to { scale: 1; opacity: 1; }
}

.item-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
}
.item-name {
  font-size: 17px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-price {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
}
.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}
.item-desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink-soft);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 720px) {
  .item {
    grid-template-columns: 84px minmax(0, 1fr);
    padding: 12px;
    gap: 12px;
  }
  .item-photo {
    width: 84px;
    height: 84px;
  }
  .item-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
