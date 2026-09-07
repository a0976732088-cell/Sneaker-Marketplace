<!-- 詳細資料彈窗 -->
 <template>
  <div v-if="event" class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">
      <!-- Modal 頁首 -->
      <div class="modal-header" :style="{ backgroundColor: event.color || '#1e293b' }">
        <span class="type-badge">{{ event.eventType || 'EVENT' }}</span>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>

      <!-- Modal 內容區 -->
      <div class="modal-body">
        <h2 class="event-title">{{ event.title }}</h2>

        <div class="info-row">
          <span class="info-icon">📅</span>
          <span>{{ event.date }}</span>
        </div>

        <div class="info-row" v-if="event.time">
          <span class="info-icon">🕒</span>
          <span>{{ event.time }}</span>
        </div>

        <!-- 活動圖片展示 -->
        <div class="photo-wrapper">
          <img
            :src="`http://localhost:8080/api/calendar/event/${event.id}/photo`"
            alt="Event Photo"
            class="event-photo"
            @error="(e) => (e.target.src = 'https://picsum.photos/400/200')"
          />
        </div>

        <!-- 活動描述 -->
        <div class="description-section" v-if="event.description">
          <label>活動詳情</label>
          <p>{{ event.description }}</p>
        </div>
      </div>

      <!-- Modal 頁尾 -->
      <div class="modal-footer">
        <button class="btn-close-modal" @click="$emit('close')">關閉</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  event: {
    type: Object,
    default: null,
  },
});

defineEmits(["close"]);
</script>

<style scoped>
/* Modal 背景遮罩 */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease-out;
}

/* Modal 主卡片 */
.modal-card {
  background: #ffffff;
  width: 90%;
  max-width: 480px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  animation: slideUp 0.25s ease-out;
}

/* Header */
.modal-header {
  padding: 16px 20px;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type-badge {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.25);
  padding: 4px 10px;
  border-radius: 12px;
}

.close-btn {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 24px;
  cursor: pointer;
  line-height: 1;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.close-btn:hover {
  opacity: 1;
}

/* Body */
.modal-body {
  padding: 20px 24px;
}

.event-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 16px 0;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 15px;
  margin-bottom: 8px;
}

.info-icon {
  font-size: 16px;
}

.photo-wrapper {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  max-height: 200px;
  background: #f1f5f9;
}

.event-photo {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.description-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.description-section label {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: block;
  margin-bottom: 6px;
}

.description-section p {
  color: #334155;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-line;
}

/* Footer */
.modal-footer {
  padding: 12px 24px 20px;
  display: flex;
  justify-content: flex-end;
}

.btn-close-modal {
  background: #f1f5f9;
  border: none;
  color: #475569;
  padding: 8px 20px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* 動畫 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>