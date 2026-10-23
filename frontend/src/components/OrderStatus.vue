<template>
  <span class="status" :class="`status--${status}`">{{ statusText }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: Number,
    required: true
  }
})

const statusMap = {
  0: '待付款',
  1: '已付款',
  2: '已发货',
  3: '已完成',
  4: '已取消'
}

const statusText = computed(() => statusMap[props.status] || '未知')
</script>

<style scoped>
.status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
  white-space: nowrap;
  color: var(--muted);
  background: #eceef6;
}
.status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status--0 { color: #6b5100; background: var(--highlight); }
.status--1 { color: var(--ultra); background: var(--ultra-mist); }
.status--2 { color: var(--ok); background: #d4f2e1; }
.status--3 { color: var(--muted); background: #eceef6; }
.status--4 { color: var(--stamp); background: #fbe9ec; }
</style>
