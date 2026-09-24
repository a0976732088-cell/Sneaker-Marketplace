<template>
  <!-- 商品介紹 -->
  <div class="col-12">
    <label class="form-label" for="auction-description">
      商品介紹
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <div class="description-composer">
      <div class="description-template-header">
        <div>
          <p class="description-template-title">快速套用介紹範本</p>
          <p class="description-template-hint">
            選擇後可直接修改，補充實際鞋況與配件內容。
          </p>
        </div>

        <button
          type="button"
          class="description-custom-button"
          :class="{
            'is-active':
              selectedDescriptionOption === CUSTOM_OPTION_VALUE,
          }"
          :aria-pressed="
            selectedDescriptionOption === CUSTOM_OPTION_VALUE
          "
          @click="emit('start-custom-description')"
        >
          自行撰寫
        </button>
      </div>

      <div
        class="description-template-grid"
        aria-label="商品介紹範本"
      >
        <button
          v-for="(option, index) in AUCTION_DESCRIPTION_OPTIONS"
          :key="option"
          type="button"
          class="description-template-card"
          :class="{
            'is-active': selectedDescriptionOption === option,
          }"
          :aria-pressed="selectedDescriptionOption === option"
          @click="emit('apply-description-template', option)"
        >
          <span class="description-template-index">
            範本 {{ index + 1 }}
          </span>
          <span class="description-template-copy">{{ option }}</span>
        </button>
      </div>

      <textarea
        id="auction-description"
        :ref="setFieldElement('description')"
        v-model.trim="form.description"
        class="form-control description-editor"
        :class="{ 'is-invalid-field': fieldErrors.description }"
        rows="5"
        required
        :aria-invalid="Boolean(fieldErrors.description)"
        :aria-describedby="fieldErrors.description ? 'auction-description-error' : undefined"
        placeholder="請描述商品狀況、使用情形、盒裝與配件內容"
        @input="emit('description-input')"
      ></textarea>
    </div>

    <p
      v-if="fieldErrors.description"
      id="auction-description-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.description }}
    </p>
  </div>

  <!-- 瑕疵說明 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-defect-option">
      瑕疵說明
    </label>

    <select
      id="auction-defect-option"
      v-model="selectedFormOptions.defectNote"
      class="form-select"
      @change="emit('form-option-change', 'defectNote')"
    >
      <option value="">請選擇（可不填）</option>
      <option
        v-for="option in AUCTION_DEFECT_NOTE_OPTIONS"
        :key="option"
        :value="option"
      >
        {{ option }}
      </option>
      <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
    </select>

    <div
      v-if="selectedFormOptions.defectNote === CUSTOM_OPTION_VALUE"
      class="custom-field"
    >
      <label class="custom-field-label" for="auction-defect-custom">
        請輸入自訂內容
      </label>
      <textarea
        id="auction-defect-custom"
        v-model.trim="form.defectNote"
        class="form-control"
        rows="3"
        maxlength="1000"
      ></textarea>
    </div>
  </div>

  <!-- 真偽來源 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-authenticity-option">
      真偽／來源說明
    </label>

    <select
      id="auction-authenticity-option"
      v-model="selectedFormOptions.authenticityNote"
      class="form-select"
      @change="emit('form-option-change', 'authenticityNote')"
    >
      <option value="">請選擇（可不填）</option>
      <option
        v-for="option in AUCTION_AUTHENTICITY_NOTE_OPTIONS"
        :key="option"
        :value="option"
      >
        {{ option }}
      </option>
      <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
    </select>

    <div
      v-if="selectedFormOptions.authenticityNote === CUSTOM_OPTION_VALUE"
      class="custom-field"
    >
      <label
        class="custom-field-label"
        for="auction-authenticity-custom"
      >
        請輸入自訂內容
      </label>
      <textarea
        id="auction-authenticity-custom"
        v-model.trim="form.authenticityNote"
        class="form-control"
        rows="3"
        maxlength="1000"
      ></textarea>
    </div>
  </div>

  <!-- 商品所在地 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-location">
      商品所在地
    </label>

    <select
      id="auction-location"
      :ref="setFieldElement('location')"
      v-model.trim="form.location"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.location }"
      :aria-invalid="Boolean(fieldErrors.location)"
      :aria-describedby="
        fieldErrors.location ? 'auction-location-error' : undefined
      "
      @change="emit('revalidate-field', 'location')"
    >
      <option value="">請選擇縣市（可不填）</option>
      <option
        v-for="city in TAIWAN_CITIES"
        :key="city"
        :value="city"
      >
        {{ city }}
      </option>
    </select>

    <p
      v-if="fieldErrors.location"
      id="auction-location-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.location }}
    </p>
  </div>
