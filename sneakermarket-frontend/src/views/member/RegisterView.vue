<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { register } from "@/service/authService";

const router = useRouter();

const account = ref("");
const password = ref("");
const confirmPassword = ref("");
const name = ref("");
const email = ref("");
const phone = ref("");
const address = ref("");

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

function getBackendMessage(error, fallback) {
  if (error?.data?.message) {
    return error.data.message;
  }

  if (typeof error?.data === "string" && error.data) {
    return error.data;
  }

  if (error?.message) {
    return error.message;
  }

  return fallback;
}

async function handleRegister() {
  errorMessage.value = "";
  successMessage.value = "";

  const cleanAccount = account.value.trim();
  const cleanName = name.value.trim();
  const cleanEmail = email.value.trim();
  const cleanPhone = phone.value.trim();
  const cleanAddress = address.value.trim();

  if (!cleanAccount) {
    errorMessage.value = "請輸入帳號";
    return;
  }

  if (!password.value) {
    errorMessage.value = "請輸入密碼";
    return;
  }

  if (password.value.length < 8) {
    errorMessage.value = "密碼至少需要 8 碼";
    return;
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = "兩次輸入的密碼不一致";
    return;
  }

  if (!cleanName) {
    errorMessage.value = "請輸入姓名";
    return;
  }

  if (!cleanEmail) {
    errorMessage.value = "請輸入 Email";
    return;
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailPattern.test(cleanEmail)) {
    errorMessage.value = "Email 格式不正確";
    return;
  }

  if (!cleanPhone) {
    errorMessage.value = "請輸入手機號碼";
    return;
  }

  // 09 開頭 + 後面 8 個數字，總共 10 碼
  const phonePattern = /^09\d{8}$/;

  if (!phonePattern.test(cleanPhone)) {
    errorMessage.value = "手機格式錯誤，需為 09 開頭後接 8 個數字";
    return;
  }

  loading.value = true;

  try {
    const result = await register({
      account: cleanAccount,
      password: password.value,
      name: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      address: cleanAddress,
    });

    console.log("註冊成功：", result);

    successMessage.value = "註冊成功，即將前往登入頁面";

    setTimeout(() => {
      router.push("/login");
    }, 1000);
  } catch (error) {
    console.error("註冊失敗：", error);

    errorMessage.value = getBackendMessage(
      error,
      "註冊失敗，請確認輸入資料"
    );
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h1 class="register-title">建立會員帳號</h1>

      <div v-if="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="alert alert-success">
        {{ successMessage }}
      </div>

      <form @submit.prevent="handleRegister">
        <div class="mb-3">
          <label for="account" class="form-label">帳號</label>
          <input
            id="account"
            v-model="account"
            type="text"
            class="form-control"
            placeholder="請輸入帳號"
            autocomplete="username"
            :disabled="loading"
          />
          <div class="form-text">帳號不可與既有會員重複。</div>
        </div>

        <div class="mb-3">
          <label for="name" class="form-label">姓名</label>
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

        <div class="mb-3">
          <label for="email" class="form-label">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            class="form-control"
            placeholder="example@email.com"
            autocomplete="email"
            :disabled="loading"
          />
          <div class="form-text">Email 不可與既有會員重複。</div>
        </div>

        <div class="mb-3">
          <label for="phone" class="form-label">手機</label>
          <input
            id="phone"
            v-model="phone"
            type="tel"
            class="form-control"
            placeholder="0912345678"
            autocomplete="tel"
            maxlength="10"
            :disabled="loading"
          />
          <div class="form-text">格式：09 開頭後接 8 個數字，例如 0912345678。</div>
        </div>

        <div class="mb-3">
          <label for="address" class="form-label">地址</label>
          <input
            id="address"
            v-model="address"
            type="text"
            class="form-control"
            placeholder="請輸入地址（可不填）"
            autocomplete="street-address"
            :disabled="loading"
          />
        </div>

        <div class="mb-3">
          <label for="password" class="form-label">密碼</label>
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

        <div class="mb-4">
          <label for="confirmPassword" class="form-label">確認密碼</label>
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

        <button type="submit" class="btn btn-dark w-100" :disabled="loading">
          {{ loading ? "註冊中..." : "建立帳號" }}
        </button>

        <div class="text-center mt-4">
          <span class="text-secondary">已經有帳號？</span>
          <RouterLink to="/login" class="login-link">登入</RouterLink>
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