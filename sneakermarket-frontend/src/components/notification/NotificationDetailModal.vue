<!-- components/notification/NotificationDetailModal.vue -->
<template>
  <div ref="modalRef" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title fw-bold">查看通知詳情</h5>
          <button type="button" class="btn-close" @click="closeModal"></button>
        </div>
        <div class="modal-body">
          <form v-if="notification">
            <div class="mb-3">
              <label class="form-label text-muted small">通知標題</label>
              <input :value="notification.title" type="text" class="form-control" readonly />
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label text-muted small">通知類型</label>
                <input :value="notification.notificationType" type="text" class="form-control" readonly />
              </div>
              <div class="col-md-6">
                <label class="form-label text-muted small">目標類型 (Target Type)</label>
                <input :value="notification.targetType || '無'" type="text" class="form-control" readonly />
              </div>
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label text-muted small">目標 ID (Target ID)</label>
                <input :value="notification.targetId || '無'" type="text" class="form-control" readonly />
              </div>
              <div class="col-md-6">
                <label class="form-label text-muted small">跳轉 URL (Target URL)</label>
                <input :value="notification.targetUrl || '無'" type="text" class="form-control" readonly />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label text-muted small">通知內文</label>
              <textarea :value="notification.content" rows="4" class="form-control" readonly></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label text-muted small">建立時間</label>
              <input :value="formatDate(notification.createdAt)" type="text" class="form-control" readonly />
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeModal">關閉</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Modal } from 'bootstrap';

const props = defineProps({
  notification: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['close']);

const modalRef = ref(null);
let modalInstance = null;

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
    modalInstance.show();
  }
});

const closeModal = () => {
  modalInstance.hide();
  emit('close');
};

const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-TW', { hour12: false });
};
</script>