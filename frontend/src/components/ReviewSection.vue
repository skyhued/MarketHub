<template>
  <section class="reviews">
    <div class="reviews-head">
      <div>
        <h2 class="section-title">买家评价</h2>
        <p v-if="reviews.length" class="reviews-sum">
          <span class="reviews-avg">{{ average }}</span>
          <el-rate :model-value="Number(average)" disabled allow-half size="small" />
          <span>{{ reviews.length }} 条评价</span>
        </p>
      </div>

      <template v-if="userStore.isLoggedIn">
        <el-tooltip v-if="!hasEligibleOrder" :content="disabledHint" placement="top">
          <span><el-button disabled>写评价</el-button></span>
        </el-tooltip>
        <el-button v-else type="primary" @click="openDialog">
          <el-icon><EditPen /></el-icon><span>写评价</span>
        </el-button>
      </template>
    </div>

    <div v-if="loading" class="reviews-loading">加载评价中……</div>

    <ul v-else-if="reviews.length > 0" class="review-list">
      <li v-for="review in reviews" :key="review.id" class="review">
        <div class="review-who">
          <span class="review-avatar" :class="`tint-${tintFor(review.username)}`">
            {{ review.username?.charAt(0)?.toUpperCase() }}
          </span>
          <div class="review-meta">
            <span class="review-name">{{ review.username }}</span>
            <span class="review-time">{{ dateTime(review.createTime) }}</span>
          </div>
          <el-rate :model-value="review.rating" disabled size="small" class="review-rate" />
        </div>
        <p class="review-body">{{ review.content }}</p>
      </li>
    </ul>

    <p v-else class="reviews-empty">还没有人评价。确认收货后，买家可以在这里留下评价。</p>

    <el-dialog v-model="showDialog" title="写评价" width="min(520px, 92vw)" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="评价哪一笔订单" prop="orderId">
          <el-select v-model="form.orderId" placeholder="选择订单" style="width: 100%">
            <el-option
              v-for="order in eligibleOrders"
              :key="order.id"
              :label="`${order.orderNo}（${dateOnly(order.createTime)}）`"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="打分" prop="rating">
          <el-rate v-model="form.rating" :texts="['非常差', '较差', '一般', '较好', '非常好']" show-text />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="描述商品的实际成色和使用感受"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">发布评价</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { getProductReviewsAPI, addReviewAPI, getMyOrdersAPI, getMyReviewsAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { dateOnly, dateTime, tintFor } from '@/utils/format'

const props = defineProps({
  productId: { type: [Number, String], required: true }
})

/** 订单状态：3 = 已完成（确认收货后） */
const ORDER_COMPLETED = 3

const userStore = useUserStore()
const reviews = ref([])
const loading = ref(false)
const showDialog = ref(false)
const submitting = ref(false)
const formRef = ref(null)
/** 当前用户已完成、且包含这件商品、且还没评过的订单，评价时必须指定其中之一 */
const eligibleOrders = ref([])
/** 已经评过的订单 id，用来把这些订单从下拉里剔掉 */
const reviewedOrderIds = ref([])

const form = reactive({
  orderId: null,
  rating: 5,
  content: ''
})

const rules = {
  orderId: [{ required: true, message: '请选择要评价的订单', trigger: 'change' }],
  rating: [{ required: true, message: '请选择评分' }],
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

const hasEligibleOrder = computed(() => eligibleOrders.value.length > 0)

const disabledHint = computed(() =>
  reviewedOrderIds.value.length > 0
    ? '这件商品你已经评价过了'
    : '确认收货后才能评价这件商品'
)

const average = computed(() => {
  if (!reviews.value.length) return '0.0'
  const sum = reviews.value.reduce((s, r) => s + (Number(r.rating) || 0), 0)
  return (sum / reviews.value.length).toFixed(1)
})

async function loadReviews() {
  loading.value = true
  try {
    const res = await getProductReviewsAPI(props.productId)
    reviews.value = res.data || []
  } catch (e) {
    reviews.value = []
  } finally {
    loading.value = false
  }
}

/** 能不能评价由后端判定，这里只提前列出可选订单并剔除已评过的。 */
async function loadEligibleOrders() {
  if (!userStore.isLoggedIn) {
    eligibleOrders.value = []
    reviewedOrderIds.value = []
    return
  }
  try {
    const [ordersRes, mineRes] = await Promise.all([
      getMyOrdersAPI(),
      getMyReviewsAPI(props.productId)
    ])
    reviewedOrderIds.value = (mineRes.data || []).map(review => review.orderId)
    eligibleOrders.value = (ordersRes.data || []).filter(order =>
      order.status === ORDER_COMPLETED &&
      (order.items || []).some(item => String(item.productId) === String(props.productId)) &&
      !reviewedOrderIds.value.includes(order.id)
    )
  } catch (e) {
    eligibleOrders.value = []
    reviewedOrderIds.value = []
  }
}

function openDialog() {
  if (!hasEligibleOrder.value) {
    ElMessage.warning('确认收货后才能评价这件商品')
    return
  }
  form.orderId = eligibleOrders.value[0].id
  showDialog.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await addReviewAPI({
      productId: props.productId,
      orderId: form.orderId,
      rating: form.rating,
      content: form.content
    })
    ElMessage.success('评价已发布')
    showDialog.value = false
    form.rating = 5
    form.content = ''
    // 该订单的这件商品已经评过，不能再评
    await Promise.all([loadReviews(), loadEligibleOrders()])
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

watch(() => props.productId, () => {
  loadReviews()
  loadEligibleOrders()
})
onMounted(() => {
  loadReviews()
  loadEligibleOrders()
})
</script>

<style scoped>
.reviews {
  margin-top: 64px;
}

.reviews-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.reviews-head .section-title {
  margin-bottom: 4px;
}
.reviews-head .el-icon {
  margin-right: 6px;
}

.reviews-sum {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--muted);
}
.reviews-avg {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1;
  color: var(--ink);
}

.reviews-loading,
.reviews-empty {
  padding: 28px 24px;
  border: 2px dashed var(--line);
  border-radius: 14px;
  color: var(--muted);
  font-size: 14px;
  text-align: center;
}

.review-list {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.review {
  padding: 18px 20px;
  background: var(--paper);
  border-radius: 14px;
  box-shadow: var(--shadow-tag);
}

.review-who {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-family: var(--font-display);
  font-size: 18px;
}
.tint-blue { background: #dcddfb; color: #2b2bd9; }
.tint-orange { background: #ffe0d1; color: #c93a0a; }
.tint-yellow { background: #fff1a8; color: #7a5d00; }
.tint-green { background: #d4f2e1; color: #0f6b3f; }
.tint-pink { background: #ffd9e8; color: #b01a58; }
.tint-lilac { background: #e6dcfb; color: #5f28b3; }

.review-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.review-name {
  font-size: 14px;
  font-weight: 700;
}
.review-time {
  font-size: 12px;
  color: var(--faint);
}
.review-rate {
  flex-shrink: 0;
}

.review-body {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.75;
  color: var(--ink-soft);
  white-space: pre-wrap;
}

@media (max-width: 560px) {
  .review-list {
    grid-template-columns: 1fr;
  }
}
</style>
