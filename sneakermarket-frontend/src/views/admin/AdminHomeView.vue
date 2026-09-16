<script setup>
import { ref, onMounted, watch } from "vue";

import { useRouter, useRoute } from "vue-router";

import { getCurrentMember } from "@/service/authService";

import {
  getAllMembers,
  createMember,
  updateMember,
  deleteMember,
} from "@/service/memberService";

const router = useRouter();

const route = useRoute();

// =====================================================
// 管理員權限
// =====================================================

const checkingPermission = ref(true);

const currentAdmin = ref(null);

// =====================================================
// 會員資料
// =====================================================

const members = ref([]);

const loading = ref(true);

const errorMessage = ref("");

const successMessage = ref("");

// =====================================================
// 新增會員
// =====================================================

const showCreateForm = ref(false);

const creating = ref(false);

const newMember = ref({
  account: "",
  password: "",
  name: "",
  email: "",
  phone: "",
  address: "",
});

// =====================================================
// 修改會員
// =====================================================

const editingMemberId = ref(null);

const updating = ref(false);

const editMember = ref({
  name: "",
  email: "",
  phone: "",
  address: "",
});

// =====================================================
// API Error
// =====================================================

function getApiErrorMessage(error, fallbackMessage) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  if (error?.message) {
    return error.message;
  }

  return fallbackMessage;
}

// =====================================================
// Email 驗證
// =====================================================

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// =====================================================
// 手機驗證
// =====================================================

function isValidPhone(phone) {
  return /^09\d{8}$/.test(phone);
}

// =====================================================
// 管理員權限
// =====================================================

async function checkAdminPermission() {
  try {
    const member = await getCurrentMember();

    if (member.role !== "ROLE_ADMIN") {
      await router.replace({
        name: "forbidden",
      });

      return false;
    }

    currentAdmin.value = member;

    return true;
  } catch (error) {
    console.error("管理員驗證失敗：", error);

    await router.replace({
      name: "login",

      query: {
        redirect: "/admin",
      },
    });

    return false;
  } finally {
    checkingPermission.value = false;
  }
}

// =====================================================
// 取得會員
// =====================================================

async function loadMembers() {
  loading.value = true;

  errorMessage.value = "";

  try {
    const response = await getAllMembers();

    members.value = response.data;
  } catch (error) {
    console.error("取得會員資料失敗：", error);

    errorMessage.value = getApiErrorMessage(error, "取得會員資料失敗");
  } finally {
    loading.value = false;
  }
}

// =====================================================
// 重設新增會員
// =====================================================

function resetCreateForm() {
  newMember.value = {
    account: "",
    password: "",
    name: "",
    email: "",
    phone: "",
    address: "",
  };
}

// =====================================================
// 開啟新增會員
//
// 除了打開表單，也同步把網址改成：
// /admin?action=create-member
// =====================================================

async function openCreateMember() {
  showCreateForm.value = true;

  successMessage.value = "";

  errorMessage.value = "";

  await router.replace({
    name: "admin",

    query: {
      action: "create-member",
    },
  });
}

// =====================================================
// 關閉新增會員
//
// 重要：
// 除了關閉表單，也把網址從
//
// /admin?action=create-member
//
// 改回
//
// /admin
//
// 這樣 Sidebar 的「新增會員」就不會繼續反白
// =====================================================

async function closeCreateMember() {
  showCreateForm.value = false;

  resetCreateForm();

  successMessage.value = "";

  errorMessage.value = "";

  await router.replace({
    name: "admin",
  });
}

// =====================================================
// 新增會員
// =====================================================

