<!-- 活動列表與分頁 -->
<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle">
      <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>活動標題</th>
          <th>類型</th>
          <th>狀態</th>
          <th>開始時間</th>
          <th>結束時間</th>
          <th>操作</th>
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
          <td>{{ formatDate(event.startAt) }}</td>
          <td>{{ formatDate(event.endAt) }}</td>
          <td>
            <button
              class="btn btn-sm btn-outline-info me-1"
              @click="$emit('open-detail', event)"
            >
              查看
            </button>
            <button
              class="btn btn-sm btn-outline-primary me-1"
              @click="$emit('open-edit', event)"
            >
              編輯
            </button>
            <button
              class="btn btn-sm btn-outline-danger"
              @click="$emit('delete', event.eventId)"
            >
              刪除
            </button>
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
