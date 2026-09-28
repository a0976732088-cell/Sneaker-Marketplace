<template>
  <div v-if="isVisibleRoute" class="chatbot-widget">
    <!-- 浮動按鈕：滑鼠移上去時，左邊跳出對話框 -->
    <div v-if="!isOpen" class="chatbot-toggle-wrap">
      <span class="chatbot-hover-bubble" aria-hidden="true">你好！有需要幫忙嗎？</span>
      <button
        class="chatbot-toggle-btn"
        aria-label="開啟客服機器人"
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
    </div>

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
          <span>酷客 Kicks 客服機器人</span>
        </div>
        <button class="chatbot-close-btn" aria-label="關閉並清除對話" @click="closeChat">✕</button>
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
            {{ q.question }}
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

// 快捷問題：答案直接寫在前端，不呼叫 Gemini，就算 API 掛掉也能秒回
// 內容需與後端 ChatbotService 的 SYSTEM_PROMPT 保持一致
const quickQuestions = [
  { question: '運費怎麼算？', answer: '全站運費固定 NT$100，不分金額，目前沒有滿額免運喔。' },
  { question: '付款方式有哪些？', answer: '目前支援綠界信用卡付款與 LINE Pay，不提供轉帳付款與貨到付款。' },
  { question: '可以超商取貨嗎？', answer: '可以！結帳時可選擇宅配到府，或 7-11 超商店到店取貨，還能用地圖挑選門市。' },
  { question: '大概多久會到貨？', answer: '台北市約 2-5 個工作天，其他地區約 3-6 個工作天；出貨時間為週一至週五（不含國定假日）。' }
]

// 每次對話的編號：關閉視窗時換新編號，舊的回覆就算晚到也不會塞進新對話
let conversationId = 0

// 按叉叉：關閉視窗並清空對話，下次打開是全新的對話
function closeChat() {
  isOpen.value = false
  messages.value = []
  inputText.value = ''
  isLoading.value = false
  conversationId++
}

// 點快捷問題：直接顯示寫好的答案，不打 API
async function askQuickQuestion(item) {
  const currentId = conversationId
  messages.value.push({ role: 'user', text: item.question })
  isLoading.value = true
  await scrollToBottom()

  // 停頓 0.5 秒，讓回覆看起來像機器人在回答
  setTimeout(async () => {
    if (currentId !== conversationId) return
    messages.value.push({ role: 'bot', text: item.answer })
    isLoading.value = false
    await scrollToBottom()
  }, 500)
}

async function sendMessage() {
  const question = inputText.value.trim()
  if (!question) return

  const currentId = conversationId
  messages.value.push({ role: 'user', text: question })
  inputText.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const response = await httpClient.post('/chatbot/ask', { question }, { timeout: 30000 })
    if (currentId !== conversationId) return
    messages.value.push({ role: 'bot', text: response.data.answer })
  } catch (error) {
    if (currentId !== conversationId) return
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

/* ===== 滑鼠移到機器人上時跳出的對話框 ===== */
.chatbot-toggle-wrap {
  position: relative;
}

.chatbot-hover-bubble {
  position: absolute;
  right: calc(100% + 14px);
  top: 50%;
  transform: translate(6px, -50%);
  padding: 10px 14px;
  border-radius: 12px;
  background-color: #ffffff;
  color: #111111;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

/* 對話框右邊的小尖角，指向機器人 */
.chatbot-hover-bubble::after {
  content: "";
  position: absolute;
  right: -6px;
  top: 50%;
  width: 12px;
  height: 12px;
  background-color: #ffffff;
  transform: translateY(-50%) rotate(45deg);
  border-radius: 2px;
}

/* 滑鼠移到按鈕上（或用鍵盤 Tab 選到按鈕）時顯示 */
.chatbot-toggle-wrap:hover .chatbot-hover-bubble,
.chatbot-toggle-wrap:focus-within .chatbot-hover-bubble {
  opacity: 1;
  transform: translate(0, -50%);
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