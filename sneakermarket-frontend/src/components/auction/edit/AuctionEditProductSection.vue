<script setup>
import { computed } from "vue";
import {
  AUCTION_SHOE_SIZES,
  CUSTOM_OPTION_VALUE,
} from "@/constants/auctionFormOptions";

const props = defineProps({
  form: { type: Object, required: true },
  brands: { type: Array, required: true },
  categories: { type: Array, required: true },
  editable: { type: Boolean, required: true },
  auctionStatus: { type: String, required: true },
  selectedModelOption: { type: String, required: true },
  availableModelOptions: { type: Array, required: true },
  fieldErrors: { type: Object, required: true },
});

const emit = defineEmits([
  "update:selectedModelOption",
  "brand-change",
  "model-option-change",
  "revalidate-field",
]);

// The UI selector remains owned by AuctionEditView. Keep native select v-model behavior.
const selectedModelOption = computed({
  get: () => props.selectedModelOption,
  set: (value) => emit("update:selectedModelOption", value),
});

</script>

<template>
  <!-- 品牌 -->
  <div class="col-md-6" data-validation-field="brandId">
    <label for="auction-edit-brand" class="form-label">
      品牌
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-edit-brand"
      v-model.number="form.brandId"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.brandId }"
      required
      :disabled="!editable || auctionStatus === 'ACTIVE'"
      :aria-invalid="Boolean(fieldErrors.brandId)"
      :aria-describedby="
        fieldErrors.brandId ? 'auction-edit-brand-error' : undefined
      "
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
      id="auction-edit-brand-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.brandId }}
    </p>

    <div v-if="auctionStatus === 'ACTIVE'" class="form-text">
      競標開始後不可修改品牌／分類。
    </div>
  </div>

  <!-- 分類 -->
  <div class="col-md-6" data-validation-field="categoryId">
    <label for="auction-edit-category" class="form-label">
      分類
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-edit-category"
      v-model.number="form.categoryId"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.categoryId }"
      required
      :disabled="!editable || auctionStatus === 'ACTIVE'"
      :aria-invalid="Boolean(fieldErrors.categoryId)"
      :aria-describedby="
        fieldErrors.categoryId
          ? 'auction-edit-category-error'
          : undefined
      "
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
      id="auction-edit-category-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.categoryId }}
    </p>
  </div>

  <!-- 商品名稱 -->
  <div class="col-md-8" data-validation-field="productName">
    <label for="auction-edit-product-name" class="form-label">
      商品名稱
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <input
      id="auction-edit-product-name"
      v-model.trim="form.productName"
      type="text"
      maxlength="150"
      class="form-control"
      :class="{ 'is-invalid-field': fieldErrors.productName }"
      required
      :disabled="!editable"
      :aria-invalid="Boolean(fieldErrors.productName)"
      :aria-describedby="
        fieldErrors.productName
          ? 'auction-edit-product-name-error'
          : undefined
      "
      @input="emit('revalidate-field', 'productName')"
    />

    <p
      v-if="fieldErrors.productName"
      id="auction-edit-product-name-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.productName }}
    </p>
  </div>

  <!-- 型號 -->
  <div class="col-md-4">
    <label class="form-label" for="auction-edit-model-option">
      型號／系列
    </label>

    <select
      id="auction-edit-model-option"
      v-model="selectedModelOption"
      class="form-select"
      :disabled="!editable || !form.brandId"
      @change="emit('model-option-change')"
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
      class="custom-option-field"
    >
      <label
        class="custom-option-label"
        for="auction-edit-model-custom"
      >
        自訂型號／系列
      </label>
      <input
        id="auction-edit-model-custom"
        v-model.trim="form.model"
        type="text"
        maxlength="100"
        class="form-control"
        :disabled="!editable"
        placeholder="例如：聯名款名稱或特殊型號"
      />
    </div>
  </div>

  <!-- 尺寸 -->
  <div class="col-md-6" data-validation-field="size">
    <label for="auction-edit-size" class="form-label">
      尺寸
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-edit-size"
      v-model.trim="form.size"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.size }"
      required
      :disabled="!editable"
      :aria-invalid="Boolean(fieldErrors.size)"
      :aria-describedby="
        fieldErrors.size ? 'auction-edit-size-error' : undefined
      "
      @change="emit('revalidate-field', 'size')"
    >
      <option disabled value="">請選擇尺寸</option>
      <option
        v-for="sizeOption in AUCTION_SHOE_SIZES"
        :key="sizeOption"
        :value="sizeOption"
      >
        {{ sizeOption }}
      </option>
    </select>

    <p
      v-if="fieldErrors.size"
      id="auction-edit-size-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.size }}
    </p>
  </div>

  <!-- 商品狀況 -->
  <div class="col-md-6" data-validation-field="condition">
    <label for="auction-edit-condition" class="form-label">
      商品狀況
      <span class="required-mark" aria-hidden="true">*</span>
    </label>

    <select
      id="auction-edit-condition"
      v-model="form.condition"
      class="form-select"
      :class="{ 'is-invalid-field': fieldErrors.condition }"
      required
      :disabled="!editable"
      :aria-invalid="Boolean(fieldErrors.condition)"
      :aria-describedby="
        fieldErrors.condition
          ? 'auction-edit-condition-error'
          : undefined
      "
      @change="emit('revalidate-field', 'condition')"
    >
      <option value="NEW">全新</option>

      <option value="LIKE_NEW">近全新</option>

      <option value="USED">二手</option>
    </select>

    <p
      v-if="fieldErrors.condition"
      id="auction-edit-condition-error"
      class="field-error"
      role="alert"
    >
      {{ fieldErrors.condition }}
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
