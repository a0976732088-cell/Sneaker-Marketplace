<script setup>
import { VueDatePicker } from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import { zhTW } from "date-fns/locale";
import { ref, watch } from "vue";
import AdvertisementTargetSelector from "./AdvertisementTargetSelector.vue";

const props = defineProps({
  show: {
    type: Boolean,
    required: true,
  },
  form: {
    type: Object,
    required: true,
  },
  isEditing: {
    type: Boolean,
    required: true,
  },
  currentImageRule: {
    type: Object,
    required: true,
  },
  isUploadingImage: {
    type: Boolean,
    required: true,
  },
  minimumStartAt: {
    type: Date,
    default: null,
  },
  isStartAtLocked: {
    type: Boolean,
    required: true,
  },
  toast: {
    type: String,
    required: true,
  },
  toastType: {
    type: String,
    required: true,
  },
});

const emit = defineEmits([
  "close",
  "submit",
  "image-change",
  "placement-change",
]);
const showTargetSelector = ref(false);
const selectedTarget = ref(null);
const CATEGORY_NAMES = {
  1: "籃球鞋",
  2: "慢跑鞋",
  3: "休閒鞋",
  4: "滑板鞋",
};
watch(
  () => props.show,
  (isOpen) => {
    if (isOpen) {
      // 每次開啟建立／編輯抽屜，都清除前一次暫存在元件內的選取名稱。
      selectedTarget.value =
        props.form.targetType === "CATEGORY" && props.form.targetId
          ? {
              id: props.form.targetId,
              name: CATEGORY_NAMES[props.form.targetId],
            }
          : null;
      showTargetSelector.value = false;
    }
  },
);

function handleTargetSelect(target) {
  props.form.targetId = target.id;
  props.form.targetUrl = null;
  selectedTarget.value = target;
  showTargetSelector.value = false;
}

function clearTarget() {
  props.form.targetId = null;
  props.form.targetUrl = "";
  selectedTarget.value = null;
  showTargetSelector.value = false;
}

function handleTargetTypeChange() {
  clearTarget();
}

function getTargetLabel(targetType) {
  const labels = {
    PRODUCT: "商品",
    AUCTION: "競標商品",
    CATEGORY: "商品分類",
  };
  return labels[targetType] || "目標";
}
</script>

