<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

import { getCurrentMember } from "@/service/authService";

import {
  getAllMembers,
  createMember,
  updateMember,
  deleteMember,
} from "@/service/memberService";

const router = useRouter();

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
// Admin 功能模組
// =====================================================
const adminModules = [
  {
    title: "會員系統",
    description: "會員資料新增、查詢、修改與刪除。",
    type: "member",
  },

  {
    title: "一般商城",
    description: "商品、購物車、結帳與一般訂單。",
    to: {
      name: "admin-products",
    },
  },

  {
    title: "廣告系統",
    description: "首頁廣告展示與後台管理。",
    to: {
      name: "advertisement",
    },
  },

  {
    title: "行事曆 / 通知",
    description: "事件追蹤、提醒與站內通知。",
    to: {
      name: "eventManage",
    },
  },

  {
    title: "競標系統",
    description: "管理競標、下架／恢復顯示與批量刪除。",
    to: {
      name: "admin-auctions",
    },
  },
];

// =====================================================
// API 錯誤訊息
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
// 管理員權限確認
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
// 滾動到會員管理
// =====================================================
function scrollToMemberManagement() {
  document.getElementById("member-management")?.scrollIntoView({
    behavior: "smooth",

    block: "start",
  });
}

// =====================================================
// 取得會員
//
// Soft Delete 後：
// 後端只會回傳 deleted_at IS NULL。
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
// 重設新增表單
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
  } catch (error) {
    console.error("新增會員失敗：", error);

    errorMessage.value = getApiErrorMessage(error, "新增會員失敗");
  } finally {
    creating.value = false;
  }
}

// =====================================================
// 開始修改會員
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

    errorMessage.value = getApiErrorMessage(error, "修改會員失敗");
  } finally {
    updating.value = false;
  }
}

// =====================================================
// Soft Delete 會員
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
// 完整日期時間
//
// DB / API：
// 2026-09-15T11:50:48
//
// 顯示完整格式：
// 2026-09-15 11:50:48
// =====================================================
function formatDateTimeFull(dateTime) {
  if (!dateTime) {
    return "-";
  }

  return dateTime.replace("T", " ").substring(0, 19);
}

// =====================================================
// ★ 日期時間縮短顯示
//
// 完整：
// 2026-09-15 11:50:48
//
// 表格：
// 2026...
//
// 只顯示前 4 個字元。
// =====================================================
function formatDateTimePreview(dateTime) {
  const fullDateTime = formatDateTimeFull(dateTime);

  if (fullDateTime === "-") {
    return "-";
  }

  const chars = Array.from(fullDateTime);

  /*
   * 4 個字以內就完整顯示
   */
  if (chars.length <= 4) {
    return fullDateTime;
  }

  /*
   * 超過 4 個字：
   *
   * 2026...
   */
  return chars.slice(0, 4).join("") + "...";
}

// =====================================================
// 地址縮短
//
// 高雄市左營區博愛三路100號
//
// ↓
//
// 高雄市...
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
// 初始化
// =====================================================
onMounted(async () => {
  const allowed = await checkAdminPermission();

  if (allowed) {
    await loadMembers();
  }
});
</script>

