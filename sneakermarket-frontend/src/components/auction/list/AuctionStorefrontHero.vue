<template>
  <div class="container pt-4">
    <section class="auction-hero" aria-labelledby="auction-hero-title">
      <div class="hero-visual" aria-hidden="true">
        <img :src="auctionHeroImage" alt="" />
      </div>

      <div class="hero-inner">
        <div class="hero-content">
          <div class="hero-brand" aria-label="酷客 Kicks">
            <span class="hero-brand-local">酷客</span>
            <span class="hero-brand-kicks">KICKS</span>
            <span class="hero-brand-line" aria-hidden="true"></span>
          </div>
          <p class="hero-kicker">SNEAKER AUCTION</p>

          <h1 id="auction-hero-title" class="hero-title">競標商城</h1>
          <p class="hero-copy">
            熱門球鞋限時競價，出價競逐你的下一雙收藏
          </p>

          <form
            class="hero-search"
            role="search"
            @submit.prevent="emit('search')"
          >
            <label class="visually-hidden" for="auction-keyword">
              搜尋競標商品
            </label>

            <div class="hero-search-field">
              <svg
                class="hero-search-icon"
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <circle cx="11" cy="11" r="6.75" />
                <path d="m16 16 4.25 4.25" />
              </svg>

              <input
                id="auction-keyword"
                v-model="keywordModel"
                type="search"
                autocomplete="off"
                placeholder="搜尋球鞋名稱、型號或品牌"
              />
            </div>

            <button
              type="submit"
              class="hero-search-btn"
              :disabled="loading"
            >
              <span>{{ loading ? "搜尋中" : "搜尋" }}</span>
              <svg
                class="hero-search-arrow"
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <path d="M5 12h13" />
                <path d="m14 7 5 5-5 5" />
              </svg>
            </button>
          </form>

          <ul class="hero-features" aria-label="競標特色">
            <li class="hero-feature-item">
              <span class="hero-feature-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="m14.5 5.5 4 4" />
                  <path d="m4 16 8.5-8.5 4 4L8 20H4v-4Z" />
                  <path d="M13 18h7" />
                  <path d="M11 21h11" />
                </svg>
              </span>
              <span class="hero-feature-text">
                <strong>限時競價</strong>
                <small>競標倒數</small>
              </span>
            </li>

            <li class="hero-feature-item">
              <span class="hero-feature-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M4 12h4l2.25-5 3.5 10L16 12h4" />
                  <path d="M5 5.5a9 9 0 1 1-1 11" />
                </svg>
              </span>
              <span class="hero-feature-text">
                <strong>即時出價</strong>
                <small>動態更新</small>
              </span>
            </li>

            <li class="hero-feature-item">
              <span class="hero-feature-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path d="M3 9h18" />
                  <path d="M7 15h3" />
                </svg>
              </span>
              <span class="hero-feature-text">
                <strong>多元付款</strong>
                <small>LINE Pay・轉帳</small>
              </span>
            </li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";

import auctionHeroImage from "@/assets/images/auction/auction-hero-editorial.png";

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
  --hero-cream: #f6f0e9;
  --hero-beige: #e9ddd0;
  --hero-sand: #d6c2ae;
  --hero-muted: #756b62;
  --hero-ink: #24211e;
  --hero-black: #161514;

  position: relative;
  isolation: isolate;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 2014 / 781;
  border: 1px solid rgba(83, 67, 52, 0.08);
  border-radius: 1.25rem;
  background:
    radial-gradient(circle at 18% 4%, rgba(255, 255, 255, 0.9), transparent 35%),
    linear-gradient(105deg, #f5eee6 0%, #eee3d7 42%, #d7c5b2 100%);
  box-shadow: 0 22px 54px rgba(62, 48, 36, 0.1);
  color: var(--hero-ink);
}

.auction-hero::after {
  position: absolute;
  z-index: 1;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(246, 240, 233, 0.22), transparent 48%),
    radial-gradient(circle at 34% 80%, rgba(255, 255, 255, 0.24), transparent 42%);
  content: "";
  pointer-events: none;
}

.hero-visual {
  position: absolute;
  z-index: 0;
  inset: 0 0 0 auto;
  width: 60%;
  height: 100%;
  overflow: hidden;
}

