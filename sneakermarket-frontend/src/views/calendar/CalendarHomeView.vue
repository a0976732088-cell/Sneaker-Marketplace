<template>
  <section>
    <h1 class="h3">行事曆</h1>
    <div class="alert alert-secondary mt-3">
      <strong>BASE 預留頁面</strong><br />
      後續放月／週視圖、事件搜尋、關注與提醒設定。
    </div>
    <p class="text-secondary mb-0">主要負責：行事曆／通知系統負責人</p>
<button type="button" class="btn btn-primary" @click="goToManage">Event-Manage</button>    <br>
    <br>
    
<div class="calendar-widget">
    <!-- 頂部控制列：月份導覽 & 視圖切換按鈕 -->
    <div class="calendar-header">
      <div class="nav-group">
        <button class="icon-btn" @click="changeMonth(-1)">&lt;</button>
        <h2>{{ currentYear }} 年 {{ currentMonth + 1 }} 月</h2>
        <button class="icon-btn" @click="changeMonth(1)">&gt;</button>
      </div>

      <!-- 視圖切換切換器 (Month / List) -->
      <div class="view-switch">
        <button 
          :class="{ active: currentView === 'month' }" 
          @click="currentView = 'month'"
        >
          月曆
        </button>
        <button 
          :class="{ active: currentView === 'list' }" 
          @click="currentView = 'list'"
        >
          列表
        </button>
      </div>
    </div>

    <!-- 1. 月曆檢視 (Month Grid) -->
    <div v-if="currentView === 'month'" class="month-view">
      <div class="weekdays">
        <div v-for="day in weekDaysList" :key="day">{{ day }}</div>
      </div>

      <div class="days">
        <!-- 補空白格 -->
        <div v-for="b in firstDayOffset" :key="'b-'+b" class="day empty"></div>

        <!-- 日期數值與活動點 -->
        <div
          v-for="day in daysInMonth"
          :key="day"
          class="day"
          :class="{ 
            'today': isToday(day),
            'selected': isSelected(day),
            'has-event': getEventsForDay(day).length > 0
          }"
          @click="selectDate(day)"
        >
          <span class="day-num">{{ day }}</span>
          
          <!-- 事件指示點/標籤 -->
          <div class="event-dots" v-if="getEventsForDay(day).length">
            <span 
              v-for="evt in getEventsForDay(day)" 
              :key="evt.id" 
              class="dot"
              :style="{ backgroundColor: evt.color || '#4f46e5' }"
            ></span>
          </div>
        </div>
      </div>

      <!-- 點擊特定日期後顯示當日活動卡片 -->
      <div class="day-events-preview" v-if="selectedDate">
        <h3>{{ selectedDateFormatted }} 的活動</h3>
        <div v-if="selectedDayEvents.length === 0" class="no-events">今日無排定活動</div>
        <div 
          v-for="evt in selectedDayEvents" 
          :key="evt.id" 
          class="event-card"
          :style="{ borderLeftColor: evt.color }"
        >
          <div class="event-time">{{ evt.time }}</div>
          <div class="event-title">{{ evt.title }}</div>
        </div>
      </div>
    </div>

    <!-- 2. 列表檢視 (List View) -->
    <div v-else class="list-view">
      <div v-if="currentMonthEvents.length === 0" class="no-events-large">
        本月尚無任何預定活動
      </div>

      <div 
        v-for="evt in currentMonthEvents" 
        :key="evt.id" 
        class="list-event-item"
      >
        <div class="date-badge" :style="{ backgroundColor: evt.color || '#4f46e5' }">
          <span class="badge-day">{{ getEventDayNumber(evt.date) }}</span>
          <!-- <span class="badge-month">{{ currentMonth + 1 }}月</span> -->
        </div>
        <div class="event-details">
          <div class="event-title">{{ evt.title }}</div>
          <div class="event-time">🕒 {{ evt.time }}</div>
          <div class="event-desc" v-if="evt.description">{{ evt.description }}</div>
        </div>
      </div>
    </div>
  </div>

  </section>
</template>


<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

//跳轉Event管理頁面 
const router = useRouter()

const goToManage = () => {
  router.push('/calendar/eventManage') // 這裡填入你目標頁面的路由路徑 (path) 或名稱 (name)
}

// 預設檢視模式：'month' | 'list'
const currentView = ref('month')
const weekDaysList = ['日', '一', '二', '三', '四', '五', '六']

const currentDate = ref(new Date())
const selectedDate = ref(new Date())

// 模擬活動資料數據 (可改為 API 載入)
const events = ref([
  { id: 1, date: '2026-08-05', time: '10:00 - 11:30', title: '團隊每週開會', color: '#4f46e5', description: '線上 Google Meet 會議' },
  { id: 2, date: '2026-08-15', time: '14:00 - 17:00', title: '產品設計工作坊', color: '#ec4899', description: '討論 UI/UX 升級規範' },
  { id: 3, date: '2026-08-15', time: '18:30', title: '部門聚餐', color: '#10b981' },
  { id: 4, date: '2026-08-31', time: '09:00', title: '新版本 上線發布', color: '#f59e0b' }
])

