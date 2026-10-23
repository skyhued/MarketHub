<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-sub">账号资料与安全</p>
      </div>
    </div>

    <div class="profile">
      <aside class="badge-wrap">
        <span class="lanyard" aria-hidden="true"></span>
        <div class="badge">
          <span class="badge-slot" aria-hidden="true"></span>
          <div class="badge-top">
            <span class="badge-brand">MarketHub</span>
            <span class="badge-role">{{ roleName }}</span>
          </div>
          <div class="badge-body">
            <button type="button" class="badge-avatar" aria-label="更换头像" @click="triggerUpload">
              <el-avatar :size="104" :src="previewAvatar">
                {{ userStore.nickname?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="badge-avatar-edit"><el-icon><Camera /></el-icon></span>
            </button>
            <input ref="fileInput" type="file" accept="image/*" hidden @change="handleFileChange" />
            <p class="badge-name">{{ userStore.nickname }}</p>
            <p class="badge-user mono">@{{ userStore.username }}</p>
            <p v-if="avatarFile" class="badge-pending">新头像会在点「保存修改」后生效</p>
          </div>
          <div class="badge-foot mono">ID {{ String(userStore.userId ?? '').padStart(6, '0') }}</div>
        </div>
      </aside>

      <div class="panel settings">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="info">
            <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-position="top" class="settings-form form-strong">
              <el-form-item label="用户名（登录用，不能改）">
                <el-input :model-value="userStore.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="infoForm.nickname" placeholder="别人看到的名字" />
              </el-form-item>
              <el-button type="primary" :loading="saving" @click="handleUpdateInfo">保存修改</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="settings-form form-strong">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <el-form-item label="新密码（至少 6 位）" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <el-form-item label="再输一次新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <p class="settings-note">改密码后，所有设备都需要用新密码重新登录。</p>
              <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <ShopApplyCard />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserAPI, uploadAvatarAPI, changePasswordAPI } from '@/api'
import { avatarImageUrl } from '@/utils/format'
import ShopApplyCard from '@/components/ShopApplyCard.vue'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('info')
const fileInput = ref(null)
const saving = ref(false)
const changingPwd = ref(false)
const infoFormRef = ref(null)
const pwdFormRef = ref(null)
const avatarFile = ref(null)

const infoForm = reactive({
  nickname: userStore.nickname || ''
})

const roleName = computed(() => ({ ADMIN: '管理员', MERCHANT: '商家', USER: '买家' }[userStore.userInfo?.role] || '会员'))

const previewAvatar = computed(() => {
  if (avatarFile.value) {
    return URL.createObjectURL(avatarFile.value)
  }
  return avatarImageUrl(userStore.avatarUrl)
})

const infoRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

onMounted(() => {
  infoForm.nickname = userStore.nickname || ''
})

function triggerUpload() {
  fileInput.value.click()
}

function handleFileChange(e) {
  const file = e.target.files[0]
  if (file) {
    avatarFile.value = file
  }
}

async function handleUpdateInfo() {
  const valid = await infoFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateUserAPI({ nickname: infoForm.nickname })

    if (avatarFile.value) {
      const res = await uploadAvatarAPI(avatarFile.value)
      userStore.setUserInfo({ ...userStore.userInfo, avatarUrl: res.data, nickname: infoForm.nickname })
      avatarFile.value = null
    } else {
      userStore.setUserInfo({ ...userStore.userInfo, nickname: infoForm.nickname })
    }

    ElMessage.success('修改已保存')
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPwd.value = true
  try {
    await changePasswordAPI({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    // 后端已作废所有旧 token（包括当前这个），需要用新密码重新登录
    userStore.clear()
    ElMessage.success('密码已修改，请用新密码重新登录')
    router.push('/login')
  } catch (e) {
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.profile {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 40px;
  align-items: start;
}

.badge-wrap {
  position: relative;
  padding-top: 64px;
}
.lanyard {
  position: absolute;
  top: 0;
  left: 50%;
  width: 30px;
  height: 84px;
  margin-left: -15px;
  background: repeating-linear-gradient(90deg, var(--highlight) 0 10px, #f5cf1a 10px 20px);
  border-radius: 0 0 4px 4px;
  z-index: 0;
}
.lanyard::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  width: 20px;
  height: 16px;
  margin-left: -10px;
  border-radius: 4px;
  background: #9a9cb8;
  box-shadow: inset 0 -3px 0 rgba(0, 0, 0, 0.2);
}

.badge {
  position: relative;
  z-index: 1;
  background: var(--paper);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 1px 0 rgba(21, 21, 46, 0.06), 0 24px 40px -20px rgba(21, 21, 46, 0.45);
  rotate: 2deg;
  transform-origin: 50% -64px;
  animation: badge-in 1s var(--ease-swing) both;
}
@keyframes badge-in {
  from { rotate: -10deg; }
  to { rotate: 2deg; }
}

.badge-slot {
  position: absolute;
  top: 12px;
  left: 50%;
  width: 44px;
  height: 10px;
  margin-left: -22px;
  border-radius: 5px;
  background: var(--board);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.25);
  z-index: 2;
}

.badge-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 36px 20px 14px;
  background: var(--ultra);
  color: #fff;
}
.badge-brand {
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1;
}
.badge-role {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--highlight);
  color: var(--ink);
  font-size: 12px;
  font-weight: 800;
}

.badge-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 20px 20px;
  text-align: center;
}

.badge-avatar {
  position: relative;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: none;
  cursor: pointer;
}
.badge-avatar :deep(.el-avatar) {
  border: 4px solid var(--paper);
  box-shadow: 0 0 0 2px var(--line);
  background: var(--highlight);
  color: var(--ink);
  font-family: var(--font-display);
  font-size: 44px;
}
.badge-avatar-edit {
  position: absolute;
  right: 2px;
  bottom: 4px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  border: 3px solid var(--paper);
  transition: background 0.2s;
}
.badge-avatar:hover .badge-avatar-edit {
  background: var(--ultra);
}

.badge-name {
  margin-top: 14px;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1.1;
  overflow-wrap: anywhere;
}
.badge-user {
  margin-top: 4px;
  font-size: 13px;
  color: var(--muted);
}
.badge-pending {
  margin-top: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--highlight);
  font-size: 12px;
  font-weight: 600;
}

.badge-foot {
  padding: 10px 20px;
  border-top: 1.5px dashed var(--line);
  font-size: 12px;
  color: var(--muted);
  text-align: center;
  letter-spacing: 0.2em;
}

.settings {
  padding: 12px 32px 32px;
}
.settings-form {
  max-width: 440px;
  padding-top: 12px;
}
.settings-note {
  margin: -4px 0 18px;
  font-size: 13px;
  color: var(--muted);
}

@media (max-width: 820px) {
  .profile {
    grid-template-columns: 1fr;
  }
  .badge-wrap {
    width: min(300px, 100%);
    margin: 0 auto;
  }
  .settings {
    padding: 8px 18px 24px;
  }
}
</style>