async function handleCreateMember() {
  errorMessage.value = "";

  successMessage.value = "";

  const data = {
    account: newMember.value.account.trim(),

    password: newMember.value.password,

    name: newMember.value.name.trim(),

    email: newMember.value.email.trim(),

    phone: newMember.value.phone.trim(),

    address: newMember.value.address.trim(),
  };

  if (
    !data.account ||
    !data.password ||
    !data.name ||
    !data.email ||
    !data.phone
  ) {
    errorMessage.value = "請把必填欄位填寫完整";

    return;
  }

  if (data.password.length < 8) {
    errorMessage.value = "密碼至少需要 8 碼";

    return;
  }

  if (!isValidEmail(data.email)) {
    errorMessage.value = "Email 格式不正確";

    return;
  }

  if (!isValidPhone(data.phone)) {
    errorMessage.value = "手機格式錯誤，需為 09 開頭後接 8 個數字";

    return;
  }

  creating.value = true;

  try {
    await createMember(data);

    successMessage.value = "會員新增成功";

    resetCreateForm();

    showCreateForm.value = false;

    await loadMembers();

    // =============================================
    // 新增成功後網址回到 /admin
    // 避免「新增會員」繼續反白
    // =============================================
    await router.replace({
      name: "admin",
    });
  } catch (error) {
    console.error("新增會員失敗：", error);

    errorMessage.value = getApiErrorMessage(error, "新增會員失敗");
  } finally {
    creating.value = false;
  }
}

// =====================================================
// 開始修改
// =====================================================

function startEditMember(member) {
  errorMessage.value = "";

  successMessage.value = "";

  editingMemberId.value = member.memberId;

  editMember.value = {
    name: member.name || "",

    email: member.email || "",

    phone: member.phone || "",

    address: member.address || "",
  };
}

// =====================================================
// 取消修改
// =====================================================

function cancelEditMember() {
  editingMemberId.value = null;

  editMember.value = {
    name: "",
    email: "",
    phone: "",
    address: "",
  };
}

// =====================================================
// 修改會員
// =====================================================

async function handleUpdateMember(memberId) {
  errorMessage.value = "";

  successMessage.value = "";

  const data = {
    name: editMember.value.name.trim(),

    email: editMember.value.email.trim(),

    phone: editMember.value.phone.trim(),

    address: editMember.value.address.trim(),
  };

  if (!data.name || !data.email || !data.phone) {
    errorMessage.value = "姓名、Email、手機不可為空";

    return;
  }

  if (!isValidEmail(data.email)) {
    errorMessage.value = "Email 格式不正確";

    return;
  }

  if (!isValidPhone(data.phone)) {
    errorMessage.value = "手機格式錯誤，需為 09 開頭後接 8 個數字";

    return;
  }

  updating.value = true;

  try {
    await updateMember(memberId, data);

    successMessage.value = "會員資料修改成功";

    cancelEditMember();

    await loadMembers();
  } catch (error) {
    console.error("修改會員失敗：", error);

    errorMessage.value = getApiErrorMessage(error, "會員資料修改失敗");
  } finally {
    updating.value = false;
  }
}

// =====================================================
// 刪除會員
// =====================================================

async function handleDeleteMember(member) {
  errorMessage.value = "";

  successMessage.value = "";

  if (member.memberId === currentAdmin.value?.memberId) {
    errorMessage.value = "管理員不可刪除自己的管理員帳號";

    return;
  }

  const confirmed = window.confirm(
    `確定要刪除會員「${member.account}」嗎？\n刪除後此會員將無法登入。`,
  );

  if (!confirmed) {
    return;
  }

  try {
    await deleteMember(member.memberId);

    successMessage.value = `會員 ${member.account} 已刪除`;

    await loadMembers();
  } catch (error) {
    console.error("刪除會員失敗：", error);

    errorMessage.value = getApiErrorMessage(error, "刪除會員失敗");
  }
}

// =====================================================
// 完整時間
// =====================================================

function formatDateTimeFull(dateTime) {
  if (!dateTime) {
    return "-";
  }

  return dateTime.replace("T", " ").substring(0, 19);
}

// =====================================================
// 時間預覽
// =====================================================

function formatDateTimePreview(dateTime) {
  const fullDateTime = formatDateTimeFull(dateTime);

  if (fullDateTime === "-") {
    return "-";
  }

  const chars = Array.from(fullDateTime);

  if (chars.length <= 4) {
    return fullDateTime;
  }

  return chars.slice(0, 4).join("") + "...";
}

// =====================================================
// 地址預覽
// =====================================================

function formatAddressPreview(address) {
  if (!address) {
    return "尚未填寫";
  }

  const chars = Array.from(address);

  if (chars.length <= 3) {
    return address;
  }

  return chars.slice(0, 3).join("") + "...";
}

