<script setup>
import AuctionImageGallery from "@/components/auction/AuctionImageGallery.vue";

defineProps({
  imageUrls: {
    type: Array,
    default: () => [],
  },
  productName: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <div class="card mb-4">
    <div class="card-header">商品圖片</div>

    <div class="card-body">
      <p class="image-section-label">目前刊登圖片</p>
      <div class="edit-image-gallery">
        <AuctionImageGallery
          :image-urls="imageUrls"
          :product-name="productName || '競標商品'"
        />
      </div>
      <p class="image-readonly-note">
        商品圖片會沿用目前刊登內容，儲存本次修改時不會更動圖片。
      </p>
    </div>
  </div>
</template>

<style scoped>
.image-section-label {
  display: block;
  margin-bottom: 0.4rem;
  color: #6c757d;
  font-size: 0.82rem;
  font-weight: 700;
}

.edit-image-gallery {
  max-width: 32rem;
}

.image-readonly-note {
  margin: 0.75rem 0 0;
  color: #6c757d;
  font-size: 0.875rem;
}
</style>