const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())

const firstDayOffset = computed(() => new Date(currentYear.value, currentMonth.value, 1).getDay())
const daysInMonth = computed(() => new Date(currentYear.value, currentMonth.value + 1, 0).getDate())

// 切換月份
const changeMonth = (delta) => {
  const d = new Date(currentDate.value)
  d.setMonth(d.getMonth() + delta)
  currentDate.value = d
}

// 日期格式輔助函式 YYYY-MM-DD
const formatDateKey = (year, month, day) => {
  const m = String(month + 1).padStart(2, '0')
  const d = String(day).padStart(2, '0')
  return `${year}-${m}-${d}`
}

// 取得特定日期的事件
const getEventsForDay = (day) => {
  const dateKey = formatDateKey(currentYear.value, currentMonth.value, day)
  return events.value.filter(e => e.date === dateKey)
}

// 取得整個當月的事件（供列表檢視按時間排序）
const currentMonthEvents = computed(() => {
  const prefix = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}`
  return events.value
    .filter(e => e.date.startsWith(prefix))
    .sort((a, b) => new Date(a.date) - new Date(b.date))
})

const isToday = (day) => {
  const t = new Date()
  return day === t.getDate() && currentMonth.value === t.getMonth() && currentYear.value === t.getFullYear()
}

const isSelected = (day) => {
  if (!selectedDate.value) return false
  return (
    day === selectedDate.value.getDate() &&
    currentMonth.value === selectedDate.value.getMonth() &&
    currentYear.value === selectedDate.value.getFullYear()
  )
}

const selectDate = (day) => {
  selectedDate.value = new Date(currentYear.value, currentMonth.value, day)
}

const selectedDateFormatted = computed(() => {
  if (!selectedDate.value) return ''
  return `${selectedDate.value.getMonth() + 1} 月 ${selectedDate.value.getDate()} 日`
})

const selectedDayEvents = computed(() => {
  if (!selectedDate.value) return []
  return getEventsForDay(selectedDate.value.getDate())
})

const getEventDayNumber = (dateStr) => {
  return parseInt(dateStr.split('-')[2], 10)
}
</script>

<style scoped>
.calendar-widget {
  background: #ecdfdf;
  width: 100%;
  max-width: 100%;
  border-radius: 20px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  font-family: system-ui, -apple-system, sans-serif;
  margin: 0 auto;
}

/* 頂部導覽列 */
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
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

/* 月份按鈕 */
.icon-btn {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
}

/* 視圖切換開關 */
.view-switch {
  background: rgba(255, 255, 255, 0.1);
  padding: 3px;
  border-radius: 10px;
  display: flex;
}

.view-switch button {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 6px 12px;
  font-size: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.view-switch button.active {
  background: #4f46e5;
  color: #ffffff;
  font-weight: 600;
}

/* 月曆視圖 */
/* 星期一到日 */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: #000000;
  font-size: 20px;
  font-weight: 600;
  padding: 16px 16px 8px;
}

.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 8px 16px 16px;
  gap: 6px;
}

.day {
  aspect-ratio: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  padding: 20px 0;
}

.day:hover:not(.empty) {
  background-color: #f1f5f9;
}

.day.today {
  color: #ee5b05;
  font-weight: bold;
}

.day.selected {
  background-color: #4f46e5 !important;
  color: #fff !important;
}

.event-dots {
  display: flex;
  gap: 3px;
  margin-top: 4px;
  position: static; /* 讓圓點自然排在數字下方，避免壓字 */
}

.dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
}

/* 底部當日預覽 */
.day-events-preview {
  padding: 16px 24px 24px;
  border-top: 1px solid #f1f5f9;
  background: #fafafa;
}

.day-events-preview h3 {
  font-size: 25px;
  color: #032049;
  margin-bottom: 12px;
}

.event-card {
  background: #ffffff;
  padding: 10px 14px;
  border-radius: 8px;
  border-left: 4px solid #4f46e5;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
  margin-bottom: 8px;
}

.event-time {
  font-size: 20px;
  color: #5f7ca5;
}

.event-title {
  font-size: 25px;
  font-weight: 600;
  color: #1e293b;
}

/* 列表視圖 */
.list-view {
  padding: 20px 24px;
  /* max-height: 400px;
  overflow-y: auto; */
}

.list-event-item {
  height: 150px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.date-badge {
  color: #fff;
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.badge-day {
  font-size: 20px;
  font-weight: bold;
  line-height: 1;
}

.badge-month {
  font-size: 20px;
  opacity: 0.9;
}

.event-desc {
  font-size: 20px;
  color: #64748b;
  margin-top: 2px;
}

.no-events, .no-events-large {
  color: #94a3b8;
  font-size: 20px;
  text-align: center;
  padding: 16px 0;
}
</style>