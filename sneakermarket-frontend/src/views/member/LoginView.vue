<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/service/authService'

const router = useRouter()

// ================================
// 表單資料
// ================================
const account = ref('')
const password = ref('')

// ================================
// 畫面狀態
// ================================
const errorMessage = ref('')
const loading = ref(false)


// ================================
// 登入功能
// ================================
async function handleLogin() {

  errorMessage.value = ''

  // 帳號檢查
  if (!account.value.trim()) {
    errorMessage.value = '請輸入帳號'
    return
  }

  // 密碼檢查
  if (!password.value) {
    errorMessage.value = '請輸入密碼'
    return
  }

  loading.value = true

  try {

    // 呼叫登入 API
    await login({
      account: account.value.trim(),
      password: password.value
    })

    console.log('登入成功')

    // 登入成功 → 會員中心
    await router.push('/member')

  } catch (error) {

    console.error('登入失敗：', error)

    // fetch 版本使用 error.status
    if (
      error.status === 400 ||
      error.status === 401
    ) {

      errorMessage.value = '帳號或密碼錯誤'

    } else if (error.status === 403) {

      errorMessage.value = '此帳號目前沒有登入權限'

    } else if (error.status === 404) {

      errorMessage.value = '找不到登入 API，請確認後端路徑'

    } else if (error.status >= 500) {

      errorMessage.value = '伺服器發生錯誤，請稍後再試'

    } else {

      errorMessage.value =
        '無法連線到伺服器，請確認 Spring Boot 是否已啟動'
    }

  } finally {

    loading.value = false
  }
}
</script>


<template>
  <div class="login-page">

    <div class="login-card">

      <!-- 標題 -->
      <h1 class="login-title">
        會員登入
      </h1>


      <!-- 錯誤訊息 -->
      <div
        v-if="errorMessage"
        class="alert alert-danger"
      >
        {{ errorMessage }}
      </div>


      <!-- 登入表單 -->
      <form @submit.prevent="handleLogin">


        <!-- 帳號 -->
        <div class="mb-3">

          <label
            for="account"
            class="form-label"
          >
            帳號
          </label>

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


        <!-- 密碼 -->
        <div class="mb-4">

          <label
            for="password"
            class="form-label"
          >
            密碼
          </label>

          <input
            id="password"
            v-model="password"
            type="password"
            class="form-control"
            placeholder="請輸入密碼"
            autocomplete="current-password"
            :disabled="loading"
          />

        </div>


        <!-- 登入按鈕 -->
        <button
          type="submit"
          class="btn btn-dark w-100"
          :disabled="loading"
        >

          <span v-if="loading">
            登入中...
          </span>

          <span v-else>
            登入
          </span>

        </button>


        <!-- 註冊連結 -->
        <div class="text-center mt-4">

          <span class="text-secondary">
            還沒有會員帳號？
          </span>

          <RouterLink
            to="/register"
            class="register-link"
          >
            立即註冊
          </RouterLink>

        </div>

      </form>

    </div>

  </div>
</template>


<style scoped>
.login-page {
  min-height: calc(100vh - 120px);

  display: flex;
  justify-content: center;
  align-items: flex-start;

  padding-top: 70px;

  background-color: #f8f9fa;
}


.login-card {
  width: 100%;
  max-width: 420px;

  background-color: #ffffff;

  padding: 32px;

  border-radius: 8px;
}


.login-title {
  margin-bottom: 32px;

  text-align: center;

  font-size: 32px;
  font-weight: 500;
}


.form-label {
  font-weight: 500;
}


.form-control {
  min-height: 42px;
}


.btn {
  min-height: 42px;
}


.register-link {
  margin-left: 6px;

  color: #111;

  font-weight: 600;

  text-decoration: none;
}


.register-link:hover {
  text-decoration: underline;
}
</style>