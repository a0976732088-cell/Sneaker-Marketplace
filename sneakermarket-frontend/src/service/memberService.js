import httpClient from "./httpClient";


// ADMIN 會員 CRUD

// 查詢全部會員
export function getAllMembers() {
  return httpClient.get("/members");
}

// 查詢單一會員
export function getMemberById(id) {
  return httpClient.get(`/members/${id}`);
}

// 新增會員
export function createMember(data) {
  return httpClient.post("/members", data);
}

// Admin 修改指定會員
export function updateMember(id, data) {
  return httpClient.put(`/members/${id}`, data);
}

// Admin 刪除指定會員
export function deleteMember(id) {
  return httpClient.delete(`/members/${id}`);
}


// 目前登入會員自己的操作
// 修改自己的姓名 / Email / 手機 / 地址
export function updateMyProfile(data) {
  return httpClient.put("/members/me", data);
}

// 修改自己的密碼
export function changeMyPassword(data) {
  return httpClient.put("/members/me/password", data);
}

// 刪除自己的帳號
export function deleteMyAccount() {
  return httpClient.delete("/members/me");
}