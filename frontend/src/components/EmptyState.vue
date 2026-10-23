<template>
  <div class="empty">
    <div class="empty-tag" aria-hidden="true">
      <span class="empty-hole"></span>
      <span class="empty-lines"></span>
    </div>
    <p class="empty-title">{{ title }}</p>
    <p v-if="hint" class="empty-hint">{{ hint }}</p>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  hint: { type: String, default: '' }
})
</script>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 56px 16px;
}

.empty-tag {
  position: relative;
  width: 92px;
  height: 120px;
  margin-bottom: 20px;
  border: 2.5px dashed #b4b7cf;
  border-radius: 0 0 12px 12px;
  clip-path: polygon(22px 0, calc(100% - 22px) 0, 100% 22px, 100% 100%, 0 100%, 0 22px);
  background: rgba(255, 255, 255, 0.6);
  rotate: -6deg;
}
.empty-hole {
  position: absolute;
  top: 12px;
  left: 50%;
  width: 12px;
  height: 12px;
  margin-left: -6px;
  border-radius: 50%;
  border: 2.5px solid #b4b7cf;
}
.empty-lines {
  position: absolute;
  left: 18px;
  right: 18px;
  top: 50px;
  height: 40px;
  background: repeating-linear-gradient(#d5d8e8 0 3px, transparent 3px 14px);
  border-radius: 2px;
}

.empty-title {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--ink);
}

.empty-hint {
  margin-top: 6px;
  font-size: 14px;
  color: var(--muted);
}

.empty-action {
  margin-top: 20px;
}
</style>