// =====================================================
// 根據 URL 決定新增表單是否開啟
// =====================================================

function handleQueryAction() {
  // /admin?action=create-member
  if (route.query.action === "create-member") {
    showCreateForm.value = true;

    return;
  }

  // /admin
  showCreateForm.value = false;
}

// =====================================================
// 初始化
// =====================================================

onMounted(async () => {
  const allowed = await checkAdminPermission();

  if (allowed) {
    await loadMembers();

    handleQueryAction();
  }
});

// =====================================================
// URL Query 改變時同步畫面
//
// 例如：
// /admin
// ↓
// /admin?action=create-member
// ↓
// /admin
// =====================================================

watch(
  () => route.query.action,

  () => {
    handleQueryAction();
  },
);
</script>

<template>
  <div class="admin-member-page">
    <!-- =========================================
         權限驗證
    ========================================== -->

    <div v-if="checkingPermission" class="status-box">
      正在驗證管理員權限...
    </div>

    <template v-else-if="currentAdmin">
      <!-- =====================================
           Page Header
      ====================================== -->

      <div class="page-header">
        <div>
          <p class="eyebrow">MEMBER MANAGEMENT</p>

          <h1>會員管理</h1>

          <p class="page-description">
            管理 SneakerMarket 會員資料、建立會員、 修改會員與刪除會員。
          </p>

          <p class="admin-info">
            目前登入管理員：

            <strong>
              {{ currentAdmin.account }}
            </strong>
          </p>
        </div>

        <!-- =================================
             右上角 新增 / 關閉
        ================================== -->

        <button
          type="button"
          class="create-button"
          @click="showCreateForm ? closeCreateMember() : openCreateMember()"
        >
          {{ showCreateForm ? "關閉新增" : "+ 新增會員" }}
        </button>
      </div>

      <!-- =====================================
           新增會員
      ====================================== -->

      <section v-if="showCreateForm" class="panel create-panel">
        <div class="panel-header">
          <div>
            <h2>新增會員</h2>

            <p>建立新的商城會員帳號</p>
          </div>
        </div>

        <div class="form-grid">
          <div class="form-group">
            <label> 帳號 * </label>

            <input
              v-model="newMember.account"
              type="text"
              placeholder="請輸入帳號"
            />
          </div>

          <div class="form-group">
            <label> 密碼 * </label>

            <input
              v-model="newMember.password"
              type="password"
              placeholder="至少 8 碼"
            />
          </div>

          <div class="form-group">
            <label> 姓名 * </label>

            <input v-model="newMember.name" type="text" />
          </div>

          <div class="form-group">
            <label> Email * </label>

            <input v-model="newMember.email" type="email" />
          </div>

          <div class="form-group">
            <label> 手機 * </label>

            <input
              v-model="newMember.phone"
              type="tel"
              maxlength="10"
              placeholder="0912345678"
            />
          </div>

          <div class="form-group">
            <label> 地址 </label>

            <input v-model="newMember.address" type="text" />
          </div>
        </div>

        <div class="form-actions">
          <!-- =================================
               取消時也清除 URL Query
          ================================== -->

          <button
            type="button"
            class="secondary-button"
            @click="closeCreateMember"
          >
            取消
          </button>

          <button
            type="button"
            class="primary-button"
            :disabled="creating"
            @click="handleCreateMember"
          >
            {{ creating ? "新增中..." : "確認新增" }}
          </button>
        </div>
      </section>

      <!-- =====================================
           Message
      ====================================== -->

      <div v-if="successMessage" class="message-box success-box">
        {{ successMessage }}
      </div>

      <div v-if="errorMessage" class="message-box error-box">
        {{ errorMessage }}
      </div>

      <!-- =====================================
           Member List
      ====================================== -->

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>會員列表</h2>

            <p>
              共

              <strong>
                {{ members.length }}
              </strong>

              位有效會員
            </p>
          </div>
        </div>

        <div v-if="loading" class="status-box">會員資料載入中...</div>

        <div v-else-if="members.length === 0" class="status-box">
          目前沒有會員資料
        </div>

        <div v-else class="member-table-wrapper">
          <table class="member-table">
            <thead>
              <tr>
                <th>ID</th>

                <th>帳號</th>

                <th>姓名</th>

                <th>Email</th>

                <th>手機</th>

                <th>地址</th>

                <th>角色</th>

                <th>註冊時間</th>

                <th>操作</th>
              </tr>
            </thead>

            <tbody>
              <template v-for="member in members" :key="member.memberId">
                <tr>
                  <td>
                    {{ member.memberId }}
                  </td>

                  <td>
                    {{ member.account }}
                  </td>

                  <td>
                    {{ member.name }}
                  </td>

                  <td>
                    {{ member.email }}
                  </td>

                  <td>
                    {{ member.phone || "尚未填寫" }}
                  </td>

                  <td>
                    <span
                      class="truncate-cell"
                      :title="member.address || '尚未填寫'"
                    >
                      {{ formatAddressPreview(member.address) }}
                    </span>
                  </td>

                  <td>
                    <span class="role-badge">
                      {{ member.role }}
                    </span>
                  </td>

                  <td>
                    <span
                      class="truncate-cell"
                      :title="formatDateTimeFull(member.createdAt)"
                    >
                      {{ formatDateTimePreview(member.createdAt) }}
                    </span>
                  </td>

                  <td>
                    <div class="table-actions">
                      <!-- 修改：藍色 -->

                      <button
                        type="button"
                        class="text-button edit-text"
                        @click="startEditMember(member)"
                      >
                        修改
                      </button>

                      <!-- 刪除：紅色 -->

                      <button
                        v-if="member.memberId !== currentAdmin.memberId"
                        type="button"
                        class="text-button delete-text"
                        @click="handleDeleteMember(member)"
                      >
                        刪除
                      </button>

                      <span v-else class="current-admin-text">
                        目前管理員
                      </span>
                    </div>
                  </td>
                </tr>

                <!-- =============================
                     Edit Member
                ============================== -->

                <tr v-if="editingMemberId === member.memberId" class="edit-row">
                  <td colspan="9">
                    <div class="edit-panel">
                      <div class="edit-title">
                        修改會員：

                        <strong>
                          {{ member.account }}
                        </strong>
                      </div>

                      <div class="form-grid">
                        <div class="form-group">
                          <label> 姓名 </label>

                          <input v-model="editMember.name" type="text" />
                        </div>

                        <div class="form-group">
                          <label> Email </label>

                          <input v-model="editMember.email" type="email" />
                        </div>

                        <div class="form-group">
                          <label> 手機 </label>

                          <input
                            v-model="editMember.phone"
                            type="tel"
                            maxlength="10"
                          />
                        </div>

                        <div class="form-group">
                          <label> 地址 </label>

                          <input v-model="editMember.address" type="text" />
                        </div>
                      </div>

                      <div class="form-actions">
                        <button
                          type="button"
                          class="secondary-button"
                          @click="cancelEditMember"
                        >
                          取消
                        </button>

                        <button
                          type="button"
                          class="primary-button"
                          :disabled="updating"
                          @click="handleUpdateMember(member.memberId)"
                        >
                          {{ updating ? "儲存中..." : "儲存修改" }}
                        </button>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.admin-member-page {
  width: 100%;
}

