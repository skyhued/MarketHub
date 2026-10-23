<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑商品' : '发布闲置' }}</h1>
        <p class="page-sub">
          {{ isEdit ? '保存后生效；库存只能通过补货增加。' : '提交后由管理员审核，通过后即上架。' }}
        </p>
      </div>
    </div>

    <div class="form-layout">
      <aside class="preview" aria-label="吊牌预览">
        <p class="preview-label">吊牌预览</p>
        <div class="preview-tag">
          <span class="preview-hole" aria-hidden="true"></span>
          <div class="preview-photo-wrap">
            <div class="preview-photo">
              <ProductImage :src="imagePath" :name="form.name || '商品名称'" />
            </div>
            <PriceSticker class="preview-price" :price="form.price || 0" />
          </div>
          <p class="preview-name">{{ form.name || '商品名称' }}</p>
          <p class="preview-meta">{{ categoryName || '分类' }}</p>
        </div>
      </aside>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="panel form">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="如：九成新机械键盘" maxlength="100" show-word-limit />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="价格（元）" prop="price">
            <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>

          <!-- 新建时是初始库存，没有已存在的订单，直接填绝对值没问题 -->
          <el-form-item v-if="!isEdit" label="库存（件）" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :max="999999" style="width: 100%" />
          </el-form-item>
        </div>

        <!-- 编辑时不能直接覆盖库存：stock 里已扣掉待付款订单占用的部分，补货要走 restock。 -->
        <div v-if="isEdit" class="stock-box">
          <div class="stock-now">
            <span class="stock-label">可售库存</span>
            <span class="stock-value">{{ form.stock }}<small>件</small></span>
            <span v-if="lockedQuantity > 0" class="stock-locked">另有 {{ lockedQuantity }} 件被待付款订单占用</span>
          </div>
          <div class="restock">
            <el-input-number v-model="restockQuantity" :min="1" :max="999999" aria-label="补货数量" />
            <el-button type="primary" :loading="restocking" @click="handleRestock">补货</el-button>
          </div>
          <p class="stock-hint">补货只增不减，不会影响已经被订单占用的数量。</p>
        </div>

        <el-form-item label="图片">
          <el-upload
            drag
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
            class="uploader"
          >
            <div class="upload-inner">
              <el-icon :size="28"><UploadFilled /></el-icon>
              <span>{{ imagePath ? '点击或拖入图片，替换当前图片' : '点击或拖入图片上传' }}</span>
              <small>没有图片时，吊牌上会显示商品名</small>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="成色、使用时长、有没有瑕疵、配件是否齐全……"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '提交审核' }}
          </el-button>
          <el-button size="large" @click="router.back()">取消</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  getCategoriesAPI, createProductAPI, updateProductAPI, getProductByIdAPI,
  uploadProductImageAPI, restockProductAPI, getMySoldOrdersAPI
} from '@/api'
import { useUserStore } from '@/stores/user'
import ProductImage from '@/components/ProductImage.vue'
import PriceSticker from '@/components/PriceSticker.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
/** 上传接口返回、并最终存进库里的相对路径（如 products/xxx.jpg） */
const imagePath = ref('')
const categories = ref([])

/** 待付款订单里占用的件数。商品详情页是匿名可访问的，这个数只在编辑页给主人看 */
const lockedQuantity = ref(0)
const restockQuantity = ref(1)
const restocking = ref(false)
/** 商品归属，用来判断能不能用 my-sold 算锁定数（管理员编辑别人的商品时算不出来） */
const productOwnerId = ref(null)

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  categoryId: null,
  price: null,
  stock: 0,
  description: ''
})

const categoryName = computed(() => categories.value.find(c => c.id === form.categoryId)?.name)

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入商品库存', trigger: 'blur' }]
}

