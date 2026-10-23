import request from '@/utils/request'

export const loginAPI = (data) => request.post('/users/login', data)
export const logoutAPI = () => request.post('/users/logout', null, { silent: true })
export const registerAPI = (data) => request.post('/users/register', data)
// 应用启动时用来判断是否已登录，未登录返回 401 属于正常情况，不弹提示
export const getCurrentUserAPI = () => request.get('/users/me', { silent: true })
export const getUserByIdAPI = (id) => request.get(`/users/${id}`)
export const updateUserAPI = (data) => request.put('/users/update', data)
export const uploadAvatarAPI = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const changePasswordAPI = (data) => request.post('/users/change-password', data)

export const getProductsAPI = () => request.get('/products')
export const getAdminProductsAPI = (params) => request.get('/admin/products', { params })
export const getOnSaleProductsAPI = () => request.get('/products/on-sale')
export const getProductByIdAPI = (id) => request.get(`/products/${id}`)
export const searchProductsAPI = (params) => request.get('/products/search', { params })
export const createProductAPI = (data) => request.post('/products', data)
export const updateProductAPI = (data) => request.put('/products', data)
export const deleteProductAPI = (id) => request.delete(`/products/${id}`)
// 补货只增不减：库存里已经扣掉了待付款订单占用的部分，不能直接覆盖
export const restockProductAPI = (id, quantity) => request.put(`/products/${id}/restock`, { quantity })
export const toggleProductStatusAPI = (id) => request.put(`/products/${id}/toggle-status`)
export const uploadProductImageAPI = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/products/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getCategoriesAPI = () => request.get('/categories')

export const getCartListAPI = () => request.get('/cart')
export const addToCartAPI = (data) => request.post('/cart', data)
export const updateCartQuantityAPI = (data) => request.put('/cart/quantity', data)
export const removeFromCartAPI = (productId) => request.delete(`/cart/${productId}`)
export const updateCartCheckedAPI = (data) => request.put('/cart/checked', data)

export const getFavoriteListAPI = () => request.get('/favorite')
export const addFavoriteAPI = (productId) => request.post(`/favorite/${productId}`)
export const removeFavoriteAPI = (productId) => request.delete(`/favorite/${productId}`)
export const checkFavoriteAPI = (productId) => request.get(`/favorite/check/${productId}`)
export const getFavoriteCountAPI = () => request.get('/favorite/count')

export const getMyOrdersAPI = () => request.get('/orders/my-orders')
export const getMySoldOrdersAPI = () => request.get('/orders/my-sold')
export const getOrderByIdAPI = (id) => request.get(`/orders/${id}`)
export const getOrderByNoAPI = (orderNo) => request.get(`/orders/no/${orderNo}`)
export const createOrderFromCartAPI = (data) => request.post('/orders/from-cart', data)
export const payOrderAPI = (id) => request.post(`/orders/${id}/pay`)
export const shipOrderAPI = (id) => request.post(`/orders/${id}/ship`)
export const confirmReceiptAPI = (id) => request.post(`/orders/${id}/confirm`)
export const cancelOrderAPI = (id) => request.post(`/orders/${id}/cancel`)

export const addReviewAPI = (data) => request.post('/reviews', data)
export const getProductReviewsAPI = (productId) => request.get(`/reviews/${productId}`)
// 只返回当前用户在这件商品下的评价，用来把已评过的订单从下拉里剔除
export const getMyReviewsAPI = (productId) => request.get(`/reviews/mine/${productId}`)

export const auditProductAPI = (productId, status) => request.post('/admin/product/audit', null, {
  params: { productId, status }
})
export const updateUserStatusAPI = (userId, status) => request.post('/admin/user/status', null, {
  params: { userId, status }
})
export const deleteUserAPI = (id) => request.delete(`/admin/users/${id}`)
export const getAdminStatsAPI = () => request.get('/admin/stats')
export const getAllUsersAPI = () => request.get('/admin/users')
export const getAllOrdersAPI = () => request.get('/admin/orders')
export const getRecommendAPI = () => request.get('/recommend/me')
export const getRelatedProductsAPI = (productId) => request.get(`/recommend/related/${productId}`)

export const applyMerchantAPI = (data) => request.post('/merchant/apply', data)
export const getMyMerchantApplicationAPI = () => request.get('/merchant/my-application')
export const markMerchantResultSeenAPI = () => request.post('/merchant/result-seen', null, { silent: true })
export const getMerchantApplicationsAPI = (status) => request.get('/merchant/applications', { params: { status } })
export const getMerchantPendingCountAPI = () => request.get('/merchant/pending-count', { silent: true })
export const getMerchantsAPI = () => request.get('/merchant/merchants')
export const approveMerchantAPI = (id) => request.post(`/merchant/applications/${id}/approve`)
export const rejectMerchantAPI = (id, reason) => request.post(`/merchant/applications/${id}/reject`, null, { params: { reason } })
export const revokeMerchantAPI = (userId, reason) => request.post(`/merchant/merchants/${userId}/revoke`, null, { params: { reason } })

// 模型在本地跑，第一次提问要等它把模型加载进显存，所以单独给一个很宽松的超时，
// 不能沿用 request.js 里那个 15 秒的默认值。
export const askAssistantAPI = (question) => request.post('/ai/ask', { question }, { timeout: 180000 })
export const getAssistantStatusAPI = () => request.get('/ai/admin/status', { silent: true })
export const reindexAssistantAPI = () => request.post('/ai/admin/reindex', null, { timeout: 600000 })
export const warmupAssistantAPI = () => request.post('/ai/admin/warmup', null, { timeout: 300000 })
