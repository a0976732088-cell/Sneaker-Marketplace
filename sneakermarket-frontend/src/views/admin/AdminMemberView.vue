<template>
  <div class="container py-5">
    <!-- 頁面標題 -->
    <div
      class="d-flex justify-content-between align-items-center mb-4"
    >
      <div>
        <h1 class="h2 mb-2">會員管理</h1>
        <p class="text-secondary mb-0">
          SneakerMarket 後台會員資料
        </p>
      </div>

      <RouterLink
        :to="{ name: 'admin' }"
        class="btn btn-outline-secondary"
      >
        返回管理後台
      </RouterLink>
    </div>

    <!-- 權限檢查 -->
    <div
      v-if="checkingPermission"
      class="alert alert-secondary"
    >
      正在驗證管理員權限...
    </div>

    <!-- 會員管理內容 -->
    <section v-else-if="currentAdmin">
      <div class="d-flex justify-content-end mb-4">
        <button
          type="button"
          class="btn btn-primary"
          @click="showCreateForm = !showCreateForm"
        >
          + 新增會員
        </button>
      </div>

      <!-- 新增會員 -->
      <div
        v-if="showCreateForm"
        class="card shadow-sm mb-4"
      >
        <div class="card-body">
          <h5 class="card-title mb-4">新增會員</h5>

          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label">帳號 *</label>
              <input
                v-model="newMember.account"
                class="form-control"
                type="text"
                placeholder="請輸入帳號"
              />
            </div>

            <div class="col-md-6">
              <label class="form-label">密碼 *</label>
              <input
                v-model="newMember.password"
                class="form-control"
                type="password"
                placeholder="至少 8 碼"
              />
            </div>

            <div class="col-md-6">
              <label class="form-label">姓名 *</label>
              <input
                v-model="newMember.name"
                class="form-control"
                type="text"
              />
            </div>

            <div class="col-md-6">
              <label class="form-label">Email *</label>
              <input
                v-model="newMember.email"
                class="form-control"
                type="email"
              />
            </div>

            <div class="col-md-6">
              <label class="form-label">手機 *</label>
              <input
                v-model="newMember.phone"
                class="form-control"
                type="tel"
                maxlength="10"
                placeholder="0912345678"
              />
            </div>

            <div class="col-md-6">
              <label class="form-label">地址</label>
              <input
                v-model="newMember.address"
                class="form-control"
                type="text"
              />
            </div>
          </div>

          <div
            class="d-flex justify-content-end gap-2 mt-4"
          >
            <button
              type="button"
              class="btn btn-outline-secondary"
              @click="closeCreateForm"
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

      <!-- 操作結果 -->
      <div
        v-if="successMessage"
        class="alert alert-success"
      >
        {{ successMessage }}
      </div>

      <div
        v-if="errorMessage"
        class="alert alert-danger"
      >
        {{ errorMessage }}
      </div>

      <!-- 載入狀態 -->
      <div
        v-if="loading"
        class="alert alert-secondary"
      >
        會員資料載入中...
      </div>

      <div
        v-else-if="members.length === 0"
        class="alert alert-warning"
      >
        目前沒有會員資料
      </div>

      <!-- 會員列表 -->
      <div v-else class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-dark">
            <tr>
              <th>ID</th>
              <th>帳號</th>
              <th>姓名</th>
              <th>Email</th>
              <th>手機</th>
              <th>地址</th>
              <th>角色</th>
              <th>註冊時間</th>
              <th style="min-width: 150px">操作</th>
            </tr>
          </thead>

          <tbody>
            <template
              v-for="member in members"
              :key="member.memberId"
            >
              <tr>
                <td>{{ member.memberId }}</td>
                <td>{{ member.account }}</td>
                <td>{{ member.name }}</td>
                <td>{{ member.email }}</td>
                <td>
                  {{ member.phone || "尚未填寫" }}
                </td>
                <td>
                  {{ member.address || "尚未填寫" }}
                </td>
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
                <td>
                  {{ formatDateTime(member.createdAt) }}
                </td>
                <td>
                  <div
                    class="d-flex gap-2 align-items-center"
                  >
                    <button
                      type="button"
                      class="btn btn-sm btn-outline-primary"
                      @click="startEditMember(member)"
                    >
                      修改
                    </button>

                    <button
                      v-if="
                        member.memberId !==
                        currentAdmin.memberId
                      "
                      type="button"
                      class="btn btn-sm btn-outline-danger"
                      @click="handleDeleteMember(member)"
                    >
                      刪除
                    </button>

                    <span
                      v-else
                      class="small text-secondary"
                    >
                      目前管理員
                    </span>
                  </div>
                </td>
              </tr>

              <!-- 編輯會員 -->
              <tr
                v-if="
                  editingMemberId === member.memberId
                "
              >
                <td colspan="9">
                  <div class="card bg-light border-0">
                    <div class="card-body">
                      <h5 class="mb-3">
                        修改會員：{{ member.account }}
                      </h5>

                      <div class="row g-3">
                        <div class="col-md-6">
                          <label class="form-label">
                            姓名
                          </label>
                          <input
                            v-model="editMember.name"
                            type="text"
                            class="form-control"
                          />
                        </div>

                        <div class="col-md-6">
                          <label class="form-label">
                            Email
                          </label>
                          <input
                            v-model="editMember.email"
                            type="email"
                            class="form-control"
                          />
                        </div>

                        <div class="col-md-6">
                          <label class="form-label">
                            手機
                          </label>
                          <input
                            v-model="editMember.phone"
                            type="tel"
                            maxlength="10"
                            class="form-control"
                          />
                        </div>

                        <div class="col-md-6">
                          <label class="form-label">
                            地址
                          </label>
                          <input
                            v-model="editMember.address"
                            type="text"
                            class="form-control"
                          />
                        </div>
                      </div>

                      <div
                        class="d-flex justify-content-end gap-2 mt-3"
                      >
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
                          @click="
                            handleUpdateMember(
                              member.memberId
                            )
                          "
                        >
                          {{
                            updating
                              ? "儲存中..."
                              : "儲存修改"
                          }}
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
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { getCurrentMember } from "@/service/authService";
import {
  createMember,
  deleteMember,
  getAllMembers,
  updateMember,
} from "@/service/memberService";

