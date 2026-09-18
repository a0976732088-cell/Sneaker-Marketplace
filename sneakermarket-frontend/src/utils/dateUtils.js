export const WEEK_DAYS = ["日", "一", "二", "三", "四", "五", "六"];

export const MONTH_NAMES = [
  "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
  "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
];

export const formatTimeStr = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

export const getEventMonthName = (dateStr) => {
  if (!dateStr) return "";
  const m = parseInt(dateStr.split("-")[1], 10) - 1;
  return MONTH_NAMES[m] || "";
};

export const getEventDayNumber = (dateStr) => {
  if (!dateStr) return "";
  return parseInt(dateStr.split("-")[2], 10);
};

export const formatDateKey = (year, month, day) => {
  const m = String(month + 1).padStart(2, "0");
  const d = String(day).padStart(2, "0");
  return `${year}-${m}-${d}`;
};

export const isEventOnDate = (event, dateKey) => {
  if (!event.date) return false;

  const endDate = event.endAt
    ? event.endAt.replace(" ", "T").split("T")[0]
    : event.date;

  return event.date <= dateKey && dateKey <= endDate;
};