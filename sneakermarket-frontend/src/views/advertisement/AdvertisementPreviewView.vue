<script setup>
import AdvertisementDisplay from "./AdvertisementDisplay.vue";
</script>

<template>
  <section class="advertisement-preview">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1>檢視廣告</h1>

      <RouterLink class="btn btn-primary" to="/ads"> 返回廣告管理 </RouterLink>
    </div>

    <AdvertisementDisplay />
  </section>
</template>
