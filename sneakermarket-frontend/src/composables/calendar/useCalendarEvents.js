import { computed, ref, watch } from "vue";
import { eventPage, mapEventData } from "@/service/calendarService";
import { formatDateKey, isEventOnDate } from "@/utils/dateUtils";

export function useCalendarEvents() {
  const currentDate = ref(new Date());
  const selectedDate = ref(new Date());
  const events = ref([]);
  const loading = ref(false);
  const error = ref("");

  const currentYear = computed(() => currentDate.value.getFullYear());
  const currentMonth = computed(() => currentDate.value.getMonth());

  const selectedDayEvents = computed(() => {
    const key = formatDateKey(
      selectedDate.value.getFullYear(),
      selectedDate.value.getMonth(),
      selectedDate.value.getDate(),
    );

    return events.value.filter((event) => isEventOnDate(event, key));
  });

  function changeMonth(delta) {
    // 從每月 1 日計算，避免 31 日切到短月份時跳過一個月。
    currentDate.value = new Date(
      currentYear.value,
      currentMonth.value + delta,
      1,
    );
    selectedDate.value = new Date(currentDate.value);
  }

  function selectDate(date) {
    selectedDate.value = date;
  }

  function goToToday() {
  const today = new Date();
  currentDate.value = new Date(
    today.getFullYear(),
    today.getMonth(),
    1,
  );
  selectedDate.value = today;
}

  let latestRequest = 0;

async function fetchMonthEvents() {
  const requestId = ++latestRequest;
  const year = currentYear.value;
  const month = currentMonth.value + 1;
  const monthText = String(month).padStart(2, "0");
  const lastDay = new Date(year, month, 0).getDate();

  loading.value = true;
  error.value = "";

  try {
    const allRows = [];
    const pageSize = 100;
    let pageNumber = 1;
    let totalPages = 1;

    do {
      const response = await eventPage({
        startAt: `${year}-${monthText}-01T00:00:00`,
        endAt: `${year}-${monthText}-${lastDay}T23:59:59`,
        overlap: true,
        pageNumber,
        pageSize,
      });

      // 使用者切換月份後，忽略舊月份尚未完成的請求。
      if (requestId !== latestRequest) return;

      const payload = response.data?.data ?? response.data;
      const rows = Array.isArray(payload?.content)
        ? payload.content
        : Array.isArray(payload)
          ? payload
          : [];

      allRows.push(...rows);

      totalPages = Number.isInteger(payload?.totalPages)
        ? payload.totalPages
        : rows.length === pageSize
          ? pageNumber + 1
          : pageNumber;

      pageNumber += 1;
    } while (pageNumber <= totalPages);

    events.value = mapEventData(allRows).filter(
      (event) =>
        !["INACTIVE", "DRAFT", "CANCELLED"].includes(event.status),
    );
  } catch (cause) {
    if (requestId !== latestRequest) return;
    events.value = [];
    error.value = "活動載入失敗，請稍後再試。";
    console.error("抓取活動失敗：", cause);
  } finally {
    if (requestId === latestRequest) loading.value = false;
  }
}

  watch([currentYear, currentMonth], fetchMonthEvents, {
    immediate: true,
  });

  return {
    currentYear,
    currentMonth,
    selectedDate,
    selectedDayEvents,
    events,
    loading,
    error,
    changeMonth,
    selectDate,
    fetchMonthEvents,
    goToToday,
  };
}
