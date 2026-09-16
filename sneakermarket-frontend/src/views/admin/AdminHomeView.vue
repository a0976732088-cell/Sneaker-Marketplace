<template>
  <div class="admin-home">
    <span class="admin-eyebrow">
      SNEAKERMARKET ADMIN
    </span>

    <h1>管理後台總覽</h1>

    <p>
      請從左側選單選擇要管理的後台系統。
    </p>

    <div class="welcome-card">
      <h2>歡迎進入管理後台</h2>

      <p>
        會員、商城、廣告、行事曆通知與競標系統，
        都可以從左側導覽列進入。
      </p>
    </div>
  </div>
</template>

<style scoped>
.admin-eyebrow {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.15em;
}

.admin-home h1 {
  margin: 10px 0;
  color: #0f172a;
  font-size: 36px;
  font-weight: 800;
}

.admin-home > p,
.welcome-card p {
  color: #64748b;
}

.welcome-card {
  margin-top: 28px;
  padding: 28px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.welcome-card h2 {
  margin-bottom: 12px;
  font-size: 20px;
}
</style>