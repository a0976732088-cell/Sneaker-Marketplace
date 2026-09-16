<!-- components/calendar/EventEditModal.vue -->
<template>
  <div ref="modalRef" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content shadow-lg border-0 rounded-4">
        <div class="modal-header">
          <h5 class="modal-title fw-bold">
            編輯活動 (ID: {{ editForm.eventId }})
          </h5>
          <button type="button" class="btn-close" @click="closeModal"></button>
        </div>
        <div class="modal-body p-4">
          <form @submit.prevent="submitUpdate">
            <div class="mb-3">
              <label class="form-label">
                活動標題 <span class="text-danger">*</span>
              </label>
              <input
                v-model.trim="editForm.title"
                type="text"
                class="form-control"
                required
              />
            </div>

            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label">
                  活動類型 <span class="text-danger">*</span>
                </label>
                <select
                  v-model="editForm.eventType"
                  class="form-select"
                  required
                >
                  <option value="RELEASE">新品發售</option>
                  <option value="SALE">優惠活動</option>
                  <option value="EVENT">品牌活動</option>
                  <option value="NOTICE">公告</option>
                  <option value="OTHER">其他</option>
                </select>
              </div>

              <div class="col-md-6 mb-3">
                <label class="form-label">
                  狀態 <span class="text-danger">*</span>
                </label>
                <select v-model="editForm.status" class="form-select" required>
                  <option value="ACTIVE">啟用</option>
                  <option value="INACTIVE">停用</option>
                  <option value="DRAFT">草稿</option>
                  <option value="CANCELLED">取消</option>
                </select>
              </div>
            </div>

            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label">
                  開始時間 <span class="text-danger">*</span>
                </label>
                <input
                  v-model="editForm.startAt"
                  type="datetime-local"
                  class="form-control"
                  required
                />
              </div>

              <div class="col-md-6 mb-3">
                <label class="form-label">結束時間</label>
                <input
                  v-model="editForm.endAt"
                  type="datetime-local"
                  class="form-control"
                />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">活動描述</label>
              <textarea
                v-model.trim="editForm.description"
                class="form-control"
                rows="4"
              ></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label">修改圖片</label>
              <input
                type="file"
                class="form-control"
                accept="image/*"
                @change="handleFileChange"
              />
              <div class="mt-2">
                <img
                  :src="
                    photoPreview ||
                    `/api/calendar/event/${editForm.eventId}/photo`
                  "
                  style="max-height: 150px"
                  class="img-thumbnail"
                  @error="(e) => (e.target.style.display = 'none')"
                />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">活動連結</label>
              <input
                v-model.trim="editForm.targetUrl"
                type="url"
                class="form-control"
              />
            </div>

            <div class="d-flex justify-content-end gap-2 mt-4">
              <button
                type="button"
                class="btn btn-outline-secondary"
                @click="closeModal"
              >
                取消
              </button>
              <button type="submit" class="btn btn-primary" :disabled="loading">
                儲存修改
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from "vue";
import { Modal } from "bootstrap";
import { updateEvent } from "@/service/eventService";

const props = defineProps({
  event: { type: Object, required: true },
});

const emit = defineEmits(["close", "success"]);

const modalRef = ref(null);
let modalInstance = null;

const loading = ref(false);
const photoPreview = ref(null);

const editForm = reactive({
  eventId: null,
  title: "",
  eventType: "",
  status: "",
  brandId: null,
  createdBy: null,
  startAt: "",
  endAt: "",
  description: "",
  targetUrl: "",
  sourceType: "",
  sourceId: null,
});

const formatForDateTimeInput = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const pad = (num) => String(num).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`;
};

const handleHidden = () => {
  emit("close");
};

onMounted(() => {
  Object.assign(editForm, {
    ...props.event,
    photo: null,
    startAt: formatForDateTimeInput(props.event.startAt),
    endAt: formatForDateTimeInput(props.event.endAt),
  });

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

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.readAsDataURL(file);
  reader.onload = () => {
    editForm.photo = reader.result.split(",")[1];
    photoPreview.value = reader.result;
  };
};

const closeModal = () => {
  if (modalInstance) {
    modalInstance.hide();
  }
};

const submitUpdate = async () => {
  if (props.event.status === "CANCELLED") {
    alert("已取消活動不能重新啟用或修改");
    return;
  }
  const isChangingToCancelled =
    props.event.status !== "CANCELLED" && editForm.status === "CANCELLED";

  if (isChangingToCancelled) {
    const confirmed = confirm(
      "確定要取消這個活動嗎？\n取消後將通知目前的關注會員。",
    );

    if (!confirmed) {
      return;
    }
  }

  loading.value = true;

  try {
    await updateEvent(editForm.eventId, editForm);

    alert(
      isChangingToCancelled ? "活動已取消，關注會員將收到通知。" : "更新成功！",
    );

    emit("success");
    closeModal();
  } catch (error) {
    console.error("更新失敗", error);
    alert("更新失敗，請檢查伺服器狀態。");
  } finally {
    loading.value = false;
  }
};
</script>
