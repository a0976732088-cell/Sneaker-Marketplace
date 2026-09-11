<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

import { getCurrentMember } from "@/service/authService";
import {
  updateMyProfile,
  changeMyPassword,
  deleteMyAccount,
} from "@/service/memberService";

const router = useRouter();

const member = ref(null);
const loading = ref(true);
const errorMessage = ref("");
const successMessage = ref("");

// =====================================================
// 個人資料修改
// =====================================================
const isEditing = ref(false);
const savingProfile = ref(false);

const editForm = ref({
  name: "",
  email: "",
  phone: "",
  address: "",
});

// =====================================================
// 密碼修改
// =====================================================
const showPasswordForm = ref(false);
const changingPassword = ref(false);

const passwordForm = ref({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
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

async function loadCurrentMember() {
  loading.value = true;
  errorMessage.value = "";

  try {
    member.value = await getCurrentMember();
    console.log("目前登入會員：", member.value);
  } catch (error) {
    console.error("取得會員資料失敗：", error);
    await router.replace("/login");
  } finally {
    loading.value = false;
  }
}

function startEdit() {
  errorMessage.value = "";
  successMessage.value = "";

  editForm.value = {
    name: member.value?.name || "",
    email: member.value?.email || "",
    phone: member.value?.phone || "",
    address: member.value?.address || "",
  };

  isEditing.value = true;
}

function cancelEdit() {
  isEditing.value = false;
  errorMessage.value = "";
  successMessage.value = "";
}

async function saveMember() {
  errorMessage.value = "";
  successMessage.value = "";

  const cleanName = editForm.value.name.trim();
  const cleanEmail = editForm.value.email.trim();
  const cleanPhone = editForm.value.phone.trim();
  const cleanAddress = editForm.value.address.trim();

  if (!cleanName) {
    errorMessage.value = "姓名不可以空白";
    return;
  }

  if (!cleanEmail) {
    errorMessage.value = "Email 不可以空白";
    return;
  }

  if (!isValidEmail(cleanEmail)) {
    errorMessage.value = "Email 格式不正確";
    return;
  }

  if (!cleanPhone) {
    errorMessage.value = "手機號碼不可以空白";
    return;
  }

  if (!isValidPhone(cleanPhone)) {
    errorMessage.value = "手機格式錯誤，需為 09 開頭後接 8 個數字";
    return;
  }

  savingProfile.value = true;

  try {
    const response = await updateMyProfile({
      name: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      address: cleanAddress,
    });

    member.value = response.data;
    successMessage.value = "會員資料修改成功";
    isEditing.value = false;
  } catch (error) {
    console.error("修改會員失敗：", error);
    errorMessage.value = getApiErrorMessage(error, "會員資料修改失敗");
  } finally {
    savingProfile.value = false;
  }
}

function resetPasswordForm() {
  passwordForm.value = {
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  };
}

async function handleChangePassword() {
  errorMessage.value = "";
  successMessage.value = "";

  if (!passwordForm.value.currentPassword) {
    errorMessage.value = "請輸入目前密碼";
    return;
  }

  if (!passwordForm.value.newPassword) {
    errorMessage.value = "請輸入新密碼";
    return;
  }

  if (passwordForm.value.newPassword.length < 8) {
    errorMessage.value = "新密碼至少需要 8 碼";
    return;
  }

  if (
    passwordForm.value.newPassword !==
    passwordForm.value.confirmPassword
  ) {
    errorMessage.value = "兩次輸入的新密碼不一致";
    return;
  }

  changingPassword.value = true;

  try {
    await changeMyPassword(passwordForm.value);

    successMessage.value = "密碼修改成功";
    showPasswordForm.value = false;
    resetPasswordForm();
  } catch (error) {
    console.error("修改密碼失敗：", error);
    errorMessage.value = getApiErrorMessage(error, "密碼修改失敗");
  } finally {
    changingPassword.value = false;
  }
}

async function handleDeleteAccount() {
  errorMessage.value = "";
  successMessage.value = "";

  if (member.value?.role === "ROLE_ADMIN") {
    errorMessage.value = "管理員不可刪除自己的管理員帳號";
    return;
  }

  const confirmed = window.confirm(
    `確定要刪除帳號「${member.value?.account}」嗎？\n刪除後將無法復原。`
  );

  if (!confirmed) {
    return;
  }

  try {
    await deleteMyAccount();

    alert("帳號已刪除");
    await router.replace("/login");
  } catch (error) {
    console.error("刪除帳號失敗：", error);
    errorMessage.value = getApiErrorMessage(error, "刪除帳號失敗");
  }
}

onMounted(() => {
  loadCurrentMember();
});
</script>

<template>
  <div class="member-page">
    <div v-if="loading" class="loading-area">會員資料載入中...</div>

    <template v-else-if="member">
      <nav class="member-nav">
        <router-link to="/member">個人檔案</router-link>
        <a href="#" @click.prevent>訂單</a>
        <a href="#" @click.prevent>競標</a>
        <a href="#security" @click.prevent>設定</a>
      </nav>

      <main class="profile-container">
        <div class="avatar">
          <span>{{ member.name?.charAt(0) || "M" }}</span>
        </div>

        <div class="profile-info">
          <h1>{{ member.name }}</h1>
          <p class="member-account">{{ member.account }}</p>
          <p>{{ member.email }}</p>
        </div>
      </main>

      <section class="profile-detail-section">
        <div class="section-header">
          <div>
            <h2>個人檔案</h2>
            <p class="section-description">修改姓名與聯絡資料</p>
          </div>

          <button v-if="!isEditing" class="edit-button" @click="startEdit">
            編輯
          </button>

          <div v-else class="edit-actions">
            <button
              class="cancel-button"
              :disabled="savingProfile"
              @click="cancelEdit"
            >
              取消
            </button>

            <button
              class="save-button"
              :disabled="savingProfile"
              @click="saveMember"
            >
              {{ savingProfile ? "儲存中..." : "儲存" }}
            </button>
          </div>
        </div>

        <div v-if="successMessage" class="message success-message">
          {{ successMessage }}
        </div>

        <div v-if="errorMessage" class="message error-message">
          {{ errorMessage }}
        </div>

        <div class="profile-details">
          <div class="detail-item">
            <span class="detail-label">帳號</span>
            <span>{{ member.account }}</span>
          </div>

          <div class="detail-item">
            <span class="detail-label">姓名</span>
            <input
              v-if="isEditing"
              v-model="editForm.name"
              type="text"
              class="edit-input"
              placeholder="請輸入姓名"
              :disabled="savingProfile"
            />
            <span v-else>{{ member.name }}</span>
          </div>

          <div class="detail-item">
            <span class="detail-label">Email</span>
            <input
              v-if="isEditing"
              v-model="editForm.email"
              type="email"
              class="edit-input"
              placeholder="請輸入 Email"
              :disabled="savingProfile"
            />
            <span v-else>{{ member.email }}</span>
          </div>

          <div class="detail-item">
            <span class="detail-label">手機</span>
            <input
              v-if="isEditing"
              v-model="editForm.phone"
              type="tel"
              maxlength="10"
              class="edit-input"
              placeholder="0912345678"
              :disabled="savingProfile"
            />
            <span v-else>{{ member.phone }}</span>
          </div>

          <div class="detail-item">
            <span class="detail-label">地址</span>
            <input
              v-if="isEditing"
              v-model="editForm.address"
              type="text"
              class="edit-input"
              placeholder="請輸入地址"
              :disabled="savingProfile"
            />
            <span v-else>{{ member.address || "尚未填寫" }}</span>
          </div>

          <div class="detail-item">
            <span class="detail-label">會員角色</span>
            <span>{{ member.role }}</span>
          </div>
        </div>
      </section>

      <section id="security" class="security-section">
        <div class="security-card">
          <div class="security-header">
            <div>
              <h2>密碼設定</h2>
              <p>修改密碼前需輸入目前密碼。</p>
            </div>

            <button
              class="outline-button"
              @click="showPasswordForm = !showPasswordForm"
            >
              {{ showPasswordForm ? "收起" : "修改密碼" }}
            </button>
          </div>

          <div v-if="showPasswordForm" class="password-form">
            <div class="form-group">
              <label>目前密碼</label>
              <input
                v-model="passwordForm.currentPassword"
                type="password"
                class="edit-input"
                autocomplete="current-password"
              />
            </div>

            <div class="form-group">
              <label>新密碼</label>
              <input
                v-model="passwordForm.newPassword"
                type="password"
                class="edit-input"
                placeholder="至少 8 碼"
                autocomplete="new-password"
              />
            </div>

            <div class="form-group">
              <label>確認新密碼</label>
              <input
                v-model="passwordForm.confirmPassword"
                type="password"
                class="edit-input"
                autocomplete="new-password"
              />
            </div>

            <div class="password-actions">
              <button
                class="save-button"
                :disabled="changingPassword"
                @click="handleChangePassword"
              >
                {{ changingPassword ? "修改中..." : "確認修改密碼" }}
              </button>
            </div>
          </div>
        </div>

        <div class="security-card danger-card">
          <div class="security-header">
            <div>
              <h2>刪除帳號</h2>
              <p v-if="member.role !== 'ROLE_ADMIN'">
                刪除後無法復原，請確認後再操作。
              </p>
              <p v-else>
                管理員帳號不可從自己的會員頁面刪除。
              </p>
            </div>

            <button
              class="delete-button"
              :disabled="member.role === 'ROLE_ADMIN'"
              @click="handleDeleteAccount"
            >
              刪除我的帳號
            </button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.member-page {
  min-height: 100vh;
  background: #fff;
  color: #111;
}

.loading-area {
  padding: 80px;
  text-align: center;
  font-size: 18px;
}

.member-nav {
  display: flex;
  justify-content: center;
  gap: 35px;
  padding: 30px 20px;
  border-bottom: 1px solid #eee;
}

.member-nav a {
  color: #111;
  text-decoration: none;
  font-weight: 600;
}

.profile-container {
  display: flex;
  align-items: center;
  gap: 25px;
  padding: 55px 50px 35px;
}

.avatar {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 600;
}

.profile-info h1 {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 500;
}

.profile-info p {
  margin: 4px 0;
  color: #666;
}

.member-account {
  font-weight: 600;
  color: #333 !important;
}

.profile-detail-section,
.security-section {
  padding: 30px 50px 50px;
}

.section-header,
.security-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.section-header h2,
.security-header h2 {
  margin: 0;
  font-size: 28px;
}

.section-description,
.security-header p {
  margin: 8px 0 0;
  color: #777;
}

.edit-button {
  border: none;
  background: none;
  font-weight: 600;
  cursor: pointer;
}

.edit-actions,
.password-actions {
  display: flex;
  gap: 12px;
}

.cancel-button,
.save-button,
.outline-button,
.delete-button {
  padding: 9px 18px;
  border-radius: 5px;
  cursor: pointer;
  font-weight: 600;
}

.cancel-button,
.outline-button {
  border: 1px solid #999;
  background: #fff;
}

.save-button {
  border: 1px solid #111;
  background: #111;
  color: #fff;
}

.delete-button {
  border: 1px solid #c62828;
  background: #fff;
  color: #c62828;
}

.delete-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.profile-details {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 28px 60px;
  margin-top: 35px;
}

.detail-item,
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-label,
.form-group label {
  color: #777;
  font-size: 14px;
}

.edit-input {
  width: 100%;
  max-width: 420px;
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 16px;
}

.message {
  margin-top: 20px;
  padding: 12px 14px;
  border-radius: 4px;
}

.success-message {
  background: #e8f5e9;
  color: #256029;
}

.error-message {
  background: #fdecea;
  color: #b42318;
}

.security-section {
  border-top: 1px solid #eee;
}

.security-card {
  padding: 28px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  margin-bottom: 22px;
}

.password-form {
  display: grid;
  gap: 18px;
  margin-top: 28px;
}

.danger-card {
  border-color: #f1c7c7;
}

@media (max-width: 768px) {
  .member-nav {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .profile-container,
  .profile-detail-section,
  .security-section {
    padding-left: 20px;
    padding-right: 20px;
  }

  .profile-details {
    grid-template-columns: 1fr;
  }

  .section-header,
  .security-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
