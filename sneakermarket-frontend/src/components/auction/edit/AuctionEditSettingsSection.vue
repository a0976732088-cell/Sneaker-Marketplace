<template>
  <!-- 競標設定 -->
  <div class="card mb-4">
    <div class="card-header">競標設定</div>

    <div class="card-body">
      <div class="row g-3">
        <!-- 起標價 -->
        <div class="col-md-6" data-validation-field="startPrice">
          <label for="auction-edit-start-price" class="form-label">
            起標價（NT$）
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <input
            id="auction-edit-start-price"
            v-model.number="form.startPrice"
            type="number"
            min="1"
            step="1"
            class="form-control"
            :class="{ 'is-invalid-field': fieldErrors.startPrice }"
            required
            :disabled="!editable"
            :aria-invalid="Boolean(fieldErrors.startPrice)"
            :aria-describedby="
              fieldErrors.startPrice
                ? 'auction-edit-start-price-error'
                : undefined
            "
            @input="emit('revalidate-field', 'startPrice')"
          />

          <p
            v-if="fieldErrors.startPrice"
            id="auction-edit-start-price-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.startPrice }}
          </p>
        </div>

        <!-- 最低加價 -->
        <div class="col-md-6" data-validation-field="minIncrement">
          <label for="auction-edit-min-increment" class="form-label">
            最低加價（NT$）
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <input
            id="auction-edit-min-increment"
            v-model.number="form.minIncrement"
            type="number"
            min="1"
            step="1"
            class="form-control"
            :class="{ 'is-invalid-field': fieldErrors.minIncrement }"
            required
            :disabled="!editable"
            :aria-invalid="Boolean(fieldErrors.minIncrement)"
            :aria-describedby="
              fieldErrors.minIncrement
                ? 'auction-edit-min-increment-error'
                : undefined
            "
            @input="emit('revalidate-field', 'minIncrement')"
          />

          <p
            v-if="fieldErrors.minIncrement"
            id="auction-edit-min-increment-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.minIncrement }}
          </p>
        </div>

        <!-- 開始時間 -->
        <div class="col-md-6" data-validation-field="startAt">
          <label for="auction-edit-start-at" class="form-label">
            開始時間
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <input
            id="auction-edit-start-at"
            v-model="form.startAt"
            type="datetime-local"
            title="台北時間（UTC+8）"
            class="form-control"
            :class="{ 'is-invalid-field': fieldErrors.startAt }"
            required
            :disabled="!editable || auctionStatus === 'ACTIVE'"
            :aria-invalid="Boolean(fieldErrors.startAt)"
            :aria-describedby="
              fieldErrors.startAt
                ? 'auction-edit-start-at-error'
                : undefined
            "
            @input="emit('revalidate-field', 'startAt')"
          />

          <p
            v-if="fieldErrors.startAt"
            id="auction-edit-start-at-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.startAt }}
          </p>

          <div v-if="auctionStatus === 'ACTIVE'" class="form-text">
            競標開始後不可修改開始時間。
          </div>
        </div>

        <!-- 結束時間 -->
        <div class="col-md-6" data-validation-field="endAt">
          <label for="auction-edit-end-at" class="form-label">
            結束時間
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <input
            id="auction-edit-end-at"
            v-model="form.endAt"
            type="datetime-local"
            title="台北時間（UTC+8）"
            class="form-control"
            :class="{ 'is-invalid-field': fieldErrors.endAt }"
            required
            :disabled="!editable"
            :aria-invalid="Boolean(fieldErrors.endAt)"
            :aria-describedby="
              fieldErrors.endAt ? 'auction-edit-end-at-error' : undefined
            "
            @input="emit('revalidate-field', 'endAt')"
          />

          <p
            v-if="fieldErrors.endAt"
            id="auction-edit-end-at-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.endAt }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  editable: { type: Boolean, required: true },
  auctionStatus: { type: String, required: true },
  fieldErrors: { type: Object, required: true },
});

const emit = defineEmits(["revalidate-field"]);
</script>

<style scoped>
.required-mark {
  margin-left: 0.2rem;
  color: #dc3545;
}

.field-error {
  margin-top: 0.4rem;
  margin-bottom: 0;
  color: #dc3545;
  font-size: 0.875rem;
  font-weight: 600;
}

.form-control.is-invalid-field {
  border-color: #dc3545;
}

.form-control.is-invalid-field:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.16);
}
</style>