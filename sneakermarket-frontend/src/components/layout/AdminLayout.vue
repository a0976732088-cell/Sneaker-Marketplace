<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <RouterLink :to="{ name: 'admin' }" class="sidebar-brand">
        <span class="brand-logo">SM</span>

        <span>
          <strong>酷客 Kicks</strong>
          <small>管理後台</small>
        </span>
      </RouterLink>

      <p class="sidebar-label">後台系統</p>

      <nav class="sidebar-navigation">
        <RouterLink
          :to="{ name: 'admin' }"
          class="sidebar-link"
          exact-active-class="active"
        >
          <span class="sidebar-icon">⌂</span>
          後台首頁
        </RouterLink>

        <RouterLink
          v-for="item in menuItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="sidebar-link"
          active-class="active"
        >
          <span class="sidebar-icon">
            {{ item.icon }}
          </span>

          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <RouterLink :to="{ name: 'home' }" class="front-link">
          ← 返回前台
        </RouterLink>
      </div>
    </aside>

    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
const menuItems = [
  {
    name: "admin-members",
    label: "會員系統",
    icon: "♙",
  },
  {
    name: "admin-commerce",
    label: "商城系統",
    icon: "▣",
  },
  {
    name: "admin-auctions",
    label: "競標系統",
    icon: "◇",
  },
  {
    name: "admin-advertisements",
    label: "廣告系統",
    icon: "◫",
  },
  {
    name: "admin-calendar",
    label: "行事曆／通知",
    icon: "◷",
  },
];
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: calc(100vh - 60px);
  background: #f5f7fb;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex: 0 0 240px;
  flex-direction: column;
  width: 240px;
  height: 100vh;
  padding: 24px 16px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  box-shadow: 4px 0 18px rgba(15, 23, 42, 0.04);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px 24px;
  color: #0f172a;
  text-decoration: none;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-brand strong,
.sidebar-brand small {
  display: block;
}

.sidebar-brand small {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 12px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  background: #111827;
  border-radius: 12px;
}

.sidebar-label {
  margin: 24px 12px 10px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.sidebar-navigation {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.sidebar-link:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.sidebar-link.active {
  color: #ffffff;
  background: #111827;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.15);
}

.sidebar-icon {
  display: inline-flex;
  justify-content: center;
  width: 22px;
  font-size: 17px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.front-link {
  display: block;
  padding: 10px 12px;
  color: #64748b;
  font-size: 13px;
  text-decoration: none;
  border-radius: 8px;
}

.front-link:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.admin-main {
  min-width: 0;
  flex: 1;
  padding: 32px;
}

/*
 * 這兩段必須放在 @media 外面，
 * 桌面版才會生效。
 */
.admin-main :deep(.ad-admin-shell) {
  width: 100%;
  max-width: 100%;
  margin: 0;
  margin-bottom: 0;
}

.admin-main :deep(.ad-workspace) {
  width: 100%;
  padding: 24px;
}

/* 廣告表格在後台 Layout 中改為自適應寬度 */
.admin-main :deep(.ad-admin-shell .table-wrap) {
  width: 100%;
  overflow-x: hidden;
}

.admin-main :deep(.ad-admin-shell table) {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

/* 重新分配五個欄位比例 */
.admin-main :deep(.ad-admin-shell .column-ad) {
  width: 30%;
}

.admin-main :deep(.ad-admin-shell .column-placement) {
  width: 17%;
}

.admin-main :deep(.ad-admin-shell .column-period) {
  width: 23%;
}

.admin-main :deep(.ad-admin-shell .column-status) {
  width: 17%;
}

.admin-main :deep(.ad-admin-shell .column-actions) {
  width: 13%;
}

/* 縮小表格左右內距 */
.admin-main :deep(.ad-admin-shell th),
.admin-main :deep(.ad-admin-shell td) {
  padding-right: 10px;
  padding-left: 10px;
}

/* 取消廣告內容欄原本的最低寬度 */
.admin-main :deep(.ad-admin-shell .ad-identity) {
  min-width: 0;
  gap: 8px;
}

/* 後台 Sidebar 模式使用較小縮圖 */
.admin-main :deep(.ad-admin-shell .ad-thumbnail) {
  width: 110px;
  height: 82px;
  flex: 0 0 110px;
}

/* 過長標題顯示省略符號 */
.admin-main :deep(.ad-admin-shell .ad-identity > div:last-child) {
  min-width: 0;
}

.admin-main :deep(.ad-admin-shell .ad-identity strong),
.admin-main :deep(.ad-admin-shell .ad-identity small) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 操作按鈕保持靠右 */
.admin-main :deep(.ad-admin-shell .row-actions) {
  width: auto;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .admin-layout {
    display: block;
  }

  .admin-sidebar {
    position: static;
    width: 100%;
    height: auto;
  }

  .sidebar-navigation {
    overflow-x: auto;
    flex-direction: row;
  }

  .sidebar-link {
    flex: 0 0 auto;
  }

  .sidebar-footer {
    display: none;
  }

  .admin-main {
    padding: 20px 12px;
  }
}
</style>
