<template>
  <AuthShell>
    <template #title>注册账号，<br /><em>开始买卖。</em></template>
    <template #sub>注册后可加购物车、收藏和下单。</template>

    <h2 class="auth-heading">注册</h2>
    <p class="auth-hint">只需要用户名和密码</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="handleRegister">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" />
      </el-form-item>
      <el-form-item label="昵称（选填，别人看到的名字）" prop="nickname">
        <el-input v-model="form.nickname" :prefix-icon="EditPen" placeholder="不填则使用用户名" />
      </el-form-item>
      <el-form-item label="密码（至少 6 位）" prop="password">
        <el-input v-model="form.password" type="password" :prefix-icon="Lock" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="再输一次密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" :prefix-icon="Lock" show-password autocomplete="new-password" />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" class="auth-submit" @click="handleRegister">注册</el-button>
    </el-form>

    <p class="auth-foot">
      已经有账号？<router-link :to="{ path: '/login', query: route.query }">去登录</router-link>
    </p>
  </AuthShell>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AuthShell from '@/components/AuthShell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const registerData = {
      username: form.username,
      password: form.password,
      nickname: form.nickname || form.username
    }
    await userStore.register(registerData)
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: route.query })
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>
