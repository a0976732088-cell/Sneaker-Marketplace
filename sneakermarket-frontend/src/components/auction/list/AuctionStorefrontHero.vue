<template>
  <div class="auction-hero">
    <div class="container py-5">
      <span class="hero-icon" aria-hidden="true">👟</span>

      <div class="hero-content">
        <p class="hero-brand text-uppercase small mb-2 fw-semibold">
          SneakerMarket
        </p>
        <h1 class="display-5 fw-bold mb-2">競標商城</h1>
        <p class="hero-copy mb-4">
          熱門球鞋限時競價，出價競逐你的下一雙收藏
        </p>

        <form
          class="d-flex gap-2 hero-search"
          role="search"
          @submit.prevent="emit('search')"
        >
          <label class="visually-hidden" for="auction-keyword">
            搜尋競標商品
          </label>

          <input
            id="auction-keyword"
            v-model="keywordModel"
            type="text"
            class="form-control"
            placeholder="搜尋商品名稱或型號"
          />

          <button
            type="submit"
            class="btn btn-light px-4 hero-search-btn"
            :disabled="loading"
          >
            {{ loading ? "搜尋中" : "搜尋" }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  modelValue: { type: String, required: true },
  loading: { type: Boolean, required: true },
});

const emit = defineEmits(["update:modelValue", "search"]);

const keywordModel = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});
</script>

<style scoped>
.auction-hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0a0a0a 0%, #232323 100%);
  color: #fff;
}

.auction-hero .container {
  position: relative;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-brand,
.hero-copy {
  color: #dee2e6;
}

.hero-brand {
  font-size: 1rem;
  letter-spacing: 0.15em;
}

.hero-copy {
  font-size: 1rem;
  line-height: 1.5;
}

.hero-icon {
  position: absolute;
  right: -1rem;
  top: 50%;
  transform: translateY(-50%) rotate(-15deg);
  font-size: 14rem;
  line-height: 1;
  opacity: 0.1;
  pointer-events: none;

  font-family:
    "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif;
}

.hero-search {
  width: 100%;
  max-width: 380px;
}

.hero-search input {
  min-width: 0;
  font-size: 1rem;
}

.hero-search-btn {
  flex-shrink: 0;
  white-space: nowrap;
}
</style>
