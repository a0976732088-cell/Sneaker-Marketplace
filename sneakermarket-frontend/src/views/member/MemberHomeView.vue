<!-- 原本的 MemberHomeView -->
<!-- <template>
  <section>
    <h1 class="h3">會員中心</h1>
    <div class="alert alert-secondary mt-3">
      <strong>BASE 預留頁面</strong><br />
      後續放登入、註冊、會員資料與折價券相關頁面。
    </div>
    <p class="text-secondary mb-0">主要負責：會員系統負責人</p>
  </section>
</template> -->

<!-- 測試 Router -->
<!-- 若執行看到 Member Page Works!, Router 連線正常 -->
<!-- <template>
  <div>
    <h1>Member Page Works!</h1>
  </div>
</template> -->

<script setup>
import { onMounted, ref } from "vue";
import { getAllMembers } from "@/service/memberService";

const members = ref([]);
const loading = ref(false);
const errorMessage = ref("");

async function loadMembers() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getAllMembers();
    members.value = response.data;
  } catch (error) {
    console.error(error);
    errorMessage.value = "會員資料載入失敗";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadMembers();
});
</script>

<template>
  <div class="container py-4">
    <h2 class="mb-4">會員管理</h2>

    <div v-if="loading">資料載入中...</div>

    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <table v-else class="table table-bordered table-hover">
      <thead>
        <tr>
          <th>ID</th>
          <th>帳號</th>
          <th>姓名</th>
          <th>Email</th>
          <th>電話</th>
          <th>角色</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="member in members" :key="member.memberId">
          <td>{{ member.memberId }}</td>
          <td>{{ member.account }}</td>
          <td>{{ member.name }}</td>
          <td>{{ member.email }}</td>
          <td>{{ member.phone }}</td>
          <td>{{ member.role }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
