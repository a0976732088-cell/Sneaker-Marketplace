<template>
  <div
    v-if="open"
    class="cvs-picker-modal"
    role="presentation"
    @click.self="emit('close')"
  >
    <section
      class="cvs-picker-modal__dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cvs-picker-title"
    >
      <header class="cvs-picker-modal__header">
        <div>
          <p class="eyebrow">CVS pickup</p>
          <h2 id="cvs-picker-title">選擇取貨門市</h2>
        </div>
        <button
          type="button"
          class="cvs-picker-modal__close"
          aria-label="關閉門市選擇"
          :disabled="isSelecting"
          @click="emit('close')"
        >
          ×
        </button>
      </header>

      <div class="cvs-picker-modal__body">
        <div class="cvs-store-options" role="group" aria-label="選擇超商品牌">
          <button
            v-for="option in AUCTION_CVS_STORE_OPTIONS"
            :key="option.value"
            type="button"
            class="cvs-store-option"
            :class="{ 'cvs-store-option--active': selectedStoreType === option.value }"
            :aria-pressed="selectedStoreType === option.value"
            :disabled="isLoading || isSelecting"
            @click="emit('change-store-type', option.value)"
          >
            {{ option.label }}
          </button>
        </div>

        <div class="cvs-store-search">
          <div class="form-field">
            <label for="auction-cvs-city">縣市</label>
            <select id="auction-cvs-city" v-model="filters.city">
              <option value="">全部縣市</option>
              <option v-for="city in TAIWAN_CITIES" :key="city" :value="city">
                {{ city }}
              </option>
            </select>
          </div>
          <div class="form-field">
            <label for="auction-cvs-keyword">搜尋門市</label>
            <input
              id="auction-cvs-keyword"
              v-model="filters.keyword"
              type="search"
              maxlength="80"
              placeholder="門市名稱、店號或地址"
              autocomplete="off"
            />
          </div>
        </div>

        <p v-if="isLoading" class="cvs-store-state" role="status">
          門市資料載入中…
        </p>
        <div v-else-if="loadError" class="notice notice--error" role="alert">
          <p>{{ loadError }}</p>
          <button type="button" class="secondary-button" @click="emit('reload')">
            重新載入門市
          </button>
        </div>
        <p v-else-if="!hasSearchCriteria" class="cvs-store-state">
          請先選擇縣市或輸入門市關鍵字。
        </p>
        <p v-else-if="filteredStores.length === 0" class="cvs-store-state">
          查無符合條件的門市。
        </p>
        <template v-else>
          <p v-if="hasTooManyStores" class="notice notice--warning">
            符合門市過多，請縮小搜尋範圍。
          </p>
          <p class="cvs-store-result-count">
            顯示 {{ filteredStores.length }} 筆門市（最多 100 筆）
          </p>
          <ul class="cvs-store-results" aria-label="超商門市搜尋結果">
            <li v-for="store in filteredStores" :key="`${store.storeType}-${store.storeId}`">
              <div class="cvs-store-result__details">
                <strong>{{ store.storeName }}</strong>
                <span>門市代碼：{{ store.storeId }}</span>
                <span>地址：{{ store.storeAddress }}</span>
                <span v-if="store.storePhone">電話：{{ store.storePhone }}</span>
              </div>
              <div class="cvs-store-result__actions">
                <button
                  type="button"
                  class="secondary-button"
                  @click="emit('open-map', store)"
                >
                  查看地圖
                </button>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="isSelecting || isCurrentStore(store)"
                  @click="emit('select-store', store)"
                >
                  {{ isCurrentStore(store) ? "已選擇" : "選擇此門市" }}
                </button>
              </div>
            </li>
          </ul>
        </template>

        <p v-if="selectionError" class="field-error" role="alert">
          {{ selectionError }}
        </p>
      </div>

      <footer class="cvs-picker-modal__footer">
        <button
          type="button"
          class="secondary-button"
          :disabled="isSelecting"
          @click="emit('close')"
        >
          取消
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { AUCTION_CVS_STORE_OPTIONS } from "@/constants/auctionCvs";
import { TAIWAN_CITIES } from "@/constants/auctionFormOptions";

defineProps({
  open: { type: Boolean, required: true },
  selectedStoreType: { type: String, required: true },
  filters: { type: Object, required: true },
  isLoading: { type: Boolean, required: true },
  isSelecting: { type: Boolean, required: true },
  loadError: { type: String, required: true },
  selectionError: { type: String, required: true },
  hasSearchCriteria: { type: Boolean, required: true },
  filteredStores: { type: Array, required: true },
  hasTooManyStores: { type: Boolean, required: true },
  isCurrentStore: { type: Function, required: true },
});

