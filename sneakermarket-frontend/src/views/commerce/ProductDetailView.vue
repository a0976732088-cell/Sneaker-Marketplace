<script setup>
import { useRoute } from 'vue-router'
import { ref, onMounted } from 'vue'
import httpClient from '@/service/httpClient'

const route = useRoute()
const product = ref(null)
const loading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    const response = await httpClient.get(`/products/${route.params.id}`)
    product.value = response.data
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端是否啟動'
  } finally {
    loading.value = false
  }
})
</script>
<template>
  <section>
    <div v-if="loading" class="text-secondary mt-3">載入中...</div>

    <div v-else-if="errorMessage" class="alert alert-danger mt-3">
      {{ errorMessage }}
    </div>

    <div v-else>
      <h1 class="h3">{{ product.productName }}</h1>
      <p class="text-secondary">{{ product.brandName }} · {{ product.status }}</p>
      
      <p v-if="product.description">{{ product.description }}</p>
      <p v-else class="text-secondary fst-italic">尚無商品介紹</p>

      <!-- 圖片的 v-for -->
       <img v-for="(url, index) in product.images" :key="index" :src="url" :alt="product.productName" style="width: 150px; height: 150px; object-fit: cover;">

      <!-- 規格的 v-for -->
       <h2 class="h5 mt-4">規格</h2>
        <ul class="list-group">
        <li v-for="(v, index) in product.variants" :key="index" class="list-group-item">
            {{ v.size }} / {{ v.color }} — NT$ {{ v.price }}（庫存：{{ v.stock }}）
        </li>
</ul>
    </div>
  </section>
</template>