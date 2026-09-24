<script setup>
defineProps({
  visible: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(["retry"]);
</script>

<template>
  <aside
    v-if="visible"
    class="adblock-notice"
    role="alert"
    aria-labelledby="adblock-notice-title"
  >
    <div class="adblock-notice__content">
      <h2 id="adblock-notice-title" class="adblock-notice__title">
        無法顯示贊助內容
      </h2>

      <p class="adblock-notice__message">
        我們偵測到廣告內容可能被瀏覽器阻擋。廣告收入有助於平台持續營運，
        若方便，請將本站加入允許清單。
      </p>
    </div>

    <button
      type="button"
      class="adblock-notice__retry"
      @click="emit('retry')"
    >
      我已關閉，重新檢查
    </button>
  </aside>
</template>

<style scoped>
.adblock-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  max-width: 1200px;
  margin: 16px auto;
  padding: 20px 24px;
  border: 1px solid #f0c36d;
  border-radius: 12px;
  background: #fff8e8;
  color: #3d321f;
}

.adblock-notice__content {
  min-width: 0;
}

.adblock-notice__title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
}

.adblock-notice__message {
  margin: 0;
  line-height: 1.6;
}

.adblock-notice__retry {
  flex: 0 0 auto;
  padding: 10px 16px;
  border: 0;
  border-radius: 8px;
  background: #191919;
  color: #fff;
  cursor: pointer;
}

.adblock-notice__retry:hover {
  background: #363636;
}

.adblock-notice__retry:focus-visible {
  outline: 3px solid rgba(25, 25, 25, 0.3);
  outline-offset: 3px;
}

@media (max-width: 720px) {
  .adblock-notice {
    align-items: stretch;
    flex-direction: column;
  }

  .adblock-notice__retry {
    width: 100%;
  }
}
</style>