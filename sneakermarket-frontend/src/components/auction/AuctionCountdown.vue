<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";

import { parseAuctionTime } from "@/utils/auctionTime";

const props = defineProps({
  endAt: {
    type: String,
    default: "",
  },
  status: {
    type: String,
    default: "",
  },
});

const terminalStatuses = new Set(["SOLD", "UNSOLD", "CANCELLED", "REMOVED"]);
const now = ref(Date.now());
let timerId = null;

const remainingTime = computed(() => {
  if (terminalStatuses.has(props.status)) {
    return "已結束";
  }

  const endTime = parseAuctionTime(props.endAt).getTime();
  if (!Number.isFinite(endTime)) {
    return "-";
  }

  const totalSeconds = Math.floor((endTime - now.value) / 1000);
  if (totalSeconds <= 0) {
    return "已到截止時間";
  }

  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (days > 0) {
    return `${days} 天 ${hours} 小時 ${minutes} 分`;
  }

  return [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, "0"))
    .join(":");
});

onMounted(() => {
  timerId = setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onUnmounted(() => {
  if (timerId !== null) {
    clearInterval(timerId);
  }
});
</script>

<template>
  <span class="auction-countdown">{{ remainingTime }}</span>
</template>

<style scoped>
.auction-countdown {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.015em;
}
</style>
