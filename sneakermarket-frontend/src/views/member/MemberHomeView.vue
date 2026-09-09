<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

import { getCurrentMember, logout } from "@/service/authService";

import { updateMember } from "@/service/memberService";

const router = useRouter();

// ========================
// 會員資料
// ========================

const member = ref(null);

const loading = ref(true);

const errorMessage = ref("");

const successMessage = ref("");

// ========================
// 編輯狀態
// ========================

const isEditing = ref(false);

const saving = ref(false);

// 編輯中的資料
const editForm = ref({
  email: "",
  phone: "",
  address: "",
});

// ========================
// 取得目前登入會員
// ========================

async function loadCurrentMember() {
  loading.value = true;

  errorMessage.value = "";

  try {
    member.value = await getCurrentMember();

    console.log("目前登入會員：", member.value);
  } catch (error) {
    console.error("取得會員資料失敗：", error);

    errorMessage.value = "尚未登入，請先登入";

    router.push("/login");
  } finally {
    loading.value = false;
  }
}

// ========================
// 開始編輯
// ========================

function startEdit() {
  errorMessage.value = "";

  successMessage.value = "";

  // 把目前會員資料複製到編輯表單
  editForm.value = {
    email: member.value?.email || "",

    phone: member.value?.phone || "",

    address: member.value?.address || "",
  };

  isEditing.value = true;
}

// ========================
// 取消編輯
// ========================

function cancelEdit() {
  isEditing.value = false;

  errorMessage.value = "";

  successMessage.value = "";

  editForm.value = {
    email: "",
    phone: "",
    address: "",
  };
}

// ========================
// 驗證 Email
// ========================

function isValidEmail(email) {
  const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  return pattern.test(email);
}

// ========================
// 驗證手機
// ========================

function isValidPhone(phone) {
  const pattern = /^09\d{8}$/;

  return pattern.test(phone);
}

// ========================
// 儲存會員資料
// ========================

async function saveMember() {
  errorMessage.value = "";

  successMessage.value = "";

  // ------------------------
  // Email 檢查
  // ------------------------

  if (!editForm.value.email.trim()) {
    errorMessage.value = "Email 不可以空白";

    return;
  }

  if (!isValidEmail(editForm.value.email.trim())) {
    errorMessage.value = "Email 格式不正確";

    return;
  }

  // ------------------------
  // 手機檢查
  // ------------------------

  if (!editForm.value.phone.trim()) {
    errorMessage.value = "手機號碼不可以空白";

    return;
  }

  if (!isValidPhone(editForm.value.phone.trim())) {
    errorMessage.value = "手機格式錯誤，例如：0912345678";

    return;
  }

  saving.value = true;

  try {
    const response = await updateMember(member.value.memberId, {
      email: editForm.value.email.trim(),

      phone: editForm.value.phone.trim(),

      address: editForm.value.address.trim(),
    });

    console.log("修改會員成功：", response.data);

    // axios response.data
    // 直接更新畫面上的 member
    member.value = response.data;

    successMessage.value = "會員資料修改成功";

    isEditing.value = false;
  } catch (error) {
    console.error("修改會員失敗：", error);

    // axios 的錯誤通常會在
    // error.response.data
    const backendMessage = error.response?.data;

    if (typeof backendMessage === "string") {
      errorMessage.value = backendMessage;
    } else {
      errorMessage.value = "會員資料修改失敗";
    }
  } finally {
    saving.value = false;
  }
}

// ========================
// 登出
// ========================

async function handleLogout() {
  try {
    await logout();

    console.log("登出成功");
  } catch (error) {
    console.error("登出失敗：", error);
  } finally {
    router.push("/login");
  }
}

// ========================
// 頁面載入時
// ========================

onMounted(() => {
  loadCurrentMember();
});
</script>