const router = useRouter();

const checkingPermission = ref(true);
const currentAdmin = ref(null);

const members = ref([]);
const loading = ref(true);

const errorMessage = ref("");
const successMessage = ref("");

const showCreateForm = ref(false);
const creating = ref(false);
const updating = ref(false);

const editingMemberId = ref(null);

const newMember = ref({
  account: "",
  password: "",
  name: "",
  email: "",
  phone: "",
  address: "",
});

const editMember = ref({
  name: "",
  email: "",
  phone: "",
  address: "",
});

function getApiErrorMessage(error, fallbackMessage) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  return fallbackMessage;
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPhone(phone) {
  return /^09\d{8}$/.test(phone);
}

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
    console.error("管理員驗證失敗：");

    await router.replace({
      name: "login",
      query: {
        redirect: "/admin/members",
      },
    });

    return false;
  } finally {
    checkingPermission.value = false;
  }
}

async function loadMembers() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getAllMembers();
    members.value = response.data;
  } catch (error) {
    console.error("取得會員資料失敗：");

    errorMessage.value = getApiErrorMessage(
      error,
      "取得會員資料失敗"
    );
  } finally {
    loading.value = false;
  }
}

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

function closeCreateForm() {
  showCreateForm.value = false;
  resetCreateForm();
}

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
    errorMessage.value =
      "手機格式錯誤，需為 09 開頭後接 8 個數字";
    return;
  }

  creating.value = true;

  try {
    await createMember(data);

    successMessage.value = "會員新增成功";
    closeCreateForm();
    await loadMembers();
  } catch (error) {
    console.error("新增會員失敗：");

    errorMessage.value = getApiErrorMessage(
      error,
      "新增會員失敗"
    );
  } finally {
    creating.value = false;
  }
}

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

function cancelEditMember() {
  editingMemberId.value = null;

  editMember.value = {
    name: "",
    email: "",
    phone: "",
    address: "",
  };
}

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
    errorMessage.value =
      "姓名、Email、手機不可為空";
    return;
  }

  if (!isValidEmail(data.email)) {
    errorMessage.value = "Email 格式不正確";
    return;
  }

  if (!isValidPhone(data.phone)) {
    errorMessage.value =
      "手機格式錯誤，需為 09 開頭後接 8 個數字";
    return;
  }

  updating.value = true;

  try {
    await updateMember(memberId, data);

    successMessage.value = "會員資料修改成功";
    cancelEditMember();
    await loadMembers();
  } catch (error) {
    console.error("修改會員失敗：");

    errorMessage.value = getApiErrorMessage(
      error,
      "修改會員失敗"
    );
  } finally {
    updating.value = false;
  }
}

async function handleDeleteMember(member) {
  errorMessage.value = "";
  successMessage.value = "";

  if (
    member.memberId === currentAdmin.value?.memberId
  ) {
    errorMessage.value =
      "管理員不可刪除自己的管理員帳號";
    return;
  }

  const confirmed = window.confirm(
    `確定要刪除會員「${member.account}」嗎？`
  );

  if (!confirmed) {
    return;
  }

  try {
    await deleteMember(member.memberId);

    successMessage.value =
      `會員 ${member.account} 已刪除`;

    await loadMembers();
  } catch (error) {
    console.error("刪除會員失敗：");

    errorMessage.value = getApiErrorMessage(
      error,
      "刪除會員失敗"
    );
  }
}

function formatDateTime(dateTime) {
  if (!dateTime) {
    return "-";
  }

  return dateTime
    .replace("T", " ")
    .substring(0, 19);
}

onMounted(async () => {
  const allowed = await checkAdminPermission();

  if (allowed) {
    await loadMembers();
  }
});
</script>