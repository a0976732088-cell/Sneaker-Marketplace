<template>
  <!-- 出價操作 -->
  <section class="bid-panel" aria-labelledby="bid-panel-title">
    <div class="section-title-row">
      <div>
        <p>限時競價</p>
        <h2 id="bid-panel-title">準備出價</h2>
      </div>
      <span>最低 {{ formatTwd(auction.nextMinimumBid) }}</span>
    </div>

    <div v-if="auction.isOwner" class="owner-bid-notice">
      賣家不可對自己的競標出價。
    </div>

    <form v-else @submit.prevent="$emit('submit')">
      <div class="bid-stepper">
        <button
          type="button"
          :disabled="
            !canPlaceBid || submitting || candidateAtMinimum
          "
          aria-label="減少一個最低加價級距"
          @click="$emit('decrease')"
        >
          −
        </button>

        <strong aria-live="polite">
          {{ formatTwd(candidateBid) }}
        </strong>

        <button
          type="button"
          :disabled="!canPlaceBid || submitting"
          aria-label="增加一個最低加價級距"
          @click="$emit('increase')"
        >
          +
        </button>
      </div>

      <button
        type="submit"
        class="place-bid-button"
        :disabled="!canPlaceBid || submitting"
      >
        {{
          submitting
            ? "出價中..."
            : `確認出價 ${formatTwd(candidateBid)}`
        }}
      </button>

      <p v-if="!canPlaceBid" class="bid-availability-message">
        {{ availabilityMessage }}
      </p>

      <div v-if="successMessage" class="alert alert-success mt-3 mb-0">
        {{ successMessage }}
      </div>

      <div v-if="errorMessage" class="alert alert-danger mt-3 mb-0">
        {{ errorMessage }}
      </div>
    </form>
  </section>
</template>

<script setup>
import { formatTwd } from "@/utils/auctionMoney";

defineProps({
  auction: { type: Object, required: true },
  candidateBid: { type: Number, required: true },
  submitting: { type: Boolean, required: true },
  canPlaceBid: { type: Boolean, required: true },
  availabilityMessage: { type: String, required: true },
  candidateAtMinimum: { type: Boolean, required: true },
  successMessage: { type: String, required: true },
  errorMessage: { type: String, required: true },
});

defineEmits(["increase", "decrease", "submit"]);
</script>

<style scoped>
.bid-panel {
  margin-top: 1.5rem;
  padding: 1.25rem;
  background: #f8f9fa;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.section-title-row p {
  margin: 0 0 0.25rem;
  color: #495057;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.66rem;
  font-weight: 700;
}

.section-title-row h2 {
  margin: 0;
  color: #111;
  font-size: 1.15rem;
  font-weight: 800;
}

.section-title-row > span {
  color: #495057;
  font-size: 0.75rem;
  white-space: nowrap;
}

.owner-bid-notice,
.bid-availability-message {
  margin: 0;
  color: #495057;
  font-size: 0.82rem;
  line-height: 1.6;
}

.bid-stepper {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.9rem;
}

.bid-stepper button {
  width: 44px;
  height: 44px;
  border: 1px solid #ced4da;
  border-radius: 50%;
  background: #fff;
  color: #111;
  font-size: 1.25rem;
  line-height: 1;
}

.bid-stepper button:hover:not(:disabled) {
  border-color: #111;
}

.bid-stepper button:disabled {
  background: #e9ecef;
  color: #495057;
  cursor: not-allowed;
}

.bid-stepper strong {
  min-width: 0;
  color: #111;
  font-size: clamp(1.35rem, 3vw, 1.8rem);
  text-align: center;
  white-space: nowrap;
}

.place-bid-button {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 2rem;
  font-size: 0.88rem;
  font-weight: 700;
  text-decoration: none;
}

.place-bid-button {
  width: 100%;
  padding: 0.7rem 1.5rem;
  border: 1px solid #111;
  background: #111;
  color: #fff;
}

.place-bid-button:hover:not(:disabled) {
  background: #2c2c2c;
}

.place-bid-button:disabled {
  border-color: #ced4da;
  background: #e9ecef;
  color: #495057;
  cursor: not-allowed;
}

.bid-availability-message {
  margin-top: 0.65rem;
  text-align: center;
}

@media (max-width: 479.98px) {
  .bid-panel {
    margin-right: -0.25rem;
    margin-left: -0.25rem;
    padding: 1rem;
  }

  .bid-stepper {
    grid-template-columns: 40px minmax(0, 1fr) 40px;
  }

  .bid-stepper button {
    width: 40px;
    height: 40px;
  }

  .bid-stepper strong {
    font-size: 1.25rem;
  }
}
</style>