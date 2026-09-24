<template>
  <div v-if="isVisibleRoute" class="chatbot-widget">
    <!-- 浮動按鈕 -->
    <button
      v-if="!isOpen"
      class="chatbot-toggle-btn"
      @click="isOpen = true"
    >
      <svg viewBox="0 0 64 64" width="34" height="34" aria-hidden="true">
        <circle cx="32" cy="32" r="30" fill="#37373c" />
        <line x1="32" y1="24" x2="32" y2="18" stroke="#fafafa" stroke-width="2.2" stroke-linecap="round" />
        <circle cx="32" cy="16" r="2.4" fill="#fafafa" />
        <circle cx="13" cy="42" r="3" fill="#fafafa" />
        <circle cx="51" cy="42" r="3" fill="#fafafa" />
        <rect x="18" y="27" width="28" height="24" rx="8" fill="#fafafa" />
        <circle cx="26" cy="40" r="2.6" fill="#37373c" />
        <circle cx="38" cy="40" r="2.6" fill="#37373c" />
        <path d="M 24 46 Q 32 51 40 46" fill="none" stroke="#37373c" stroke-width="2.2" stroke-linecap="round" />
      </svg>
    </button>

    <!-- 聊天視窗 -->
    <div v-else class="chatbot-panel">
      <div class="chatbot-header">
        <div class="chatbot-header-left">
          <svg class="chatbot-header-avatar" viewBox="0 0 64 64" width="26" height="26" aria-hidden="true">
            <circle cx="32" cy="32" r="30" fill="#37373c" />
            <line x1="32" y1="24" x2="32" y2="18" stroke="#fafafa" stroke-width="2.2" stroke-linecap="round" />
            <circle cx="32" cy="16" r="2.4" fill="#fafafa" />
            <circle cx="13" cy="42" r="3" fill="#fafafa" />
            <circle cx="51" cy="42" r="3" fill="#fafafa" />
            <rect x="18" y="27" width="28" height="24" rx="8" fill="#fafafa" />
            <circle cx="26" cy="40" r="2.6" fill="#37373c" />
            <circle cx="38" cy="40" r="2.6" fill="#37373c" />
            <path d="M 24 46 Q 32 51 40 46" fill="none" stroke="#37373c" stroke-width="2.2" stroke-linecap="round" />
          </svg>
          <span>SneakerMarket 客服機器人</span>
        </div>
        <button class="chatbot-close-btn" @click="isOpen = false">✕</button>
      </div>

      <div class="chatbot-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="chatbot-empty-hint">
          你好！有什麼購物上的問題都可以問我。
        </div>
        <div v-if="messages.length === 0" class="chatbot-quick-replies">
          <p class="chatbot-quick-title">猜你想問...</p>
          <button
            v-for="(q, index) in quickQuestions"
            :key="index"
            type="button"
            class="chatbot-quick-reply-btn"
            @click="askQuickQuestion(q)"
          >
            {{ q }}
          </button>
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['chatbot-message', msg.role === 'user' ? 'from-user' : 'from-bot']"
        >
          {{ msg.text }}
        </div>
        <div v-if="isLoading" class="chatbot-message from-bot chatbot-loading">
          思考中...
        </div>
      </div>

      <form class="chatbot-input-area" @submit.prevent="sendMessage">
        <input
          v-model="inputText"
          type="text"
          placeholder="輸入你的問題..."
          :disabled="isLoading"
        />
        <button type="submit" :disabled="isLoading || !inputText.trim()">
          送出
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import httpClient from '@/service/httpClient'

const route = useRoute()
const isVisibleRoute = computed(
  () => route.path === '/products' || route.path.startsWith('/products/')
)

const isOpen = ref(false)
const inputText = ref('')
const isLoading = ref(false)
const messages = ref([])
const messagesContainer = ref(null)
const quickQuestions = ['運費怎麼算？', '付款方式有哪些？', '大概多久會到貨？']

function askQuickQuestion(question) {
  inputText.value = question
  sendMessage()
}

async function sendMessage() {
  const question = inputText.value.trim()
  if (!question) return

  messages.value.push({ role: 'user', text: question })
  inputText.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const response = await httpClient.post('/chatbot/ask', { question }, { timeout: 30000 })
    messages.value.push({ role: 'bot', text: response.data.answer })
  } catch (error) {
    console.error('聊天機器人呼叫失敗', error)
    messages.value.push({
      role: 'bot',
      text: '抱歉，機器人暫時無法回覆，請稍後再試或聯繫客服。'
    })
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.chatbot-widget {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

.chatbot-toggle-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.chatbot-panel {
  width: 320px;
  height: 440px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chatbot-header {
  background-color: #111;
  color: white;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.chatbot-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chatbot-header-avatar {
  flex-shrink: 0;
  display: block;
}

.chatbot-close-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
}

.chatbot-messages {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chatbot-empty-hint {
  color: #888;
  font-size: 14px;
}

.chatbot-quick-replies {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.chatbot-quick-title {
  font-size: 12px;
  color: #888;
  margin: 4px 0 2px;
}

.chatbot-quick-reply-btn {
  border: 1px solid #ccc;
  background: white;
  border-radius: 16px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #111;
  text-align: left;
  width: fit-content;
}

.chatbot-quick-reply-btn:hover {
  background-color: #f1f1f1;
}

.chatbot-message {
  max-width: 80%;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.4;
  white-space: pre-wrap;
}

.from-user {
  align-self: flex-end;
  background-color: #111;
  color: white;
}

.from-bot {
  align-self: flex-start;
  background-color: #f1f1f1;
  color: #111;
}

.chatbot-loading {
  font-style: italic;
  color: #888;
}

.chatbot-input-area {
  display: flex;
  border-top: 1px solid #eee;
  padding: 8px;
  gap: 8px;
}

.chatbot-input-area input {
  flex: 1;
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px;
  font-size: 14px;
}

.chatbot-input-area button {
  border: none;
  background-color: #111;
  color: white;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
}

.chatbot-input-area button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>