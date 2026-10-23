import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

/** HTTP 请求工具：统一经过网关 8080，认证靠 httpOnly Cookie；silent 可关掉错误提示和跳转。 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

/** 可选登录接口返回 X-Token-Expired 时清掉登录态并提示一次。 */
function handleTokenExpired(response) {
  if (response?.headers?.['x-token-expired'] !== '1') return

  const userStore = useUserStore()
  if (!userStore.isLoggedIn) return

  userStore.clear()
  ElMessage.warning('登录已失效，请重新登录')
}

request.interceptors.response.use(
  response => {
    handleTokenExpired(response)

    const res = response.data
    // 适应微服务统一响应格式 R<T>（code + message + data）
    if (res.code === 200) {
      return res
    }
    if (!response.config.silent) ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    handleTokenExpired(error.response)

    if (error.config?.silent) return Promise.reject(error)

    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        const userStore = useUserStore()
        const wasLoggedIn = userStore.isLoggedIn
        userStore.clear()
        ElMessage.error(wasLoggedIn ? '登录已失效，请重新登录' : '请先登录')
        const current = router.currentRoute.value
        if (current.path !== '/login') {
          router.push({ path: '/login', query: { redirect: current.fullPath } })
        }
      } else if (status === 403) {
        ElMessage.error('没有权限访问')
      } else {
        ElMessage.error(data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
