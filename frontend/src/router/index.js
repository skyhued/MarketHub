import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },

  {
      path: '/',
      name: 'ProductList',
      component: () => import('@/views/product/ProductList.vue'),
      meta: { title: '商品列表' },
      alias: '/products'
    },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/ProductDetail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/product/create',
    name: 'CreateProduct',
    component: () => import('@/views/product/ProductForm.vue'),
    meta: { title: '发布商品', requiresAuth: true, perm: 'product:create' }
  },
  {
    path: '/product/edit/:id',
    name: 'EditProduct',
    component: () => import('@/views/product/ProductForm.vue'),
    meta: { title: '编辑商品', requiresAuth: true, perm: 'product:update' }
  },

  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/cart/Cart.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },

  {
    path: '/favorite',
    name: 'Favorite',
    component: () => import('@/views/favorite/Favorite.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },

  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('@/views/order/OrderList.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/OrderDetail.vue'),
    meta: { title: '订单详情', requiresAuth: true }
  },

  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/products',
    name: 'AdminProductAudit',
    component: () => import('@/views/admin/ProductAudit.vue'),
    meta: { title: '商品审核', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/merchants',
    name: 'AdminMerchantAudit',
    component: () => import('@/views/admin/MerchantAudit.vue'),
    meta: { title: '开店审核', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUserManage',
    component: () => import('@/views/admin/UserManage.vue'),
    meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - MarketHub` : 'MarketHub'

  const userStore = useUserStore()
  await userStore.init()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return '/'
  }
  if (to.meta.perm && !userStore.hasPerm(to.meta.perm)) {
    ElMessage.warning('当前账号没有权限访问该页面')
    return '/'
  }
})

export default router
