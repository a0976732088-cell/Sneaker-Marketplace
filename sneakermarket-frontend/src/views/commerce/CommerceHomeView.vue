<script setup>
import { ref, onMounted } from 'vue'
import httpClient from '@/service/httpClient'

const products = ref([])
const loading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    const response = await httpClient.get('/products')
    products.value = response.data
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端是否啟動'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <h1 class="h3">一般商城</h1>

    <div v-if="loading" class="text-secondary mt-3">載入中...</div>

    <div v-else-if="errorMessage" class="alert alert-danger mt-3">
      {{ errorMessage }}
    </div>

    <ul v-else class="list-group mt-3">
      <li v-for="p in products" :key="p.productId" class="list-group-item">
        {{ p.productName }}（{{ p.brandName }} · {{ p.status }}）
      </li>
    </ul>

    <p class="text-secondary mt-3 mb-0">主要負責：商城系統負責人</p>
  </section>
</template>