<template>
  <section class="create-section create-image-section">
    <header class="section-heading">
      <p class="section-eyebrow">STEP 02</p>
      <h2>商品圖片</h2>
      <p>上傳清楚的商品照片，完整呈現球鞋外觀與細節。</p>
    </header>

    <div
      class="image-upload-panel"
      :class="{ 'is-invalid-panel': fieldErrors.images }"
    >
      <label class="create-label" for="auction-images">
        選擇圖片
        <span class="required-mark" aria-hidden="true">*</span>
      </label>

      <input
        id="auction-images"
        :ref="setFieldElement('images')"
        type="file"
        class="create-control create-file-input"
        :class="{ 'is-invalid-field': fieldErrors.images }"
        accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
        multiple
        :aria-invalid="Boolean(fieldErrors.images)"
        :aria-describedby="
          fieldErrors.images
            ? 'auction-image-help auction-images-error'
            : 'auction-image-help'
        "
        @change="emit('change', $event)"
      />

      <p id="auction-image-help" class="upload-help">
        至少 1 張，最多 {{ MAX_AUCTION_IMAGE_COUNT }} 張；支援
        JPG、JPEG、PNG、WEBP。
      </p>

      <p
        v-if="fieldErrors.images"
        id="auction-images-error"
        class="field-error"
        role="alert"
      >
        {{ fieldErrors.images }}
      </p>

      <div v-if="images.length > 0" class="selected-files">
        <p class="selected-files-title">
          已選擇 {{ images.length }} 張圖片
        </p>

        <ul class="file-list">
          <li
            v-for="(image, index) in images"
            :key="`${image.name}-${index}`"
          >
            {{ image.name }}
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
import { MAX_AUCTION_IMAGE_COUNT } from "@/constants/auctionFormOptions";

defineProps({
  images: { type: Array, required: true },
  fieldErrors: { type: Object, required: true },
  setFieldElement: { type: Function, required: true },
});

const emit = defineEmits(["change"]);
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

.create-label {
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

.create-control {
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

.create-control:focus {
  outline: 0;
  border-color: #111;
  box-shadow: 0 0 0 3px rgb(17 17 17 / 10%);
}

.create-control.is-invalid-field {
  border-color: #dc3545;
}

.create-control.is-invalid-field:focus {
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

.image-upload-panel {
  padding: 1.5rem;
  background: #f8f9fa;
  border: 1px dashed #ced4da;
  border-radius: 0.5rem;
}

.image-upload-panel.is-invalid-panel {
  border-color: #dc3545;
}

.create-file-input {
  height: auto;
  padding: 0.35rem;
  background: #fff;
}

.create-file-input::file-selector-button {
  min-height: 38px;
  margin-right: 0.85rem;
  padding: 0.55rem 1rem;
  color: #fff;
  font-weight: 700;
  background: #111;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

.upload-help {
  margin: 0.7rem 0 0;
  color: #6c757d;
  font-size: 0.85rem;
  line-height: 1.6;
}

.selected-files {
  margin-top: 1.4rem;
  padding-top: 1.25rem;
  border-top: 1px solid #dee2e6;
}

.selected-files-title {
  margin: 0 0 0.75rem;
  color: #212529;
  font-size: 0.9rem;
  font-weight: 700;
}

.file-list {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.file-list li {
  position: relative;
  overflow-wrap: anywhere;
  padding-left: 1rem;
  color: #6c757d;
  font-size: 0.88rem;
  line-height: 1.5;
}

.file-list li::before {
  position: absolute;
  top: 0.65em;
  left: 0;
  width: 5px;
  height: 5px;
  content: "";
  background: #111;
  border-radius: 50%;
}

@media (max-width: 767.98px) {
  .create-section {
    padding: 2.5rem 0;
  }
}

@media (max-width: 479.98px) {
  .section-heading {
    margin-bottom: 1.5rem;
  }

  .image-upload-panel {
    padding: 1.25rem 1rem;
  }
}
</style>
