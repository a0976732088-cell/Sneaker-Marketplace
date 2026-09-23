<script setup>
import AdvertisementShowcase from "@/components/advertisement/AdvertisementShowcase.vue";
</script>

<template>
  <section class="advertisement-preview">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="preview-title">檢視廣告</h1>

      <RouterLink class="btn return-admin-button" to="/admin/advertisements"> 返回廣告管理 </RouterLink>
    </div>

    <AdvertisementShowcase />
  </section>
</template>
<style scoped>
.return-admin-button {
  border: 1px solid #111827;
  border-radius: 4px;
  color: #ffffff;
  background: #111827; /* 配合平台的黑色風格 */
  text-decoration: none;
}

.return-admin-button:hover {
  border-color: #000000;
  color: #ffffff;
  background: #000000;
}
.preview-title {
  margin: 0;
  color: #111827;
  font-size: 38px;
  font-weight: 700;
  letter-spacing: -0.04em;
}

</style>