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
  editable: { type: Boolean, required: true },
  fieldErrors: { type: Object, required: true },
  selectedDescriptionOption: { type: String, required: true },
  selectedFormOptions: { type: Object, required: true },
  legacyLocationOption: { type: String, required: true },
});

const emit = defineEmits([
  "apply-description-template",
  "start-custom-description",
  "description-input",
  "form-option-change",
  "revalidate-field",
]);
</script>

<template>
  <!-- 商品介紹 -->
  <div class="col-12" data-validation-field="description">
    <label for="auction-edit-description" class="form-label">
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
          :disabled="!editable"
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
          :disabled="!editable"
          @click="emit('apply-description-template', option)"
        >
          <span class="description-template-index">
            範本 {{ index + 1 }}
          </span>
          <span class="description-template-copy">{{ option }}</span>
        </button>
      </div>

      <textarea
        id="auction-edit-description"
        v-model.trim="form.description"
        class="form-control description-editor"
        :class="{ 'is-invalid-field': fieldErrors.description }"
        rows="5"
        required
        :disabled="!editable"
        :aria-invalid="Boolean(fieldErrors.description)"
        :aria-describedby="
          fieldErrors.description
            ? 'auction-edit-description-error'
            : undefined
        "
        placeholder="請描述商品狀況、使用情形、盒裝與配件內容"
        @input="emit('description-input')"
      ></textarea>
    </div>

    <p
      v-if="fieldErrors.description"
      id="auction-edit-description-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.description }}
    </p>
  </div>

  <!-- 瑕疵說明 -->
  <div class="col-md-6">
    <label for="auction-edit-defect-note-option" class="form-label">
      瑕疵說明
    </label>

    <select
      id="auction-edit-defect-note-option"
      v-model="selectedFormOptions.defectNote"
      class="form-select"
      :disabled="!editable"
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
      class="custom-option-field"
    >
      <label for="auction-edit-defect-note-custom" class="custom-option-label">
        請輸入自訂瑕疵說明
      </label>
      <textarea
        id="auction-edit-defect-note-custom"
        v-model.trim="form.defectNote"
        class="form-control"
        rows="3"
        maxlength="1000"
        data-custom-input
        :disabled="!editable"
      ></textarea>
    </div>
  </div>

  <!-- 真偽來源 -->
  <div class="col-md-6">
    <label
      for="auction-edit-authenticity-note-option"
      class="form-label"
    >
      真偽／來源說明
    </label>

    <select
      id="auction-edit-authenticity-note-option"
      v-model="selectedFormOptions.authenticityNote"
      class="form-select"
      :disabled="!editable"
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
      v-if="
        selectedFormOptions.authenticityNote ===
        CUSTOM_OPTION_VALUE
      "
      class="custom-option-field"
    >
      <label
        for="auction-edit-authenticity-note-custom"
        class="custom-option-label"
      >
        請輸入自訂真偽／來源說明
      </label>
      <textarea
        id="auction-edit-authenticity-note-custom"
        v-model.trim="form.authenticityNote"
        class="form-control"
        rows="3"
        maxlength="1000"
        data-custom-input
        :disabled="!editable"
      ></textarea>
    </div>
  </div>

  <!-- 商品所在地 -->
  <div class="col-md-6" data-validation-field="location">
    <label class="form-label" for="auction-edit-location">
      商品所在地
    </label>

    <select
      id="auction-edit-location"
      v-model.trim="form.location"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.location }"
      :disabled="!editable"
      :aria-invalid="Boolean(fieldErrors.location)"
      :aria-describedby="
        fieldErrors.location
          ? 'auction-edit-location-error'
          : undefined
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
      <option
        v-if="legacyLocationOption"
        :value="legacyLocationOption"
      >
        {{ legacyLocationOption }}（舊資料）
      </option>
    </select>

    <p
      v-if="fieldErrors.location"
      id="auction-edit-location-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.location }}
    </p>
  </div>
</template>

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

.form-control.is-invalid-field,
.form-select.is-invalid-field {
  border-color: #dc3545;
}

.form-control.is-invalid-field:focus,
.form-select.is-invalid-field:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.16);
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
.description-template-hint {
  margin: 0;
}

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
}

.description-custom-button:hover:not(:disabled),
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
}

.description-template-card:hover:not(:disabled),
.description-template-card:focus-visible {
  outline: 0;
  border-color: #6c757d;
  box-shadow: 0 0.35rem 0.8rem rgb(17 17 17 / 8%);
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

.description-custom-button:disabled,
.description-template-card:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}


.custom-option-field {
  margin-top: 0.75rem;
}

.custom-option-label {
  display: block;
  margin-bottom: 0.4rem;
  color: #6c757d;
  font-size: 0.82rem;
  font-weight: 700;
}
</style>
