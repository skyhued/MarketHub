<template>
  <div class="page page-narrow">
    <router-link to="/orders" class="link-back">
      <el-icon><ArrowLeft /></el-icon>回到订单
    </router-link>

    <TagLoader v-if="loading" />

    <template v-else-if="order">
      <!-- 下单 → 付款 → 发货 → 收货 -->
      <ol v-if="order.status !== 4" class="steps">
        <li
          v-for="(step, i) in steps"
          :key="step.label"
          class="step"
          :class="{ done: order.status >= i, current: order.status === i - 1 }"
        >
          <span class="step-dot">
            <el-icon v-if="order.status >= i"><Check /></el-icon>
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="step-label">{{ step.label }}</span>
          <span v-if="step.time" class="step-time mono">{{ dateTime(step.time) }}</span>
        </li>
      </ol>

      <div class="layout">
        <section class="state">
          <h1 class="state-title">{{ statusText }}</h1>
          <p class="state-desc">{{ statusDesc }}</p>

          <div v-if="hasActions" class="state-actions">
            <el-button v-if="order.status === 0 && isBuyer && userStore.hasPerm('order:pay')" type="primary" size="large" @click="handlePay">
              付款 ¥{{ money(order.actualAmount || order.totalAmount) }}
            </el-button>
            <el-button v-if="order.status === 1 && isSeller && userStore.hasPerm('order:ship')" type="primary" size="large" @click="handleShip">
              发货
            </el-button>
            <el-button v-if="order.status === 2 && isBuyer && userStore.hasPerm('order:confirm')" type="primary" size="large" @click="handleConfirm">
              确认收货
            </el-button>
            <el-button v-if="isCancelable && isBuyer && userStore.hasPerm('order:cancel')" size="large" @click="handleCancel">
              取消订单
            </el-button>
          </div>

          <p v-if="order.status === 3" class="state-next">
            可以到商品页给这次购买写评价。
          </p>
        </section>

        <div class="receipt-wrap">
          <div class="receipt">
            <span class="receipt-stamp stamp" :class="stampTone">{{ stampText }}</span>
            <p class="receipt-title">MarketHub</p>
            <p class="receipt-caption">购物小票</p>
            <hr class="receipt-rule" />
            <div class="receipt-line"><span>单号</span><span>{{ order.orderNo }}</span></div>
            <div class="receipt-line"><span>下单</span><span>{{ dateTime(order.createTime) }}</span></div>
            <div v-if="order.payTime" class="receipt-line"><span>付款</span><span>{{ dateTime(order.payTime) }}</span></div>
            <hr class="receipt-rule" />

            <router-link
              v-for="item in order.items"
              :key="item.id"
              :to="`/product/${item.productId}`"
              class="ticket-item"
            >
              <span class="ticket-thumb">
                <ProductImage :src="item.productImage" :name="item.productName" compact />
              </span>
              <span class="ticket-name">{{ item.productName }}</span>
              <span class="ticket-qty">{{ money(item.unitPrice) }} ×{{ item.quantity }}</span>
              <span class="ticket-sub">{{ money(item.unitPrice * item.quantity) }}</span>
            </router-link>

            <hr class="receipt-rule" />
            <div class="receipt-line"><span>商品总额</span><span>¥{{ money(order.totalAmount) }}</span></div>
            <div v-if="order.discountAmount" class="receipt-line">
              <span>优惠</span><span>-¥{{ money(order.discountAmount) }}</span>
            </div>
            <div class="ticket-total">
              <span>{{ order.status === 0 ? '应付' : '实付' }}</span>
              <span class="ticket-amount"><small>¥</small>{{ money(order.actualAmount || order.totalAmount) }}</span>
            </div>
            <p class="ticket-thanks">谢谢惠顾 · 旧物有新主</p>
          </div>
        </div>
      </div>
    </template>

    <EmptyState v-else title="订单不存在" hint="订单号可能有误，或不属于当前账号">
      <el-button type="primary" @click="router.push('/orders')">回到订单</el-button>
    </EmptyState>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import {
  getOrderByIdAPI, payOrderAPI, shipOrderAPI, confirmReceiptAPI, cancelOrderAPI
} from '@/api'
import { money, dateTime } from '@/utils/format'
import ProductImage from '@/components/ProductImage.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const order = ref(null)
const loading = ref(true)

const steps = computed(() => [
  { label: '下单', time: order.value?.createTime },
  { label: '付款', time: order.value?.payTime },
  { label: '发货' },
  { label: '收货' }
])

const statusText = computed(() => {
  const texts = { 0: '等待付款', 1: '已付款，等待发货', 2: '已发货', 3: '交易完成', 4: '订单已取消' }
  return texts[order.value?.status] || '未知状态'
})

const statusDesc = computed(() => {
  const descs = {
    0: '付款后卖家发货，超时未付款将自动取消',
    1: '卖家备货中，发货前可取消',
    2: '商品已发出，请确认收货',
    3: '交易已完成',
    4: '订单已取消，占用的库存已退回'
  }
  return descs[order.value?.status] || ''
})

const stampText = computed(() => ({ 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }[order.value?.status] || ''))
const stampTone = computed(() => ({ 0: 'stamp--blue', 1: 'stamp--blue', 2: 'stamp--green', 3: 'stamp--green', 4: '' }[order.value?.status] ?? ''))

// 管理员可以代任何一方操作，与后端的归属校验一致
const isSeller = computed(() => userStore.isAdmin || order.value?.sellerId === userStore.userId)
const isBuyer = computed(() => userStore.isAdmin || order.value?.userId === userStore.userId)

