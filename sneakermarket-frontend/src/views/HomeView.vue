<template>
  <section class="py-4">
    <div class="p-4 p-md-5 bg-body-tertiary rounded-3 border">
      <h1 class="display-6 fw-bold">SneakerMarket</h1>
      <p class="lead mb-4">球鞋電子商城＋競標平台 Vue 前端 BASE。</p>

      <div class="d-flex flex-wrap gap-2">
        <RouterLink class="btn btn-primary" to="/products">進入一般商城</RouterLink>
        <RouterLink class="btn btn-outline-primary" to="/auctions">進入競標商城</RouterLink>
        <RouterLink class="btn btn-outline-secondary" to="/system-check">測試後端連線</RouterLink>
      </div>
    </div>

    <div class="row g-3 mt-2">
      <div v-for="module in modules" :key="module.title" class="col-12 col-md-6 col-xl-4">
        <div class="card h-100 shadow-sm">
          <div class="card-body">
            <h2 class="h5 card-title">{{ module.title }}</h2>
            <p class="card-text text-secondary">{{ module.description }}</p>
            <RouterLink :to="module.to" class="btn btn-sm btn-outline-dark">開啟模組</RouterLink>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
const modules = [
  { title: '會員系統', description: '登入、註冊、會員中心與折價券。', to: '/member' },
  { title: '一般商城', description: '商品、購物車、結帳與一般訂單。', to: '/products' },
  { title: '廣告系統', description: '首頁廣告展示與後台管理。', to: '/ads' },
  { title: '行事曆／通知', description: '事件追蹤、提醒與站內通知。', to: '/calendar' },
  { title: '競標系統', description: '競標商品、出價、結標與競標訂單。', to: '/auctions' },
]
</script>
