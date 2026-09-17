<script setup>
defineProps({
  currentStep: {
    type: Number,
    required: true
  }
})
</script>

<template>
  <div class="mx-auto mb-5" style="width: 100%; max-width: 360px; position: relative;">
    <div style="position: absolute; top: 16px; left: 0; right: 0; height: 1px; background-color: #dee2e6; z-index: 0;"></div>
    <div class="d-flex justify-content-between" style="position: relative; z-index: 1;">
      <div class="text-center">
        <div
          class="rounded-circle mx-auto d-flex align-items-center justify-content-center"
          :class="currentStep >= 1 ? 'bg-dark text-white' : 'bg-white border text-secondary'"
          style="width:32px;height:32px;"
        >1</div>
        <p class="small mt-1 mb-0">購物車</p>
      </div>
      <div class="text-center">
        <div
          class="rounded-circle mx-auto d-flex align-items-center justify-content-center"
          :class="currentStep >= 2 ? 'bg-dark text-white' : 'bg-white border text-secondary'"
          style="width:32px;height:32px;"
        >2</div>
        <p class="small mt-1 mb-0">填寫資料</p>
      </div>
      <div class="text-center">
        <div
          class="rounded-circle mx-auto d-flex align-items-center justify-content-center"
          :class="currentStep >= 3 ? 'bg-dark text-white' : 'bg-white border text-secondary'"
          style="width:32px;height:32px;"
        >3</div>
        <p class="small mt-1 mb-0">完成訂購</p>
      </div>
    </div>
  </div>
</template>