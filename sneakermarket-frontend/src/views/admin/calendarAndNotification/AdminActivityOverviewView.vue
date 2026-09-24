<template>
  <section class="operation-page">
    <header class="overview-header">
      <div>
        <h1>活動營運中心</h1>
        <p>掌握活動、會員關注與通知發送狀態。</p>
      </div>

      <div class="header-actions">
        <div ref="rangeMenuRef" class="range-control">
          <button
            type="button"
            class="range-select"
            aria-label="選擇統計區間"
            aria-haspopup="menu"
            :aria-expanded="rangeMenuOpen"
            @click.stop="rangeMenuOpen = !rangeMenuOpen"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 3v3M17 3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" />
            </svg>

            <span>近 {{ selectedRange }} 天</span>
            <span class="control-chevron" :class="{ open: rangeMenuOpen }">▾</span>
          </button>

          <Transition name="range-menu">
            <div v-if="rangeMenuOpen" class="range-menu" role="menu">
              <button
                v-for="option in rangeOptions"
                :key="option.value"
                type="button"
                class="range-menu-item"
                :class="{ active: selectedRange === option.value }"
                role="menuitemradio"
                :aria-checked="selectedRange === option.value"
                @click="selectRange(option.value)"
              >
                <span>{{ option.label }}</span>
                <span v-if="selectedRange === option.value" class="range-check">✓</span>
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <NotificationCalendarDashboard
      :range-days="selectedRange"
      @navigate="navigate"
    />
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import NotificationCalendarDashboard from "@/components/dashboard/NotificationCalendarDashboard.vue";

const router = useRouter();
const selectedRange = ref(30);
const rangeMenuOpen = ref(false);
const rangeMenuRef = ref(null);
const rangeOptions = [
  { label: "近 7 天", value: 7 },
  { label: "近 14 天", value: 14 },
  { label: "近 30 天", value: 30 },
];

const selectRange = (value) => {
  selectedRange.value = value;
  rangeMenuOpen.value = false;
};

const closeRangeMenuOnOutsideClick = (event) => {
  if (!rangeMenuRef.value?.contains(event.target)) {
    rangeMenuOpen.value = false;
  }
};

const closeRangeMenuOnEscape = (event) => {
  if (event.key === "Escape") {
    rangeMenuOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener("click", closeRangeMenuOnOutsideClick);
  document.addEventListener("keydown", closeRangeMenuOnEscape);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", closeRangeMenuOnOutsideClick);
  document.removeEventListener("keydown", closeRangeMenuOnEscape);
});

const navigate = (target) => {
  const routes = {
    search: "admin-events",
    NotificationManager: "admin-notifications",
    NotificationLog: "admin-notification-logs",
  };

  if (routes[target]) {
    router.push({ name: routes[target] });
  }
};
</script>

<style scoped>
.operation-page {
  width: 100%;
  min-width: 0;
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.overview-header h1 {
  margin: 0;
  color: #171916;
  font-size: clamp(32px, 3vw, 44px);
  font-weight: 850;
  letter-spacing: -0.055em;
  line-height: 1.05;
}

.overview-header p {
  margin: 7px 0 0;
  color: #747871;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-control {
  position: relative;
}

.range-select {
  display: inline-flex;
  min-width: 140px;
  height: 48px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border: 1px solid #d7dce1;
  border-radius: 7px;
  color: #202321;
  background: #ffffff;
  font-size: 15px;
  font-weight: 650;
  box-shadow: 0 1px 2px rgb(23 27 25 / 3%);
  cursor: pointer;
}

.range-select svg {
  width: 20px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.control-chevron {
  font-size: 10px;
  transition: transform 160ms ease;
}

.control-chevron.open {
  transform: rotate(180deg);
}

.range-menu {
  position: absolute;
  z-index: 30;
  top: calc(100% + 8px);
  right: 0;
  width: 100%;
  min-width: 150px;
  overflow: hidden;
  padding: 5px;
  border: 1px solid #dedfdb;
  border-radius: 7px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgb(20 24 22 / 14%);
}

.range-menu-item {
  display: flex;
  width: 100%;
  height: 38px;
  align-items: center;
  justify-content: space-between;
  padding: 0 11px;
  border: 0;
  border-radius: 5px;
  color: #343934;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.range-menu-item:hover {
  background: #f3f4f1;
}

.range-menu-item.active {
  color: #182019;
  background: #edf2ed;
  font-weight: 750;
}

.range-check {
  color: #5f8064;
  font-size: 13px;
}

.range-menu-enter-active,
.range-menu-leave-active {
  transition: opacity 140ms ease, transform 140ms ease;
  transform-origin: top right;
}

.range-menu-enter-from,
.range-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

@media (max-width: 640px) {
  .overview-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .range-control,
  .range-select {
    width: 100%;
  }

  .range-menu {
    min-width: 100%;
  }
}
</style>
