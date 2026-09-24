<script setup>
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

defineProps({
  bids: {
    type: Array,
    required: true,
  },
});
</script>

<template>
  <!-- 出價紀錄 -->
  <section class="bid-history-section" aria-label="出價紀錄">
    <div v-if="bids.length === 0" class="empty-bid-history">
      目前尚無出價紀錄。
    </div>

    <div v-else class="table-responsive">
      <table class="table bid-history-table">
        <thead>
          <tr>
            <th>出價者</th>
            <th>出價金額</th>
            <th>出價時間</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(bid, index) in bids"
            :key="`${bid.createdAt}-${index}`"
          >
            <td>
              {{ bid.bidderName || "會員資料未提供" }}
            </td>
            <td>{{ formatTwd(bid.bidAmount) }}</td>
            <td>{{ formatAuctionTime(bid.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.bid-history-section {
  width: 100%;
}

.empty-bid-history {
  padding: 2rem;
  background: #f8f9fa;
  color: #495057;
  text-align: center;
}

.bid-history-table {
  width: 100%;
  min-width: 620px;
  margin-bottom: 0;
}

.bid-history-table th {
  padding: 0.8rem 0.5rem;
  border-bottom-color: #212529;
  color: #495057;
  font-size: 0.82rem;
  font-weight: 700;
}

.bid-history-table td {
  padding: 1rem 0.5rem;
  color: #343a40;
  font-size: 0.95rem;
  vertical-align: middle;
}

.bid-history-table td:nth-child(2) {
  color: #111;
  font-weight: 700;
}
</style>
