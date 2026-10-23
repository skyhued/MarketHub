<template>
  <section v-if="visible" class="shop-card panel">
    <header class="shop-head">
      <h2 class="section-title">我的店铺</h2>
      <span v-if="application" class="stamp" :class="stampClass">{{ statusText }}</span>
    </header>

    <TagLoader v-if="loading" />

    <template v-else-if="isMerchant">
      <p class="shop-line">
        当前店铺 <strong class="shop-name">{{ shopName }}</strong>
      </p>
      <p class="shop-hint">在「发布闲置」发布商品，在「订单 → 我卖出的」发货</p>
      <el-button v-if="showCongrats" type="primary" @click="markSeen">知道了</el-button>
    </template>

    <template v-else-if="application && application.status === 0">
      <p class="shop-line">
        店铺「<strong class="shop-name">{{ application.shopName }}</strong>」审核中
      </p>
      <p class="shop-hint">提交时间：{{ dateTime(application.createTime) }}</p>
    </template>

    <template v-else-if="application && application.status === 2">
      <p class="shop-line">
        店铺「<strong class="shop-name">{{ application.shopName }}</strong>」申请未通过
      </p>
      <p class="shop-reason">原因：{{ application.rejectReason || '管理员未填写' }}</p>
      <el-button type="primary" @click="openForm">重新申请</el-button>
    </template>

    <template v-else-if="application && application.status === 3">
      <p class="shop-line">商家资格已被收回</p>
      <p class="shop-reason">原因：{{ application.revokeReason || '管理员未填写' }}</p>
      <p class="shop-hint">店名「{{ application.shopName }}」仍归你所有</p>
      <el-button type="primary" @click="openForm">重新申请</el-button>
    </template>

    <template v-else>
      <p class="shop-line">申请开店</p>
      <p class="shop-hint">提交后由管理员审核，通过即可发布商品、管理订单与发货</p>
      <el-button type="primary" @click="openForm">申请开店</el-button>
    </template>

    <el-dialog v-model="formVisible" title="开店申请" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="店铺名" prop="shopName">
          <el-input v-model="form.shopName" maxlength="20" show-word-limit placeholder="全站唯一，最多 20 字" />
        </el-form-item>
        <el-form-item label="主营类目" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择主营类目" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="20" placeholder="便于买家联系你" />
        </el-form-item>
        <el-form-item label="申请说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="说明经营内容与货源" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  applyMerchantAPI,
  getMyMerchantApplicationAPI,
  markMerchantResultSeenAPI,
  getCategoriesAPI
} from '@/api'
import { dateTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import TagLoader from '@/components/TagLoader.vue'

const userStore = useUserStore()

const application = ref(null)
const categories = ref([])
const loading = ref(true)
const submitting = ref(false)
const formVisible = ref(false)
const formRef = ref(null)

const form = reactive({ shopName: '', categoryId: null, contactPhone: '', description: '' })

const isMerchant = computed(() => userStore.userInfo?.role === 'MERCHANT')
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
/** 管理员不参与开店，整张卡片都不显示 */
const visible = computed(() => !isAdmin.value)
const shopName = computed(() => userStore.userInfo?.shopName || application.value?.shopName || '')
/** 审核通过后管理员侧已把 token 作废，用户重新登录时第一次看到这条提示 */
const showCongrats = computed(() =>
  isMerchant.value && application.value?.status === 1 && application.value?.resultSeen === 0
)

const statusText = computed(() => {
  if (isMerchant.value) return '营业中'
  const map = { 0: '审核中', 1: '已通过', 2: '已驳回', 3: '已收回' }
  return map[application.value?.status] ?? '未申请'
})
const stampClass = computed(() => {
  if (isMerchant.value || application.value?.status === 1) return 'stamp--green'
  if (application.value?.status === 2) return ''
  if (application.value?.status === 3) return 'stamp--grey'
  return 'stamp--blue'
})

const rules = {
  shopName: [{ required: true, message: '请填写店铺名', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择主营类目', trigger: 'change' }],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^[0-9+\- ]{6,20}$/, message: '电话格式不正确', trigger: 'blur' }
  ],
  description: [{ required: true, message: '请填写申请说明', trigger: 'blur' }]
}

async function loadApplication() {
  loading.value = true
  try {
    const res = await getMyMerchantApplicationAPI()
    application.value = res.data || null
  } catch (e) {
    application.value = null
  } finally {
    loading.value = false
  }
}

async function openForm() {
  formVisible.value = true
  Object.assign(form, {
    shopName: isMerchant.value ? '' : (application.value?.shopName || ''),
    categoryId: application.value?.categoryId ?? null,
    contactPhone: application.value?.contactPhone || '',
    description: application.value?.description || ''
  })
  if (!categories.value.length) {
    try {
      const res = await getCategoriesAPI()
      categories.value = res.data || []
    } catch (e) {
      categories.value = []
    }
  }
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await applyMerchantAPI({ ...form })
    ElMessage.success('申请已提交，等待审核')
    formVisible.value = false
    await loadApplication()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

async function markSeen() {
  try {
    await markMerchantResultSeenAPI()
    if (application.value) application.value.resultSeen = 1
  } catch (e) {
  }
}

onMounted(async () => {
  await userStore.init()
  if (isAdmin.value) {
    loading.value = false
    return
  }
  await loadApplication()
})
</script>

<style scoped>
.shop-card {
  margin-top: 20px;
}

.shop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.shop-head .section-title {
  margin-bottom: 0;
}

.shop-line {
  font-size: 15px;
  color: var(--ink-soft);
}
.shop-name {
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 400;
  color: var(--ultra-deep);
}

.shop-hint {
  margin: 6px 0 14px;
  font-size: 13px;
  color: var(--muted);
}

.shop-reason {
  margin: 8px 0 12px;
  padding: 10px 12px;
  border-left: 3px solid var(--stamp);
  border-radius: 0 8px 8px 0;
  background: #fbe9ec;
  font-size: 13px;
  line-height: 1.6;
  color: var(--stamp);
}
</style>
