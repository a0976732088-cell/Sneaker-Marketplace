<script setup>
import { ref, watch } from "vue";

import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";

const props = defineProps({
  imageUrl: {
    type: String,
    default: "",
  },
  imageAlt: {
    type: String,
    default: "競標商品圖片",
  },
  status: {
    type: String,
    required: true,
  },
  statusTone: {
    type: String,
    default: "neutral",
    validator: (value) =>
      ["neutral", "positive", "warning", "danger"].includes(value),
  },
});

const imageFailed = ref(false);

watch(
  () => props.imageUrl,
  () => {
    imageFailed.value = false;
  },
);
</script>

<template>
  <article class="auction-record-item">
    <header class="record-header">
      <div class="record-header__copy">
        <slot name="header" />
      </div>

      <span class="record-status" :class="`record-status--${statusTone}`">
        {{ status }}
      </span>
    </header>

    <div class="record-body">
      <div class="record-image-wrap">
        <img
          v-if="imageUrl && !imageFailed"
          class="record-image"
          :src="resolveAuctionImageUrl(imageUrl)"
          :alt="imageAlt"
          loading="lazy"
          @error="imageFailed = true"
        />

        <div v-else class="record-image record-image--fallback" role="img" :aria-label="`${imageAlt}暫無圖片`">
          <span>暫無圖片</span>
        </div>
      </div>

      <section class="record-product">
        <slot name="product" />
      </section>

      <section v-if="$slots.default" class="record-content">
        <slot />
      </section>

      <footer v-if="$slots.actions" class="record-actions">
        <slot name="actions" />
      </footer>
    </div>
  </article>
</template>

<style scoped>
.auction-record-item {
  overflow: hidden;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  background: #fff;
  color: #111;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.auction-record-item:hover {
  border-color: #ced4da;
  box-shadow: 0 8px 24px rgba(17, 17, 17, 0.05);
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-width: 0;
  padding: 15px 20px;
  border-bottom: 1px solid #e9ecef;
  background: #f8f9fa;
}

.record-header__copy {
  min-width: 0;
  color: #6c757d;
  font-size: 0.875rem;
}

.record-header :deep(.record-reference) {
  margin: 0;
  overflow-wrap: anywhere;
}

.record-status {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.25;
  white-space: nowrap;
}

.record-status--neutral {
  background: #e9ecef;
  color: #343a40;
}

.record-status--positive {
  background: #e7f4ec;
  color: #24613c;
}

.record-status--warning {
  background: #fff3cd;
  color: #765a00;
}

.record-status--danger {
  background: #fbe9e7;
  color: #a12b21;
}

.record-body {
  display: grid;
  grid-template-columns: 138px minmax(190px, 1.2fr) minmax(220px, 1fr) auto;
  align-items: center;
  gap: 22px;
  min-width: 0;
  padding: 22px;
}

.record-image-wrap {
  width: 138px;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 8px;
  background: #f1f3f5;
}

.record-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.record-image--fallback {
  display: grid;
  place-items: center;
  padding: 12px;
  color: #868e96;
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
}

.record-product,
.record-content {
  min-width: 0;
}

.record-product :deep(.record-product-title) {
  margin: 0;
  color: #111;
  font-size: 1.05rem;
  font-weight: 650;
  line-height: 1.4;
}

.record-product :deep(.record-product-meta) {
  margin: 7px 0 0;
  color: #6c757d;
  font-size: 0.875rem;
  line-height: 1.5;
}

.record-content :deep(.record-metrics) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
  margin: 0;
}

.record-content :deep(.record-metric) {
  min-width: 0;
}

.record-content :deep(.record-metric__label) {
  display: block;
  margin-bottom: 4px;
  color: #6c757d;
  font-size: 0.78rem;
}

.record-content :deep(.record-metric__value) {
  display: block;
  color: #111;
  font-size: 1rem;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.record-content :deep(.record-supporting-text) {
  margin: 15px 0 0;
  color: #6c757d;
  font-size: 0.82rem;
  line-height: 1.5;
}

.record-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  flex-direction: column;
  gap: 9px;
  min-width: 116px;
}

.record-actions :deep(.record-action) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 8px 13px;
  border: 1px solid #adb5bd;
  border-radius: 6px;
  background: #fff;
  color: #111;
  font-size: 0.86rem;
  font-weight: 650;
  line-height: 1.2;
  text-decoration: none;
  white-space: nowrap;
}

.record-actions :deep(.record-action:hover),
.record-actions :deep(.record-action:focus-visible) {
  border-color: #111;
}

.record-actions :deep(.record-action--primary) {
  border-color: #111;
  background: #111;
  color: #fff;
}

.record-actions :deep(.record-action--primary:hover),
.record-actions :deep(.record-action--primary:focus-visible) {
  background: #343a40;
}

.record-actions :deep(.record-action-notice) {
  max-width: 180px;
  margin: 0;
  color: #6c757d;
  font-size: 0.78rem;
  line-height: 1.45;
  text-align: right;
}

@media (max-width: 992px) {
  .record-body {
    grid-template-columns: 122px minmax(0, 1fr);
    align-items: start;
  }

  .record-image-wrap {
    width: 122px;
  }

  .record-content,
  .record-actions {
    grid-column: 1 / -1;
  }

  .record-actions {
    align-items: center;
    justify-content: flex-end;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .record-actions :deep(.record-action-notice) {
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 576px) {
  .record-header {
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
  }

  .record-body {
    grid-template-columns: 92px minmax(0, 1fr);
    gap: 16px;
    padding: 16px;
  }

  .record-image-wrap {
    width: 92px;
  }

  .record-content :deep(.record-metrics) {
    grid-template-columns: 1fr;
    gap: 13px;
  }

  .record-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .record-actions :deep(.record-action) {
    width: 100%;
  }
}
</style>