async function loadCategories() {
  try {
    const res = await getCategoriesAPI()
    categories.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function loadProduct() {
  if (!isEdit.value) return
  try {
    const res = await getProductByIdAPI(route.params.id)
    const p = res.data
    productOwnerId.value = p.userId
    form.name = p.name || ''
    form.categoryId = p.categoryId || null
    form.price = p.price || null
    form.stock = p.stock ?? 0
    form.description = p.description || ''
    imagePath.value = p.imageUrl || ''
    loadLockedQuantity()
  } catch (e) {
    ElMessage.error('加载商品信息失败')
    router.push('/')
  }
}

/** 锁定数只在自己名下计算；管理员编辑别人的商品时无法求值。 */
async function loadLockedQuantity() {
  if (!isEdit.value || !userStore.isLoggedIn) return
  if (productOwnerId.value != null && userStore.userId !== productOwnerId.value) {
    lockedQuantity.value = 0
    return
  }
  try {
    const res = await getMySoldOrdersAPI()
    const pending = (res.data || []).filter(order => order.status === 0)
    lockedQuantity.value = pending.reduce((sum, order) => {
      const mine = (order.items || [])
        .filter(item => String(item.productId) === String(route.params.id))
      return sum + mine.reduce((s, item) => s + (item.quantity || 0), 0)
    }, 0)
  } catch (e) {
    lockedQuantity.value = 0
  }
}

async function handleRestock() {
  const quantity = restockQuantity.value
  if (!quantity || quantity < 1) {
    ElMessage.warning('补货数量要大于 0')
    return
  }
  restocking.value = true
  try {
    await restockProductAPI(route.params.id, quantity)
    ElMessage.success(`已补货 ${quantity} 件`)
    restockQuantity.value = 1
    const res = await getProductByIdAPI(route.params.id)
    form.stock = res.data?.stock ?? form.stock
  } catch (e) {
  } finally {
    restocking.value = false
  }
}

async function handleUpload(options) {
  try {
    const res = await uploadProductImageAPI(options.file)
    if (res.code === 200 && res.data) {
      imagePath.value = res.data
      ElMessage.success('图片已上传')
      options.onSuccess(res.data)
    } else {
      ElMessage.error(res.message || '上传失败')
      options.onError(new Error(res.message))
    }
  } catch (e) {
    options.onError(e)
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = { ...form, imageUrl: imagePath.value }

    if (isEdit.value) {
      data.id = parseInt(route.params.id)
      // 库存不通过编辑接口提交，改库存只能走补货
      delete data.stock
      await updateProductAPI(data)
      ElMessage.success('修改已保存')
    } else {
      await createProductAPI(data)
      ElMessage.success('已提交，等待审核')
    }
    router.push('/')
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadProduct()
})
</script>

<style scoped>
.form-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 40px;
  align-items: start;
}

.preview {
  position: sticky;
  top: 96px;
}
.preview-label {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 14px;
  text-align: center;
}
.preview-tag {
  position: relative;
  padding: 36px 14px 18px;
  background: var(--paper);
  border-radius: 12px;
  clip-path: polygon(28px 0, calc(100% - 28px) 0, 100% 28px, 100% 100%, 0 100%, 0 28px);
  rotate: -2deg;
  filter: drop-shadow(0 14px 18px rgba(21, 21, 46, 0.16));
}
.preview-hole {
  position: absolute;
  top: 12px;
  left: 50%;
  width: 14px;
  height: 14px;
  margin-left: -7px;
  border-radius: 50%;
  background: var(--board);
  box-shadow: 0 0 0 3px #c9ccdf;
}
.preview-photo-wrap {
  position: relative;
}
.preview-photo {
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  overflow: hidden;
}
.preview-price {
  position: absolute;
  right: -6px;
  bottom: -18px;
}
.preview-name {
  margin-top: 22px;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
}
.preview-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

.form {
  padding: 28px 32px 32px;
}
.form :deep(.el-form-item__label) {
  font-weight: 700;
  color: var(--ink);
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0 16px;
}

.stock-box {
  margin-bottom: 22px;
  padding: 16px 18px;
  border: 2px dashed var(--line);
  border-radius: 12px;
}
.stock-now {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.stock-label {
  font-weight: 700;
}
.stock-value {
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1;
}
.stock-value small {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--muted);
  margin-left: 3px;
}
.stock-locked {
  font-size: 13px;
  font-weight: 600;
  color: #9a5b00;
  background: var(--highlight);
  padding: 2px 8px;
  border-radius: 6px;
}
.restock {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}
.stock-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--muted);
}

.uploader {
  width: 100%;
}
.uploader :deep(.el-upload-dragger) {
  padding: 28px 16px;
  border: 2px dashed var(--line);
  border-radius: 12px;
  background: #fafbfe;
  transition: border-color 0.2s, background 0.2s;
}
.uploader :deep(.el-upload-dragger:hover),
.uploader :deep(.el-upload-dragger.is-dragover) {
  border-color: var(--ultra);
  background: var(--ultra-mist);
}
.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--ink-soft);
  font-size: 14px;
  font-weight: 600;
}
.upload-inner .el-icon {
  color: var(--ultra);
}
.upload-inner small {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

@media (max-width: 820px) {
  .form-layout {
    grid-template-columns: 1fr;
  }
  .preview {
    position: static;
    width: 220px;
    margin: 0 auto;
  }
  .form {
    padding: 22px 18px;
  }
}
</style>