/* =========================================
   HEADER
========================================= */

.page-header {
  display: flex;

  align-items: flex-start;

  justify-content: space-between;

  gap: 30px;

  margin-bottom: 30px;
}

.eyebrow {
  margin: 0 0 8px;

  color: #777777;

  font-size: 11px;

  font-weight: 700;

  letter-spacing: 0.16em;
}

.page-header h1 {
  margin: 0;

  font-size: 30px;

  font-weight: 500;

  color: #111111;
}

.page-description {
  margin: 10px 0 6px;

  color: #666666;

  font-size: 14px;
}

.admin-info {
  margin: 0;

  color: #777777;

  font-size: 13px;
}

/* =========================================
   PANEL
========================================= */

.panel {
  margin-bottom: 24px;

  padding: 28px;

  background: #ffffff;

  border: 1px solid #dddddd;

  border-radius: 6px;
}

.panel-header {
  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 24px;
}

.panel-header h2 {
  margin: 0;

  font-size: 21px;

  font-weight: 500;

  color: #111111;
}

.panel-header p {
  margin: 6px 0 0;

  color: #777777;

  font-size: 13px;
}

/* =========================================
   BUTTON
========================================= */

.create-button,
.primary-button,
.secondary-button {
  min-height: 40px;

  padding: 9px 16px;

  border-radius: 4px;

  font-size: 13px;

  cursor: pointer;
}

