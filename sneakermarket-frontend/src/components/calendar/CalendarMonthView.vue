<!-- 月曆網格 (含當日預覽) -->
 <template>
  <div class="month-view">
    <!-- 星期標頭 -->
    <div class="weekdays">
      <div v-for="day in weekDaysList" :key="day">{{ day }}</div>
    </div>

    <!-- 日期網格 -->
    <div class="days">
      <div
        v-for="b in firstDayOffset"
        :key="'b-' + b"
        class="day empty"
      ></div>
      <div
        v-for="day in daysInMonth"
        :key="day"
        class="day"
        :class="{
          today: isToday(day),
          selected: isSelected(day),
          'has-event': getEventsForDay(day).length > 0,
        }"
        @click="selectDate(day)"
      >
        <span class="day-num">{{ day }}</span>
        <div class="event-dots" v-if="getEventsForDay(day).length">
          <span
            v-for="evt in getEventsForDay(day)"
            :key="evt.id"
            class="dot"
            :style="{ backgroundColor: evt.color }"
          ></span>
        </div>
      </div>
    </div>

    <!-- 點擊特定日期的當日預覽區塊 -->
    <div class="day-events-preview" v-if="selectedDate">
      <h3>{{ selectedDateFormatted }} 的活動</h3>
      <div v-if="selectedDayEvents.length === 0" class="no-events">
        今日無排定活動
      </div>
      <div
        v-for="evt in selectedDayEvents"
        :key="evt.id"
        class="event-card cursor-pointer"
        :style="{ borderLeftColor: evt.color }"
        @click="$emit('open-detail', evt)"
      >
        <div class="event-time">{{ evt.time }}</div>
        <div class="event-title">{{ evt.title }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";

const props = defineProps({
  events: {
    type: Array,
    default: () => [],
  },
  currentYear: {
    type: Number,
    required: true,
  },
  currentMonth: {
    type: Number,
    required: true,
  },
});

defineEmits(["open-detail"]);

const weekDaysList = ["日", "一", "二", "三", "四", "五", "六"];
const selectedDate = ref(new Date());

const firstDayOffset = computed(() =>
  new Date(props.currentYear, props.currentMonth, 1).getDay()
);

const daysInMonth = computed(() =>
  new Date(props.currentYear, props.currentMonth + 1, 0).getDate()
);

const formatDateKey = (year, month, day) => {
  const m = String(month + 1).padStart(2, "0");
  const d = String(day).padStart(2, "0");
  return `${year}-${m}-${d}`;
};

const getEventsForDay = (day) => {
  const dateKey = formatDateKey(props.currentYear, props.currentMonth, day);
  return props.events.filter((e) => e.date === dateKey);
};

const isToday = (day) => {
  const t = new Date();
  return (
    day === t.getDate() &&
    props.currentMonth === t.getMonth() &&
    props.currentYear === t.getFullYear()
  );
};

const isSelected = (day) => {
  if (!selectedDate.value) return false;
  return (
    day === selectedDate.value.getDate() &&
    props.currentMonth === selectedDate.value.getMonth() &&
    props.currentYear === selectedDate.value.getFullYear()
  );
};

const selectDate = (day) => {
  selectedDate.value = new Date(props.currentYear, props.currentMonth, day);
};

const selectedDateFormatted = computed(() => {
  if (!selectedDate.value) return "";
  return `${selectedDate.value.getMonth() + 1} 月 ${selectedDate.value.getDate()} 日`;
});

const selectedDayEvents = computed(() => {
  if (!selectedDate.value) return [];
  return getEventsForDay(selectedDate.value.getDate());
});
</script>

<style scoped>
/* ---------------- 月曆視圖 ---------------- */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  padding: 12px 12px 4px;
}

.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 4px 12px 12px;
  gap: 4px;
}

.day {
  height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  font-size: 14px;
}

.day:hover:not(.empty) {
  background-color: #e2e8f0;
}

.day.today {
  color: #ee5b05;
  font-weight: bold;
}

.day.selected {
  background-color: #6366f1 !important;
  color: #fff !important;
}

.event-dots {
  display: flex;
  gap: 3px;
  margin-top: 2px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.day-events-preview {
  padding: 16px 24px 24px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.event-card {
  background: #f8fafc;
  padding: 10px 14px;
  border-radius: 8px;
  border-left: 30px solid #6366f1;
  margin-bottom: 8px;
}

.cursor-pointer {
  cursor: pointer;
}

.no-events {
  color: #94a3b8;
  font-size: 16px;
  text-align: center;
  padding: 24px 0;
}
</style>