<template>
  <div class="calendar-home">
    <CalendarHero />

    <div class="page-controls">
      <div class="list-controls">
        <CalendarViewTabs v-model="currentView" />

        <div
          v-if="currentView === 'list' || currentView === 'map'"
          class="month-controls"
        >
          <span>{{ currentYear }} 年 {{ currentMonth + 1 }} 月</span>
          <button type="button" aria-label="上一個月" @click="changeMonth(-1)">
            ‹
          </button>
          <button type="button" aria-label="下一個月" @click="changeMonth(1)">
            ›
          </button>
        </div>
      </div>
    </div>

    <p v-if="error" class="status-message error">{{ error }}</p>
    <p v-if="followError" class="status-message error">{{ followError }}</p>
    <p v-if="loading" class="status-message">正在載入活動…</p>
    <p v-if="calendarError" class="status-message error">
      會員行事曆：{{ calendarError }}
    </p>
    <p v-if="calendarLoading" class="status-message">正在載入會員行事曆…</p>

    <div
      class="content-grid"
      :class="{
        'single-column': currentView === 'month' || currentView === 'map',
      }"
    >
      <main class="main-content">
        <CalendarListView
          v-if="currentView === 'list'"
          v-show="!loading"
          :events="events"
          :current-year="currentYear"
          :current-month="currentMonth"
          :followed-ids="followedIds"
          :follow-loading-ids="followLoadingIds"
          @open-detail="openDetail"
          @toggle-follow="toggleFollow"
        />

        <CalendarMapView
          v-else-if="currentView === 'map'"
          v-show="!loading"
          :events="events"
          @open-detail="openDetail"
        />
        <MemberMonthView
          v-else-if="followStatus === 'ready'"
          v-show="!calendarLoading"
          :events="followedCalendarEvents"
          :selected-day-events="followedSelectedDayEvents"
          :current-year="calendarYear"
          :current-month="calendarMonth"
          :selected-date="calendarSelectedDate"
          :follow-loading-ids="followLoadingIds"
          @change-month="changeCalendarMonth"
          @select-date="selectCalendarDate"
          @go-today="goCalendarToday"
          @open-detail="openDetail"
          @toggle-follow="toggleFollow"
        />

        <div v-else-if="followStatus === 'guest'" class="member-login-prompt">
          <h2>會員行事曆</h2>
          <p>登入後，這裡會顯示你關注的活動。</p>
          <RouterLink to="/login">登入會員</RouterLink>
        </div>
      </main>

      <aside v-if="currentView === 'list'" class="member-calendar-column">
        <MemberCalendarPanel
          v-if="followStatus === 'ready'"
          v-show="!calendarLoading"
          :events="followedCalendarEvents"
          :selected-day-events="followedSelectedDayEvents"
          :current-year="calendarYear"
          :current-month="calendarMonth"
          :selected-date="calendarSelectedDate"
          :followed-ids="followedIds"
          :follow-loading-ids="followLoadingIds"
          @change-month="changeCalendarMonth"
          @select-date="selectCalendarDate"
          @open-detail="openDetail"
          @toggle-follow="toggleFollow"
        />

        <div v-if="followStatus === 'guest'" class="member-login-prompt">
          <h2>會員行事曆</h2>
          <p>登入後，這裡會顯示你關注的活動。</p>
          <RouterLink to="/login">登入會員</RouterLink>
        </div>

        <GoogleCalendarConnectionCard />
      </aside>
    </div>

    <CalendarEventDetailModal
      v-if="selectedModalEvent"
      :event="selectedModalEvent"
      @close="selectedModalEvent = null"
    />
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import CalendarHero from "@/components/calendar/CalendarHero.vue";
import CalendarViewTabs from "@/components/calendar/CalendarViewTabs.vue";
import CalendarListView from "@/components/calendar/CalendarListView.vue";
import MemberCalendarPanel from "@/components/calendar/MemberCalendarPanel.vue";
import GoogleCalendarConnectionCard from "@/components/calendar/GoogleCalendarConnectionCard.vue";
import CalendarEventDetailModal from "@/components/calendar/CalendarEventDetailModal.vue";
import { useCalendarEvents } from "@/composables/calendar/useCalendarEvents";
import { useEventFollows } from "@/composables/calendar/useEventFollows";
import MemberMonthView from "@/components/calendar/MemberMonthView.vue";
import CalendarMapView from "@/components/calendar/CalendarMapView.vue";

const currentView = ref("list");
const selectedModalEvent = ref(null);

const {
  currentYear,
  currentMonth,
  events,
  loading,
  error,
  changeMonth,
} = useCalendarEvents();

const {
  currentYear: calendarYear,
  currentMonth: calendarMonth,
  selectedDate: calendarSelectedDate,
  selectedDayEvents: calendarSelectedDayEvents,
  events: calendarEvents,
  loading: calendarLoading,
  error: calendarError,
  changeMonth: changeCalendarMonth,
  selectDate: selectCalendarDate,
  goToToday: goCalendarToday,
} = useCalendarEvents();

const {
  followedIds,
  followLoadingIds,
  followError,
  followStatus,
  toggleFollow,
} = useEventFollows();

const followedCalendarEvents = computed(() =>
  calendarEvents.value.filter((event) => followedIds.value.has(event.id)),
);

const followedSelectedDayEvents = computed(() =>
  calendarSelectedDayEvents.value.filter((event) =>
    followedIds.value.has(event.id),
  ),
);

function openDetail(event) {
  selectedModalEvent.value = event;
}
</script>

<style scoped>
.calendar-home {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-controls {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 18px;
}

.list-controls {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-bottom: 1px solid #e4e6e7;
}

.list-controls :deep(.view-tabs) {
  min-width: 0;
  gap: 8px;
  border-bottom: 0;
}

.list-controls :deep(.view-tabs button) {
  padding: 10px 4px 8px;
  font-size: 12px;
}

.month-controls {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  color: #202326;
  font-size: 15px;
  font-weight: 700;
}

.month-controls button {
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 8px;
  background: #eef1f0;
  color: #30373b;
  font-size: 22px;
  cursor: pointer;
  display: grid;
  place-items: center;
  padding: 0;
  line-height: 1;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  align-items: start;
  gap: 18px;
}

.content-grid.single-column {
  grid-template-columns: minmax(0, 1fr);
}

.main-content {
  min-width: 0;
}

.member-calendar-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 18px;
}

.status-message {
  margin: 0;
  padding: 12px 16px;
  border-radius: 10px;
  background: #eef1f0;
  color: #4c5459;
}

.status-message.error {
  background: #f8e9e5;
  color: #8c3b32;
}

@media (max-width: 1100px) {
  .page-controls,
  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
