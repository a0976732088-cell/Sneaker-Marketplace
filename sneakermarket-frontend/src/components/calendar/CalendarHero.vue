<template>
  <section class="calendar-hero">
    <img
      v-if="imageUrl && !imageFailed"
      class="hero-image"
      :src="imageUrl"
      alt="綠色籃球鞋活動主視覺"
      @error="imageFailed = true"
    />

    <div class="hero-shade"></div>

    <div class="hero-content">
      <p class="eyebrow">SNEAKER EVENTS</p>
      <h1>活動探索</h1>
      <p class="description">
        探索全台值得參加的球鞋活動，
      </p>
      <p class="description">
        從展覽、市集到社群聚會，與同好一起讓熱愛持續發生。
      </p>
    </div>
  </section>
</template>

<script setup>
import { ref } from "vue";

defineProps({
  imageUrl: {
    type: String,
    default: "/images/calendar/hero-kobe6.png",
  },
});

const imageFailed = ref(false);
</script>

<style scoped>
.calendar-hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 250px;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(115deg, #f5f4f1 25%, #d6ddd8 100%);
  color: #202326;
}

.hero-image,
.hero-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-image {
  object-fit: cover;
  object-position: center;
}

.hero-shade {
  background: linear-gradient(
    90deg,
    rgb(250 249 247 / 96%) 0%,
    rgb(250 249 247 / 82%) 35%,
    rgb(250 249 247 / 0%) 68%
  );
}

.hero-content {
  position: relative;
  z-index: 1;
  width: min(55%, 650px);
  padding: 34px 48px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #67766d;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

h1 {
  margin: 0 0 12px;
  font-size: clamp(34px, 4vw, 58px);
  font-weight: 800;
  letter-spacing: 0.08em;
}

.description {
  max-width: 530px;
  margin: 0;
  color: #4c5459;
  font-size: 15px;
  line-height: 1.8;
}

@media (max-width: 700px) {
  .calendar-hero {
    min-height: 300px;
    align-items: flex-start;
  }

  .hero-image {
    object-fit: contain;
    object-position: right center;
  }

  .hero-shade {
    background: linear-gradient(
      180deg,
      rgb(250 249 247 / 96%),
      rgb(250 249 247 / 60%) 62%,
      rgb(250 249 247 / 15%)
    );
  }

  .hero-content {
    width: 100%;
    padding: 24px;
  }

  .description {
    font-size: 13px;
  }
}
</style>
