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

export const markAllNotificationsAsRead = () => {
  return httpClient.put("/notification/read-all");
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

export const getEmailJobs = (status = "FAILED", page = 0) =>
  httpClient.get("/admin/notification-email-jobs", {
    params: { status, page },
  });

export const retryEmailJob = (jobId) =>
  httpClient.post(`/admin/notification-email-jobs/${jobId}/retry`);

export const getMyNotificationPreference = () => {
  return httpClient.get(
    "/notifications/preferences/me",
  );
};

export const updateMyNotificationPreference = (
  payload,
) => {
  return httpClient.put(
    "/notifications/preferences/me",
    payload,
  );
};