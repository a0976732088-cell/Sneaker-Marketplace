<template>
  <section class="checkout-panel" aria-labelledby="shipping-heading">
    <div class="panel-heading">
      <div>
        <p class="panel-step">01</p>
        <h2 id="shipping-heading">收件資訊</h2>
      </div>
      <button
        type="button"
        class="text-button"
        :disabled="!canOperateCheckout || isBusy || !hasMemberProfile"
        @click="emit('fill-from-member')"
      >
        帶入目前會員資料
      </button>
    </div>

    <div v-if="deadlineNotice" class="notice notice--warning" role="status">
      {{ deadlineNotice }}
    </div>

    <div v-if="routeNotice" class="notice notice--warning" role="status">
      {{ routeNotice }}
    </div>

    <fieldset class="form-grid" :disabled="!canOperateCheckout || isBusy">
      <div class="form-field form-field--full">
        <label for="auction-checkout-email">Email</label>
        <input
          id="auction-checkout-email"
          type="email"
          :value="memberEmail"
          autocomplete="email"
          readonly
        />
        <p class="field-hint">Email 僅供顯示，不會寫入競標訂單。</p>
      </div>

      <div class="form-field">
        <label for="auction-receiver-name">收件人</label>
        <input
          id="auction-receiver-name"
          v-model="shippingForm.receiverName"
          type="text"
          maxlength="50"
          autocomplete="name"
          :aria-invalid="Boolean(shippingErrors.receiverName)"
          :aria-describedby="shippingErrors.receiverName ? 'auction-receiver-name-error' : undefined"
          @input="shippingErrors.receiverName = ''"
        />
        <p
          v-if="shippingErrors.receiverName"
          id="auction-receiver-name-error"
          class="field-error"
        >
          {{ shippingErrors.receiverName }}
        </p>
      </div>

      <div class="form-field">
        <label for="auction-receiver-phone">電話</label>
        <input
          id="auction-receiver-phone"
          v-model="shippingForm.receiverPhone"
          type="tel"
          maxlength="20"
          autocomplete="tel"
          :aria-invalid="Boolean(shippingErrors.receiverPhone)"
          :aria-describedby="shippingErrors.receiverPhone ? 'auction-receiver-phone-error' : undefined"
          @input="shippingErrors.receiverPhone = ''"
        />
        <p
          v-if="shippingErrors.receiverPhone"
          id="auction-receiver-phone-error"
          class="field-error"
        >
          {{ shippingErrors.receiverPhone }}
        </p>
      </div>

      <div v-if="!isCvsShipping" class="form-field form-field--full">
        <label for="auction-shipping-address">配送地址</label>
        <textarea
          id="auction-shipping-address"
          v-model="shippingForm.shippingAddress"
          rows="3"
          maxlength="300"
          autocomplete="street-address"
          :aria-invalid="Boolean(shippingErrors.shippingAddress)"
          :aria-describedby="shippingErrors.shippingAddress ? 'auction-shipping-address-error' : undefined"
          @input="shippingErrors.shippingAddress = ''"
        ></textarea>
        <p
          v-if="shippingErrors.shippingAddress"
          id="auction-shipping-address-error"
          class="field-error"
        >
          {{ shippingErrors.shippingAddress }}
        </p>
      </div>

      <div v-else class="cvs-store-picker form-field--full">
        <span class="cvs-store-picker__label">取貨超商</span>

        <div class="cvs-selection-card">
          <p class="cvs-selection-card__label">目前取貨門市</p>
          <template v-if="hasSelectedCvsStore">
            <strong>{{ selectedCvsStoreLabel }} {{ cvsSelection.cvsStoreName }}</strong>
            <span>門市代碼：{{ displayValue(cvsSelection.cvsStoreId) }}</span>
            <span>地址：{{ displayValue(cvsSelection.cvsAddress) }}</span>
            <span v-if="cvsSelection.cvsTelephone">
              電話：{{ cvsSelection.cvsTelephone }}
            </span>
          </template>
          <p v-else class="cvs-selection-card__empty">未選擇門市</p>
          <div class="cvs-selection-card__actions">
            <button
              v-if="hasSelectedCvsStore"
              type="button"
              class="secondary-button"
              @click="emit('open-store-map', cvsSelection)"
            >
              查看地圖
            </button>
            <button
              type="button"
              class="secondary-button"
              @click="emit('open-cvs-picker')"
            >
              {{ hasSelectedCvsStore ? "更換門市" : "選擇門市" }}
            </button>
          </div>
        </div>

        <slot name="cvs-picker" />

        <p v-if="!hasSelectedCvsStore" class="field-hint">
          請先選擇超商取貨門市
        </p>
        <p v-if="shippingErrors.shippingAddress" class="field-error">
          {{ shippingErrors.shippingAddress }}
        </p>
      </div>

      <div class="readonly-fact form-field--full">
        <span>配送方式</span>
        <strong>{{ displayValue(order.shippingMethod) }}</strong>
      </div>
    </fieldset>
  </section>