.hero-visual::before {
  position: absolute;
  z-index: 1;
  inset: 0;
  background: linear-gradient(
    90deg,
    var(--hero-cream) 0%,
    rgba(246, 240, 233, 0.92) 10%,
    rgba(246, 240, 233, 0.42) 27%,
    transparent 48%
  );
  content: "";
  pointer-events: none;
}

.hero-visual img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center right;
  filter: saturate(0.88) contrast(0.98);
}

.hero-inner {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: clamp(2.75rem, 4vw, 4.25rem);
}

.hero-content {
  position: relative;
  width: min(50%, 650px);
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  width: min(100%, 25rem);
  color: var(--hero-ink);
  line-height: 1;
}

.hero-brand-local {
  font-size: 1rem;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-brand-kicks {
  font-size: 0.92rem;
  font-weight: 700;
  letter-spacing: 0.2em;
}

.hero-brand-line {
  flex: 1;
  height: 1px;
  background-color: rgba(36, 33, 30, 0.3);
}

.hero-kicker {
  margin: 0.65rem 0 0;
  color: var(--hero-muted);
  font-size: 0.66rem;
  font-weight: 600;
  letter-spacing: 0.28em;
}

.hero-title {
  margin: 1.1rem 0 0;
  color: var(--hero-ink);
  font-size: clamp(3rem, 4vw, 4.5rem);
  font-weight: 800;
  line-height: 1.02;
  letter-spacing: -0.04em;
}

.hero-copy {
  max-width: 32rem;
  margin: 0.75rem 0 0;
  color: var(--hero-muted);
  font-size: clamp(1rem, 1.15vw, 1.16rem);
  font-weight: 500;
  line-height: 1.7;
}

.hero-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  box-sizing: border-box;
  width: 100%;
  max-width: 600px;
  min-height: 64px;
  margin-top: 1.45rem;
  padding: 0.35rem 0.4rem 0.35rem 1.15rem;
  border: 1px solid rgba(50, 40, 30, 0.08);
  border-radius: 1.05rem;
  background-color: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 35px rgba(67, 51, 38, 0.1);
  backdrop-filter: blur(12px);
  transition:
    border-color 200ms ease,
    box-shadow 200ms ease,
    background-color 200ms ease;
}

.hero-search:focus-within {
  border-color: rgba(121, 91, 63, 0.38);
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 0 0 3px rgba(165, 128, 92, 0.15),
    0 15px 40px rgba(67, 51, 38, 0.14);
}

.hero-search-field {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 0.7rem;
  min-width: 0;
}

.hero-search-icon {
  width: 1.35rem;
  height: 1.35rem;
  flex: 0 0 auto;
  stroke: var(--hero-ink);
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-search-field input {
  min-width: 0;
  flex: 1;
  padding: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--hero-ink);
  font: inherit;
  font-size: 1rem;
  line-height: 1.4;
}

.hero-search-field input::placeholder {
  color: rgba(71, 64, 58, 0.58);
  opacity: 1;
}

.hero-search-field input::-webkit-search-cancel-button {
  cursor: pointer;
}

.hero-search-btn {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  min-width: 135px;
  height: 52px;
  padding: 0 1.25rem;
  border: 0;
  border-radius: 0.82rem;
  background-color: #252321;
  color: #fff;
  font: inherit;
  font-size: 0.98rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 200ms ease,
    box-shadow 200ms ease,
    opacity 200ms ease,
    transform 200ms ease;
}

.hero-search-btn:hover:not(:disabled) {
  background-color: var(--hero-black);
  box-shadow: 0 8px 18px rgba(37, 35, 33, 0.2);
  transform: translateY(-1px);
}

.hero-search-btn:active:not(:disabled) {
  box-shadow: none;
  transform: translateY(0);
}

.hero-search-btn:focus-visible {
  outline: 3px solid rgba(140, 101, 67, 0.34);
  outline-offset: 3px;
}

.hero-search-btn:disabled {
  opacity: 0.58;
  cursor: wait;
}