<template>
  <div v-if="show" class="drawer-backdrop">
    <form class="create-drawer" @submit.prevent="emit('submit')">
      <header>
        <div>
          <span>
            {{ isEditing ? "EDIT CAMPAIGN" : "NEW CAMPAIGN" }}
          </span>

          <h2>
            {{ isEditing ? "編輯廣告" : "建立新廣告" }}
          </h2>

          <p>
            {{
              isEditing
                ? "修改目前這則廣告的內容與設定。"
                : "填寫廣告內容並上傳圖片，以建立廣告草稿。"
            }}
          </p>
        </div>
        <button type="button" aria-label="關閉" @click="emit('close')">
          ×
        </button>
      </header>
      <div class="drawer-body">
        <label
          >廣告名稱
          <input
            v-model.trim="form.adName"
            required
            placeholder="例如：九月新品上市"
        /></label>
        <div class="form-grid">
          <label
            >廣告類型<select v-model="form.adType" required>
              <option value="NEW_PRODUCT">新品推薦</option>
              <option value="HOT_PRODUCT">熱門商品</option>
              <option value="AUCTION">競標活動</option>
              <!-- <option value="DISCOUNT">折扣優惠</option> -->
            </select></label
          >
          <label
            >顯示版位<select
              v-model="form.adPlacement"
              @change="emit('placement-change')"
            >
              <option value="BANNER">首頁橫幅</option>
              <option value="POPUP">首頁彈窗</option>
              <option value="SMALL_BLOCK">商品頁側欄</option>
              <option value="CATEGORY_CARD">分類卡片</option>
            </select>
          </label>
        </div>
        <div class="image-upload-field">
          <span class="image-upload-label">圖片上傳</span>
          <p class="image-upload-guidance">
            建議：{{ currentImageRule.width }} ×
            {{ currentImageRule.height }} px | 比例{{
              currentImageRule.ratioLabel
            }}
            | 最低{{ currentImageRule.minimumWidth }} ×
            {{ currentImageRule.minimumHeight }} px
          </p>

          <input
            id="advertisement-image-upload"
            class="image-upload-input"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            :disabled="isUploadingImage"
            @change="emit('image-change', $event)"
          />

          <label
            for="advertisement-image-upload"
            class="image-upload-control"
            :class="{
              'is-uploading': isUploadingImage,
              'is-uploaded': form.imageUrl && !isUploadingImage,
            }"
          >
            <span class="image-upload-icon">
              {{ isUploadingImage ? "…" : form.imageUrl ? "✓" : "↑" }}
            </span>

            <span class="image-upload-copy">
              <strong>
                {{
                  isUploadingImage
                    ? "圖片上傳中"
                    : form.imageUrl
                      ? "圖片已上傳，可點擊更換"
                      : "選擇廣告圖片"
                }}
              </strong>
            </span>
          </label>
        </div>
        <div class="form-grid">
          <label>
            導向類型
            <select
              v-model="form.targetType"
              required
              @change="handleTargetTypeChange"
            >
              <option value="PRODUCT">一般商品</option>
              <option value="AUCTION">競標商品</option>
              <option value="CATEGORY">商品分類</option>
              <!-- <option value="COUPON">優惠券</option> -->
              <!-- <option value="URL">外部網址</option> -->
            </select>
          </label>

          <!-- <label v-if="form.targetType === 'URL'">
                導向網址
                <input
                  v-model.trim="form.targetUrl"
                  type="url"
                  maxlength="500"
                  required
                  placeholder="https://example.com/activity"
                />
              </label> -->

          <!-- 這段不是必須的，測試功能所使用正式會需要依賴其他組員API -->
          <!-- <label v-else>
              導向目標編號
              <input
                v-model.number="form.targetId"
                type="number"
                min="1"
                required
                placeholder="請輸入目標 ID"
              />
            </label> -->
        </div>
        <section class="target-picker">
          <div class="target-picker-header">
            <div>
              <span>
                指定{{ getTargetLabel(form.targetType) }}
                {{ form.targetType === "CATEGORY" ? "（必填）" : "（選填）" }}
              </span>

              <small v-if="selectedTarget">
                已選擇：{{ selectedTarget.name }}
              </small>

              <small v-else-if="form.targetType === 'CATEGORY'">
                分類導向必須選擇一個商品分類。
              </small>

              <small v-else>
                未指定時，廣告會前往{{
                  form.targetType === "PRODUCT"
                    ? "一般商城列表"
                    : "競標商城列表"
                }}。
              </small>
            </div>

            <div class="target-picker-actions">
              <button
                v-if="form.targetId"
                type="button"
                class="clear-target-button"
                @click="clearTarget"
              >
                清除
              </button>

              <button
                type="button"
                class="select-target-button"
                @click="showTargetSelector = true"
              >
                選擇{{ getTargetLabel(form.targetType) }}
              </button>
            </div>
          </div>
        </section>
        <label
          >廣告說明<textarea
            v-model="form.adDescription"
            rows="3"
            placeholder="輸入活動簡介..."
          ></textarea>
        </label>
        <div class="form-grid">
          <label>
            開始時間
            <VueDatePicker
              v-model="form.startAt"
              :disabled="isStartAtLocked"
              :min-date="minimumStartAt"
              model-type="yyyy-MM-dd'T'HH:mm"
              :locale="zhTW"
              :teleport="true"
              six-weeks
              :formats="{ input: 'yyyy/MM/dd HH:mm' }"
              :time-config="{ is24: true, timePickerInline: true }"
              placeholder="選擇日期與時間"
            />
          </label>

          <label>
            結束時間
            <VueDatePicker
              v-model="form.endAt"
              model-type="yyyy-MM-dd'T'HH:mm"
              :locale="zhTW"
              :teleport="true"
              :transitions="false"
              six-weeks
              :formats="{ input: 'yyyy/MM/dd HH:mm' }"
              :time-config="{ is24: true, timePickerInline: true }"
              placeholder="選擇日期與時間"
            />
          </label>
        </div>
      </div>

      <Transition name="toast">
        <div v-if="toast" class="form-notice" :class="toastType" role="status">
          {{ toastType === "success" ? "✓" : "X" }} {{ toast }}
        </div>
      </Transition>

      <footer>
        <button type="button" @click="emit('close')">取消</button>
        <button
          class="submit"
          type="submit"
          :disabled="isUploadingImage || !form.imageUrl"
        >
          {{
            isUploadingImage
              ? "圖片上傳中…"
              : isEditing
                ? "儲存修改"
                : "建立草稿"
          }}
        </button>
      </footer>
    </form>
    <AdvertisementTargetSelector
      v-if="showTargetSelector"
      :target-type="form.targetType"
      @select="handleTargetSelect"
      @close="showTargetSelector = false"
    />
  </div>
</template>