<template>
  <div class="member-page">
    <!-- Loading -->
    <div v-if="loading" class="loading-area">會員資料載入中...</div>

    <!-- 會員中心 -->
    <template v-else-if="member">
      <!-- 上方會員資訊 -->
      <div class="member-topbar">
        <div></div>

        <div class="member-topbar-right">
          <span> 您好，{{ member.name }} </span>

          <button class="logout-button" @click="handleLogout">登出</button>
        </div>
      </div>

      <!-- 會員功能選單 -->
      <nav class="member-nav">
        <router-link to="/member"> 個人檔案 </router-link>

        <a href="#" @click.prevent> 訂單 </a>

        <a href="#" @click.prevent> 最愛 </a>

        <a href="#" @click.prevent> 設定 </a>
      </nav>

      <!-- 會員基本資料 -->
      <main class="profile-container">
        <!-- 頭像 -->
        <div class="avatar">
          <span>
            {{ member.name?.charAt(0) || "M" }}
          </span>
        </div>

        <!-- 會員名稱 -->
        <div class="profile-info">
          <h1>
            {{ member.name }}
          </h1>

          <p class="member-account">
            {{ member.account }}
          </p>

          <p>自 2026 年 9 月成為 SneakerMarket 會員</p>
        </div>
      </main>

      <!-- 會員詳細資料 -->
      <section class="profile-detail-section">
        <div class="section-header">
          <h2>個人檔案</h2>

          <!-- 尚未編輯 -->
          <button v-if="!isEditing" class="edit-button" @click="startEdit">
            編輯
          </button>

          <!-- 編輯中 -->
          <div v-else class="edit-actions">
            <button
              class="cancel-button"
              :disabled="saving"
              @click="cancelEdit"
            >
              取消
            </button>

            <button class="save-button" :disabled="saving" @click="saveMember">
              {{ saving ? "儲存中..." : "儲存" }}
            </button>
          </div>
        </div>

        <!-- 成功訊息 -->
        <div v-if="successMessage" class="message success-message">
          {{ successMessage }}
        </div>

        <!-- 錯誤訊息 -->
        <div v-if="errorMessage" class="message error-message">
          {{ errorMessage }}
        </div>

        <div class="profile-details">
          <!-- 帳號 -->
          <div class="detail-item">
            <span class="detail-label"> 帳號 </span>

            <span>
              {{ member.account }}
            </span>
          </div>

          <!-- 姓名 -->
          <div class="detail-item">
            <span class="detail-label"> 姓名 </span>

            <span>
              {{ member.name }}
            </span>
          </div>

          <!-- Email -->
          <div class="detail-item">
            <span class="detail-label"> Email </span>

            <input
              v-if="isEditing"
              v-model="editForm.email"
              type="email"
              class="edit-input"
              placeholder="請輸入 Email"
              :disabled="saving"
            />

            <span v-else>
              {{ member.email }}
            </span>
          </div>

          <!-- 手機 -->
          <div class="detail-item">
            <span class="detail-label"> 手機 </span>

            <input
              v-if="isEditing"
              v-model="editForm.phone"
              type="tel"
              class="edit-input"
              placeholder="例如：0912345678"
              :disabled="saving"
            />

            <span v-else>
              {{ member.phone }}
            </span>
          </div>

          <!-- 地址 -->
          <div class="detail-item">
            <span class="detail-label"> 地址 </span>

            <input
              v-if="isEditing"
              v-model="editForm.address"
              type="text"
              class="edit-input"
              placeholder="請輸入地址"
              autocomplete="street-address"
              :disabled="saving"
            />

            <span v-else>
              {{ member.address || "尚未填寫" }}
            </span>
          </div>

          <!-- 會員角色 -->
          <div class="detail-item">
            <span class="detail-label"> 會員角色 </span>

            <span>
              {{ member.role }}
            </span>
          </div>
        </div>
      </section>

      <!-- 興趣 -->
      <section class="interest-section">
        <div class="section-header">
          <h2>興趣關注</h2>

          <button class="edit-button" @click.prevent>編輯</button>
        </div>

        <div class="interest-menu">
          <span class="active"> 全部 </span>

          <span> 慢跑鞋 </span>

          <span> 籃球鞋 </span>

          <span> 休閒鞋 </span>

          <span> 復古鞋 </span>

          <!-- <span> 城市 </span> -->
        </div>
      </section>

      <!-- Footer -->
      <footer class="member-footer">
        <!-- 資源 -->
        <div class="footer-column">
          <h3>資源</h3>

          <a href="#" @click.prevent> 加入會員 </a>

          <a href="#" @click.prevent> 跑鞋搜尋工具 </a>

          <a href="#" @click.prevent> 潮鞋搜尋工具 </a>

          <a href="#" @click.prevent> 傳送意見回饋 </a>
        </div>

        <!-- 協助 -->
        <div class="footer-column">
          <h3>協助</h3>

          <a href="#" @click.prevent> 取得協助 </a>

          <a href="#" @click.prevent> 訂單狀態 </a>

          <a href="#" @click.prevent> 聯絡我們 </a>
        </div>

        <!-- 公司 -->
        <div class="footer-column">
          <h3>公司</h3>

          <a href="#" @click.prevent> 關於 SneakerMarket </a>

          <a href="#" @click.prevent> 最新消息 </a>

          <a href="#" @click.prevent> 職涯發展 </a>

          <a href="#" @click.prevent> 環境永續性 </a>

          <!-- <a href="#" @click.prevent> 影響力 </a> -->
        </div>
      </footer>
    </template>
  </div>
</template>

<style scoped>
/* =========================
   整體
========================= */

