<!-- components/notification/NotificationEditModal.vue -->
<template>
  <div ref="modalRef" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title fw-bold">編輯通知</h5>
          <button type="button" class="btn-close" @click="closeModal"></button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="submitForm">
            <div class="mb-3">
              <label class="form-label">
                通知標題 <span class="text-danger">*</span>
              </label>
              <input
                v-model="formData.title"
                type="text"
                class="form-control"
                required
              />
            </div>

            <div class="row mb-3">
              <!-- 🎯 通知類型 (補上 v-model) -->
              <div class="col-md-6">
                <label for="notificationType" class="form-label">
                  通知類型 <span class="text-danger">*</span>
                </label>
                <select
                  v-model="formData.notificationType"
                  class="form-select"
                  id="notificationType"
                  required
                >
                  <option value="" disabled>請選擇通知類型</option>
                  <option value="EVENT_REMINDER">
                    活動提醒 (EVENT_REMINDER)
                  </option>
                  <option value="SYSTEM_NOTICE">
                    系統公告 (SYSTEM_NOTICE)
                  </option>
                  <option value="ORDER_UPDATE">訂單更新 (ORDER_UPDATE)</option>
                  <option value="PROMOTION">促銷優惠 (PROMOTION)</option>
                </select>
              </div>

              <!-- 🎯 對象類型 (補上 v-model) -->
              <div class="col-md-6">
                <label for="targetType" class="form-label"
                  >對象類型 (Target Type)</label
                >
                <select
                  v-model="formData.targetType"
                  class="form-select"
                  id="targetType"
                >
                  <option value="">請選擇對象類型</option>
                  <option value="EVENT">活動 (EVENT)</option>
                  <option value="ORDER">訂單 (ORDER)</option>
                  <option value="AUCTION">競標 (AUCTION)</option>
                  <option value="USER">使用者 (USER)</option>
                </select>
              </div>
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label">目標 ID (Target ID)</label>
                <input
                  v-model.number="formData.targetId"
                  type="number"
                  class="form-control"
                />
              </div>
              <div class="col-md-6">
                <label class="form-label">跳轉 URL (Target URL)</label>
                <input
                  v-model="formData.targetUrl"
                  type="text"
                  class="form-control"
                />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">
                通知內文 <span class="text-danger">*</span>
              </label>
              <textarea
                v-model="formData.content"
                rows="4"
                class="form-control"
                required
              ></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeModal">
            取消
          </button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="loading"
            @click="submitForm"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-1"
            ></span>
            儲存變更
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue";
import { updateNotification } from "@/service/notificationService";
import { Modal } from "bootstrap";

const props = defineProps({
  notification: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["close", "success"]);

const modalRef = ref(null);
let modalInstance = null;
const loading = ref(false);

const formData = ref({
  notificationId: null,
  title: "",
  notificationType: "",
  content: "",
  targetType: "",
  targetId: null,
  targetUrl: "",
});

const handleHidden = () => {
  emit("close");
};

onMounted(() => {
  if (modalRef.value) {
    const el = modalRef.value;
    modalInstance = new Modal(el);
    el.addEventListener("hidden.bs.modal", handleHidden);
    modalInstance.show();
  }
});

onUnmounted(() => {
  if (modalRef.value) {
    modalRef.value.removeEventListener("hidden.bs.modal", handleHidden);
  }
  if (modalInstance) {
    modalInstance.dispose();
  }
});

watch(
  () => props.notification,
  (newVal) => {
    if (newVal) {
      formData.value = { ...newVal };
    }
  },
  { immediate: true },
);

const closeModal = () => {
  if (modalInstance) {
    modalInstance.hide();
  }
};

const submitForm = async () => {
  loading.value = true;
  try {
    await updateNotification(formData.value.notificationId, formData.value);
    alert("更新成功！");
    closeModal();
    emit("success");
  } catch (err) {
    console.error("更新失敗", err);
    alert("操作失敗，請檢查輸入內容。");
  } finally {
    loading.value = false;
  }
};
</script>