</template>

<script setup>
defineProps({
  order: { type: Object, required: true },
  memberEmail: { type: String, required: true },
  shippingForm: { type: Object, required: true },
  shippingErrors: { type: Object, required: true },
  isCvsShipping: { type: Boolean, required: true },
  canOperateCheckout: { type: Boolean, required: true },
  isBusy: { type: Boolean, required: true },
  hasMemberProfile: { type: Boolean, required: true },
  deadlineNotice: { type: String, required: true },
  routeNotice: { type: String, required: true },
  hasSelectedCvsStore: { type: Boolean, required: true },
  selectedCvsStoreLabel: { type: String, required: true },
  cvsSelection: { type: Object, required: true },
});

const emit = defineEmits(["fill-from-member", "open-cvs-picker", "open-store-map"]);

function displayValue(value) {
  return value === null || value === undefined || value === "" ? "—" : value;
}
</script>

<style scoped>
.checkout-panel { padding: 26px; border: 1px solid #e9ecef; border-radius: 10px; background: #fff; }
.panel-heading { display: flex; align-items: start; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.panel-heading h2 { margin: 0; font-size: 1.15rem; }
.panel-step { margin: 0 0 6px; color: #6c757d; font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.text-button { border: 0; background: transparent; color: #111; font-weight: 650; text-decoration: underline; text-underline-offset: 4px; }
.text-button:disabled { color: #adb5bd; cursor: not-allowed; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; min-width: 0; margin: 0; padding: 0; border: 0; }
.form-field { min-width: 0; }
.form-field--full { grid-column: 1 / -1; }
.form-field label { display: block; margin-bottom: 7px; color: #343a40; font-size: .85rem; font-weight: 650; }
.form-field input, .form-field select, .form-field textarea { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; color: #111; }
.form-field input:focus, .form-field select:focus, .form-field textarea:focus { border-color: #111; outline: 2px solid rgba(17,17,17,.12); }
.form-field input:disabled, .form-field select:disabled, .form-field textarea:disabled, .form-field input[readonly] { background: #f8f9fa; color: #6c757d; }
.form-field [aria-invalid="true"] { border-color: #b42318; }
.field-hint, .field-error { margin: 7px 0 0; font-size: .78rem; line-height: 1.5; }
.field-hint { color: #6c757d; }
.field-error { color: #b42318; }
.cvs-store-picker { display: grid; gap: 14px; }
.cvs-store-picker__label { color: #343a40; font-size: .85rem; font-weight: 650; }
.cvs-selection-card { display: grid; gap: 5px; padding: 15px; border: 1px solid #e9ecef; border-radius: 8px; background: #f8f9fa; }
.cvs-selection-card__label, .cvs-selection-card__empty { margin: 0; color: #6c757d; font-size: .8rem; }
.cvs-selection-card strong { overflow-wrap: anywhere; }
.cvs-selection-card span { color: #495057; font-size: .84rem; line-height: 1.45; overflow-wrap: anywhere; }
.cvs-selection-card__actions { display: flex; flex-wrap: wrap; gap: 8px; justify-self: start; margin-top: 8px; }
.readonly-fact { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px; border-radius: 7px; background: #f8f9fa; color: #6c757d; }
.readonly-fact strong { color: #111; }
.notice { padding: 12px 14px; border-radius: 7px; font-size: .88rem; line-height: 1.55; }
.notice--warning { margin-bottom: 18px; background: #fff3cd; color: #765a00; }
.secondary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 10px 17px; border: 1px solid #111; border-radius: 6px; background: #fff; color: #111; font-weight: 700; text-decoration: none; }
.secondary-button:disabled { border-color: #dee2e6; background: #f8f9fa; color: #adb5bd; cursor: not-allowed; }

@media (max-width: 575.98px) {
  .checkout-panel { padding: 20px; }
  .panel-heading { flex-direction: column; gap: 10px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-field--full { grid-column: auto; }
  .secondary-button { width: 100%; }
}
</style>
