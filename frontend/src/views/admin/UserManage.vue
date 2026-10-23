<template>
  <AdminLayout title="用户管理" :sub="loading ? '' : `共 ${users.length} 个账号`">
    <div class="panel table-panel">
      <el-table v-loading="loading" :data="users" style="width: 100%">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="who">
              <el-avatar :size="38" :src="avatarImageUrl(row.avatarUrl)" class="who-avatar">
                {{ (row.nickname || row.username)?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="who-text">
                <span class="who-name">{{ row.nickname || row.username }}</span>
                <span class="who-user mono">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <span class="role" :class="`role--${(row.role?.roleCode || 'USER').toLowerCase()}`">
              {{ roleName(row.role?.roleCode) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="state" :class="{ off: row.status !== 1 }">{{ row.status === 1 ? '正常' : '已禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loading && users.length === 0" title="暂无用户" />
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUserAPI, getAllUsersAPI, updateUserStatusAPI } from '@/api'
import { avatarImageUrl, dateTime } from '@/utils/format'
import AdminLayout from '@/components/AdminLayout.vue'
import EmptyState from '@/components/EmptyState.vue'

const users = ref([])
const loading = ref(false)

function roleName(code) {
  return { ADMIN: '管理员', MERCHANT: '商家' }[code] || '买家'
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getAllUsersAPI()
    users.value = (res.data || []).map(u => ({
      ...u,
      status: u.status ?? 1
    }))
  } catch (e) {
    users.value = []
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    if (newStatus === 0) {
      await ElMessageBox.confirm(`禁用后「${user.username}」会立即退出登录，并且无法再登录。`, '禁用这个账号？', {
        type: 'warning',
        confirmButtonText: '禁用',
        cancelButtonText: '取消'
      })
    }
    await updateUserStatusAPI(user.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    await loadUsers()
  } catch (e) {
  }
}

async function handleDelete(user) {
  try {
    await ElMessageBox.confirm(`删除后「${user.username}」的账号无法恢复。`, '删除这个用户？', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteUserAPI(user.id)
    ElMessage.success('已删除')
    await loadUsers()
  } catch (e) {
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.table-panel {
  padding: 8px 12px 12px;
}

.who {
  display: flex;
  align-items: center;
  gap: 12px;
}
.who-avatar {
  flex-shrink: 0;
  background: var(--ultra-mist);
  color: var(--ultra);
  font-family: var(--font-display);
  font-size: 18px;
}
.who-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.who-name {
  font-weight: 700;
  color: var(--ink);
}
.who-user {
  font-size: 12px;
  color: var(--muted);
}

.role {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  background: #eceef6;
  color: var(--ink-soft);
}
.role--admin {
  background: var(--ink);
  color: var(--highlight);
}
.role--merchant {
  background: var(--highlight);
  color: var(--ink);
}

.state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ok);
}
.state::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}
.state.off {
  color: var(--stamp);
}
</style>