<template>
  <div class="container py-5">
    <!-- ==================================================
         驗證管理員權限
    =================================================== -->
    <div v-if="checkingPermission" class="alert alert-secondary">
      正在驗證管理員權限...
    </div>

    <template v-else-if="currentAdmin">
      <!-- ==================================================
           管理後台標題
      =================================================== -->
      <div class="mb-4">
        <h1 class="h2 mb-2">管理後台</h1>

        <p class="text-secondary mb-1">SneakerMarket 系統管理中心</p>

        <p class="text-secondary mb-0">
          目前登入管理員：

          <strong>
            {{ currentAdmin.account }}
          </strong>
        </p>
      </div>

      <!-- ==================================================
           後台模組
      =================================================== -->
      <section class="mb-5">
        <div class="row g-4">
          <div
            v-for="module in adminModules"
            :key="module.title"
            class="col-12 col-md-6"
          >
            <div class="card h-100 shadow-sm">
              <div class="card-body p-4">
                <h2 class="h4 mb-3">
                  {{ module.title }}
                </h2>

                <p class="text-secondary mb-4">
                  {{ module.description }}
                </p>

                <button
                  v-if="module.type === 'member'"
                  type="button"
                  class="btn btn-outline-dark"
                  @click="scrollToMemberManagement"
                >
                  開啟模組
                </button>

                <RouterLink v-else :to="module.to" class="btn btn-outline-dark">
                  開啟模組
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
      </section>

      <hr class="my-5" />

      <!-- ==================================================
           會員管理
      =================================================== -->
      <section id="member-management">
        <div class="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h2 class="h3 mb-1">會員管理</h2>

            <p class="text-secondary mb-0">SneakerMarket 後台會員資料</p>
          </div>

          <button
            type="button"
            class="btn btn-primary"
            @click="showCreateForm = !showCreateForm"
          >
            + 新增會員
          </button>
        </div>

        <!-- ==================================================
             新增會員
        =================================================== -->
        <div v-if="showCreateForm" class="card shadow-sm mb-4">
          <div class="card-body">
            <h5 class="card-title mb-4">新增會員</h5>

            <div class="row g-3">
              <!-- 帳號 -->
              <div class="col-md-6">
                <label class="form-label"> 帳號 * </label>

                <input
                  v-model="newMember.account"
                  class="form-control"
                  type="text"
                  placeholder="請輸入帳號"
                />
              </div>

              <!-- 密碼 -->
              <div class="col-md-6">
                <label class="form-label"> 密碼 * </label>

                <input
                  v-model="newMember.password"
                  class="form-control"
                  type="password"
                  placeholder="至少 8 碼"
                />
              </div>

              <!-- 姓名 -->
              <div class="col-md-6">
                <label class="form-label"> 姓名 * </label>

                <input
                  v-model="newMember.name"
                  class="form-control"
                  type="text"
                />
              </div>

              <!-- Email -->
              <div class="col-md-6">
                <label class="form-label"> Email * </label>

                <input
                  v-model="newMember.email"
                  class="form-control"
                  type="email"
                />
              </div>

              <!-- 手機 -->
              <div class="col-md-6">
                <label class="form-label"> 手機 * </label>

                <input
                  v-model="newMember.phone"
                  class="form-control"
                  type="tel"
                  maxlength="10"
                  placeholder="0912345678"
                />
              </div>

              <!-- 地址 -->
              <div class="col-md-6">
                <label class="form-label"> 地址 </label>

                <input
                  v-model="newMember.address"
                  class="form-control"
                  type="text"
                />
              </div>
            </div>

            <!-- 按鈕 -->
            <div class="d-flex justify-content-end gap-2 mt-4">
              <button
                type="button"
                class="btn btn-outline-secondary"
                @click="
                  showCreateForm = false;
                  resetCreateForm();
                "
              >
                取消
              </button>

              <button
                type="button"
                class="btn btn-primary"
                :disabled="creating"
                @click="handleCreateMember"
              >
                {{ creating ? "新增中..." : "確認新增" }}
              </button>
            </div>
          </div>
        </div>

        <!-- 成功訊息 -->
        <div v-if="successMessage" class="alert alert-success">
          {{ successMessage }}
        </div>

        <!-- Error -->
        <div v-if="errorMessage" class="alert alert-danger">
          {{ errorMessage }}
        </div>

        <!-- Loading -->
        <div v-if="loading" class="alert alert-secondary">
          會員資料載入中...
        </div>

        <!-- 無會員 -->
        <div v-else-if="members.length === 0" class="alert alert-warning">
          目前沒有會員資料
        </div>

        <!-- ==================================================
             會員 Table
        =================================================== -->
        <div v-else class="table-responsive">
          <table class="table table-hover align-middle">
            <thead class="table-dark">
              <tr>
                <th>ID</th>

                <th>帳號</th>

                <th>姓名</th>

                <th>Email</th>

                <th>手機</th>

                <!-- 地址 -->
                <th class="address-header">地址</th>

                <th>角色</th>

                <!-- ★ 註冊時間 -->
                <th class="datetime-header">註冊時間</th>

                <th style="min-width: 150px">操作</th>
              </tr>
            </thead>

            <tbody>
              <template v-for="member in members" :key="member.memberId">
                <tr>
                  <!-- ID -->
                  <td>
                    {{ member.memberId }}
                  </td>

                  <!-- 帳號 -->
                  <td>
                    {{ member.account }}
                  </td>

                  <!-- 姓名 -->
                  <td>
                    {{ member.name }}
                  </td>

                  <!-- Email -->
                  <td>
                    {{ member.email }}
                  </td>

                  <!-- 手機 -->
                  <td>
                    {{ member.phone || "尚未填寫" }}
                  </td>

                  <!-- =================================================
                       地址

                       高雄市左營區博愛三路100號
                       ↓
                       高雄市...
                  ================================================== -->
                  <td class="address-cell">
                    <span
                      class="address-preview"
                      :title="member.address || '尚未填寫'"
                    >
                      {{ formatAddressPreview(member.address) }}
                    </span>
                  </td>

                  <!-- 角色 -->
                  <td>
                    <span
                      class="badge"
                      :class="
                        member.role === 'ROLE_ADMIN'
                          ? 'bg-dark'
                          : 'bg-secondary'
                      "
                    >
                      {{ member.role }}
                    </span>
                  </td>

                  <!-- =================================================
                       ★ 註冊時間

                       完整：
                       2026-09-15 11:50:48

                       顯示：
                       2026...

                       title：
                       完整時間
                  ================================================== -->
                  <td class="datetime-cell">
                    <span
                      class="datetime-preview"
                      :title="formatDateTimeFull(member.createdAt)"
                    >
                      {{ formatDateTimePreview(member.createdAt) }}
                    </span>
                  </td>

                  <!-- 操作 -->
                  <td>
                    <div class="d-flex gap-2 align-items-center">
                      <!-- 修改 -->
                      <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        @click="startEditMember(member)"
                      >
                        修改
                      </button>

                      <!-- 刪除 -->
                      <button
                        v-if="member.memberId !== currentAdmin.memberId"
                        type="button"
                        class="btn btn-sm btn-outline-danger"
                        @click="handleDeleteMember(member)"
                      >
                        刪除
                      </button>

                      <!-- Admin 自己 -->
                      <span v-else class="small text-secondary">
                        目前管理員
                      </span>
                    </div>
                  </td>
                </tr>

                <!-- ==================================================
                     修改會員
                =================================================== -->
                <tr v-if="editingMemberId === member.memberId">
                  <td colspan="9">
                    <div class="card bg-light border-0">
                      <div class="card-body">
                        <h5 class="mb-3">
                          修改會員：
                          {{ member.account }}
                        </h5>

                        <div class="row g-3">
                          <!-- 姓名 -->
                          <div class="col-md-6">
                            <label class="form-label"> 姓名 </label>

                            <input
                              v-model="editMember.name"
                              type="text"
                              class="form-control"
                            />
                          </div>

                          <!-- Email -->
                          <div class="col-md-6">
                            <label class="form-label"> Email </label>

                            <input
                              v-model="editMember.email"
                              type="email"
                              class="form-control"
                            />
                          </div>

                          <!-- 手機 -->
                          <div class="col-md-6">
                            <label class="form-label"> 手機 </label>

                            <input
                              v-model="editMember.phone"
                              type="tel"
                              maxlength="10"
                              class="form-control"
                            />
                          </div>

                          <!-- 地址 -->
                          <div class="col-md-6">
                            <label class="form-label"> 地址 </label>

                            <input
                              v-model="editMember.address"
                              type="text"
                              class="form-control"
                            />
                          </div>
                        </div>

                        <div class="d-flex justify-content-end gap-2 mt-3">
                          <button
                            type="button"
                            class="btn btn-outline-secondary"
                            @click="cancelEditMember"
                          >
                            取消
                          </button>

                          <button
                            type="button"
                            class="btn btn-primary"
                            :disabled="updating"
                            @click="handleUpdateMember(member.memberId)"
                          >
                            {{ updating ? "儲存中..." : "儲存修改" }}
                          </button>
                        </div>
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
/* =====================================================
   地址
===================================================== */

