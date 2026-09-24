<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="action-dialog-overlay"
      role="presentation"
      @click.self="emit('close')"
      @keydown.esc="emit('close')"
    >
      <section
        class="card shadow-lg action-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auction-action-dialog-title"
      >
        <form @submit.prevent="emit('confirm')">
          <div
            class="card-header d-flex justify-content-between align-items-start gap-3"
          >
            <div>
              <p class="text-uppercase text-secondary small mb-1">操作確認</p>
              <h2 id="auction-action-dialog-title" class="h5 mb-0">
                {{ title }}
              </h2>
            </div>
            <button
              class="btn-close"
              type="button"
              aria-label="關閉"
              :disabled="submitting"
              @click="emit('close')"
            />
          </div>

          <div class="card-body">
            <div class="alert mb-3" :class="alertClass">
              {{ warning }}
            </div>
            <p class="fw-semibold mb-3">{{ targetText }}</p>

            <div v-if="action !== 'restore'">
              <label class="form-label" for="auction-action-reason"
                >{{ actionLabel }}原因</label
              >
              <textarea
                id="auction-action-reason"
                v-model="reasonModel"
                class="form-control"
                rows="4"
                maxlength="500"
                autofocus
                :placeholder="`請輸入${actionLabel}原因（必填，最多 500 字）`"
              />
              <div class="d-flex justify-content-between form-text mb-3">
                <span
                  v-if="reason.trim().length === 0"
                  class="text-danger"
                  >原因不可空白</span
                >
                <span v-else>請填寫可供後續查核的具體原因</span>
                <span>{{ reason.length }}/500</span>
              </div>
            </div>

            <div class="form-check confirmation-check">
              <input
                id="auction-action-confirmed"
                v-model="confirmedModel"
                class="form-check-input"
                type="checkbox"
              />
              <label class="form-check-label" for="auction-action-confirmed">
                我已核對競標資料，確認執行{{ actionLabel }}
              </label>
            </div>

            <div
              v-if="error"
              class="alert alert-danger mt-3 mb-0"
              role="alert"
            >
              {{ error }}
            </div>
          </div>

          <div class="card-footer d-flex justify-content-end gap-2">
            <button
              class="btn btn-outline-secondary"
              type="button"
              :disabled="submitting"
              @click="emit('close')"
            >
              取消
            </button>
            <button
              class="btn"
              :class="submitClass"
              type="submit"
              :disabled="!valid || submitting"
            >
              {{ submitting ? "處理中…" : `確認${actionLabel}` }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  open: { type: Boolean, required: true },
  action: { type: String, required: true },
  reason: { type: String, required: true },
  confirmed: { type: Boolean, required: true },
  error: { type: String, required: true },
  title: { type: String, required: true },
  targetText: { type: String, required: true },
  warning: { type: String, required: true },
  actionLabel: { type: String, required: true },
  alertClass: { type: String, required: true },
  submitClass: { type: String, required: true },
  valid: { type: Boolean, required: true },
  submitting: { type: Boolean, required: true },
});

const emit = defineEmits([
  "update:reason",
  "update:confirmed",
  "close",
  "confirm",
]);

const reasonModel = computed({
  get: () => props.reason,
  set: (value) => emit("update:reason", value),
});
const confirmedModel = computed({
  get: () => props.confirmed,
  set: (value) => emit("update:confirmed", value),
});
</script>

<style scoped>
.action-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1080;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  padding: 1.25rem;
  background: rgba(17, 24, 39, 0.62);
}
.action-dialog {
  width: min(100%, 36rem);
}
.confirmation-check {
  padding: 0.9rem 0.9rem 0.9rem 2.4rem;
  border: 1px solid var(--bs-border-color);
  border-radius: 0.5rem;
  background: var(--bs-light);
}
textarea {
  resize: vertical;
}
</style>
