<script setup>
import AdvertisementDisplay from "./AdvertisementDisplay.vue";
</script>

<template>
  <h1>SneakerMarket</h1>
  <p>球鞋商城與競標平台</p>
  <main class="demo-home">
    <AdvertisementDisplay />

    <section class="demo-content">
      <div class="demo-grid">
        <article>
          <h2>熱門商品</h2>
          <p>未來放熱門商品清單。</p>
        </article>

        <article>
          <h2>最新上架</h2>
          <p>未來放新品商品清單。</p>
        </article>

        <article>
          <h2>競標活動</h2>
          <p>未來放競標活動清單。</p>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.demo-home {
  padding: 24px;
}

.demo-content {
  max-width: 1200px;
  margin: 32px auto 0;
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.demo-grid article {
  min-height: 150px;
  padding: 24px;
  border: 1px solid #dee2e6;
  border-radius: 12px;
  background: #fff;
}

@media (max-width: 768px) {
  .demo-grid {
    grid-template-columns: 1fr;
  }
}
</style>
