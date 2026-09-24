<template>
  <section class="checkout-panel" aria-labelledby="payment-heading">
    <div class="panel-heading">
      <div>
        <p class="panel-step">02</p>
        <h2 id="payment-heading">付款資訊</h2>
      </div>
      <span class="payment-method-label">{{ paymentMethodLabel }}</span>
    </div>

    <fieldset
      v-if="isCreditCard"
      class="form-grid"
      :disabled="!canOperateCheckout || isBusy"
    >
      <div class="form-field form-field--full">
        <label for="auction-card-holder">持卡人姓名</label>
        <input
          id="auction-card-holder"
          v-model="cardForm.cardHolderName"
          type="text"
          autocomplete="cc-name"
          :aria-invalid="Boolean(cardErrors.cardHolderName)"
          :aria-describedby="cardErrors.cardHolderName ? 'auction-card-holder-error' : undefined"
          @input="cardErrors.cardHolderName = ''"
        />
        <p v-if="cardErrors.cardHolderName" id="auction-card-holder-error" class="field-error">
          {{ cardErrors.cardHolderName }}
        </p>
      </div>

      <div class="form-field form-field--full">
        <label for="auction-card-number">信用卡號</label>
        <input
          id="auction-card-number"
          :value="cardForm.cardNumber"
          type="text"
          inputmode="numeric"
          maxlength="19"
          autocomplete="cc-number"
          placeholder="0000 0000 0000 0000"
          :aria-invalid="Boolean(cardErrors.cardNumber)"
          :aria-describedby="cardErrors.cardNumber ? 'auction-card-number-error' : undefined"
          @input="$emit('card-number-input', $event)"
        />
        <p v-if="cardErrors.cardNumber" id="auction-card-number-error" class="field-error">
          {{ cardErrors.cardNumber }}
        </p>
      </div>

      <div class="form-field">
        <label for="auction-card-expiry">有效期限</label>
        <input
          id="auction-card-expiry"
          :value="cardForm.cardExpiry"
          type="text"
          inputmode="numeric"
          maxlength="5"
          autocomplete="cc-exp"
          placeholder="MM/YY"
          :aria-invalid="Boolean(cardErrors.cardExpiry)"
          :aria-describedby="cardErrors.cardExpiry ? 'auction-card-expiry-error' : undefined"
          @input="$emit('card-expiry-input', $event)"
        />
        <p v-if="cardErrors.cardExpiry" id="auction-card-expiry-error" class="field-error">
          {{ cardErrors.cardExpiry }}
        </p>
      </div>

      <div class="form-field">
        <label for="auction-card-cvv">安全碼</label>
        <input
          id="auction-card-cvv"
          :value="cardForm.cardCvv"
          type="password"
          inputmode="numeric"
          maxlength="4"
          autocomplete="cc-csc"
          placeholder="CVV"
          :aria-invalid="Boolean(cardErrors.cardCvv)"
          :aria-describedby="cardErrors.cardCvv ? 'auction-card-cvv-error' : undefined"
          @input="$emit('card-cvv-input', $event)"
        />
        <p v-if="cardErrors.cardCvv" id="auction-card-cvv-error" class="field-error">
          {{ cardErrors.cardCvv }}
        </p>
      </div>

      <p class="simulation-note form-field--full">
        此為模擬付款，卡片資料只用於本頁格式驗證，不會傳送或儲存。
      </p>
    </fieldset>

    <fieldset
      v-else-if="isTransfer"
      class="form-grid"
      :disabled="!canOperateCheckout || isBusy"
    >
      <div class="transfer-instructions form-field--full">
        <p class="simulation-label">Demo／模擬轉帳資訊</p>
        <dl>
          <div><dt>銀行名稱</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.bankName }}</dd></div>
          <div><dt>銀行代碼</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.bankCode }}</dd></div>
          <div><dt>銀行帳號</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.accountNumber }}</dd></div>
          <div><dt>戶名</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.accountName }}</dd></div>
          <div><dt>應付金額</dt><dd>{{ formatTwd(order.totalAmount) }}</dd></div>
        </dl>
      </div>

      <div class="form-field">
        <label for="auction-remitter-name">匯款人姓名</label>
        <input
          id="auction-remitter-name"
          v-model="transferForm.remitterName"
          type="text"
          autocomplete="off"
          :aria-invalid="Boolean(transferErrors.remitterName)"
          :aria-describedby="transferErrors.remitterName ? 'auction-remitter-name-error' : undefined"
          @input="transferErrors.remitterName = ''"
        />
        <p v-if="transferErrors.remitterName" id="auction-remitter-name-error" class="field-error">
          {{ transferErrors.remitterName }}
        </p>
      </div>

      <div class="form-field">
        <label for="auction-account-last-five">轉帳帳號末五碼</label>
        <input
          id="auction-account-last-five"
          :value="transferForm.accountLastFive"
          type="text"
          inputmode="numeric"
          maxlength="5"
          autocomplete="off"
          placeholder="12345"
          :aria-invalid="Boolean(transferErrors.accountLastFive)"
          :aria-describedby="transferErrors.accountLastFive ? 'auction-account-last-five-error' : undefined"
          @input="$emit('account-last-five-input', $event)"
        />
        <p v-if="transferErrors.accountLastFive" id="auction-account-last-five-error" class="field-error">
          {{ transferErrors.accountLastFive }}
        </p>
      </div>

      <p class="simulation-note form-field--full">
        此為模擬轉帳確認，匯款人與末五碼不會傳送或儲存。
      </p>
    </fieldset>

    <div v-else-if="isLinePay" class="line-pay-instructions">
      <strong>LINE Pay</strong>
      <p v-if="isWaitingForLinePay">
        等待 LINE Pay 完成付款…<br />請在 LINE Pay 付款視窗完成操作。
      </p>
      <p v-else>
        將開啟 LINE Pay 付款視窗。<br />付款完成後會自動前往競標訂單。
      </p>
    </div>

    <div v-else class="notice notice--error" role="alert">
      目前付款方式無法處理，請稍後再試。
    </div>
  </section>
