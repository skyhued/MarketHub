<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">收藏</h1>
        <p v-if="favoriteList.length" class="page-sub">共 {{ favoriteList.length }} 件收藏</p>
      </div>
    </div>

    <TagLoader v-if="loading" />

    <div v-else-if="favoriteList.length" class="tag-grid">
      <ProductCard
        v-for="(item, i) in favoriteList"
        :key="item.id"
        :product="toProduct(item)"
        :index="i"
      >
        <template #actions>
          <span class="fav-date">{{ dateOnly(item.createTime) }} 收藏</span>
          <button type="button" class="fav-remove" @click="handleRemove(item)">
            <el-icon><Close /></el-icon>移出收藏
          </button>
        </template>
      </ProductCard>
    </div>

    <EmptyState v-else title="暂无收藏" hint="在商品页收藏后，商品会显示在这里">
      <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
    </EmptyState>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { getFavoriteListAPI, removeFavoriteAPI } from '@/api'
import { dateOnly } from '@/utils/format'
import ProductCard from '@/components/ProductCard.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const favoriteList = ref([])
const loading = ref(true)

/** 收藏接口的字段名带 product 前缀，转成吊牌组件认识的商品结构 */
function toProduct(item) {
  return {
    id: item.productId,
    name: item.productName,
    price: item.productPrice,
    imageUrl: item.productImage
  }
}

async function loadFavorites() {
  loading.value = true
  try {
    const res = await getFavoriteListAPI()
    favoriteList.value = res.data || []
  } catch (e) {
    favoriteList.value = []
  } finally {
    loading.value = false
  }
}

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(`把「${item.productName}」移出收藏？`, '移出收藏', {
      confirmButtonText: '移出',
      cancelButtonText: '保留',
      type: 'warning'
    })
    await removeFavoriteAPI(item.productId)
    favoriteList.value = favoriteList.value.filter(f => f.productId !== item.productId)
    ElMessage.success('已移出收藏')
  } catch (e) {
  }
}

onMounted(loadFavorites)
</script>

<style scoped>
.fav-date {
  font-size: 12px;
  color: var(--muted);
  line-height: 30px;
  white-space: nowrap;
}

.fav-remove {
  flex-shrink: 0;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 12px;
  border: 1.5px solid var(--line);
  border-radius: 999px;
  background: var(--paper);
  color: var(--ink-soft);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.fav-remove:hover {
  border-color: var(--stamp);
  color: var(--stamp);
}
</style>
