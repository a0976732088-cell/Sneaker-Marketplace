<template>
  <section class="create-section">
    <header class="section-heading">
      <p class="section-eyebrow">STEP 03</p>
      <h2>交易資訊</h2>
      <p>說明運送、付款與售後方式，建立清楚的交易共識。</p>
    </header>

    <div class="create-field-grid">
      <!-- 運送方式 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-shipping-method">
          運送方式
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <select
          id="auction-shipping-method"
          :ref="setFieldElement('shippingMethod')"
          v-model="selectedFormOptions.shippingMethod"
          class="form-select"
          :class="{ 'is-invalid-field': fieldErrors.shippingMethod }"
          required
          :aria-invalid="Boolean(fieldErrors.shippingMethod)"
          :aria-describedby="fieldErrors.shippingMethod ? 'auction-shipping-method-error' : undefined"
          @change="emit('form-option-change', 'shippingMethod')"
        >
          <option value="">請選擇運送方式</option>
          <option
            v-for="option in AUCTION_SHIPPING_METHOD_OPTIONS"
            :key="option"
            :value="option"
          >
            {{ option }}
          </option>
        </select>

        <p
          v-if="fieldErrors.shippingMethod"
          id="auction-shipping-method-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.shippingMethod }}
        </p>
      </div>

      <!-- 運費 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-shipping-fee">
          運費（NT$）
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <input
          id="auction-shipping-fee"
          :ref="setFieldElement('shippingFee')"
          v-model.number="form.shippingFee"
          type="number"
          min="0"
          step="1"
          class="form-control"
          :class="{ 'is-invalid-field': fieldErrors.shippingFee }"
          required
          :aria-invalid="Boolean(fieldErrors.shippingFee)"
          :aria-describedby="fieldErrors.shippingFee ? 'auction-shipping-fee-error' : undefined"
          @input="emit('revalidate-field', 'shippingFee')"
        />

        <p
          v-if="fieldErrors.shippingFee"
          id="auction-shipping-fee-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.shippingFee }}
        </p>
      </div>

      <!-- 付款方式 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-payment-method">
          付款方式
          <span class="required-mark" aria-hidden="true">*</span>
        </label>

        <select
          id="auction-payment-method"
          :ref="setFieldElement('paymentMethod')"
          v-model="selectedFormOptions.paymentMethod"
          class="form-select"
          :class="{ 'is-invalid-field': fieldErrors.paymentMethod }"
          required
          :aria-invalid="Boolean(fieldErrors.paymentMethod)"
          :aria-describedby="fieldErrors.paymentMethod ? 'auction-payment-method-error' : undefined"
          @change="emit('form-option-change', 'paymentMethod')"
        >
          <option value="">請選擇付款方式</option>
          <option
            v-for="option in AUCTION_PAYMENT_METHOD_OPTIONS"
            :key="option"
            :value="option"
          >
            {{ option }}
          </option>
        </select>

        <p
          v-if="fieldErrors.paymentMethod"
          id="auction-payment-method-error"
          class="field-error"
          role="alert"
        >
          {{ fieldErrors.paymentMethod }}
        </p>
      </div>

      <!-- 保固 -->
      <div class="col-md-6">
        <label class="form-label" for="auction-warranty-option">
          保固／售後說明
        </label>

        <select
          id="auction-warranty-option"
          v-model="selectedFormOptions.warrantyNote"
          class="form-select"
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
          class="custom-field"
        >
          <label class="custom-field-label" for="auction-warranty-custom">
            請輸入自訂內容
          </label>
          <textarea
            id="auction-warranty-custom"
            v-model.trim="form.warrantyNote"
            class="form-control"
            rows="3"
            maxlength="500"
          ></textarea>
        </div>
      </div>

      <fieldset class="delivery-options col-12">
        <legend class="form-label">
          交付選項
          <span class="required-mark" aria-hidden="true">*</span>
        </legend>

        <div class="delivery-option-grid">
          <label
            class="delivery-option-card"
            :class="{
              'is-selected':
                deliveryOption === DELIVERY_OPTION_VALUES.SHIPPING_ONLY,
            }"
            for="delivery-shipping-only"
          >
            <input
              id="delivery-shipping-only"
              v-model="deliveryOptionModel"
              type="radio"
              name="deliveryOption"
              :value="DELIVERY_OPTION_VALUES.SHIPPING_ONLY"
              class="delivery-option-radio"
              required
            />
            <span>
              <strong>僅提供寄送</strong>
              <small>依上方選擇的運送方式與運費寄送。</small>
            </span>
          </label>

          <label
            class="delivery-option-card"
            :class="{
              'is-selected':
                deliveryOption === DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP,
            }"
            for="delivery-shipping-or-meetup"
          >
            <input
              id="delivery-shipping-or-meetup"
              v-model="deliveryOptionModel"
              type="radio"
              name="deliveryOption"
              :value="DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP"
              class="delivery-option-radio"
              required
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

      <div class="option-grid return-option-grid">
        <!-- 退貨 -->
        <div class="option-item">
          <input
            id="allowReturn"
            v-model="form.allowReturn"
            type="checkbox"
            class="option-checkbox"
          />

          <label for="allowReturn" class="option-label">
            <span>允許退貨</span>
            <small>依刊登內容向得標者說明可接受的退貨方式。</small>
          </label>
        </div>
      </div>
    </div>
  </section>
