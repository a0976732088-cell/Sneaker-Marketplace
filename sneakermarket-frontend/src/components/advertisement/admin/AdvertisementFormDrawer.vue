<script setup>
import AdvertisementScheduleFields from "./AdvertisementScheduleFields.vue";
import AdvertisementImageUploadField from "./AdvertisementImageUploadField.vue";
import AdvertisementTargetField from "./AdvertisementTargetField.vue";

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
        <!-- BANNER 和 SMALL_BLOCK 可以指定顯示頁面 -->
        <label
          v-if="
            form.adPlacement === 'BANNER' || form.adPlacement === 'SMALL_BLOCK'
          "
          >廣告顯示頁面

          <select v-model.number="form.sortOrder" required>
            <option :value="0">首頁</option>
            <option :value="1">一般商城</option>
            <option :value="2">競標商城</option>
          </select>
        </label>

        <!-- POPUP 使用 sortOrder 區分顯示對象 -->
        <label v-if="form.adPlacement === 'POPUP'">
          顯示對象

          <select v-model.number="form.sortOrder" required>
            <option :value="0">未登入會員</option>
            <option :value="1">已登入會員</option>
          </select>
        </label>

        <AdvertisementImageUploadField
          :current-image-rule="currentImageRule"
          :image-url="form.imageUrl"
          :is-uploading-image="isUploadingImage"
          @image-change="emit('image-change', $event)"
        />

        <!-- 未登入會員 POPUP：固定前往註冊頁，不需設定導向 -->
        <p
          v-if="form.adPlacement === 'POPUP' && form.sortOrder === 0"
          class="target-help"
        >
          未登入會員固定前往會員註冊頁
        </p>

        <!-- 已登入 POPUP 與其他廣告，維持原本的導向設定 -->
        <AdvertisementTargetField
          v-else
          v-model:target-type="form.targetType"
          v-model:target-id="form.targetId"
          v-model:target-url="form.targetUrl"
        />

        <label
          >廣告說明<textarea
            v-model="form.adDescription"
            rows="3"
            placeholder="輸入活動簡介..."
          ></textarea>
        </label>
        <AdvertisementScheduleFields
          v-model:start-at="form.startAt"
          v-model:end-at="form.endAt"
          :minimum-start-at="minimumStartAt"
          :is-start-at-locked="isStartAtLocked"
        />
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
</style>