</template>

<script setup>
import { AUCTION_TRANSFER_ACCOUNT } from "@/constants/auctionPayment";
import { formatTwd } from "@/utils/auctionMoney";

defineProps({
  order: { type: Object, required: true },
  isCreditCard: { type: Boolean, required: true },
  isTransfer: { type: Boolean, required: true },
  isLinePay: { type: Boolean, required: true },
  paymentMethodLabel: { type: String, required: true },
  canOperateCheckout: { type: Boolean, required: true },
  isBusy: { type: Boolean, required: true },
  cardForm: { type: Object, required: true },
  cardErrors: { type: Object, required: true },
  transferForm: { type: Object, required: true },
  transferErrors: { type: Object, required: true },
  isWaitingForLinePay: { type: Boolean, required: true },
});

defineEmits([
  "card-number-input",
  "card-expiry-input",
  "card-cvv-input",
  "account-last-five-input",
]);
</script>

<style scoped>
.checkout-panel { padding: 26px; border: 1px solid #e9ecef; border-radius: 10px; background: #fff; }
.panel-heading { display: flex; align-items: start; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.panel-heading h2 { margin: 0; font-size: 1.15rem; }
.panel-step { margin: 0 0 6px; color: #6c757d; font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.payment-method-label { padding: 5px 10px; border-radius: 999px; background: #f1f3f5; color: #343a40; font-size: .8rem; font-weight: 700; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; min-width: 0; margin: 0; padding: 0; border: 0; }
.form-field { min-width: 0; }
.form-field--full { grid-column: 1 / -1; }
.form-field label { display: block; margin-bottom: 7px; color: #343a40; font-size: .85rem; font-weight: 650; }
.form-field input { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; color: #111; }
.form-field input:focus { border-color: #111; outline: 2px solid rgba(17,17,17,.12); }
.form-field input:disabled { background: #f8f9fa; color: #6c757d; }
.form-field [aria-invalid="true"] { border-color: #b42318; }
.field-error, .simulation-note { margin: 7px 0 0; font-size: .78rem; line-height: 1.5; }
.simulation-note { color: #6c757d; }
.field-error { color: #b42318; }
.transfer-instructions { padding: 18px; border: 1px solid #e9ecef; border-radius: 8px; background: #f8f9fa; }
.simulation-label { margin: 0 0 12px; font-weight: 700; }
.transfer-instructions dl { display: grid; gap: 9px; margin: 0; }
.transfer-instructions dl div { display: flex; justify-content: space-between; gap: 20px; }
.transfer-instructions dt { color: #6c757d; font-weight: 500; }
.transfer-instructions dd { margin: 0; font-weight: 650; text-align: right; overflow-wrap: anywhere; }
.line-pay-instructions { padding: 18px; border: 1px solid #e9ecef; border-radius: 8px; background: #f8f9fa; }
.line-pay-instructions p { margin: 8px 0 0; color: #495057; line-height: 1.6; }
.notice { padding: 12px 14px; border-radius: 7px; font-size: .88rem; line-height: 1.55; }
.notice--error { background: #fbe9e7; color: #a12b21; }

@media (max-width: 575.98px) {
  .checkout-panel { padding: 20px; }
  .panel-heading { flex-direction: column; gap: 10px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-field--full { grid-column: auto; }
}
</style>
