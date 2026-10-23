<template>
  <div class="page page-narrow">
    <div class="page-head">
      <div>
        <h1 class="page-title">订单</h1>
        <p class="page-sub">{{ orderMode === 'sell' ? '卖出的商品' : '买到的商品' }}</p>
      </div>
      <div v-if="userStore.hasPerm('order:ship')" class="mode" role="group" aria-label="订单类型">
        <button type="button" :class="{ on: orderMode === 'buy' }" :aria-pressed="orderMode === 'buy'" @click="switchMode('buy')">我买到的</button>
        <button type="button" :class="{ on: orderMode === 'sell' }" :aria-pressed="orderMode === 'sell'" @click="switchMode('sell')">我卖出的</button>
      </div>
    </div>

    <div class="tabs" role="group" aria-label="按状态筛选">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        class="tab"
        :class="{ on: activeTab === tab.value }"
        :aria-pressed="activeTab === tab.value"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}<span class="tab-count">{{ countOf(tab.value) }}</span>
      </button>
    </div>

    <TagLoader v-if="loading" />

    <ul v-else-if="shown.length" class="orders">
      <li v-for="(order, i) in shown" :key="order.id" :style="{ '--i': i }">
        <router-link :to="`/order/${order.id}`" class="order">
          <div class="order-top">
            <OrderStatus :status="order.status" />
            <span class="order-no mono">No.{{ order.orderNo }}</span>
            <span class="order-time">{{ dateTime(order.createTime) }}</span>
          </div>

          <div class="order-body">
            <div class="order-thumbs">
              <span v-for="item in (order.items || []).slice(0, 3)" :key="item.id" class="order-thumb">
                <ProductImage :src="item.productImage" :name="item.productName" compact />
              </span>
            </div>
            <div class="order-items">
              <p v-for="item in (order.items || []).slice(0, 2)" :key="item.id" class="order-item">
                <span class="order-item-name">{{ item.productName }}</span>
                <span class="order-item-qty mono">¥{{ money(item.unitPrice) }} ×{{ item.quantity }}</span>
              </p>
              <p v-if="(order.items || []).length > 2" class="order-more">还有 {{ order.items.length - 2 }} 件</p>
            </div>
            <div class="order-total">
              <span>合计</span>
              <strong class="mono">¥{{ money(order.totalAmount) }}</strong>
            </div>
            <el-icon class="order-go" aria-hidden="true"><ArrowRight /></el-icon>
          </div>
        </router-link>
      </li>
    </ul>

    <EmptyState
      v-else
      :title="activeTab === -1 ? (orderMode === 'sell' ? '暂无卖出记录' : '暂无订单') : `暂无「${currentTabLabel}」订单`"
      :hint="activeTab === -1 && orderMode === 'buy' ? '在购物车提交订单后，订单会显示在这里' : ''"
    >
      <el-button v-if="activeTab === -1 && orderMode === 'buy'" type="primary" @click="$router.push('/')">去逛逛</el-button>
    </EmptyState>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { getMyOrdersAPI, getMySoldOrdersAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { money, dateTime } from '@/utils/format'
import OrderStatus from '@/components/OrderStatus.vue'
import ProductImage from '@/components/ProductImage.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()
const orders = ref([])
const loading = ref(false)
const activeTab = ref(-1)
const orderMode = ref('buy')

const tabs = [
  { label: '全部', value: -1 },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

const currentTabLabel = computed(() => tabs.find(t => t.value === activeTab.value)?.label)

/** 两个接口都返回全部订单、不支持按状态查询，所以状态筛选在前端做 */
const shown = computed(() =>
  activeTab.value < 0 ? orders.value : orders.value.filter(o => o.status === activeTab.value)
)

function countOf(status) {
  return status < 0 ? orders.value.length : orders.value.filter(o => o.status === status).length
}

function switchMode(mode) {
  if (orderMode.value === mode) return
  orderMode.value = mode
  activeTab.value = -1
  loadOrders()
}

async function loadOrders() {
  loading.value = true
  try {
    const res = orderMode.value === 'sell' ? await getMySoldOrdersAPI() : await getMyOrdersAPI()
    orders.value = res.data || []
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.mode {
  display: inline-flex;
  padding: 4px;
  background: var(--paper);
  border-radius: 12px;
  box-shadow: var(--shadow-tag);
}
.mode button {
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.mode button.on {
  background: var(--ultra);
  color: #fff;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border: 1.5px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: var(--ink-soft);
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.tab:hover {
  border-color: var(--ultra);
}
.tab.on {
  background: var(--highlight);
  border-color: var(--ink);
  color: var(--ink);
}
.tab-count {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #eceef6;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  color: var(--muted);
}
.tab.on .tab-count {
  background: rgba(21, 21, 46, 0.12);
  color: var(--ink);
}

.orders {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.orders > li {
  animation: rise 0.4s ease both;
  animation-delay: calc(min(var(--i), 10) * 40ms);
}

.order {
  display: block;
  padding: 16px 20px 18px;
  background: var(--paper);
  border-radius: 14px;
  box-shadow: var(--shadow-tag);
  transition: transform 0.2s, box-shadow 0.2s;
}
.order:hover {
  transform: translateY(-2px);
  box-shadow: 0 1px 0 rgba(21, 21, 46, 0.06), 0 16px 28px -14px rgba(21, 21, 46, 0.4);
}

.order-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1.5px dashed var(--line);
}
.order-no {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-time {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--faint);
}

.order-body {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-top: 14px;
}

.order-thumbs {
  display: flex;
  flex-shrink: 0;
}
.order-thumb {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  overflow: hidden;
  border: 3px solid var(--paper);
  box-shadow: 0 2px 6px rgba(21, 21, 46, 0.12);
}
.order-thumb + .order-thumb {
  margin-left: -22px;
}
.order-thumb:nth-child(2) { rotate: 5deg; }
.order-thumb:nth-child(3) { rotate: -4deg; }

.order-items {
  flex: 1;
  min-width: 0;
}
.order-item {
  display: flex;
  gap: 12px;
  font-size: 14px;
  line-height: 1.7;
}
.order-item-name {
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.order-item-qty {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--muted);
  line-height: 24px;
}
.order-more {
  font-size: 12px;
  color: var(--muted);
}

.order-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--muted);
}
.order-total strong {
  font-size: 18px;
  color: var(--ink);
}

.order-go {
  flex-shrink: 0;
  color: var(--faint);
  transition: translate 0.2s, color 0.2s;
}
.order:hover .order-go {
  translate: 4px 0;
  color: var(--ultra);
}

@media (max-width: 600px) {
  .order {
    padding: 14px;
  }
  .order-time {
    display: none;
  }
  .order-body {
    flex-wrap: wrap;
    gap: 12px;
  }
  .order-items {
    flex-basis: calc(100% - 120px);
  }
  .order-total {
    flex-direction: row;
    align-items: baseline;
    gap: 8px;
    margin-left: auto;
  }
  .order-go {
    display: none;
  }
}
</style>
