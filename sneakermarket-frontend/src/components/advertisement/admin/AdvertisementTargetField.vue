<script setup>
import { ref } from "vue";
import AdvertisementTargetSelector from "./AdvertisementTargetSelector.vue";

const props = defineProps({
  targetType: {
    type: String,
    required: true,
  },

  targetId: {
    type: [Number, String],
    default: null,
  },

  targetUrl: {
    default: "",
  },
});

const emit = defineEmits([
  "update:targetType",
  "update:targetId",
  "update:targetUrl",
]);

const showTargetSelector = ref(false);

const CATEGORY_NAMES = {
  1: "籃球鞋",
  2: "慢跑鞋",
  3: "休閒鞋",
  4: "滑板鞋",
};

const selectedTarget = ref(
  props.targetType === "CATEGORY" && props.targetId
    ? {
        id: props.targetId,
        name: CATEGORY_NAMES[props.targetId],
      }
    : null,
);

function handleTargetSelect(target) {
  emit("update:targetId", target.id);
  emit("update:targetUrl", null);

  selectedTarget.value = target;
  showTargetSelector.value = false;
}

function clearTarget() {
  emit("update:targetId", null);
  emit("update:targetUrl", "");

  selectedTarget.value = null;
  showTargetSelector.value = false;
}

function handleTargetTypeChange(event) {
  emit("update:targetType", event.target.value);
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
  <div class="target-field">
    <div class="form-grid">
      <label class="target-type-label">
        導向類型

        <select
          class="target-type-select"
          :value="targetType"
          required
          @change="handleTargetTypeChange"
        >
          <option value="PRODUCT">一般商品</option>
          <option value="AUCTION">競標商品</option>
          <option value="CATEGORY">商品分類</option>
        </select>
      </label>
    </div>

    <section class="target-picker">
      <div class="target-picker-header">
        <div>
          <span>
            指定{{ getTargetLabel(targetType) }}
            {{ targetType === "CATEGORY" ? "（必填）" : "（選填）" }}
          </span>

          <small v-if="selectedTarget">
            已選擇：{{ selectedTarget.name }}
          </small>

          <small v-else-if="targetType === 'CATEGORY'">
            分類導向必須選擇一個商品分類。
          </small>

          <small v-else>
            未指定時，廣告會前往{{
              targetType === "PRODUCT"
                ? "一般商城列表"
                : "競標商城列表"
            }}。
          </small>
        </div>

        <div class="target-picker-actions">
          <button
            v-if="targetId"
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
            選擇{{ getTargetLabel(targetType) }}
          </button>
        </div>
      </div>
    </section>

    <AdvertisementTargetSelector
      v-if="showTargetSelector"
      :target-type="targetType"
      @select="handleTargetSelect"
      @close="showTargetSelector = false"
    />
  </div>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.target-type-label {
  display: block;
  margin-bottom: 17px;
  color: #495567;
  font-size: 14px;
  font-weight: 700;
}

.target-type-select {
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

.target-type-select:focus {
  border-color: var(--coral);
  box-shadow: 0 0 0 3px #ff5c4d1a;
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

@media (max-width: 520px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .target-picker-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>