/** 待付款和已付款未发货都能取消，和 OrderService.cancel 的状态判断保持一致 */
const isCancelable = computed(() => order.value?.status === 0 || order.value?.status === 1)

const hasActions = computed(() => {
  const s = order.value?.status
  return (s === 0 && isBuyer.value && userStore.hasPerm('order:pay')) ||
    (s === 1 && isSeller.value && userStore.hasPerm('order:ship')) ||
    (s === 2 && isBuyer.value && userStore.hasPerm('order:confirm')) ||
    (isCancelable.value && isBuyer.value && userStore.hasPerm('order:cancel'))
})

async function loadOrder() {
  loading.value = true
  try {
    const res = await getOrderByIdAPI(route.params.id)
    order.value = res.data
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function reloadOrder() {
  try {
    const res = await getOrderByIdAPI(route.params.id)
    order.value = res.data
  } catch (e) { /* ignore */ }
}

async function handlePay() {
  try {
    await payOrderAPI(order.value.id)
    ElMessage.success('付款成功')
    await reloadOrder()
  } catch (e) { /* ignore */ }
}

async function handleShip() {
  try {
    await shipOrderAPI(order.value.id)
    ElMessage.success('已发货')
    await reloadOrder()
  } catch (e) { /* ignore */ }
}

async function handleConfirm() {
  try {
    await confirmReceiptAPI(order.value.id)
    ElMessage.success('已确认收货')
    await reloadOrder()
  } catch (e) { /* ignore */ }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('取消后订单不能恢复，占用的库存会退回。', '取消这笔订单？', {
      type: 'warning',
      confirmButtonText: '取消订单',
      cancelButtonText: '再想想'
    })
    await cancelOrderAPI(order.value.id)
    ElMessage.success('订单已取消')
    await reloadOrder()
  } catch (e) { /* ignore */ }
}

onMounted(loadOrder)
</script>

<style scoped>
.steps {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 8px 0 36px;
}
.step {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
}
.step + .step::before {
  content: '';
  position: absolute;
  top: 17px;
  right: 50%;
  width: calc(100% - 40px);
  height: 4px;
  margin-right: 20px;
  background: var(--line);
  border-radius: 2px;
  z-index: 0;
}
.step.done + .step.done::before {
  background: var(--ultra);
}
.step-dot {
  position: relative;
  z-index: 1;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--paper);
  border: 3px solid var(--line);
  color: var(--faint);
  font-weight: 800;
  font-size: 15px;
}
.step.done .step-dot {
  background: var(--ultra);
  border-color: var(--ultra);
  color: #fff;
}
.step.current .step-dot {
  border-color: var(--ink);
  background: var(--highlight);
  color: var(--ink);
  animation: nudge 1.6s ease-in-out infinite;
}
@keyframes nudge {
  0%, 100% { scale: 1; }
  50% { scale: 1.08; }
}
.step-label {
  font-weight: 700;
  font-size: 14px;
}
.step:not(.done):not(.current) .step-label {
  color: var(--muted);
}
.step-time {
  font-size: 11px;
  color: var(--muted);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 40px;
  align-items: start;
}

.state {
  padding-top: 8px;
}
.state-title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(36px, 5vw, 56px);
  line-height: 1.05;
}
.state-desc {
  margin-top: 14px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--ink-soft);
  max-width: 34em;
}
.state-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}
.state-next {
  margin-top: 24px;
  padding: 12px 16px;
  border-radius: 10px;
  background: var(--paper);
  font-size: 14px;
  color: var(--ink-soft);
  display: inline-block;
}

.receipt {
  animation: print 0.7s ease-out both;
}
@keyframes print {
  from { clip-path: inset(0 0 100% 0); }
  to { clip-path: inset(0 0 0 0); }
}

.receipt-caption {
  text-align: center;
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.3em;
  margin-top: 2px;
}
.receipt-line > :last-child {
  word-break: break-all;
}

.receipt-stamp {
  position: absolute;
  bottom: 22px;
  left: 22px;
  font-size: 24px;
  transform: rotate(-10deg);
  opacity: 0.9;
}

.ticket-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  grid-template-areas:
    'thumb name sub'
    'thumb qty sub';
  column-gap: 10px;
  align-items: center;
  padding: 6px 0;
  border-radius: 6px;
}
.ticket-item:hover .ticket-name {
  color: var(--ultra);
  text-decoration: underline;
}
.ticket-thumb {
  grid-area: thumb;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  overflow: hidden;
}
.ticket-name {
  grid-area: name;
  font-family: var(--font-body);
  font-weight: 700;
  font-size: 13px;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ticket-qty {
  grid-area: qty;
  font-size: 12px;
  color: var(--muted);
}
.ticket-sub {
  grid-area: sub;
  color: var(--ink);
}

.ticket-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
  font-family: var(--font-body);
  font-weight: 700;
  color: var(--ink);
}
.ticket-amount {
  font-family: var(--font-marker);
  font-weight: 400;
  font-size: 32px;
  line-height: 1.1;
  color: var(--sticker);
}
.ticket-amount small {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 15px;
  margin-right: 2px;
}
.ticket-thanks {
  margin-top: 16px;
  padding-left: 96px;
  text-align: right;
  font-size: 12px;
  color: var(--faint);
  letter-spacing: 0.1em;
}

@media (max-width: 820px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .steps {
    margin-bottom: 24px;
  }
  .step-time {
    display: none;
  }
}
</style>
