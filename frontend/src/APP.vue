<template>
  <LayoutHeader v-if="!isAdminRoute" />
  <router-view v-slot="{ Component, route: r }">
    <transition name="route" mode="out-in">
      <component :is="Component" :key="r.fullPath" />
    </transition>
  </router-view>
  <AiAssistant />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LayoutHeader from '@/components/LayoutHeader.vue'
import AiAssistant from '@/components/AiAssistant.vue'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>