.create-button,
.primary-button {
  border: 1px solid #111111;

  background: #111111;

  color: #ffffff;
}

.create-button:hover,
.primary-button:hover {
  background: #2b2b2b;
}

.secondary-button {
  border: 1px solid #999999;

  background: #ffffff;

  color: #111111;
}

.primary-button:disabled {
  opacity: 0.5;

  cursor: not-allowed;
}

/* =========================================
   FORM
========================================= */

.form-grid {
  display: grid;

  grid-template-columns: repeat(2, minmax(0, 1fr));

  gap: 18px 24px;
}

.form-group {
  display: flex;

  flex-direction: column;

  gap: 7px;
}

.form-group label {
  color: #555555;

  font-size: 12px;
}

.form-group input {
  width: 100%;

  height: 42px;

  padding: 0 12px;

  border: 1px solid #cccccc;

  border-radius: 4px;

  outline: none;

  background: #ffffff;

  color: #111111;
}

.form-group input:focus {
  border-color: #111111;
}

.form-actions {
  display: flex;

  justify-content: flex-end;

  gap: 10px;

  margin-top: 24px;
}

/* =========================================
   MESSAGE
========================================= */

.message-box,
.status-box {
  margin-bottom: 20px;

  padding: 14px 16px;

  border: 1px solid #dddddd;

  background: #ffffff;

  border-radius: 4px;

  color: #444444;

  font-size: 13px;
}

.success-box {
  border-color: #cfcfcf;
}

.error-box {
  border-color: #888888;

  color: #111111;
}

/* =========================================
   TABLE
========================================= */

.member-table-wrapper {
  width: 100%;

  overflow-x: auto;
}

.member-table {
  width: 100%;

  border-collapse: collapse;

  font-size: 13px;
}

.member-table th {
  padding: 13px 12px;

  border-bottom: 1px solid #111111;

  color: #555555;

  font-size: 11px;

  font-weight: 600;

  text-align: left;

  white-space: nowrap;
}

.member-table td {
  padding: 16px 12px;

  border-bottom: 1px solid #eeeeee;

  vertical-align: middle;

  color: #222222;

  white-space: nowrap;
}

.member-table tbody tr:hover {
  background: #fafafa;
}

.truncate-cell {
  display: inline-block;

  max-width: 90px;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;

  vertical-align: middle;
}

.role-badge {
  display: inline-block;

  padding: 4px 7px;

  border: 1px solid #cccccc;

  border-radius: 3px;

  background: #fafafa;

  font-size: 10px;
}

.table-actions {
  display: flex;

  align-items: center;

  gap: 12px;
}

/* =========================================
   修改 / 刪除
========================================= */

.text-button {
  padding: 0;

  border: none;

  background: transparent;

  font-size: 12px;

  cursor: pointer;

  text-decoration: underline;

  text-underline-offset: 3px;

  transition:
    color 0.15s ease,
    opacity 0.15s ease;
}

/* 修改：藍色 */
.edit-text {
  color: #2563eb;
}

.edit-text:hover {
  color: #1d4ed8;
}

/* 刪除：紅色 */
.delete-text {
  color: #dc2626;
}

.delete-text:hover {
  color: #b91c1c;
}

.current-admin-text {
  color: #999999;

  font-size: 11px;
}

/* =========================================
   EDIT
========================================= */

.edit-row td {
  padding: 0;

  background: #fafafa;
}

.edit-panel {
  padding: 24px;

  border-bottom: 1px solid #dddddd;
}

.edit-title {
  margin-bottom: 20px;

  font-size: 14px;
}

/* =========================================
   RWD
========================================= */

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
