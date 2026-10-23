<template>
  <AuthShell>
    <template #title>登录账号，<br /><em>继续买卖。</em></template>
    <template #sub>购物车、收藏和订单都保存在账号中。</template>

    <h2 class="auth-heading">登录</h2>
    <p class="auth-hint">用你的 MarketHub 账号登录</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="handleLogin">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" :prefix-icon="Lock" show-password autocomplete="current-password" />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" class="auth-submit" @click="handleLogin">登录</el-button>
    </el-form>

    <p class="auth-foot">
      还没有账号？<router-link :to="{ path: '/register', query: route.query }">注册一个</router-link>
    </p>
  </AuthShell>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AuthShell from '@/components/AuthShell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')

    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : null
    router.push(redirect || (userStore.isAdmin ? '/admin' : '/'))
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>
