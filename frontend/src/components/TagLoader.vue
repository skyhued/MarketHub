<template>
  <div class="loader" role="status">
    <span class="loader-tag" aria-hidden="true"></span>
    <span class="loader-text">{{ text }}</span>
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '加载中' }
})
</script>

<style scoped>
.loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 72px 16px;
  color: var(--muted);
  font-size: 14px;
}

.loader-tag {
  position: relative;
  width: 34px;
  height: 44px;
  background: var(--ultra);
  border-radius: 0 0 6px 6px;
  clip-path: polygon(9px 0, calc(100% - 9px) 0, 100% 9px, 100% 100%, 0 100%, 0 9px);
  transform-origin: 50% -10px;
  animation: loader-swing 1s ease-in-out infinite alternate;
}
.loader-tag::after {
  content: '';
  position: absolute;
  top: 6px;
  left: 50%;
  width: 7px;
  height: 7px;
  margin-left: -3.5px;
  border-radius: 50%;
  background: var(--board);
}

@keyframes loader-swing {
  from { rotate: -14deg; }
  to { rotate: 14deg; }
}
</style>
