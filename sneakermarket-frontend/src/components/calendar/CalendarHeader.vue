頂部標題、年月切換、視圖按鈕
<template>
  <div class="calendar-header">
    <div class="nav-group">
      <button class="icon-btn" @click="$emit('change-month', -1)">&lt;</button>
      <h2>{{ currentYear }} 年 {{ currentMonth + 1 }} 月</h2>
      <button class="icon-btn" @click="$emit('change-month', 1)">&gt;</button>
    </div>

    <div class="view-switch">
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
  currentYear: {
    type: Number,
    required: true,
  },
  currentMonth: {
    type: Number,
    required: true,
  },
  currentView: {
    type: String,
    required: true,
  },
});

defineEmits(["change-month", "update:currentView"]);
</script>

<style scoped>
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #1e293b;
  color: #fff;
}

.nav-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-group h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
}

.view-switch {
  background: rgba(255, 255, 255, 0.1);
  padding: 4px;
  border-radius: 12px;
  display: flex;
}

.view-switch button {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 6px 16px;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
}

.view-switch button.active {
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
}
</style>

