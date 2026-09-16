<!-- 活動列表與分頁 -->
<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle event-table">
      <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>活動標題</th>
          <th>類型</th>
          <th>狀態</th>
          <th class="event-date-heading">開始時間</th>
          <th class="event-date-heading">結束時間</th>
          <th class="event-action-heading">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="event in events" :key="event.eventId">
          <td>{{ event.eventId }}</td>
          <td class="fw-bold">{{ event.title }}</td>
          <td>
            <span class="badge bg-info text-dark">{{ event.eventType }}</span>
          </td>
          <td>
            <span
              :class="[
                'badge',
                event.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary',
              ]"
            >
              {{ event.status }}
            </span>
          </td>
          <td class="event-date-cell">
            {{ formatDate(event.startAt) }}
          </td>
          <td class="event-date-cell">
            {{ formatDate(event.endAt) }}
          </td>
          <td class="event-action-cell">
            <div class="event-actions">
              <button
                type="button"
                class="event-action action-view"
                @click="$emit('open-detail', event)"
              >
                查看
              </button>

              <button
                v-if="event.status !== 'CANCELLED'"
                type="button"
                class="event-action action-edit"
                @click="$emit('open-edit', event)"
              >
                編輯
              </button>

              <button
                v-if="event.status !== 'CANCELLED'"
                type="button"
                class="event-action action-cancel"
                @click="$emit('delete', event.eventId)"
              >
                取消活動
              </button>

              <button
                v-else
                type="button"
                class="event-action action-cancelled"
                disabled
              >
                已取消
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="events.length === 0">
          <td colspan="7" class="text-center text-muted py-4">
            查無相關活動資料
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <nav v-if="totalPages > 1" class="d-flex justify-content-center mt-3">
    <ul class="pagination">
      <li :class="['page-item', currentPage === 1 ? 'disabled' : '']">
        <button
          class="page-link"
          @click="$emit('change-page', currentPage - 1)"
        >
          上一頁
        </button>
      </li>
      <li
        v-for="page in totalPages"
        :key="page"
        :class="['page-item', currentPage === page ? 'active' : '']"
      >
        <button class="page-link" @click="$emit('change-page', page)">
          {{ page }}
        </button>
      </li>
      <li :class="['page-item', currentPage === totalPages ? 'disabled' : '']">
        <button
          class="page-link"
          @click="$emit('change-page', currentPage + 1)"
        >
          下一頁
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup>
defineProps({
  events: { type: Array, default: () => [] },
  currentPage: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
});

defineEmits(["open-detail", "open-edit", "delete", "change-page"]);

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-TW", { hour12: false });
};
</script>

<style scoped>
.table-responsive {
  max-width: 100%;
  overflow-x: auto;
}

.event-action-heading,
.event-action-cell {
  position: sticky;
  right: 0;
  width: 260px;
  min-width: 260px;
}

.event-action-heading {
  z-index: 3;
  background: #eeece8;
}

.event-action-cell {
  z-index: 2;
  background: #ffffff;
  box-shadow: -8px 0 12px rgba(15, 23, 42, 0.05);
}

.event-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
}

.event-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 650;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition:
    transform 140ms ease,
    filter 140ms ease;
}

.event-action:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(0.95);
}

.event-action:disabled {
  cursor: default;
  opacity: 0.55;
}

/* 查看：霧藍灰 */
.action-view {
  color: #526168;
  background: #dce3e6;
}

/* 編輯：中性灰 */
.action-edit {
  color: #3f403d;
  background: #dfded9;
}

/* 取消：灰玫瑰 */
.action-cancel {
  min-width: 84px;
  color: #744f4b;
  background: #eadbd9;
}

/* 已取消 */
.action-cancelled {
  min-width: 84px;
  color: #64748b;
  background: #e5e7eb;
}
</style>
