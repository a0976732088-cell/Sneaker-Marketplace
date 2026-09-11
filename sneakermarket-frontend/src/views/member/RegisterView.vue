<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { register } from "@/service/authService";

const router = useRouter();

// ================================
// 表單欄位
// ================================
const account = ref("");
const password = ref("");
const confirmPassword = ref("");
const name = ref("");
const email = ref("");
const phone = ref("");
const address = ref("");

// ================================
// 畫面狀態
// ================================
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

// ================================
// 註冊
// ================================
async function handleRegister() {
  errorMessage.value = "";
  successMessage.value = "";

  // 1. 帳號
  if (!account.value.trim()) {
    errorMessage.value = "請輸入帳號";
    return;
  }

  // 2. 密碼
  if (!password.value) {
    errorMessage.value = "請輸入密碼";
    return;
  }

  // 密碼至少 8 碼
  if (password.value.length < 8) {
    errorMessage.value = "密碼至少需要 8 碼";
    return;
  }

  // 3. 確認密碼
  if (password.value !== confirmPassword.value) {
    errorMessage.value = "兩次輸入的密碼不一致";
    return;
  }

  // 4. 姓名
  if (!name.value.trim()) {
    errorMessage.value = "請輸入姓名";
    return;
  }

  // 5. Email
  if (!email.value.trim()) {
    errorMessage.value = "請輸入 Email";
    return;
  }

  // Email 格式檢查
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailPattern.test(email.value)) {
    errorMessage.value = "Email 格式不正確";
    return;
  }

  // 6. 手機
  if (!phone.value.trim()) {
    errorMessage.value = "請輸入手機號碼";
    return;
  }

  // 台灣手機格式：09xxxxxxxx
  const phonePattern = /^09\d{8}$/;

  if (!phonePattern.test(phone.value)) {
    errorMessage.value = "手機格式錯誤，例如：0912345678";
    return;
  }

  // 7. 地址
  if (!address.value.trim()) {
    errorMessage.value = "請輸入地址";
    return;
  }

  loading.value = true;

  try {
    // ================================
    // 將會員註冊資料送給後端
    // ================================
    const result = await register({
      account: account.value.trim(),
      password: password.value,
      name: name.value.trim(),
      email: email.value.trim(),
      phone: phone.value.trim(),
      address: address.value.trim(),
    });

    console.log("註冊成功：", result);

    successMessage.value = "註冊成功，即將前往登入頁面";

    // 1 秒後跳到登入頁
    setTimeout(() => {
      router.push("/login");
    }, 1000);
  } catch (error) {
    console.error("註冊失敗：", error);

    if (error.status === 400) {
      errorMessage.value = "註冊資料有誤，請檢查輸入內容";
    } else if (error.status === 409) {
      errorMessage.value = "帳號或 Email 已被使用";
    } else if (error.status >= 500) {
      errorMessage.value = "伺服器發生錯誤，請稍後再試";
    } else {
      errorMessage.value = "註冊失敗，請確認後端是否正常啟動";
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h1 class="register-title">建立會員帳號</h1>

      <!-- 錯誤訊息 -->
      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <!-- 成功訊息 -->
      <div v-if="successMessage" class="alert alert-success">
        {{ successMessage }}
      </div>

      <form @submit.prevent="handleRegister">
        <!-- 帳號 -->
        <div class="mb-3">
          <label for="account" class="form-label"> 帳號 </label>

          <input
            id="account"
            v-model="account"
            type="text"
            class="form-control"
            placeholder="請輸入帳號"
            autocomplete="username"
            :disabled="loading"
          />
        </div>

        <!-- 姓名 -->
        <div class="mb-3">
          <label for="name" class="form-label"> 姓名 </label>

          <input
            id="name"
            v-model="name"
            type="text"
            class="form-control"
            placeholder="請輸入姓名"
            autocomplete="name"
            :disabled="loading"
          />
        </div>

        <!-- Email -->
        <div class="mb-3">
          <label for="email" class="form-label"> Email </label>

          <input
            id="email"
            v-model="email"
            type="email"
            class="form-control"
            placeholder="example@email.com"
            autocomplete="email"
            :disabled="loading"
          />
        </div>

        <!-- 手機 -->
        <div class="mb-3">
          <label for="phone" class="form-label"> 手機 </label>

          <input
            id="phone"
            v-model="phone"
            type="tel"
            class="form-control"
            placeholder="0912345678"
            autocomplete="tel"
            :disabled="loading"
          />
        </div>

        <!-- 地址 -->
        <div class="mb-3">
          <label for="address" class="form-label"> 地址 </label>

          <input
            id="address"
            v-model="address"
            type="text"
            class="form-control"
            placeholder="請輸入地址"
            autocomplete="street-address"
            :disabled="loading"
          />
        </div>

        <!-- 密碼 -->
        <div class="mb-3">
          <label for="password" class="form-label"> 密碼 </label>

          <input
            id="password"
            v-model="password"
            type="password"
            class="form-control"
            placeholder="至少 8 碼"
            autocomplete="new-password"
            :disabled="loading"
          />
        </div>

        <!-- 確認密碼 -->
        <div class="mb-4">
          <label for="confirmPassword" class="form-label"> 確認密碼 </label>

          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            class="form-control"
            placeholder="請再次輸入密碼"
            autocomplete="new-password"
            :disabled="loading"
          />
        </div>

        <!-- 註冊按鈕 -->
        <button type="submit" class="btn btn-dark w-100" :disabled="loading">
          <span v-if="loading"> 註冊中... </span>

          <span v-else> 建立帳號 </span>
        </button>

        <!-- 回登入 -->
        <div class="text-center mt-4">
          <span class="text-secondary"> 已經有帳號？ </span>

          <RouterLink to="/login" class="login-link"> 登入 </RouterLink>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: calc(100vh - 120px);

  display: flex;
  justify-content: center;
  align-items: flex-start;

  padding: 60px 20px;

  background-color: #f8f9fa;
}

.register-card {
  width: 100%;
  max-width: 500px;

  background-color: white;

  padding: 36px;

  border-radius: 8px;
}

.register-title {
  margin-bottom: 32px;

  text-align: center;

  font-size: 30px;
  font-weight: 500;
}

.form-label {
  font-weight: 500;
}

.form-control {
  min-height: 42px;
}

.btn {
  min-height: 44px;
}

.login-link {
  margin-left: 6px;

  color: #111;

  font-weight: 600;

  text-decoration: none;
}

.login-link:hover {
  text-decoration: underline;
}
</style>
