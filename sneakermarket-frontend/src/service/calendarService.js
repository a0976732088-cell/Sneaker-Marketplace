import httpClient from "./httpClient.js";
import { formatTimeStr } from "@/utils/dateUtils";
import { getEventType } from "@/constants/eventTypes";

export const EVENT_TYPE_COLOR_MAP = {
  RELEASE: "#6366f1",
  SALE: "#d97706",
  EVENT: "#059669",
  NOTICE: "#dc2626",
  OTHER: "#4b5563",
};

export const eventPage = (params) => {
  return httpClient.get("/calendar/eventPage", { params });
};

// 取得圖片 URL
export const getEventPhotoUrl = (id, timestamp) => {
  if (!id) return "";
  const baseUrl = httpClient.defaults.baseURL || "http://localhost:8080/api";
  return `${baseUrl}/calendar/event/${id}/photo${timestamp ? `?t=${timestamp}` : ""}`;
};

// 🎯 Data Mapper: 將 API 資料轉為前端格式
export const mapEventData = (rawData = []) => {
  return rawData.map((item) => {
    const datePart = (value) =>
      value ? value.replace(" ", "T").split("T")[0] : "";

    const date = datePart(item.startAt);
    const endDate = datePart(item.endAt);
    const startTime = formatTimeStr(item.startAt);
    const endTime = formatTimeStr(item.endAt);

    const dateTimeRange = endDate
      ? `${date} ${startTime}     -     ${endDate} ${endTime}`
      : `${date} ${startTime}`;

    const endDayPrefix =
      endDate && endDate !== date
        ? `${
            endDate.slice(0, 4) === date.slice(0, 4)
              ? endDate.slice(5)
              : endDate
          }`.replaceAll("-", "/") + " "
        : "";

    return {
      id: item.eventId,
      date,
      dateTimeRange,
      time: endTime ? `${startTime} - ${endDayPrefix}${endTime}` : startTime,
      title: item.title,
      description: item.description,
      venueName: item.venueName || "",
      address: item.address || "",
      latitude:
        item.latitude === null || item.latitude === undefined
          ? null
          : Number(item.latitude),
      longitude:
        item.longitude === null || item.longitude === undefined
          ? null
          : Number(item.longitude),
      startAt: item.startAt,
      endAt: item.endAt,
      eventType: item.eventType || "OTHER",
      color: getEventType(item.eventType).color,
      status: item.status,
      targetUrl: item.targetUrl,
      photoUrl: item.hasPhoto ? getEventPhotoUrl(item.eventId) : "",
    };
  });
};

export const getMyEventFollows = () => {
  return httpClient.get("/calendar/events/follows/me");
};

export const followEvent = (eventId, remindBeforeMinutes = 60) => {
  return httpClient.put(`/calendar/events/${eventId}/follow`, {
    remindBeforeMinutes,
  });
};

export const unfollowEvent = (eventId) => {
  return httpClient.delete(`/calendar/events/${eventId}/follow`);
};

/**
 * 取得目前會員的 Google Calendar 連線狀態。
 */
export const getGoogleCalendarStatus = () => {
  return httpClient.get(
    "/calendar/google/status",
  );
};

/**
 * 取得 Google OAuth 授權入口。
 *
 * OAuth 必須讓整個瀏覽器跳轉，
 * 不能只使用 Axios 發送請求。
 */
export const getGoogleCalendarConnectUrl = () => {
  const baseUrl =
    httpClient.defaults.baseURL ||
    "http://localhost:8080/api";

  return (
    baseUrl.replace(/\/+$/, "") +
    "/calendar/google/connect"
  );
};

/**
 * 解除目前會員的 Google Calendar 連線。
 */
export const disconnectGoogleCalendar = () => {
  return httpClient.delete(
    "/calendar/google/connection",
  );
};
