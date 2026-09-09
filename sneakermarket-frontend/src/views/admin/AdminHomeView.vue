<script setup>
import { ref, onMounted } from "vue";
import { getAllMembers } from "@/service/memberService";

// ==============================
// 會員資料
// ==============================

const members = ref([]);

const loading = ref(true);

const errorMessage = ref("");

// ==============================
// 取得全部會員
// ==============================

async function loadMembers() {
  loading.value = true;

  errorMessage.value = "";

  try {
    const response = await getAllMembers();

    // Axios 回傳的真正資料放在 response.data
    members.value = response.data;

    console.log("全部會員資料：", members.value);
  } catch (error) {
    console.error("取得會員資料失敗：", error);

    errorMessage.value = "取得會員資料失敗";
  } finally {
    loading.value = false;
  }
}

// ==============================
// 頁面載入
// ==============================

onMounted(() => {
  loadMembers();
});
</script>

<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h1 class="h3 mb-1">會員管理</h1>

        <p class="text-secondary mb-0">SneakerMarket 後台會員資料</p>
      </div>
    </div>

    <!-- Loading -->

    <div v-if="loading" class="alert alert-secondary">會員資料載入中...</div>

    <!-- Error -->

    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <!-- 沒有會員 -->

    <div v-else-if="members.length === 0" class="alert alert-warning">
      目前沒有會員資料
    </div>

    <!-- 會員表格 -->

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
          </tr>
        </thead>

        <tbody>
          <tr v-for="member in members" :key="member.memberId">
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
              {{ member.address || "尚未填寫" }}
            </td>

            <td>
              <span class="badge bg-secondary">
                {{ member.role }}
              </span>
            </td>

            <td>
              {{ member.createdAt }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
