import httpClient from "./httpClient";

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

// 修改會員
export function updateMember(id, data) {
  return httpClient.put(`/members/${id}`, data);
}

// 刪除會員
export function deleteMember(id) {
  return httpClient.delete(`/members/${id}`);
}
