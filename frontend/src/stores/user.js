import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginAPI, logoutAPI, registerAPI, getCurrentUserAPI } from '@/api'

/** 登录态只在内存里，token 在 httpOnly Cookie 中；刷新页面由 init() 调 /me 恢复。 */
export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  let initPromise = null

  const isLoggedIn = computed(() => !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const permissions = computed(() => userInfo.value?.permissions ?? [])

  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username)
  const avatarUrl = computed(() => userInfo.value?.avatarUrl)

  function hasPerm(code) {
    return permissions.value.includes(code)
  }

  function setUserInfo(info) {
    userInfo.value = info
  }

  function clear() {
    userInfo.value = null
  }

  async function fetchCurrentUser() {
    try {
      const res = await getCurrentUserAPI()
      userInfo.value = res.data
    } catch (e) {
      userInfo.value = null
    }
  }

  /** 整个应用生命周期只请求一次 /me，路由守卫等它完成后再判断 */
  function init() {
    if (!initPromise) initPromise = fetchCurrentUser()
    return initPromise
  }

  async function login(loginData) {
    await loginAPI(loginData)
    await fetchCurrentUser()
    if (!userInfo.value) throw new Error('登录状态获取失败')
  }

  async function register(registerData) {
    return await registerAPI(registerData)
  }

  async function logout() {
    try {
      await logoutAPI()
    } finally {
      clear()
    }
  }

  return {
    userInfo, isLoggedIn, isAdmin, permissions, userId, username, nickname, avatarUrl,
    hasPerm, setUserInfo, clear, init, login, register, fetchCurrentUser, logout
  }
})