const emit = defineEmits([
  "close",
  "change-store-type",
  "reload",
  "select-store",
  "open-map",
]);
</script>

<style scoped>
.eyebrow { margin: 0 0 6px; color: #6c757d; font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.form-field { min-width: 0; }
.form-field label { display: block; margin-bottom: 7px; color: #343a40; font-size: .85rem; font-weight: 650; }
.form-field input, .form-field select { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; color: #111; }
.form-field input:focus, .form-field select:focus { border-color: #111; outline: 2px solid rgba(17,17,17,.12); }
.field-error { margin: 7px 0 0; color: #b42318; font-size: .78rem; line-height: 1.5; }
.cvs-store-options { display: flex; flex-wrap: wrap; gap: 8px; }
.cvs-store-option { min-height: 38px; padding: 8px 13px; border: 1px solid #ced4da; border-radius: 999px; background: #fff; color: #343a40; font-weight: 650; }
.cvs-store-option--active { border-color: #111; background: #111; color: #fff; }
.cvs-store-option:disabled { cursor: not-allowed; opacity: .6; }
.cvs-store-search { display: grid; grid-template-columns: minmax(150px, .45fr) minmax(0, 1fr); gap: 12px; }
.cvs-store-state, .cvs-store-result-count { margin: 0; color: #6c757d; font-size: .84rem; line-height: 1.5; }
.cvs-store-results { display: grid; gap: 10px; max-height: 430px; margin: 0; padding: 0; overflow-y: auto; list-style: none; }
.cvs-store-results li { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px; border: 1px solid #e9ecef; border-radius: 8px; }
.cvs-store-result__details { display: grid; min-width: 0; gap: 4px; }
.cvs-store-result__actions { display: flex; flex: 0 0 auto; flex-wrap: wrap; gap: 8px; }
.cvs-store-results strong, .cvs-store-results span { overflow-wrap: anywhere; }
.cvs-store-results span { color: #6c757d; font-size: .8rem; line-height: 1.45; }
.cvs-store-results .secondary-button { flex: 0 0 auto; }
.cvs-picker-modal .notice p { margin: 0 0 10px; }
.cvs-picker-modal { position: fixed; z-index: 1100; inset: 0; display: grid; place-items: center; padding: 24px; background: rgba(0, 0, 0, .52); }
.cvs-picker-modal__dialog { display: flex; width: min(760px, 100%); max-height: min(820px, calc(100vh - 48px)); flex-direction: column; overflow: hidden; border-radius: 12px; background: #fff; box-shadow: 0 22px 70px rgba(0, 0, 0, .24); }
.cvs-picker-modal__header { display: flex; align-items: start; justify-content: space-between; gap: 24px; padding: 22px 24px; border-bottom: 1px solid #e9ecef; }
.cvs-picker-modal__header h2 { margin: 0; font-size: 1.25rem; }
.cvs-picker-modal__close { display: grid; width: 38px; height: 38px; padding: 0; place-items: center; border: 0; border-radius: 50%; background: transparent; color: #343a40; font-size: 1.7rem; line-height: 1; }
.cvs-picker-modal__close:hover { background: #f1f3f5; }
.cvs-picker-modal__close:disabled { cursor: not-allowed; opacity: .5; }
.cvs-picker-modal__body { display: grid; min-height: 0; gap: 16px; padding: 22px 24px; overflow-y: auto; }
.cvs-picker-modal__footer { display: flex; justify-content: flex-end; padding: 16px 24px; border-top: 1px solid #e9ecef; }
.notice { padding: 12px 14px; border-radius: 7px; font-size: .88rem; line-height: 1.55; }
.notice--warning { margin-bottom: 18px; background: #fff3cd; color: #765a00; }
.notice--error { background: #fbe9e7; color: #a12b21; }
.secondary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 10px 17px; border: 1px solid #111; border-radius: 6px; background: #fff; color: #111; font-weight: 700; text-decoration: none; }
.secondary-button:disabled { border-color: #dee2e6; background: #f8f9fa; color: #adb5bd; cursor: not-allowed; }

@media (max-width: 575.98px) {
  .cvs-store-search { grid-template-columns: 1fr; }
  .cvs-store-results li { align-items: stretch; flex-direction: column; }
  .cvs-picker-modal { padding: 12px; }
  .cvs-picker-modal__dialog { max-height: calc(100vh - 24px); }
  .cvs-picker-modal__header, .cvs-picker-modal__body { padding: 18px; }
  .cvs-picker-modal__footer { padding: 14px 18px; }
  .secondary-button { width: 100%; }
}
</style>
