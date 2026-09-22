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

const terminalStatuses = new Set([
  "SOLD",
  "UNSOLD",
  "CANCELLED",
  "REMOVED",
]);

const now = ref(Date.now());

let timerId = null;

const remainingTime = computed(() => {
  if (terminalStatuses.has(props.status)) {
    return null;
  }

  const endTime = parseAuctionTime(props.endAt).getTime();

  if (!Number.isFinite(endTime)) {
    return null;
  }

  const totalSeconds = Math.floor(
    (endTime - now.value) / 1000,
  );

  if (totalSeconds <= 0) {
    return null;
  }

  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor(
    (totalSeconds % 86400) / 3600,
  );
  const minutes = Math.floor(
    (totalSeconds % 3600) / 60,
  );

  return {
    days: String(days).padStart(2, "0"),
    hours: String(hours).padStart(2, "0"),
    minutes: String(minutes).padStart(2, "0"),
  };
});

onMounted(() => {
  timerId = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onUnmounted(() => {
  if (timerId !== null) {
    window.clearInterval(timerId);
  }
});
</script>

<template>
  <span
    v-if="remainingTime"
    class="home-countdown"
    aria-label="競標剩餘時間"
  >
    <span class="countdown-unit">
      <b>{{ remainingTime.days }}</b>
      <small>日</small>
    </span>

    <i aria-hidden="true">:</i>

    <span class="countdown-unit">
      <b>{{ remainingTime.hours }}</b>
      <small>時</small>
    </span>

    <i aria-hidden="true">:</i>

    <span class="countdown-unit">
      <b>{{ remainingTime.minutes }}</b>
      <small>分</small>
    </span>
  </span>

  <span v-else class="countdown-ended">
    已結束
  </span>
</template>

<style scoped>
.home-countdown {
  display: inline-flex;
  align-items: flex-start;
  gap: 2px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.countdown-unit {
  display: grid;
  justify-items: center;
  gap: 3px;
}

.countdown-unit b {
  display: grid;
  min-width: 26px;
  height: 22px;
  padding: 0 4px;
  place-items: center;
  border-radius: 4px;
  color: #fff;
  background: #f04b43;
  box-shadow: 0 2px 6px rgb(240 75 67 / 22%);
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.countdown-unit small {
  color: #777;
  font-size: 12px;
  line-height: 1;
}

.home-countdown > i {
  margin-top: 3px;
  color: #f04b43;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.countdown-ended {
  color: #888;
  font-size: 11px;
}
</style>