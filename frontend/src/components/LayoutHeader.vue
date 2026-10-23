<template>
  <header class="bar">
    <div class="bar-inner">
      <router-link to="/" class="logo" aria-label="MarketHub 首页">
        <svg class="logo-mark" viewBox="0 0 32 32" aria-hidden="true">
          <path d="M9 3h14l6 6v18a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V9z" fill="#ffe14d" />
          <circle cx="16" cy="9.5" r="2.6" fill="#2b2bd9" />
          <path d="M9 17h14M9 22h9" stroke="#15152e" stroke-width="2.4" stroke-linecap="round" />
        </svg>
        <span class="logo-text">MarketHub</span>
      </router-link>

      <nav class="nav" aria-label="主导航">
        <router-link to="/" class="nav-item" :class="{ active: isHome }">
          <el-icon><Shop /></el-icon><span>逛逛</span>
        </router-link>
        <template v-if="userStore.isLoggedIn">
          <router-link to="/cart" class="nav-item" :class="{ active: route.path === '/cart' }">
            <el-icon><ShoppingCart /></el-icon><span>购物车</span>
          </router-link>
          <router-link to="/favorite" class="nav-item" :class="{ active: route.path === '/favorite' }">
            <el-icon><Star /></el-icon><span>收藏</span>
          </router-link>
          <router-link to="/orders" class="nav-item" :class="{ active: route.path.startsWith('/order') }">
            <el-icon><Tickets /></el-icon><span>订单</span>
          </router-link>
        </template>
      </nav>

      <div class="bar-right">
        <router-link
          v-if="userStore.hasPerm('product:create')"
          to="/product/create"
          class="sell-btn"
          :class="{ active: route.path === '/product/create' }"
        >
          <el-icon><Plus /></el-icon><span>发布闲置</span>
        </router-link>

        <el-dropdown v-if="userStore.isLoggedIn" trigger="click" placement="bottom-end">
          <button type="button" class="me">
            <el-avatar :size="34" :src="avatarImageUrl(userStore.avatarUrl)" class="me-avatar">
              {{ userStore.nickname?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <span class="me-name">{{ userStore.nickname }}</span>
            <el-icon class="me-caret"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" @click="router.push('/admin')">
                <el-icon><Setting /></el-icon>管理后台
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <template v-else>
          <router-link to="/login" class="auth-login">登录</router-link>
          <router-link to="/register" class="auth-register">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { avatarImageUrl } from '@/utils/format'
import { Shop, ShoppingCart, Star, Tickets, Plus, ArrowDown, User, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isHome = computed(() => route.path === '/' || route.path === '/products')

async function handleLogout() {
  await userStore.logout().catch(() => {})
  router.push('/')
}
</script>

<style scoped>
.bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--ultra);
  color: #fff;
}

.bar-inner {
  max-width: 1200px;
  height: 64px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.bar :focus-visible {
  outline-color: var(--highlight);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.logo-mark {
  width: 30px;
  height: 30px;
  rotate: -10deg;
  transition: rotate 0.4s var(--ease-swing);
}
.logo:hover .logo-mark {
  rotate: 8deg;
}
.logo-text {
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0.02em;
}

.nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  height: 100%;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  padding: 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.72);
  transition: color 0.2s;
}
.nav-item:hover {
  color: #fff;
}
.nav-item.active {
  color: #fff;
}
.nav-item.active::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 0;
  height: 5px;
  background: var(--highlight);
  border-radius: 3px 3px 0 0;
}

.bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.sell-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  background: var(--highlight);
  box-shadow: 0 3px 0 #c9a800;
  transition: transform 0.15s, box-shadow 0.15s;
}
.sell-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 0 #c9a800;
}
.sell-btn:active {
  transform: translateY(2px);
  box-shadow: 0 1px 0 #c9a800;
}

.me {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px 3px 3px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font: inherit;
  cursor: pointer;
  transition: background 0.2s;
}
.me:hover {
  background: rgba(255, 255, 255, 0.22);
}
.me-avatar {
  background: var(--highlight);
  color: var(--ink);
  font-weight: 800;
}
.me-name {
  max-width: 96px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.me-caret {
  font-size: 12px;
  opacity: 0.8;
}

.auth-login {
  height: 38px;
  display: inline-flex;
  align-items: center;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  background: var(--highlight);
}
.auth-register {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}
.auth-register:hover {
  color: #fff;
}

@media (max-width: 860px) {
  .bar-inner {
    gap: 12px;
    padding: 0 14px;
  }
  .nav-item {
    padding: 0 10px;
  }
  .nav-item span,
  .sell-btn span,
  .me-name {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }
  .nav-item .el-icon {
    font-size: 20px;
  }
  .sell-btn {
    padding: 0 11px;
  }
}

@media (max-width: 480px) {
  .logo-text {
    display: none;
  }
}
</style>
