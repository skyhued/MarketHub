<template>
  <AdminLayout title="数据概览" sub="平台当前的账号、商品和订单数量">
    <div class="stats">
      <div v-for="(s, i) in visibleCards" :key="s.key" class="stat" :style="{ '--i': i }">
        <span class="stat-hole" aria-hidden="true"></span>
        <el-icon class="stat-icon" aria-hidden="true"><component :is="s.icon" /></el-icon>
        <span class="stat-value">{{ stats[s.key] }}</span>
        <span class="stat-label">{{ s.label }}</span>
      </div>
    </div>

    <section class="panel recent">
      <h2 class="section-title">最近订单</h2>
      <el-table
        v-if="loadingOrders || recentOrders.length"
        v-loading="loadingOrders"
        :data="recentOrders"
        style="width: 100%"
        row-class-name="recent-row"
        @row-click="(row) => router.push(`/order/${row.id}`)"
      >
        <el-table-column label="订单号" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><OrderStatus :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }"><span class="mono">¥{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loadingOrders && !recentOrders.length" title="暂无订单" />
    </section>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Goods, Tickets, ChatLineSquare } from '@element-plus/icons-vue'
import { getAdminStatsAPI, getAllOrdersAPI, getAdminProductsAPI } from '@/api'
import { money, dateTime } from '@/utils/format'
import AdminLayout from '@/components/AdminLayout.vue'
import OrderStatus from '@/components/OrderStatus.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const stats = ref({})
const recentOrders = ref([])
const loadingOrders = ref(false)

const statCards = [
  { key: 'users', label: '用户', icon: User },
  { key: 'products', label: '商品', icon: Goods },
  { key: 'orders', label: '订单', icon: Tickets },
  { key: 'reviewCount', label: '评价', icon: ChatLineSquare }
]

const visibleCards = computed(() => statCards.filter(s => stats.value[s.key] != null))

/** /admin/stats 目前只返回用户数，商品数和订单数先用后台列表接口补上。 */
async function loadStats() {
  const [statsRes, productsRes, ordersRes] = await Promise.allSettled([
    getAdminStatsAPI(),
    getAdminProductsAPI(),
    getAllOrdersAPI()
  ])
  const s = statsRes.status === 'fulfilled' ? { ...(statsRes.value.data || {}) } : {}
  if (s.products == null && productsRes.status === 'fulfilled') s.products = (productsRes.value.data || []).length
  if (s.orders == null && ordersRes.status === 'fulfilled') s.orders = (ordersRes.value.data || []).length
  stats.value = s
  if (ordersRes.status === 'fulfilled') recentOrders.value = (ordersRes.value.data || []).slice(0, 10)
}

onMounted(async () => {
  loadingOrders.value = true
  try {
    await loadStats()
  } finally {
    loadingOrders.value = false
  }
})
</script>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 34px 22px 20px;
  background: var(--paper);
  border-radius: 14px;
  clip-path: polygon(22px 0, calc(100% - 22px) 0, 100% 22px, 100% 100%, 0 100%, 0 22px);
  animation: rise 0.45s ease both;
  animation-delay: calc(var(--i) * 60ms);
}
.stat-hole {
  position: absolute;
  top: 12px;
  left: 50%;
  width: 12px;
  height: 12px;
  margin-left: -6px;
  border-radius: 50%;
  background: var(--board);
  box-shadow: 0 0 0 3px #c9ccdf;
}
.stat-icon {
  position: absolute;
  top: 30px;
  right: 20px;
  font-size: 22px;
  color: var(--ultra);
  opacity: 0.35;
}
.stat-value {
  font-family: var(--font-display);
  font-size: 52px;
  line-height: 1;
}
.stat-label {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--muted);
}

.recent {
  margin-top: 28px;
}
.recent :deep(.recent-row) {
  cursor: pointer;
}

@media (max-width: 860px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }
  .stat-value {
    font-size: 40px;
  }
}
</style>
