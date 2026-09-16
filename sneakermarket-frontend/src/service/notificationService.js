import httpClient from "./httpClient";

export const createNotification = (payload) => {
  return httpClient.post(
    "/notification/createNotice",
    payload,
  );
};

export const updateNotification = (
  notificationId,
  payload,
) => {
  return httpClient.put(
    `/notification/NoticeUpdate/${notificationId}`,
    payload,
  );
};

export const deleteNotification = (notificationId) => {
  return httpClient.delete(
    "/notification/notificationDelete",
    {
      params: { notificationId },
    },
  );
};

export const getNotificationPage = (params) => {
  return httpClient.get(
    "/notification/notificationPage",
    { params },
  );
};

export const sendNotification = (
  notificationId,
  audienceType,
) => {
  return httpClient.post(
    `/notification/send/${notificationId}`,
    { audienceType },
  );
};

export const getMemberNotifications = () => {
  return httpClient.get("/notification/noticeList");
};

export const markNotificationAsRead = (
  notificationId,
) => {
  return httpClient.put(
    `/notification/read/${notificationId}`,
  );
};

export const getNotificationLogs = (params) => {
  return httpClient.get(
    "/notificationLog/searchlogs",
    { params },
  );
};

export const getNotificationCalendarDashboard = () => {
  return httpClient.get(
    "/dashboard/notification-calendar",
  );
};