.hero-search-arrow {
  width: 1.15rem;
  height: 1.15rem;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-features {
  display: flex;
  align-items: flex-start;
  gap: clamp(0.8rem, 1.5vw, 1.5rem);
  max-width: 600px;
  margin: 1.4rem 0 0;
  padding: 0;
  list-style: none;
}

.hero-feature-item {
  display: grid;
  flex: 1 1 0;
  grid-template-columns: 1.8rem minmax(0, 1fr);
  gap: 0.55rem;
  min-width: 0;
  color: var(--hero-ink);
}

.hero-feature-icon {
  display: grid;
  width: 1.8rem;
  height: 1.8rem;
  place-items: center;
}

.hero-feature-icon svg {
  width: 1.25rem;
  height: 1.25rem;
  stroke: currentColor;
  stroke-width: 1.45;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-feature-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 0.12rem;
}

.hero-feature-text strong {
  font-size: 0.78rem;
  font-weight: 750;
  line-height: 1.3;
  white-space: nowrap;
}

.hero-feature-text small {
  overflow: hidden;
  color: var(--hero-muted);
  font-size: 0.66rem;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1199.98px) {
  .auction-hero {
    min-height: 500px;
    aspect-ratio: auto;
  }

  .hero-visual {
    width: 68%;
  }

  .hero-visual::before {
    background: linear-gradient(
      90deg,
      var(--hero-cream) 0%,
      rgba(246, 240, 233, 0.95) 18%,
      rgba(246, 240, 233, 0.68) 40%,
      rgba(246, 240, 233, 0.1) 70%
    );
  }

  .hero-inner {
    min-height: 500px;
    padding: 2.5rem;
  }

  .hero-content {
    width: min(62%, 620px);
  }

  .hero-title {
    font-size: clamp(3rem, 6vw, 4.25rem);
  }
}

@media (max-width: 991.98px) {
  .hero-visual {
    width: 74%;
    opacity: 0.82;
  }

  .hero-content {
    width: min(68%, 590px);
  }

  .hero-feature-text small {
    font-size: 0.62rem;
  }
}

@media (max-width: 767.98px) {
  .auction-hero {
    min-height: auto;
    border-radius: 1rem;
  }

  .auction-hero::after {
    background: linear-gradient(
      180deg,
      rgba(246, 240, 233, 0.3),
      rgba(246, 240, 233, 0.72)
    );
  }

  .hero-visual {
    inset: 0;
    width: 100%;
    opacity: 0.3;
  }

  .hero-visual::before {
    background:
      linear-gradient(90deg, rgba(246, 240, 233, 0.98), rgba(246, 240, 233, 0.62)),
      linear-gradient(180deg, transparent 45%, var(--hero-cream) 100%);
  }

  .hero-visual img {
    object-position: 72% center;
  }

  .hero-inner {
    min-height: 0;
    padding: 2.25rem 1.25rem 1.5rem;
  }

  .hero-content {
    width: 100%;
    max-width: none;
  }

  .hero-brand {
    width: min(100%, 20rem);
  }

  .hero-brand-local {
    font-size: 0.92rem;
  }

  .hero-brand-kicks {
    font-size: 0.82rem;
  }

  .hero-kicker {
    font-size: 0.62rem;
    letter-spacing: 0.22em;
  }

  .hero-title {
    margin-top: 1rem;
    font-size: clamp(2.4rem, 12vw, 3.4rem);
  }

  .hero-copy {
    max-width: 30rem;
    margin-top: 0.6rem;
    font-size: 0.98rem;
    line-height: 1.65;
  }

  .hero-search {
    max-width: none;
    min-height: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 0.4rem;
    margin-top: 1.25rem;
    padding: 0.4rem;
    border-radius: 1rem;
  }

  .hero-search-field {
    width: 100%;
    min-height: 52px;
    padding: 0 0.7rem;
  }

  .hero-search-btn {
    width: 100%;
    min-width: 0;
    height: 50px;
  }

  .hero-features {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem 0.75rem;
    max-width: none;
    margin-top: 1.3rem;
  }

  .hero-feature-item:last-child {
    grid-column: 1 / -1;
  }

  .hero-feature-text strong {
    font-size: 0.76rem;
  }

  .hero-feature-text small {
    font-size: 0.64rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-search,
  .hero-search-btn {
    transition: none;
  }
}
</style>
