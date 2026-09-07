import httpClient from '../service/httpClient.js'
import { formatTimeStr } from '@/utils/dateUtils'

export const EVENT_TYPE_COLOR_MAP = {
  RELEASE: "#6366f1",
  SALE: "#d97706",
  EVENT: "#059669",
  NOTICE: "#dc2626",
  OTHER: "#4b5563",
};

export const eventPage = (params) => {
  return httpClient.get('/calendar/eventPage', { params });
};

// 取得圖片 URL
export const getEventPhotoUrl = (id, timestamp) => {
  if (!id) return '';
  const baseUrl = httpClient.defaults.baseURL || 'http://localhost:8080/api';
  return `${baseUrl}/calendar/event/${id}/photo${timestamp ? `?t=${timestamp}` : ''}`;
};

// 🎯 Data Mapper: 將 API 資料轉為前端格式
export const mapEventData = (rawData = []) => {
  return rawData.map((item) => {
    const datePart = item.startAt ? item.startAt.split("T")[0] : "";
    const startTime = formatTimeStr(item.startAt);
    const endTime = formatTimeStr(item.endAt);

    return {
      id: item.eventId,
      date: datePart,
      time: endTime ? `${startTime} - ${endTime}` : startTime,
      title: item.title,
      description: item.description,
      color: EVENT_TYPE_COLOR_MAP[item.eventType] || "#6366f1",
      status: item.status,
      eventType: item.eventType || "OTHER",
      targetUrl: item.targetUrl,
    };
  });
};