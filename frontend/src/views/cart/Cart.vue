<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">购物车</h1>
        <p v-if="cartList.length" class="page-sub">共 {{ cartList.length }} 件商品，勾选后结算</p>
      </div>
    </div>

    <TagLoader v-if="loading" />

    <div v-else-if="cartList.length > 0" class="cart">
      <ul class="rows">
        <li v-for="row in cartList" :key="row.id" class="row" :class="{ 'is-off': row.checked !== 1 }">
          <el-checkbox
            :model-value="row.checked === 1"
            size="large"
            class="row-check"
            :aria-label="`选中 ${row.productName}`"
            @change="(val) => handleCheck(row, val)"
          />
          <router-link :to="`/product/${row.productId}`" class="row-thumb">
            <ProductImage :src="row.productImage" :name="row.productName" compact />
          </router-link>
          <div class="row-main">
            <router-link :to="`/product/${row.productId}`" class="row-name">{{ row.productName }}</router-link>
            <span class="row-unit mono">单价 ¥{{ money(row.productPrice) }}</span>
          </div>
          <el-input-number
            :model-value="row.quantity"
            :min="1"
            :max="99"
            size="small"
            class="row-qty"
            :aria-label="`${row.productName} 数量`"
            @change="(val) => handleQuantityChange(row, val)"
          />
          <span class="row-sub mono">¥{{ money(row.productPrice * row.quantity) }}</span>
          <button type="button" class="row-del" :aria-label="`从购物车删除 ${row.productName}`" @click="handleRemove(row)">
            <el-icon><Delete /></el-icon>
          </button>
        </li>
      </ul>

      <aside class="summary receipt-wrap">
        <div class="receipt">
          <p class="receipt-title">结算单</p>
          <hr class="receipt-rule" />
          <div v-for="item in checkedItems" :key="item.id" class="receipt-line">
            <span class="summary-item">{{ item.productName }} ×{{ item.quantity }}</span>
            <span>{{ money(item.productPrice * item.quantity) }}</span>
          </div>
          <p v-if="!checkedItems.length" class="summary-none">还没有勾选商品</p>
          <hr class="receipt-rule" />
          <div class="receipt-line">
            <span>件数</span>
            <span>{{ checkedCount }}</span>
          </div>
          <div class="summary-total">
            <span>合计</span>
            <span class="summary-amount"><small>¥</small>{{ money(totalPrice) }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="summary-btn"
            :disabled="checkedCount === 0"
            @click="handleCheckout"
          >
            提交订单
          </el-button>
        </div>
      </aside>
    </div>

    <EmptyState v-else title="购物车为空" hint="在商品页加入购物车后，商品会显示在这里">
      <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
    </EmptyState>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getCartListAPI, updateCartQuantityAPI, removeFromCartAPI, updateCartCheckedAPI, createOrderFromCartAPI } from '@/api'
import { money } from '@/utils/format'
import ProductImage from '@/components/ProductImage.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const cartList = ref([])
const loading = ref(true)

const checkedItems = computed(() => cartList.value.filter(item => item.checked === 1))
const checkedCount = computed(() => checkedItems.value.reduce((sum, item) => sum + item.quantity, 0))
const totalPrice = computed(() => checkedItems.value.reduce((sum, item) => sum + item.productPrice * item.quantity, 0))

async function loadCart() {
  loading.value = true
  try {
    const res = await getCartListAPI()
    cartList.value = res.data || []
  } catch (e) {
    cartList.value = []
  } finally {
    loading.value = false
  }
}

async function handleCheck(row, checked) {
  try {
    await updateCartCheckedAPI({ productId: row.productId, checked: checked ? 1 : 0 })
    row.checked = checked ? 1 : 0
  } catch (e) {
    // ignore
  }
}

async function handleQuantityChange(row, newQuantity) {
  try {
    await updateCartQuantityAPI({ productId: row.productId, quantity: newQuantity })
    row.quantity = newQuantity
  } catch (e) {
    // ignore
  }
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(`把「${row.productName}」从购物车删除？`, '删除商品', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '保留'
    })
    await removeFromCartAPI(row.productId)
    cartList.value = cartList.value.filter(item => item.productId !== row.productId)
    ElMessage.success('已删除')
  } catch (e) {
    // cancel or error
  }
}

async function handleCheckout() {
  if (checkedCount.value === 0) return

  try {
    const cartIds = checkedItems.value.map(item => item.id)
    const res = await createOrderFromCartAPI({ cartIds })
    ElMessage.success('订单已提交')
    router.push(`/order/${res.data.id}`)
  } catch (e) {
    // ignore
  }
}

onMounted(loadCart)
</script>

<style scoped>
.cart {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 32px;
  align-items: start;
}

.rows {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: grid;
  grid-template-columns: auto 84px minmax(0, 1fr) auto 110px auto;
  align-items: center;
  gap: 18px;
  padding: 14px 18px 14px 16px;
  background: var(--paper);
  border-radius: 14px;
  box-shadow: var(--shadow-tag);
  transition: opacity 0.2s;
  animation: rise 0.4s ease both;
}
.row.is-off .row-thumb,
.row.is-off .row-main,
.row.is-off .row-sub {
  opacity: 0.5;
}

.row-thumb {
  width: 84px;
  height: 84px;
  border-radius: 10px;
  overflow: hidden;
}

.row-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.row-name {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row-name:hover {
  color: var(--ultra);
}
.row-unit {
  font-size: 12px;
  color: var(--muted);
}

.row-qty {
  width: 108px;
}

.row-sub {
  font-size: 16px;
  font-weight: 600;
  text-align: right;
  color: var(--ink);
}

.row-del {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--faint);
  font-size: 18px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.row-del:hover {
  background: #fbe9ec;
  color: var(--stamp);
}

.summary {
  position: sticky;
  top: 88px;
}
.summary-item {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.summary-none {
  padding: 6px 0;
  text-align: center;
  color: var(--faint);
}
.summary-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
  font-family: var(--font-body);
  font-weight: 700;
  color: var(--ink);
}
.summary-amount {
  font-family: var(--font-marker);
  font-weight: 400;
  font-size: 34px;
  color: var(--sticker);
  line-height: 1.1;
}
.summary-amount small {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 16px;
  margin-right: 2px;
}
.summary-btn {
  width: 100%;
  margin-top: 18px;
}

@media (max-width: 900px) {
  .cart {
    grid-template-columns: 1fr;
  }
  .summary {
    position: static;
  }
}

@media (max-width: 600px) {
  .row {
    grid-template-columns: auto 64px minmax(0, 1fr) auto;
    grid-template-areas:
      'check thumb main del'
      'check thumb qty sub';
    gap: 8px 12px;
    padding: 12px;
  }
  .row-check { grid-area: check; }
  .row-thumb { grid-area: thumb; width: 64px; height: 64px; }
  .row-main { grid-area: main; }
  .row-qty { grid-area: qty; width: 96px; }
  .row-sub { grid-area: sub; font-size: 14px; }
  .row-del { grid-area: del; }
}
</style>
