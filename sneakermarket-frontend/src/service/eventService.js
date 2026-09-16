import httpClient from './httpClient.js'

// 1. 取得活動分頁列表
export const getEventPage = (params) => {
  return httpClient.get("/calendar/eventPage", { params });
};

// 2. 新增活動
export const createEvent = (payload) => {
  return httpClient.post("/calendar/eventCreate", payload);
};

// 3. 更新活動
export const updateEvent = (eventId, payload) => {
  return httpClient.put(`/calendar/eventUpdate/${eventId}`, payload);
};

// 4. 刪除活動
export const deleteEvent = (eventId) => {
  return httpClient.delete("/calendar/eventDelete", { params: { eventId } });
};