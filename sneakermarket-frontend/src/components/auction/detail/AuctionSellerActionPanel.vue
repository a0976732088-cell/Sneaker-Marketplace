<template>
  <!-- 賣家競標操作 -->
  <section class="seller-panel">
    <h2>我的競標操作</h2>

    <div v-if="canManage" class="seller-actions">
      <RouterLink
        :to="{
          name: 'auction-edit',
          params: {
            id: auction.auctionId,
          },
        }"
        class="seller-edit-button"
      >
        編輯競標
      </RouterLink>

      <button
        type="button"
        class="seller-cancel-button"
        :disabled="cancelling"
        @click="emit('cancel')"
      >
        {{ cancelling ? "取消中..." : "取消競標" }}
      </button>
    </div>

    <p v-else class="seller-message">
      {{ manageMessage }}
    </p>
  </section>
</template>

<script setup>
defineProps({
  auction: {
    type: Object,
    required: true,
  },
  canManage: {
    type: Boolean,
    required: true,
  },
  manageMessage: {
    type: String,
    default: "",
  },
  cancelling: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(["cancel"]);
</script>

<style scoped>
.seller-panel {
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e9ecef;
}

.seller-panel h2 {
  margin: 0;
  color: #111;
  font-size: 1.15rem;
  font-weight: 800;
}

.seller-actions {
  display: flex;
  gap: 0.65rem;
  margin-top: 0.85rem;
}

.seller-edit-button,
.seller-cancel-button {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 2rem;
  font-size: 0.88rem;
  font-weight: 700;
  text-decoration: none;
  flex: 1;
  padding: 0.65rem 1rem;
}

.seller-edit-button {
  border: 1px solid #111;
  background: #111;
  color: #fff;
}

.seller-edit-button:hover {
  color: #fff;
  background: #2c2c2c;
}

.seller-cancel-button {
  border: 1px solid #ced4da;
  background: #fff;
  color: #495057;
}

.seller-cancel-button:hover:not(:disabled) {
  border-color: #dc3545;
  color: #dc3545;
}

.seller-message {
  margin: 0;
  color: #495057;
  font-size: 0.82rem;
  line-height: 1.6;
  margin-top: 0.65rem;
}

@media (max-width: 479.98px) {
  .seller-actions {
    flex-direction: column;
  }
}
</style>