.member-page {
  min-height: 100vh;
  background: #ffffff;
  color: #111;
}

/* =========================
   Loading
========================= */

.loading-area {
  padding: 80px;
  text-align: center;
  font-size: 18px;
}

/* =========================
   上方會員資訊
========================= */

.member-topbar {
  height: 50px;

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 0 50px;

  border-bottom: 1px solid #eeeeee;
}

.member-topbar-right {
  display: flex;

  align-items: center;

  gap: 18px;

  font-size: 14px;
}

.logout-button {
  border: none;

  background: none;

  color: #111;

  cursor: pointer;

  font-size: 14px;
}

.logout-button:hover {
  text-decoration: underline;
}

/* =========================
   會員選單
========================= */

.member-nav {
  display: flex;

  justify-content: center;

  gap: 35px;

  padding: 30px 0;
}

.member-nav a {
  color: #111;

  text-decoration: none;

  font-weight: 600;
}

.member-nav a:hover {
  color: #777;
}

/* =========================
   會員基本資料
========================= */

.profile-container {
  display: flex;

  align-items: center;

  gap: 25px;

  padding: 60px 50px 40px;
}

.avatar {
  width: 90px;

  height: 90px;

  border-radius: 50%;

  background: #e5e5e5;

  display: flex;

  justify-content: center;

  align-items: center;

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

/* =========================
   個人資料
========================= */

.profile-detail-section {
  padding: 30px 50px 50px;
}

.section-header {
  display: flex;

  justify-content: space-between;

  align-items: center;
}

.section-header h2 {
  margin: 0;

  font-size: 28px;
}

.edit-button {
  border: none;

  background: none;

  font-weight: 600;

  cursor: pointer;
}

/* 編輯按鈕區域 */

.edit-actions {
  display: flex;

  gap: 12px;
}

.cancel-button,
.save-button {
  padding: 8px 18px;

  border-radius: 4px;

  cursor: pointer;

  font-weight: 600;
}

.cancel-button {
  border: 1px solid #999;

  background: #ffffff;
}

.save-button {
  border: 1px solid #111;

  background: #111;

  color: #ffffff;
}

.cancel-button:disabled,
.save-button:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

.profile-details {
  display: grid;

  grid-template-columns: repeat(2, minmax(0, 1fr));

  gap: 28px 60px;

  margin-top: 35px;
}

.detail-item {
  display: flex;

  flex-direction: column;

  gap: 8px;
}

.detail-label {
  color: #777;

  font-size: 14px;
}

/* 編輯 Input */

.edit-input {
  width: 100%;

  max-width: 420px;

  padding: 10px 12px;

  border: 1px solid #cccccc;

  border-radius: 4px;

  font-size: 16px;
}

.edit-input:focus {
  outline: none;

  border-color: #111;
}

/* 訊息 */

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

/* =========================
   興趣
========================= */

.interest-section {
  padding: 40px 50px 80px;

  border-top: 1px solid #eeeeee;
}

.interest-menu {
  display: flex;

  flex-wrap: wrap;

  gap: 40px;

  margin-top: 40px;
}

.interest-menu span {
  color: #555;

  font-weight: 600;

  cursor: pointer;
}

.interest-menu span.active {
  color: #111;
}

/* =========================
   Footer
========================= */

.member-footer {
  margin-top: 40px;

  padding: 55px 65px 80px;

  border-top: 1px solid #e5e5e5;

  display: grid;

  grid-template-columns: repeat(3, minmax(0, 1fr));

  gap: 80px;

  background: #ffffff;
}

.footer-column {
  display: flex;

  flex-direction: column;

  align-items: flex-start;
}

.footer-column h3 {
  margin: 0 0 35px;

  font-size: 18px;

  font-weight: 700;

  color: #111;
}

.footer-column a {
  margin-bottom: 18px;

  color: #555;

  font-size: 16px;

  font-weight: 500;

  text-decoration: none;

  transition: color 0.2s;
}

.footer-column a:hover {
  color: #111;
}

/* =========================
   Responsive
========================= */

@media (max-width: 768px) {
  .member-topbar {
    padding: 0 20px;
  }

  .member-nav {
    justify-content: flex-start;

    overflow-x: auto;

    gap: 22px;

    padding: 25px 20px;
  }

  .profile-container {
    padding: 40px 20px;
  }

  .profile-detail-section {
    padding: 20px 20px 40px;
  }

  .profile-details {
    grid-template-columns: 1fr;
  }

  .interest-section {
    padding: 35px 20px 60px;
  }

  .member-footer {
    grid-template-columns: 1fr;

    gap: 45px;

    padding: 40px 25px 60px;
  }

  .footer-column h3 {
    margin-bottom: 20px;
  }
}
</style>
