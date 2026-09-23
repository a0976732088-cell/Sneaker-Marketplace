<template>
  <!-- 競標設定 -->
  <section class="create-section create-settings-section">
    <header class="section-heading">
      <p class="section-eyebrow">STEP 04</p>
      <h2>競標設定</h2>
      <p>設定起標價格、最低加價金額與本次競標期間。</p>
    </header>

    <div class="create-field-grid">
      <!-- 起標價 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-start-price">
          起標價（NT$）
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <input
          id="auction-start-price"
          :ref="setFieldElement('startPrice')"
          v-model.number="form.startPrice"
          type="number"
          min="1"
          step="1"
          class="form-control"
          :class="{ 'is-invalid-field': fieldErrors.startPrice }"
          required
          :aria-invalid="Boolean(fieldErrors.startPrice)"
          :aria-describedby="fieldErrors.startPrice ? 'auction-start-price-error' : undefined"
          @input="revalidateErroredField('startPrice')"
        />

        <p
          v-if="fieldErrors.startPrice"
          id="auction-start-price-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.startPrice }}
        </p>
      </div>

      <!-- 最低加價 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-min-increment">
          最低加價金額（NT$）
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <input
          id="auction-min-increment"
          :ref="setFieldElement('minIncrement')"
          v-model.number="form.minIncrement"
          type="number"
          min="1"
          step="1"
          class="form-control"
          :class="{ 'is-invalid-field': fieldErrors.minIncrement }"
          required
          :aria-invalid="Boolean(fieldErrors.minIncrement)"
          :aria-describedby="fieldErrors.minIncrement ? 'auction-min-increment-error' : undefined"
          @input="revalidateErroredField('minIncrement')"
        />

        <p
          v-if="fieldErrors.minIncrement"
          id="auction-min-increment-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.minIncrement }}
        </p>
      </div>

      <!-- 開始時間 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-start-at">
          開始時間
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <input
          id="auction-start-at"
          :ref="setFieldElement('startAt')"
          v-model="form.startAt"
          type="datetime-local" title="台北時間（UTC+8）"
          class="form-control"
          :class="{ 'is-invalid-field': fieldErrors.startAt }"
          required
          :aria-invalid="Boolean(fieldErrors.startAt)"
          :aria-describedby="fieldErrors.startAt ? 'auction-start-at-error' : undefined"
          @input="revalidateTimeFields"
        />

        <p
          v-if="fieldErrors.startAt"
          id="auction-start-at-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.startAt }}
        </p>
      </div>

      <!-- 結束時間 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-end-at">
          結束時間
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <input
          id="auction-end-at"
          :ref="setFieldElement('endAt')"
          v-model="form.endAt"
          type="datetime-local" title="台北時間（UTC+8）"
          class="form-control"
          :class="{ 'is-invalid-field': fieldErrors.endAt }"
          required
          :aria-invalid="Boolean(fieldErrors.endAt)"
          :aria-describedby="fieldErrors.endAt ? 'auction-end-at-error' : undefined"
          @input="revalidateTimeFields"
        />

        <p
          v-if="fieldErrors.endAt"
          id="auction-end-at-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.endAt }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  fieldErrors: { type: Object, required: true },
  setFieldElement: { type: Function, required: true },
  revalidateErroredField: { type: Function, required: true },
  revalidateTimeFields: { type: Function, required: true },
});
</script>

<style scoped>
*,
*::before,
*::after {
  box-sizing: border-box;
}

.create-section {
  margin: 0;
  padding: 3rem 0;
  border: 0;
  border-top: 1px solid #e9ecef;
}

.section-heading {
  max-width: 650px;
  margin-bottom: 2rem;
}

.section-eyebrow {
  margin: 0 0 0.65rem;
  color: #8a9096;
  font-size: 0.75rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.section-heading h2 {
  margin: 0;
  color: #111;
  font-size: clamp(1.55rem, 3vw, 1.9rem);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.section-heading > p:last-child {
  margin: 0.65rem 0 0;
  color: #6c757d;
  line-height: 1.7;
}

.create-field-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 1.4rem 1.5rem;
  width: 100%;
  min-width: 0;
}

.create-field-grid > [class^="col-"],
.create-field-grid > [class*=" col-"] {
  width: auto;
  max-width: none;
  min-width: 0;
  padding: 0;
}

.create-field-grid > .col-md-6 {
  grid-column: span 6;
}

.form-label {
  display: block;
  margin: 0 0 0.55rem;
  color: #212529;
  font-size: 0.85rem;
  font-weight: 700;
  line-height: 1.4;
}

.required-mark {
  margin-left: 0.2rem;
  color: #dc3545;
}

.form-control {
  display: block;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 48px;
  padding: 0.7rem 0.85rem;
  color: #212529;
  font: inherit;
  line-height: 1.4;
  background-color: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  box-shadow: none;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.form-control::placeholder {
  color: #8a9096;
}

.form-control:focus {
  outline: 0;
  border-color: #111;
  box-shadow: 0 0 0 3px rgb(17 17 17 / 10%);
}

.form-control.is-invalid-field {
  border-color: #dc3545;
}

.form-control.is-invalid-field:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 3px rgb(220 53 69 / 15%);
}

.field-error {
  margin: 0.4rem 0 0;
  color: #dc3545;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.5;
}

.create-settings-section {
  margin: 0 0 3rem;
  padding: 2.5rem;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 0.5rem;
}

@media (max-width: 767.98px) {
  .create-section {
    padding: 2.5rem 0;
  }

  .create-field-grid > .col-md-6 {
    grid-column: 1 / -1;
  }

  .create-settings-section {
    margin-bottom: 2.5rem;
    padding: 2rem 1.25rem;
  }
}

@media (max-width: 479.98px) {
  .section-heading {
    margin-bottom: 1.5rem;
  }

  .create-field-grid {
    gap: 1.2rem;
  }

  .create-settings-section {
    padding: 1.25rem 1rem;
  }
}
</style>