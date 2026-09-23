<script setup>
import { computed } from "vue";
import {
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
} from "@/constants/auctionFormOptions";

const props = defineProps({
  form: { type: Object, required: true },
  editable: { type: Boolean, required: true },
  fieldErrors: { type: Object, required: true },
  selectedFormOptions: { type: Object, required: true },
  deliveryOption: { type: String, required: true },
  legacyShippingMethodOption: { type: String, default: "" },
  legacyPaymentMethodOption: { type: String, default: "" },
});

const emit = defineEmits([
  "update:deliveryOption",
  "form-option-change",
  "revalidate-field",
]);

const deliveryOptionModel = computed({
  get: () => props.deliveryOption,
  set: (value) => emit("update:deliveryOption", value),
});
</script>

<template>
  <div class="card mb-4">
    <div class="card-header">交易資訊</div>

    <div class="card-body">
      <div class="row g-3">
        <!-- 運送方式 -->
        <div class="col-md-6" data-validation-field="shippingMethod">
          <label
            for="auction-edit-shipping-method-option"
            class="form-label"
          >
            運送方式
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <select
            id="auction-edit-shipping-method-option"
            v-model="selectedFormOptions.shippingMethod"
            class="form-select"
            :class="{ 'is-invalid-field': fieldErrors.shippingMethod }"
            required
            :disabled="!editable"
            :aria-invalid="Boolean(fieldErrors.shippingMethod)"
            :aria-describedby="fieldErrors.shippingMethod ? 'auction-edit-shipping-method-error' : undefined"
            @change="emit('form-option-change', 'shippingMethod')"
          >
            <option disabled value="">請選擇運送方式</option>
            <option
              v-for="option in AUCTION_SHIPPING_METHOD_OPTIONS"
              :key="option"
              :value="option"
            >
              {{ option }}
            </option>
            <option
              v-if="legacyShippingMethodOption"
              :value="legacyShippingMethodOption"
            >
              {{ legacyShippingMethodOption }}（舊資料）
            </option>
          </select>


          <p
            v-if="fieldErrors.shippingMethod"
            id="auction-edit-shipping-method-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.shippingMethod }}
          </p>
        </div>

        <!-- 運費 -->
        <div class="col-md-6" data-validation-field="shippingFee">
          <label for="auction-edit-shipping-fee" class="form-label">
            運費（NT$）
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <input
            id="auction-edit-shipping-fee"
            v-model.number="form.shippingFee"
            type="number"
            min="0"
            step="1"
            class="form-control"
            :class="{ 'is-invalid-field': fieldErrors.shippingFee }"
            required
            :disabled="!editable"
            :aria-invalid="Boolean(fieldErrors.shippingFee)"
            :aria-describedby="
              fieldErrors.shippingFee
                ? 'auction-edit-shipping-fee-error'
                : undefined
            "
            @input="emit('revalidate-field', 'shippingFee')"
          />

          <p
            v-if="fieldErrors.shippingFee"
            id="auction-edit-shipping-fee-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.shippingFee }}
          </p>
        </div>

        <!-- 付款方式 -->
        <div class="col-md-6" data-validation-field="paymentMethod">
          <label
            for="auction-edit-payment-method-option"
            class="form-label"
          >
            付款方式
            <span class="required-mark" aria-hidden="true">*</span>
          </label>

          <select
            id="auction-edit-payment-method-option"
            v-model="selectedFormOptions.paymentMethod"
            class="form-select"
            :class="{ 'is-invalid-field': fieldErrors.paymentMethod }"
            required
            :disabled="!editable"
            :aria-invalid="Boolean(fieldErrors.paymentMethod)"
            :aria-describedby="fieldErrors.paymentMethod ? 'auction-edit-payment-method-error' : undefined"
            @change="emit('form-option-change', 'paymentMethod')"
          >
            <option disabled value="">請選擇付款方式</option>
            <option
              v-for="option in AUCTION_PAYMENT_METHOD_OPTIONS"
              :key="option"
              :value="option"
            >
              {{ option }}
            </option>
            <option
              v-if="legacyPaymentMethodOption"
              :value="legacyPaymentMethodOption"
            >
              {{ legacyPaymentMethodOption }}（舊資料）
            </option>
          </select>


          <p
            v-if="fieldErrors.paymentMethod"
            id="auction-edit-payment-method-error"
            class="field-error"
            role="alert"
          >
            {{ fieldErrors.paymentMethod }}
          </p>
        </div>

        <!-- 交付選項 -->
        <div class="col-12">
          <fieldset class="delivery-options">
            <legend id="auction-edit-delivery-option-label" class="form-label">
              交付選項
              <span class="required-mark" aria-hidden="true">*</span>
            </legend>

            <div
              class="delivery-option-grid"
              role="radiogroup"
              aria-labelledby="auction-edit-delivery-option-label"
              aria-required="true"
            >
              <label
                class="delivery-option-card"
                :class="{
                  selected:
                    deliveryOption ===
                    DELIVERY_OPTION_VALUES.SHIPPING_ONLY,
                }"
              >
                <input
                  v-model="deliveryOptionModel"
                  type="radio"
                  name="auction-edit-delivery-option"
                  :value="DELIVERY_OPTION_VALUES.SHIPPING_ONLY"
                  :disabled="!editable"
                />
                <span>
                  <strong>僅提供寄送</strong>
                  <small>依上方選擇的運送方式與運費寄送。</small>
                </span>
              </label>

              <label
                class="delivery-option-card"
                :class="{
                  selected:
                    deliveryOption ===
                    DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP,
                }"
              >
                <input
                  v-model="deliveryOptionModel"
                  type="radio"
                  name="auction-edit-delivery-option"
                  :value="DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP"
                  :disabled="!editable"
                />
                <span>
                  <strong>提供寄送，也可協調面交</strong>
                  <small>
                    刊登仍保留寄送方式；若雙方同意，可另外協調面交。
                  </small>
                </span>
              </label>
            </div>
          </fieldset>
        </div>

        <!-- 退貨 -->
        <div class="col-md-3">
          <div class="form-check">
            <input
              id="allowReturn"
              v-model="form.allowReturn"
              type="checkbox"
              class="form-check-input"
              :disabled="!editable"
            />

            <label for="allowReturn" class="form-check-label">
              允許退貨
            </label>
          </div>
        </div>

        <!-- 保固 -->
        <div class="col-12">
          <label for="auction-edit-warranty-note-option" class="form-label">
            保固／售後說明
          </label>

          <select
            id="auction-edit-warranty-note-option"
            v-model="selectedFormOptions.warrantyNote"
            class="form-select"
            :disabled="!editable"
            @change="emit('form-option-change', 'warrantyNote')"
          >
            <option value="">請選擇（可不填）</option>
            <option
              v-for="option in AUCTION_WARRANTY_NOTE_OPTIONS"
              :key="option"
              :value="option"
            >
              {{ option }}
            </option>
            <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
          </select>

          <div
            v-if="selectedFormOptions.warrantyNote === CUSTOM_OPTION_VALUE"
            class="custom-option-field"
          >
            <label
              for="auction-edit-warranty-note-custom"
              class="custom-option-label"
            >
              請輸入自訂保固／售後說明
            </label>
            <textarea
              id="auction-edit-warranty-note-custom"
              v-model.trim="form.warrantyNote"
              class="form-control"
              rows="2"
              maxlength="500"
              data-custom-input
              :disabled="!editable"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
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

.delivery-options {
  margin: 0;
  padding: 0;
  border: 0;
}

.delivery-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.delivery-option-card {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 1rem;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
  background: #fff;
  cursor: pointer;
}

.delivery-option-card.selected {
  border-color: #111;
  box-shadow: inset 0 0 0 1px #111;
}

.delivery-option-card input {
  margin-top: 0.2rem;
  accent-color: #111;
}

.delivery-option-card strong,
.delivery-option-card small {
  display: block;
}

.delivery-option-card strong {
  color: #111;
}

.delivery-option-card small {
  margin-top: 0.25rem;
  color: #6c757d;
  line-height: 1.55;
}

.delivery-option-card:has(input:disabled) {
  cursor: not-allowed;
  opacity: 0.7;
}

@media (max-width: 767.98px) {
  .delivery-option-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>