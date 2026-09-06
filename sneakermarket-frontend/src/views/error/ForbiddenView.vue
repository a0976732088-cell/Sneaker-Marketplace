<template>
  <div class="container py-5">
    <div class="row justify-content-center">
      <div class="col-md-7 col-lg-6">
        <div class="card shadow-sm">
          <div class="card-body text-center p-5">
            <h1 class="display-5 mb-3">403</h1>

            <h2 class="h4 mb-3">無權存取</h2>

            <p class="text-muted mb-4">你沒有權限執行此操作。</p>

            <div class="d-flex justify-content-center gap-2">
              <button
                type="button"
                class="btn btn-outline-secondary"
                @click="goBack"
              >
                返回上一頁
              </button>

              <RouterLink to="/" class="btn btn-primary"> 回首頁 </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

/**
 * 返回使用者上一個頁面。
 *
 * 若沒有可靠的上一頁紀錄，
 * 則回到首頁。
 */
function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push("/");
}
</script>
