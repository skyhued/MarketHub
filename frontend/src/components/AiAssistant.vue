<template>
  <div v-if="!hidden" class="assistant">
    <transition name="drawer">
      <section v-if="open" class="assistant-panel" role="dialog" aria-label="客服助手">
        <header class="assistant-head">
          <div class="assistant-head-text">
            <h2 class="assistant-title">客服助手</h2>
            <p class="assistant-sub">基于平台帮助文档回答</p>
          </div>
          <span class="stamp stamp--blue assistant-stamp">本地模型</span>
          <button class="assistant-icon-btn" type="button" title="清空对话"
                  :disabled="messages.length === 0" @click="messages = []">
            <el-icon><Delete /></el-icon>
          </button>
          <button class="assistant-icon-btn" type="button" title="收起" @click="open = false">
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <div ref="listRef" class="assistant-body">
          <p v-if="messages.length === 0" class="assistant-hint">
            我是平台的客服助手，可以解答下单、付款、取消、收货、评价、发布商品和平台规则的问题。
          </p>

          <div v-if="messages.length === 0" class="assistant-suggestions">
            <button v-for="item in suggestions" :key="item" class="assistant-chip"
                    type="button" @click="send(item)">
              {{ item }}
            </button>
          </div>

          <div v-for="(msg, index) in messages" :key="index"
               class="assistant-row" :class="`assistant-row--${msg.role}`">
            <div class="assistant-bubble" :class="{ 'is-failed': msg.failed }">
              <p class="assistant-text">{{ msg.content }}</p>
              <p v-if="msg.latency" class="assistant-meta mono">耗时 {{ (msg.latency / 1000).toFixed(1) }}s</p>
            </div>
          </div>

          <div v-if="pending" class="assistant-row assistant-row--assistant">
            <div class="assistant-bubble assistant-bubble--typing">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>

        <footer class="assistant-foot">
          <template v-if="userStore.isLoggedIn">
            <el-input v-model="input" type="textarea" :rows="2" resize="none" maxlength="500"
                      placeholder="输入问题（回车发送，Shift+回车换行）"
                      @keydown.enter.exact.prevent="send()" />
            <el-button type="primary" size="large" :loading="pending" class="assistant-send"
                       @click="send()">
              发送
            </el-button>
          </template>
          <div v-else class="assistant-need-login">
            <p>助手只对登录用户开放。</p>
            <el-button type="primary" size="large" @click="goLogin">去登录</el-button>
          </div>
        </footer>
      </section>
    </transition>

    <button class="assistant-ball" type="button" :class="{ 'is-open': open }"
            :title="open ? '收起客服助手' : '打开客服助手'" @click="open = !open">
      <el-icon :size="22"><ChatDotRound /></el-icon>
      <span class="assistant-ball-label">问助手</span>
    </button>
  </div>
</template>

<script setup>
import { nextTick, ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Close, Delete } from '@element-plus/icons-vue'
import { askAssistantAPI } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const open = ref(false)
const input = ref('')
const pending = ref(false)
const messages = ref([])
const listRef = ref(null)

/** 后台是另一套界面，登录注册页也还没有身份，这两种页面不显示悬浮球 */
const hidden = computed(() => {
  const path = route.path
  return path.startsWith('/admin') || path === '/login' || path === '/register'
})

const suggestions = [
  '怎么取消订单？',
  '为什么我发布的商品还没上架？',
  '商品卖光了怎么补货？',
  '未付款的订单会怎样？'
]

watch(() => route.fullPath, () => { open.value = false })

async function send(text) {
  const question = (text ?? input.value).trim()
  if (!question || pending.value) return
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }

  messages.value.push({ role: 'user', content: question })
  input.value = ''
  pending.value = true
  await scrollToBottom()

  try {
    const res = await askAssistantAPI(question)
    messages.value.push({ role: 'assistant', content: res.data.answer, latency: res.data.latencyMs })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '助手这次没能回答上来。确认本机 Ollama 已经启动、模型也拉好了，再试一次。',
      failed: true
    })
  } finally {
    pending.value = false
    await scrollToBottom()
  }
}

function goLogin() {
  open.value = false
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}
</script>