</template>

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
  selectedFormOptions: { type: Object, required: true },
  deliveryOption: { type: String, required: true },
  fieldErrors: { type: Object, required: true },
  setFieldElement: { type: Function, required: true },
});

const emit = defineEmits([
  "form-option-change",
  "revalidate-field",
  "update:deliveryOption",
]);

// Proxy the parent's UI-only value so the native radio v-model keeps its behavior.
const deliveryOptionModel = computed({
  get: () => props.deliveryOption,
  set: (value) => emit("update:deliveryOption", value),
});
</script>

<style scoped>
.create-section,
.create-section * {
  box-sizing: border-box;
}

.create-section {
  margin: 0;
  padding: 3rem 0;
  border: 0;
  border-top: 1px solid #e9ecef;
}

.create-section:first-child {
  padding-top: 0;
  border-top: 0;
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

.create-field-grid > .col-12 {
  grid-column: 1 / -1;
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

.form-control::placeholder {
  color: #8a9096;
}

.form-control:focus,
.form-select:focus {
  outline: 0;
  border-color: #111;
  box-shadow: 0 0 0 3px rgb(17 17 17 / 10%);
}

.form-control.is-invalid-field,
.form-select.is-invalid-field {
  border-color: #dc3545;
}

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

.custom-field {
  margin-top: 0.8rem;
}

.custom-field-label {
  display: block;
  margin: 0 0 0.45rem;
  color: #6c757d;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.4;
}

.delivery-options {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.delivery-options > .form-label {
  width: 100%;
}

.delivery-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.delivery-option-card {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  padding: 1rem;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.delivery-option-card.is-selected {
  background: #f8f9fa;
  border-color: #111;
}

.delivery-option-radio {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 0.15rem 0.75rem 0 0;
  accent-color: #111;
}

.delivery-option-card span {
  min-width: 0;
}

.delivery-option-card strong,
.delivery-option-card small {
  display: block;
}

.delivery-option-card strong {
  color: #212529;
  font-size: 0.9rem;
}

.delivery-option-card small {
  margin-top: 0.25rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.55;
}

.option-grid {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
  min-width: 0;
}

.return-option-grid {
  grid-template-columns: minmax(0, 1fr);
}

.option-item {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  padding: 1rem;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
}

.option-checkbox {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 0.15rem 0.75rem 0 0;
  accent-color: #111;
}

.option-label {
  min-width: 0;
  margin: 0;
  cursor: pointer;
}

.option-label span {
  display: block;
  color: #212529;
  font-size: 0.9rem;
  font-weight: 700;
}

.option-label small {
  display: block;
  margin-top: 0.25rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.55;
}

.delivery-option-radio:focus-visible,
.option-checkbox:focus-visible {
  outline: 2px solid #111;
  outline-offset: 3px;
}

@media (max-width: 767.98px) {
  .create-section {
    padding: 2.5rem 0;
  }

  .create-field-grid > .col-md-6,
  .create-field-grid > .col-12 {
    grid-column: 1 / -1;
  }

  .option-grid,
  .delivery-option-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 479.98px) {
  .section-heading {
    margin-bottom: 1.5rem;
  }

  .create-field-grid {
    gap: 1.2rem;
  }
}
</style>