</template>

<script setup>
import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DESCRIPTION_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  TAIWAN_CITIES,
} from "@/constants/auctionFormOptions";

defineProps({
  form: { type: Object, required: true },
  fieldErrors: { type: Object, required: true },
  selectedDescriptionOption: { type: String, required: true },
  selectedFormOptions: { type: Object, required: true },
  setFieldElement: { type: Function, required: true },
});

const emit = defineEmits([
  "apply-description-template",
  "start-custom-description",
  "description-input",
  "form-option-change",
  "revalidate-field",
]);
</script>

<style scoped>
* { box-sizing: border-box; }

.col-md-6,
.col-12 {
  width: auto;
  max-width: none;
  min-width: 0;
  padding: 0;
}

.col-md-6 { grid-column: span 6; }
.col-12 { grid-column: 1 / -1; }

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

.form-control,
.form-select {
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

textarea.form-control {
  min-height: 112px;
  resize: vertical;
}

.form-control::placeholder { color: #8a9096; }

.form-control:focus,
.form-select:focus {
  outline: 0;
  border-color: #111;
  box-shadow: 0 0 0 3px rgb(17 17 17 / 10%);
}

.form-control.is-invalid-field,
.form-select.is-invalid-field { border-color: #dc3545; }

.form-control.is-invalid-field:focus,
.form-select.is-invalid-field:focus {
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

.custom-field { margin-top: 0.8rem; }

.custom-field-label {
  display: block;
  margin: 0 0 0.45rem;
  color: #6c757d;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.4;
}

.description-composer {
  padding: 1rem;
  background: #f8f9fa;
  border: 1px solid #e1e5e9;
  border-radius: 0.5rem;
}

.description-template-header {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 0.85rem;
}

.description-template-title,
.description-template-hint { margin: 0; }

.description-template-title {
  color: #212529;
  font-size: 0.9rem;
  font-weight: 800;
  line-height: 1.4;
}

.description-template-hint {
  margin-top: 0.18rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.5;
}

.description-custom-button {
  flex: 0 0 auto;
  min-height: 34px;
  padding: 0.35rem 0.75rem;
  color: #212529;
  font: inherit;
  font-size: 0.8rem;
  font-weight: 800;
  cursor: pointer;
  background: #fff;
  border: 1px solid #ced4da;
  border-radius: 999px;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.description-custom-button:hover,
.description-custom-button:focus-visible,
.description-custom-button.is-active {
  color: #fff;
  outline: 0;
  background: #212529;
  border-color: #212529;
}

.description-template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 0.65rem;
}

.description-template-card {
  display: flex;
  gap: 0.65rem;
  align-items: flex-start;
  min-width: 0;
  padding: 0.75rem;
  color: #343a40;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.description-template-card:hover,
.description-template-card:focus-visible {
  outline: 0;
  border-color: #6c757d;
  box-shadow: 0 0.35rem 0.8rem rgb(17 17 17 / 8%);
  transform: translateY(-1px);
}

.description-template-card.is-active {
  border-color: #212529;
  box-shadow: inset 3px 0 0 #212529;
}

.description-template-index {
  flex: 0 0 auto;
  padding: 0.18rem 0.4rem;
  color: #495057;
  font-size: 0.68rem;
  font-weight: 800;
  line-height: 1.4;
  white-space: nowrap;
  background: #e9ecef;
  border-radius: 0.25rem;
}

.description-template-card.is-active .description-template-index {
  color: #fff;
  background: #212529;
}

.description-template-copy {
  min-width: 0;
  font-size: 0.82rem;
  line-height: 1.55;
}

.description-editor.form-control {
  min-height: 140px;
  margin-top: 0.9rem;
  background: #fff;
}

@media (max-width: 767.98px) {
  .col-md-6,
  .col-12 { grid-column: 1 / -1; }
}
</style>