<style scoped>
.drawer-backdrop {
  position: fixed;
  z-index: 1090;
  inset: 0;
  display: flex;
  justify-content: flex-end;
  background: #0c121c7a;
  backdrop-filter: blur(2px);
}
.create-drawer {
  display: flex;
  flex-direction: column;
  width: min(460px, 94vw);
  height: 100%;
  background: #fff;
  box-shadow: -20px 0 50px #00000029;
}
.create-drawer header {
  display: flex;
  justify-content: space-between;
  padding: 30px 30px 22px;
  border-bottom: 1px solid var(--line);
}
.create-drawer header span {
  color: var(--coral);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.18em;
}
.create-drawer h2 {
  margin: 6px 0 5px;
  font-size: 25px;
  font-weight: 800;
}
.create-drawer header p {
  margin: 0;
  color: var(--muted);
  font-size: 11px;
}
.create-drawer header button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: #f4f5f7;
  font-size: 21px;
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 25px 30px;
}
.drawer-body label {
  display: block;
  margin-bottom: 17px;
  color: #495567;
  font-size: 10px;
  font-weight: 700;
}
.drawer-body input,
.drawer-body select,
.drawer-body textarea {
  display: block;
  width: 100%;
  margin-top: 7px;
  padding: 11px 12px;
  border: 1px solid #dfe3e9;
  border-radius: 7px;
  outline: none;
  color: var(--ink);
  background: #fff;
  font: inherit;
  font-weight: 500;
}
.drawer-body input:focus,
.drawer-body select:focus,
.drawer-body textarea:focus {
  border-color: var(--coral);
  box-shadow: 0 0 0 3px #ff5c4d1a;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.create-drawer footer {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  padding: 18px 30px;
  border-top: 1px solid var(--line);
}
.create-drawer footer button {
  padding: 10px 17px;
  border: 1px solid var(--line);
  border-radius: 7px;
  color: #596577;
  background: #fff;
  font-size: 12px;
  font-weight: 700;
}
.create-drawer footer .submit {
  border-color: var(--coral);
  color: #fff;
  background: var(--coral);
}
.toast-enter-active,
.toast-leave-active {
  transition: 0.2s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
@keyframes slide-in {
  from {
    transform: translateX(100%);
  }
}
.form-notice {
  margin: 0 30px 12px;
  padding: 10px 12px;
  border: 1px solid;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 700;
}

.form-notice.success {
  border-color: #b9dfd1;
  color: #198754;
  background: #f2fbf7;
}

.form-notice.error {
  border-color: #ffd0ca;
  color: #b42318;
  background: #fff5f3;
}

.image-upload-field {
  margin-bottom: 17px;
}

.image-upload-label {
  display: block;
  color: #495567;
  font-size: 14px;
  font-weight: 700;
}

.create-drawer .image-upload-guidance {
  margin: 5px 0 0;
  color: #6f7886;
  font-size: 12px;
  line-height: 1.55;
}

.drawer-body .image-upload-input {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.image-upload-control {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  margin-top: 7px;
  padding: 12px 14px;
  border: 1px dashed #c9d0da;
  border-radius: 7px;
  background: #fafbfc;
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
  cursor: pointer;
}

.image-upload-control:hover {
  border-color: var(--coral);
  background: #fff7f6;
}

.image-upload-input:focus + .image-upload-control {
  border-color: var(--coral);
  box-shadow: 0 0 0 3px #ff5c4d1a;
}

.image-upload-icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 50%;
  color: var(--coral);
  background: #fff0ee;
  font-size: 20px;
  font-weight: 800;
}

.image-upload-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.image-upload-copy strong {
  color: var(--ink);
  font-size: 13px;
}

.image-upload-control.is-uploading {
  cursor: wait;
  opacity: 0.7;
}

.image-upload-control.is-uploaded {
  border-style: solid;
  border-color: #b9dfd1;
  background: #f2fbf7;
}

.image-upload-control.is-uploaded .image-upload-icon {
  color: #198754;
  background: #dff5e9;
}
.create-drawer header span {
  font-size: 12px;
}

.create-drawer h2 {
  font-size: 29px;
}

.create-drawer header p {
  font-size: 14px;
}

.drawer-body label {
  font-size: 14px;
}
.target-picker {
  margin: 0 0 17px;
  padding: 12px;
  border: 1px solid #e2e6ec;
  border-radius: 8px;
  background: #fafbfc;
}

.target-picker-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.target-picker-header span,
.target-picker-header small {
  display: block;
}

.target-picker-header span {
  color: #495567;
  font-size: 14px;
  font-weight: 700;
}

.target-picker-header small {
  margin-top: 4px;
  color: #748092;
  font-size: 14px;
}

.target-picker-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.select-target-button,
.clear-target-button {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.select-target-button {
  border: 0;
  color: #fff;
  background: #2563eb;
}

.clear-target-button {
  border: 1px solid #cfd6df;
  color: #5f6b7a;
  background: #fff;
}
</style>
