<template>
  <!-- Keep the original fields as direct children of the parent STEP 01 grid. -->
  <!-- 品牌 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-brand">
      品牌
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-brand"
      :ref="setFieldElement('brandId')"
      v-model.number="form.brandId"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.brandId }"
      required
      :aria-invalid="Boolean(fieldErrors.brandId)"
      :aria-describedby="fieldErrors.brandId ? 'auction-brand-error' : undefined"
      @change="emit('brand-change')"
    >
      <option disabled value="">請選擇品牌</option>

      <option
        v-for="brand in brands"
        :key="brand.id"
        :value="brand.id"
      >
        {{ brand.name }}
      </option>
    </select>

    <p
      v-if="fieldErrors.brandId"
      id="auction-brand-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.brandId }}
    </p>
  </div>

  <!-- 分類 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-category">
      分類
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-category"
      :ref="setFieldElement('categoryId')"
      v-model.number="form.categoryId"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.categoryId }"
      required
      :aria-invalid="Boolean(fieldErrors.categoryId)"
      :aria-describedby="fieldErrors.categoryId ? 'auction-category-error' : undefined"
      @change="emit('revalidate-field', 'categoryId')"
    >
      <option disabled value="">請選擇分類</option>

      <option
        v-for="category in categories"
        :key="category.id"
        :value="category.id"
      >
        {{ category.name }}
      </option>
    </select>

    <p
      v-if="fieldErrors.categoryId"
      id="auction-category-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.categoryId }}
    </p>
  </div>

  <!-- 商品名稱 -->
  <div class="col-md-8">
    <label class="form-label" for="auction-product-name">
      商品名稱
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <input
      id="auction-product-name"
      :ref="setFieldElement('productName')"
      v-model.trim="form.productName"
      type="text"
      maxlength="150"
      class="form-control"
      :class="{ 'is-invalid-field': fieldErrors.productName }"
      required
      :aria-invalid="Boolean(fieldErrors.productName)"
      :aria-describedby="fieldErrors.productName ? 'auction-product-name-error' : undefined"
      @input="emit('revalidate-field', 'productName')"
    />

    <p
      v-if="fieldErrors.productName"
      id="auction-product-name-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.productName }}
    </p>
  </div>

  <!-- 型號 -->
  <div class="col-md-4">
    <label class="form-label" for="auction-model-option">
      型號／系列
    </label>

    <select
      id="auction-model-option"
      v-model="modelOption"
      class="form-select"
      :disabled="!form.brandId"
      @change="handleModelSelectionChange"
    >
      <option value="">
        {{ form.brandId ? "請選擇型號（可不填）" : "請先選擇品牌" }}
      </option>
      <option
        v-for="option in availableModelOptions"
        :key="option"
        :value="option"
      >
        {{ option }}
      </option>
      <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
    </select>

    <div
      v-if="selectedModelOption === CUSTOM_OPTION_VALUE"
      class="custom-field"
    >
      <label class="custom-field-label" for="auction-model-custom">
        自訂型號／系列
      </label>
      <input
        id="auction-model-custom"
        v-model.trim="form.model"
        type="text"
        maxlength="100"
        class="form-control"
        placeholder="例如：聯名款名稱或特殊型號"
      />
    </div>
  </div>

  <!-- 尺寸 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-size">
      尺寸
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-size"
      :ref="setFieldElement('size')"
      v-model.trim="form.size"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.size }"
      required
      :aria-invalid="Boolean(fieldErrors.size)"
      :aria-describedby="fieldErrors.size ? 'auction-size-error' : undefined"
      @change="emit('revalidate-field', 'size')"
    >
      <option disabled value="">請選擇尺寸</option>
      <option
        v-for="size in AUCTION_SHOE_SIZES"
        :key="size"
        :value="size"
      >
        {{ size }}
      </option>
    </select>

    <p
      v-if="fieldErrors.size"
      id="auction-size-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.size }}
    </p>
  </div>

  <!-- 商品狀況 -->
  <div class="col-md-6">
    <label class="form-label" for="auction-condition">
      商品狀況
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-condition"
      :ref="setFieldElement('condition')"
      v-model="form.condition"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.condition }"
      required
      :aria-invalid="Boolean(fieldErrors.condition)"
      :aria-describedby="fieldErrors.condition ? 'auction-condition-error' : undefined"
      @change="emit('revalidate-field', 'condition')"
    >
      <option disabled value="">請選擇商品狀況</option>

      <option value="NEW">全新</option>

      <option value="LIKE_NEW">近全新</option>

      <option value="USED">二手</option>
    </select>

    <p
      v-if="fieldErrors.condition"
      id="auction-condition-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.condition }}
    </p>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { AUCTION_SHOE_SIZES, CUSTOM_OPTION_VALUE } from "@/constants/auctionFormOptions";

const props = defineProps({
  form: { type: Object, required: true },
  brands: { type: Array, required: true },
  categories: { type: Array, required: true },
  fieldErrors: { type: Object, required: true },
  selectedModelOption: { type: String, required: true },
  availableModelOptions: { type: Array, required: true },
  setFieldElement: { type: Function, required: true },
});

const emit = defineEmits([
  "update:selectedModelOption",
  "brand-change",
  "model-option-change",
  "revalidate-field",
]);

// The select writes through to the one UI controller owned by AuctionCreateView.
const modelOption = computed({
  get: () => props.selectedModelOption,
  set: (value) => emit("update:selectedModelOption", value),
});

function handleModelSelectionChange(event) {
  // Update the Parent's controller before its existing handler reads the value.
  emit("update:selectedModelOption", event.target.value);
  emit("model-option-change");
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.col-md-8,
.col-md-6,
.col-md-4 {
  width: auto;
  max-width: none;
  min-width: 0;
  padding: 0;
}

.col-md-8 { grid-column: span 8; }
.col-md-6 { grid-column: span 6; }
.col-md-4 { grid-column: span 4; }

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

@media (max-width: 767.98px) {
  .col-md-8,
  .col-md-6,
  .col-md-4 { grid-column: 1 / -1; }
}
</style>