.address-cell {
  width: 100px;

  min-width: 100px;

  max-width: 100px;

  white-space: nowrap;
}

.address-header {
  width: 100px;

  min-width: 100px;

  white-space: nowrap;
}

.address-preview {
  display: inline-block;

  max-width: 90px;

  white-space: nowrap;

  overflow: hidden;

  text-overflow: ellipsis;

  vertical-align: middle;

  /*
   * 一般箭頭。
   *
   * 不使用 cursor: help，
   * 所以不會出現 ?
   */
  cursor: default;
}

/* =====================================================
   ★ 註冊時間
===================================================== */

/*
 * 固定時間欄位寬度。
 *
 * 防止瀏覽器放大之後變成：
 *
 * 2026-
 * 09-
 * 15
 * 11:50
 *
 * 這種換行情況。
 */
.datetime-cell {
  width: 90px;

  min-width: 90px;

  max-width: 90px;

  white-space: nowrap;
}

/*
 * Table Header
 */
.datetime-header {
  width: 90px;

  min-width: 90px;

  white-space: nowrap;
}

/*
 * 畫面只顯示：
 *
 * 2026...
 *
 * title 則保留：
 *
 * 2026-09-15 11:50:48
 */
.datetime-preview {
  display: inline-block;

  max-width: 80px;

  white-space: nowrap;

  overflow: hidden;

  text-overflow: ellipsis;

  vertical-align: middle;

  cursor: default;
}
</style>
