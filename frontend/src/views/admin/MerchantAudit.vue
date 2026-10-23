<template>
  <AdminLayout title="开店审核" :sub="subtitle">
    <el-tabs v-model="tab" @tab-change="loadTab">
      <el-tab-pane name="pending">
        <template #label>
          待审核
          <span v-if="pending.length" class="tab-count">{{ pending.length }}</span>
        </template>

        <TagLoader v-if="loading" />

        <div v-else-if="pending.length" class="queue">
          <article v-for="(a, i) in pending" :key="a.id" class="item" :style="{ '--i': i }">
            <div class="shop-tag">
              <span class="shop-tag-hole" aria-hidden="true"></span>
              <span class="shop-tag-name">{{ a.shopName }}</span>
              <span class="shop-tag-label">申请店名</span>
            </div>

            <div class="item-info">
              <div class="item-row">
                <h3 class="item-name">{{ a.nickname || a.username }}</h3>
                <span class="item-account mono">@{{ a.username }}</span>
              </div>
              <p class="item-meta">
                <span v-if="a.categoryName">主营：{{ a.categoryName }}</span>
                <span>电话：{{ a.contactPhone }}</span>
                <span>{{ dateTime(a.createTime) }} 提交</span>
              </p>
              <p class="item-desc">{{ a.description }}</p>
            </div>

            <div class="item-actions">
              <el-button :disabled="busy" @click="handleReject(a)">驳回</el-button>
              <el-button type="primary" :disabled="busy" :loading="busyId === a.id" @click="handleApprove(a)">
                通过
              </el-button>
            </div>
          </article>
        </div>

        <EmptyState v-else title="暂无待审核申请" hint="买家提交的开店申请会显示在这里" />
      </el-tab-pane>

      <el-tab-pane label="申请记录" name="all">
        <TagLoader v-if="loading" />
        <template v-else>
          <el-table v-if="history.length" :data="history" style="width: 100%">
            <el-table-column label="店铺名" min-width="130">
              <template #default="{ row }">
                <span class="cell-strong">{{ row.shopName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="申请人" min-width="140">
              <template #default="{ row }">
                {{ row.nickname || row.username }}
                <span class="cell-muted mono">@{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类目" min-width="100">
              <template #default="{ row }">{{ row.categoryName || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="status" :class="`status--${statusKey(row.status)}`">{{ statusText(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="处理说明" min-width="180">
              <template #default="{ row }">
                <span :class="{ 'cell-reason': !!reasonOf(row) }">{{ reasonOf(row) || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                <span class="cell-muted">{{ dateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-else title="暂无申请记录" />
        </template>
      </el-tab-pane>

      <el-tab-pane name="merchants">
        <template #label>
          当前商家
          <span v-if="merchants.length" class="tab-count">{{ merchants.length }}</span>
        </template>

        <TagLoader v-if="loading" />
        <template v-else>
          <el-table v-if="merchants.length" :data="merchants" style="width: 100%">
            <el-table-column label="店铺名" min-width="140">
              <template #default="{ row }">
                <span class="cell-strong">{{ row.shopName || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="账号" min-width="140">
              <template #default="{ row }">
                {{ row.nickname || row.username }}
                <span class="cell-muted mono">@{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="170">
              <template #default="{ row }">
                <span class="cell-muted">{{ dateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="right">
              <template #default="{ row }">
                <el-button size="small" :disabled="busy" @click="handleRevoke(row)">收回资格</el-button>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-else title="暂无商家" />
        </template>
      </el-tab-pane>
    </el-tabs>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMerchantApplicationsAPI,
  getMerchantsAPI,
  approveMerchantAPI,
  rejectMerchantAPI,
  revokeMerchantAPI
} from '@/api'
import { dateTime } from '@/utils/format'
import AdminLayout from '@/components/AdminLayout.vue'
import TagLoader from '@/components/TagLoader.vue'
import EmptyState from '@/components/EmptyState.vue'

/** 后端 merchant_application.status 的取值，与 ApiConstants 保持一致 */
const STATUS = { 0: '待审核', 1: '已通过', 2: '已驳回', 3: '已收回' }
const STATUS_KEY = { 0: 'pending', 1: 'passed', 2: 'rejected', 3: 'revoked' }

const tab = ref('pending')
const pending = ref([])
const history = ref([])
const merchants = ref([])
const loading = ref(false)
const busy = ref(false)
const busyId = ref(null)

const subtitle = computed(() => {
  if (loading.value) return ''
  return pending.value.length ? `${pending.value.length} 份申请待审核` : '暂无待审核申请'
})

function statusText(status) {
  return STATUS[status] ?? '未知'
}
function statusKey(status) {
  return STATUS_KEY[status] ?? 'pending'
}
/** 已驳回看驳回原因，已收回看收回原因，其余没有说明 */
function reasonOf(row) {
  if (row.status === 2) return row.rejectReason
  if (row.status === 3) return row.revokeReason
  return null
}

async function loadPending() {
  const res = await getMerchantApplicationsAPI(0)
  pending.value = res.data || []
}

async function loadHistory() {
  const res = await getMerchantApplicationsAPI()
  history.value = res.data || []
}

async function loadMerchants() {
  const res = await getMerchantsAPI()
  merchants.value = res.data || []
}

async function loadTab() {
  loading.value = true
  try {
    if (tab.value === 'pending') await loadPending()
    else if (tab.value === 'all') await loadHistory()
    else await loadMerchants()
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function handleApprove(application) {
  try {
    await ElMessageBox.confirm(
      `通过后「${application.shopName}」即成为商家，该用户需重新登录后生效。`,
      '通过开店申请',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  busy.value = true
  busyId.value = application.id
  try {
    await approveMerchantAPI(application.id)
    ElMessage.success('已通过，该用户重新登录后生效')
    await loadTab()
  } catch (e) {
  } finally {
    busy.value = false
    busyId.value = null
  }
}

async function handleReject(application) {
  const reason = await promptReason(
    `驳回「${application.shopName}」的申请`,
    '驳回原因（申请人可见）'
  )
  if (!reason) return
  busy.value = true
  try {
    await rejectMerchantAPI(application.id, reason)
    ElMessage.success('已驳回')
    await loadTab()
  } catch (e) {
  } finally {
    busy.value = false
  }
}

async function handleRevoke(merchant) {
  const reason = await promptReason(
    `收回「${merchant.shopName || merchant.username}」的商家资格`,
    '收回原因。商家有未完成订单时无法收回。'
  )
  if (!reason) return
  busy.value = true
  try {
    await revokeMerchantAPI(merchant.id, reason)
    ElMessage.success('已收回商家资格')
    await loadTab()
  } catch (e) {
  } finally {
    busy.value = false
  }
}

async function promptReason(title, placeholder) {
  try {
    const { value } = await ElMessageBox.prompt(placeholder, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '不超过 255 字',
      inputValidator: v => (v && v.trim() ? true : '必须填写原因')
    })
    return value.trim()
  } catch (e) {
    return null
  }
}

onMounted(loadTab)
</script>

<style scoped>
.tab-count {
  display: inline-block;
  min-width: 18px;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--ultra);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.queue {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.item {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  padding: 16px 20px;
  background: var(--paper);
  border-radius: 14px;
  box-shadow: var(--shadow-tag);
  animation: rise 0.4s ease both;
  animation-delay: calc(min(var(--i), 10) * 40ms);
}

.shop-tag {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  height: 92px;
  padding: 12px 10px 12px 18px;
  background: var(--ultra-mist);
  border: 2px solid var(--ultra);
  border-radius: 4px 12px 12px 4px;
  transform: rotate(-2deg);
}
.shop-tag-hole {
  position: absolute;
  left: 7px;
  top: 50%;
  width: 7px;
  height: 7px;
  margin-top: -3.5px;
  border-radius: 50%;
  background: var(--board);
  box-shadow: inset 0 0 0 1.5px var(--ultra);
}
.shop-tag-name {
  font-family: var(--font-display);
  font-size: 19px;
  line-height: 1.15;
  color: var(--ultra-deep);
  word-break: break-all;
}
.shop-tag-label {
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--muted);
}

.item-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.item-name {
  font-size: 17px;
  font-weight: 800;
}
.item-account {
  font-size: 12px;
  color: var(--muted);
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

.cell-strong {
  font-weight: 700;
}
.cell-muted {
  margin-left: 6px;
  font-size: 12px;
  color: var(--muted);
}
.cell-reason {
  color: var(--stamp);
}

.status {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.status--pending { background: var(--highlight); color: var(--ink); }
.status--passed { background: #e3f3ea; color: var(--ok); }
.status--rejected { background: #fbe9ec; color: var(--stamp); }
.status--revoked { background: #eceef6; color: var(--muted); }

@media (max-width: 860px) {
  .item {
    grid-template-columns: 104px minmax(0, 1fr);
    padding: 14px;
    gap: 14px;
  }
  .shop-tag {
    height: 78px;
    padding-left: 16px;
  }
  .shop-tag-name {
    font-size: 16px;
  }
  .item-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
