<template>
  <div class="admin">
    <aside class="side">
      <router-link to="/admin" class="side-brand">
        <span class="side-logo">MarketHub</span>
        <span class="side-sub">管理后台</span>
      </router-link>

      <nav class="side-nav" aria-label="后台导航">
        <router-link v-for="item in items" :key="item.to" :to="item.to" class="side-item" :class="{ active: route.path === item.to }">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <router-link to="/" class="side-item side-back">
        <el-icon><Back /></el-icon>
        <span>回到前台</span>
      </router-link>
    </aside>

    <main class="main">
      <header class="main-head">
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="sub" class="page-sub">{{ sub }}</p>
      </header>
      <slot />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { DataAnalysis, Stamp, Shop, User, Back } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, required: true },
  sub: { type: String, default: '' }
})

const route = useRoute()

const items = [
  { to: '/admin', label: '数据概览', icon: DataAnalysis },
  { to: '/admin/products', label: '商品审核', icon: Stamp },
  { to: '/admin/merchants', label: '开店审核', icon: Shop },
  { to: '/admin/users', label: '用户管理', icon: User }
]
</script>

<style scoped>
.admin {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  min-height: 100vh;
}

.side {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 24px 14px;
  background: var(--ink);
  color: #fff;
}
.side :focus-visible {
  outline-color: var(--highlight);
}

.side-brand {
  display: flex;
  flex-direction: column;
  padding: 0 12px 24px;
}
.side-logo {
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1;
  color: var(--highlight);
}
.side-sub {
  margin-top: 6px;
  font-size: 12px;
  letter-spacing: 0.3em;
  color: rgba(255, 255, 255, 0.5);
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 14px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.65);
  transition: background 0.2s, color 0.2s;
}
.side-item .el-icon {
  font-size: 18px;
}
.side-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.side-item.active {
  background: var(--ultra);
  color: #fff;
}

.side-back {
  margin-top: auto;
}

.main {
  padding: 36px 40px 64px;
  min-width: 0;
}
.main-head {
  margin-bottom: 28px;
}

@media (max-width: 860px) {
  .admin {
    grid-template-columns: 1fr;
  }
  .side {
    position: static;
    height: auto;
    flex-direction: row;
    align-items: center;
    gap: 8px;
    padding: 12px;
    overflow-x: auto;
  }
  .side-brand {
    padding: 0 8px 0 4px;
  }
  .side-logo {
    font-size: 22px;
  }
  .side-sub {
    display: none;
  }
  .side-nav {
    flex-direction: row;
  }
  .side-item {
    flex-shrink: 0;
    height: 38px;
    padding: 0 12px;
    font-size: 14px;
  }
  .side-back {
    margin-top: 0;
    margin-left: auto;
  }
  .main {
    padding: 24px 16px 48px;
  }
}
</style>
