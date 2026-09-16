<script setup>
defineProps({
  currentImageRule: {
    type: Object,
    required: true,
  },

  imageUrl: {
    type: String,
    default: "",
  },

  isUploadingImage: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(["image-change"]);
</script>

<template>
  <div class="image-upload-field">
    <span class="image-upload-label">
      圖片上傳
    </span>

    <p class="image-upload-guidance">
      建議：{{ currentImageRule.width }} ×
      {{ currentImageRule.height }} px |
      比例 {{ currentImageRule.ratioLabel }} |
      最低 {{ currentImageRule.minimumWidth }} ×
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
        'is-uploaded': imageUrl && !isUploadingImage,
      }"
    >
      <span class="image-upload-icon">
        {{
          isUploadingImage
            ? "…"
            : imageUrl
              ? "✓"
              : "↑"
        }}
      </span>

      <span class="image-upload-copy">
        <strong>
          {{
            isUploadingImage
              ? "圖片上傳中"
              : imageUrl
                ? "圖片已上傳，可點擊更換"
                : "選擇廣告圖片"
          }}
        </strong>
      </span>
    </label>
  </div>
</template>

<style scoped>
.image-upload-field {
  margin-bottom: 17px;
}

.image-upload-label {
  display: block;
  color: #495567;
  font-size: 14px;
  font-weight: 700;
}

.image-upload-guidance {
  margin: 5px 0 0;
  color: #6f7886;
  font-size: 12px;
  line-height: 1.55;
}

.image-upload-input {
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
</style>