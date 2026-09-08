<!-- components/calendar/CalendarHeader.vue -->
<template>
  <div class="calendar-header-container">
    <div class="date-selector">
      <span class="month-title">{{ currentYear }} 年 {{ currentMonth + 1 }} 月</span>
      <div class="btn-group ms-2">
        <button class="nav-arrow-btn" @click="$emit('change-month', -1)">‹</button>
        <button class="nav-arrow-btn" @click="$emit('change-month', 1)">›</button>
      </div>
    </div>

    <div class="view-pill-toggle">
      <button
        :class="{ active: currentView === 'month' }"
        @click="$emit('update:currentView', 'month')"
      >
        月曆
      </button>
      <button
        :class="{ active: currentView === 'list' }"
        @click="$emit('update:currentView', 'list')"
      >
        列表
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  currentView: { type: String, required: true },
});

defineEmits(["change-month", "update:currentView"]);
</script>

<style scoped>
.calendar-header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.date-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.nav-arrow-btn {
  background: #f1f5f9;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #475569;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
}
.nav-arrow-btn:hover { background: #e2e8f0; }

.view-pill-toggle {
  background: #f1f5f9;
  padding: 3px;
  border-radius: 10px;
  display: flex;
  gap: 2px;
}

.view-pill-toggle button {
  background: transparent;
  border: none;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-pill-toggle button.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}
</style>