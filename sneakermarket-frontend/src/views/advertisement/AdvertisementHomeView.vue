<script setup>
import { onMounted, onUnmounted } from "vue";
import AdvertisementFormDrawer from "@/components/advertisement/admin/AdvertisementFormDrawer.vue";
import { useAdvertisementManagement } from "@/composables/advertisement/useAdvertisementManagement";
import AdvertisementPagination from "@/components/advertisement/admin/AdvertisementPagination.vue";
import AdvertisementFilterBar from "@/components/advertisement/admin/AdvertisementFilterBar.vue";
import AdvertisementTable from "@/components/advertisement/admin/AdvertisementTable.vue";

const {
  tabs,
  activeTab,
  keyword,
  placement,
  showCreate,
  toast,
  toastType,
  loadingAds,
  loadError,
  currentPage,
  totalPages,
  totalElements,
  pageNumbers,
  firstItemNumber,
  lastItemNumber,
  filteredAds,
  form,
  currentImageRule,
  isEditing,
  isUploadingImage,
  minimumStartAt,
  isStartAtLocked,
  dashboardSummary,
  loadDashboardSummary,
  handlePlacementChange,
  handleImageUpload,
  toggle,
  closeAdvertisementDrawer,
  openCreateDrawer,
  openEditDrawer,
  createDraft,
  loadAdvertisements,
  goToPage,
  handleDeleteAdvertisement,
  updateAd,
} = useAdvertisementManagement();

let refreshTimer;
onMounted(() => {
  loadAdvertisements();
  loadDashboardSummary();

  refreshTimer = window.setInterval(() => {
    loadAdvertisements(currentPage.value, true);
    loadDashboardSummary();
  }, 60_000);
});
onUnmounted(() => {
  window.clearInterval(refreshTimer);
});
</script>

<template>
  <section class="ad-admin-shell">
    <main class="ad-workspace">
      <header class="workspace-header admin-page-heading">
        <div>
          <div class="eyebrow admin-page-eyebrow">SNEAKERMARKET ADMIN</div>
          <h1>廣告管理</h1>
          <p class="admin-page-description">建立、排程並追蹤平台上的每一則廣告活動。</p>
        </div>

        <div class="header-actions">
          <RouterLink
            class="btn preview-button d-inline-flex align-items-center justify-content-center"
            to="/ads/preview"
            >檢視廣告</RouterLink
          >
          <button class="create-button" @click="openCreateDrawer">
            <span>＋</span> 新增廣告
          </button>
        </div>
      </header>

      <div class="metric-grid">
        <article>
          <i class="coral">↗</i>
          <div>
            <span>目前投放中</span>
            <strong>
              {{
                dashboardSummary.activeAdvertisements.toLocaleString("zh-TW")
              }}
              <small>則</small>
            </strong>
          </div>
        </article>

        <article>
          <i class="blue">◫</i>
          <div>
            <span>本月曝光次數</span>
            <strong>
              <!--顯示完整類似會計數字-->
              {{ dashboardSummary.views.toLocaleString("zh-TW") }}
            </strong>
          </div>
        </article>

        <article>
          <i class="violet">⌁</i>
          <div>
            <span>本月點擊次數</span>
            <strong>
              <!--顯示完整類似會計數字-->
              {{ dashboardSummary.clicks.toLocaleString("zh-TW") }}
            </strong>
          </div>
        </article>

        <article>
          <i class="mint">◎</i>
          <div>
            <span>本月點擊率</span>
            <!--顯示小數-->
            <strong> {{ Number(dashboardSummary.ctr).toFixed(2) }}% </strong>
          </div>
        </article>
      </div>

      <div class="campaign-management-panel">
        <AdvertisementFilterBar
          v-model:active-tab="activeTab"
          v-model:keyword="keyword"
          v-model:placement="placement"
          :tabs="tabs"
        />

        <!-- 查詢 API 尚未完成時顯示載入狀態 -->
        <div
          v-if="loadingAds && filteredAds.length === 0"
          class="py-5 text-center text-secondary"
        >
          <span
            class="spinner-border spinner-border-sm me-2"
            aria-hidden="true"
          ></span>
          廣告資料載入中...
        </div>

        <!-- 查詢 API 發生錯誤時顯示錯誤訊息 -->
        <div v-else-if="loadError" class="alert alert-danger m-3" role="alert">
          <div class="d-flex justify-content-between align-items-center">
            <span>{{ loadError }}</span>

            <!-- 讓管理員可以手動重新查詢 -->
            <button
              type="button"
              class="btn btn-sm btn-outline-danger"
              @click="loadAdvertisements"
            >
              重新載入
            </button>
          </div>
        </div>

        <template v-else>
          <AdvertisementTable
            :ads="filteredAds"
            @edit="openEditDrawer"
            @toggle="toggle"
            @delete="handleDeleteAdvertisement"
          />

          <AdvertisementPagination
            :current-page="currentPage"
            :total-pages="totalPages"
            :total-elements="totalElements"
            :page-numbers="pageNumbers"
            :first-item-number="firstItemNumber"
            :last-item-number="lastItemNumber"
            :loading="loadingAds"
            @change-page="goToPage"
          />
        </template>
      </div>
    </main>

    <AdvertisementFormDrawer
      :show="showCreate"
      :form="form"
      :is-editing="isEditing"
      :current-image-rule="currentImageRule"
      :is-uploading-image="isUploadingImage"
      :minimum-start-at="minimumStartAt"
      :is-start-at-locked="isStartAtLocked"
      :toast="toast"
      :toast-type="toastType"
      @close="closeAdvertisementDrawer"
      @submit="isEditing ? updateAd() : createDraft()"
      @image-change="handleImageUpload"
      @placement-change="handlePlacementChange"
    />

    <Transition name="toast">
      <div
        v-if="toast && !showCreate"
        class="admin-toast"
        :class="toastType"
        role="status"
      >
        {{ toastType === "success" ? "✓" : "X" }} {{ toast }}
      </div>
    </Transition>
  </section>
</template>

<style scoped src="./AdvertisementHomeView.css"></style>