<style scoped>
.assistant-ball {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 1500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border: 0;
  border-radius: 999px;
  background: var(--ultra);
  color: #fff;
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 4px 0 var(--ultra-deep), 0 14px 26px -10px rgba(21, 21, 46, 0.5);
  transition: transform 0.15s var(--ease-swing), box-shadow 0.15s;
}
.assistant-ball:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--ultra-deep), 0 18px 30px -10px rgba(21, 21, 46, 0.55);
}
.assistant-ball:active {
  transform: translateY(2px);
  box-shadow: 0 1px 0 var(--ultra-deep);
}
.assistant-ball.is-open {
  background: var(--ink);
  box-shadow: 0 4px 0 #000, 0 14px 26px -10px rgba(21, 21, 46, 0.5);
}

.assistant-panel {
  position: fixed;
  right: 28px;
  bottom: 96px;
  z-index: 1500;
  display: flex;
  flex-direction: column;
  width: min(400px, calc(100vw - 40px));
  height: min(620px, calc(100vh - 140px));
  background: var(--paper);
  border-radius: 18px;
  box-shadow: var(--shadow-tag), 0 30px 60px -24px rgba(21, 21, 46, 0.45);
  overflow: hidden;
}

.assistant-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 14px;
  background: var(--ultra-mist);
  border-bottom: 1.5px dashed var(--line);
}
.assistant-head-text {
  flex: 1;
  min-width: 0;
}
.assistant-title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 24px;
  line-height: 1.1;
}
.assistant-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}
.assistant-stamp {
  font-size: 13px;
  padding: 4px 8px 3px 9px;
  border-width: 2px;
  transform: rotate(-6deg);
}
.assistant-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}
.assistant-icon-btn:hover:not(:disabled) {
  background: var(--paper);
  color: var(--ultra);
}
.assistant-icon-btn:disabled {
  opacity: 0.35;
  cursor: default;
}

.assistant-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: var(--board);
  background-image: radial-gradient(circle at center, rgba(21, 21, 46, 0.07) 1.4px, transparent 1.8px);
  background-size: 22px 22px;
}
.assistant-hint {
  font-size: 13px;
  color: var(--muted);
  background: var(--paper);
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.assistant-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.assistant-chip {
  padding: 8px 12px;
  border: 1.5px solid var(--line);
  border-radius: 999px;
  background: var(--paper);
  color: var(--ink-soft);
  font-family: var(--font-body);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, transform 0.15s;
}
.assistant-chip:hover {
  border-color: var(--ultra);
  color: var(--ultra);
  transform: translateY(-1px);
}

.assistant-row {
  display: flex;
  margin-bottom: 12px;
}
.assistant-row--user {
  justify-content: flex-end;
}
.assistant-bubble {
  max-width: 84%;
  padding: 10px 14px;
  border-radius: 14px;
  background: var(--paper);
  box-shadow: 0 1px 0 rgba(21, 21, 46, 0.05), 0 8px 18px -12px rgba(21, 21, 46, 0.4);
}
.assistant-row--user .assistant-bubble {
  background: var(--ultra);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.assistant-row--assistant .assistant-bubble {
  border-bottom-left-radius: 4px;
}
.assistant-bubble.is-failed {
  border: 1.5px solid var(--stamp);
  color: var(--stamp);
}
.assistant-text {
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
.assistant-meta {
  margin-top: 6px;
  font-size: 11px;
  opacity: 0.6;
}

.assistant-bubble--typing {
  display: inline-flex;
  gap: 5px;
  padding: 14px;
}
.assistant-bubble--typing .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--faint);
  animation: assistant-blink 1.2s infinite ease-in-out;
}
.assistant-bubble--typing .dot:nth-child(2) { animation-delay: 0.18s; }
.assistant-bubble--typing .dot:nth-child(3) { animation-delay: 0.36s; }
@keyframes assistant-blink {
  0%, 80%, 100% { opacity: 0.28; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

.assistant-foot {
  padding: 12px 16px 16px;
  background: var(--paper);
  border-top: 1.5px dashed var(--line);
}
.assistant-send {
  width: 100%;
  margin-top: 8px;
}
.assistant-need-login {
  text-align: center;
  font-size: 14px;
  color: var(--muted);
}
.assistant-need-login p {
  margin-bottom: 10px;
}
.assistant-need-login .el-button {
  width: 100%;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.22s ease, transform 0.22s var(--ease-swing);
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

@media (max-width: 720px) {
  .assistant-ball {
    right: 16px;
    bottom: 16px;
    padding: 12px 16px;
  }
  .assistant-ball-label {
    display: none;
  }
  .assistant-panel {
    right: 12px;
    left: 12px;
    bottom: 76px;
    width: auto;
    height: min(560px, calc(100vh - 110px));
  }
}
</style>
