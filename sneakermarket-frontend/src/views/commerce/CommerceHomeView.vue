<script setup>
import { ref, onMounted } from 'vue'
import httpClient from '@/service/httpClient'

const products = ref([])
const loading = ref(true)
const errorMessage = ref('')
const keyword = ref('')
const categoryId = ref(null)
const size = ref(null)

async function fetchProducts() {
  try {
    const response = await httpClient.get('/products', {
      params: { keyword: keyword.value, categoryId: categoryId.value, size: size.value }
    })
    products.value = response.data
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端是否啟動'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <section>
    <h1 class="h3">一般商城</h1>
    <input v-model="keyword" placeholder="搜尋商品名稱">
    <select v-model="categoryId">
      <option :value="null">全部分類</option>
      <option :value="1">籃球鞋</option>
      <option :value="2">慢跑鞋</option>
      <option :value="3">休閒鞋</option>
      <option :value="4">滑板鞋</option>
    </select>
    <select v-model="size">
      <option :value="null">全部尺寸</option>
      <option value="US 8">US 8</option>
      <option value="US 8.5">US 8.5</option>
      <option value="US 9">US 9</option>
      <option value="US 9.5">US 9.5</option>
      <option value="US 10">US 10</option>
      <option value="US 10.5">US 10.5</option>
      <option value="US 11">US 11</option>
    </select>
    <button @click="fetchProducts">搜尋</button>
    <div v-if="loading" class="text-secondary mt-3">載入中...</div>

    <div v-else-if="errorMessage" class="alert alert-danger mt-3">
      {{ errorMessage }}
    </div>

    <ul v-else class="list-group mt-3">
      <li v-for="p in products" :key="p.productId" class="list-group-item d-flex align-items-center gap-3">
        <router-link :to="`/products/${p.productId}`" class="d-flex align-items-center gap-3 text-decoration-none text-dark w-100">
          <img v-if="p.imageUrl" :src="p.imageUrl" :alt="p.productName" style="width: 60px; height: 60px; object-fit: cover;">
          {{ p.productName }}（{{ p.brandName }} · {{ p.status }} · NT$ {{ p.price }}）
        </router-link>
      </li>
    </ul>

    <p class="text-secondary mt-3 mb-0">主要負責：商城系統負責人</p>
  </section>
</template>