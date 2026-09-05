<script setup>
import { onMounted, onUnmounted } from "vue";
import { VueDatePicker } from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import { zhTW } from "date-fns/locale";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { faTrash, faPencil } from "@fortawesome/free-solid-svg-icons";
import { useAdvertisementManagement } from "./useAdvertisementManagement";

const {
  tabs,
  activeTab,
  keyword,
  placement,
  showCreate,
  toast,
  ads,
  loadingAds,
  loadError,
  currentPage,
  totalPages,
  totalElements,
  allAdvertisementsCount,
  pageNumbers,
  firstItemNumber,
  lastItemNumber,
  filteredAds,
  form,
  timeError,
  isEditing,
  notify,
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
  refreshTimer = window.setInterval(() => {
    loadAdvertisements(currentPage.value, true);
  }, 60_000);
});
onUnmounted(() => {
  window.clearInterval(refreshTimer);
});
</script>

<template>
  <section class="ad-admin-shell">
    <!-- 管理員資訊區塊暫時隱藏 
      <div class="sidebar-user"><div class="avatar">陳</div></div>
     -->

    <main class="ad-workspace">
      <header class="workspace-header">
        <div>
          <div class="eyebrow">ADVERTISEMENT CENTER</div>
          <h1>廣告管理</h1>
          <p>建立、排程並追蹤平台上的每一則廣告活動。</p>
        </div>
        <div class="header-actions">
          <button class="create-button" @click="openCreateDrawer">
            <span>＋</span> 新增廣告
          </button>
        </div>
      </header>

      <div class="metric-grid">
        <article>
          <i class="coral">↗</i>
          <div>
            <span>目前投放中</span><strong>2 <small>則</small></strong>
          </div>
          <em>較上週 +1</em>
        </article>
        <article>
          <i class="blue">◫</i>
          <div><span>本月曝光次數</span><strong>418.2K</strong></div>
          <em>↑ 12.8%</em>
        </article>
        <article>
          <i class="violet">⌁</i>
          <div><span>本月點擊次數</span><strong>18,742</strong></div>
          <em>↑ 8.3%</em>
        </article>
        <article>
          <i class="mint">◎</i>
          <div><span>平均點擊率</span><strong>4.48%</strong></div>
          <em>↑ 0.6%</em>
        </article>
      </div>

      <div class="ad-panel">
        <div class="panel-tabs" role="tablist">
          <button
            v-for="tab in tabs"
            :key="tab"
            :class="{ active: activeTab === tab }"
            @click="activeTab = tab"
          >
            {{ tab
            }}<span v-if="tab === '全部廣告'">{{
              allAdvertisementsCount
            }}</span>
          </button>
        </div>
        <div class="filter-row">
          <label class="search-box"
            ><span>⌕</span
            ><input
              v-model="keyword"
              type="search"
              placeholder="搜尋廣告名稱或編號..."
          /></label>
          <select v-model="placement" aria-label="篩選廣告版位">
            <option>全部版位</option>
            <option>首頁橫幅</option>
            <option>首頁彈窗</option>
            <option>商品頁側欄</option>
          </select>

          <!-- 未來有機會可以匯出到excel檔案 -->
          <button class="outline-button" @click="notify('報表已匯出')">
            ⇩ 匯出報表
          </button>
        </div>

        <!-- 查詢 API 尚未完成時顯示載入狀態 -->
        <div v-if="loadingAds" class="py-5 text-center text-secondary">
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
          <div class="table-wrap">
            <table>
              <!-- 固定各欄位比例，狀態或資料筆數改變時表格不會左右跳動。 -->
              <colgroup>
                <col class="column-ad" />
                <col class="column-placement" />
                <col class="column-period" />
                <col class="column-status" />
                <col class="column-number" />
                <col class="column-number" />
                <col class="column-ctr" />
                <col class="column-actions" />
              </colgroup>
              <thead>
                <tr>
                  <th>廣告內容</th>
                  <th>版位／類型</th>
                  <th>投放期間</th>
                  <th>狀態</th>
                  <th class="number">曝光</th>
                  <th class="number">點擊</th>
                  <th class="number">CTR</th>
                  <th><span class="visually-hidden">操作</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="ad in filteredAds" :key="ad.id">
                  <td>
                    <div class="ad-identity">
                      <!-- 有圖與無圖皆使用固定大小；圖片完整顯示、不裁切 -->
                      <div class="ad-thumbnail">
                        <!-- 有網址且圖片載入正常時顯示 -->
                        <img
                          v-if="ad.imageUrl && !ad.imageLoadFailed"
                          class="ad-thumbnail-image"
                          :src="ad.imageUrl"
                          :alt="ad.name"
                          loading="lazy"
                          @error="ad.imageLoadFailed = true"
                        />

                        <!-- 沒網址或圖片載入失敗時顯示 -->
                        <div v-else class="ad-thumbnail-empty">尚無圖片</div>
                      </div>
                      <div>
                        <strong>{{ ad.name }}</strong
                        ><small>{{ ad.desc }}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <strong class="cell-main">{{ ad.place }}</strong
                    ><small class="cell-sub">{{ ad.type }}</small>
                  </td>
                  <td>
                    <span class="period">{{ ad.period }}</span>
                  </td>
                  <td>
                    <span class="status-badge" :class="ad.status"
                      >● {{ ad.status }}</span
                    >
                  </td>
                  <td class="number">{{ ad.views }}</td>
                  <td class="number">{{ ad.clicks }}</td>
                  <td class="number">
                    <strong>{{ ad.ctr }}</strong>
                  </td>
                  <td>
                    <div class="row-actions">
                      <button title="編輯" @click="openEditDrawer(ad)">
                        <FontAwesomeIcon :icon="faPencil" />
                      </button>
                      <!--草稿與已停用顯示上架按鈕，進行中顯示停用按鈕，已結束不允許操作，所以不顯示按鈕。-->
                      <button
                        v-if="['進行中', '草稿', '已停用'].includes(ad.status)"
                        type="button"
                        :title="ad.status === '進行中' ? '停用' : '上架'"
                        @click="toggle(ad)"
                      >
                        {{ ad.status === "進行中" ? "Ⅱ" : "▶" }}
                      </button>
                      <button
                        v-if="ad.status !== '已結束'"
                        title="刪除"
                        @click="handleDeleteAdvertisement(ad)"
                      >
                        <FontAwesomeIcon :icon="faTrash" />
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!filteredAds.length">
                  <td colspan="8" class="empty-state">
                    找不到符合條件的廣告，請調整搜尋或篩選條件。
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <footer class="panel-footer">
            <span>
              顯示 {{ firstItemNumber }}–{{ lastItemNumber }}，共
              {{ totalElements }} 筆
            </span>
            <div>
              <!-- 後端頁碼從 0 開始，所以顯示時加 1。 -->
              <button
                type="button"
                aria-label="上一頁"
                :disabled="currentPage === 0 || loadingAds"
                @click="goToPage(currentPage - 1)"
              >
                ‹
              </button>
              <button
                v-for="pageNumber in pageNumbers"
                :key="pageNumber"
                type="button"
                :class="{ current: currentPage === pageNumber }"
                :disabled="loadingAds"
                @click="goToPage(pageNumber)"
              >
                {{ pageNumber + 1 }}
              </button>
              <button
                type="button"
                aria-label="下一頁"
                :disabled="
                  totalPages === 0 ||
                  currentPage >= totalPages - 1 ||
                  loadingAds
                "
                @click="goToPage(currentPage + 1)"
              >
                ›
              </button>
            </div>
          </footer>
        </template>
      </div>
    </main>

    <div
      v-if="showCreate"
      class="drawer-backdrop"
      @click.self="closeAdvertisementDrawer"
    >
      <form
        class="create-drawer"
        @submit.prevent="isEditing ? updateAd() : createDraft()"
      >
        <header>
          <div>
            <span>
              {{ isEditing ? "EDIT CAMPAIGN" : "NEW CAMPAIGN" }}
            </span>

            <h2>
              {{ isEditing ? "編輯廣告" : "建立新廣告" }}
            </h2>

            <p>
              {{
                isEditing
                  ? "修改目前這則廣告的內容與設定。"
                  : "先建立草稿，稍後可補上圖片與導向設定。"
              }}
            </p>
          </div>
          <button
            type="button"
            aria-label="關閉"
            @click="closeAdvertisementDrawer"
          >
            ×
          </button>
        </header>
        <div class="drawer-body">
          <label
            >廣告名稱
            <input
              v-model.trim="form.adName"
              required
              placeholder="例如：九月新品上市"
          /></label>
          <div class="form-grid">
            <label
              >廣告類型<select v-model="form.adType" required>
                <option value="NEW_PRODUCT">新品推薦</option>
                <option value="HOT_PRODUCT">熱門商品</option>
                <option value="AUCTION">競標活動</option>
                <option value="DISCOUNT">折扣優惠</option>
              </select></label
            >
            <label
              >顯示版位<select v-model="form.adPlacement">
                <option value="BANNER">首頁橫幅</option>
                <option value="POPUP">首頁彈窗</option>
                <option value="SMALL_BLOCK">商品頁側欄</option>
              </select></label
            >
          </div>
          <label
            >圖片網址<input
              v-model="form.imageUrl"
              required
              type="url"
              placeholder="https://example.com/banner.jpg"
          /></label>
          <div class="form-grid">
            <label>
              導向類型
              <select v-model="form.targetType" required>
                <option value="PRODUCT">一般商品</option>
                <option value="AUCTION">競標商品</option>
                <option value="COUPON">優惠券</option>
                <option value="URL">外部網址</option>
              </select>
            </label>

            <label v-if="form.targetType === 'URL'">
              導向網址
              <input
                v-model.trim="form.targetUrl"
                type="url"
                maxlength="500"
                required
                placeholder="https://example.com/activity"
              />
            </label>

            <!-- 這段不是必須的，測試功能所使用正式會需要依賴其他組員API -->
            <label v-else>
              導向目標編號
              <input
                v-model.number="form.targetId"
                type="number"
                min="1"
                required
                placeholder="請輸入目標 ID"
              />
            </label>
          </div>
          <label
            >廣告說明<textarea
              v-model="form.adDescription"
              rows="3"
              placeholder="輸入活動簡介..."
            ></textarea>
          </label>
          <div class="form-grid">
            <label>
              開始時間
              <VueDatePicker
                v-model="form.startAt"
                model-type="yyyy-MM-dd'T'HH:mm"
                :locale="zhTW"
                :teleport="true"
                six-weeks
                :formats="{ input: 'yyyy/MM/dd HH:mm' }"
                :time-config="{ is24: true, timePickerInline: true }"
                placeholder="選擇日期與時間"
              />
            </label>

            <label>
              結束時間
              <VueDatePicker
                v-model="form.endAt"
                model-type="yyyy-MM-dd'T'HH:mm"
                :locale="zhTW"
                :teleport="true"
                :transitions="false"
                six-weeks
                :formats="{ input: 'yyyy/MM/dd HH:mm' }"
                :time-config="{ is24: true, timePickerInline: true }"
                placeholder="選擇日期與時間"
              />
            </label>
          </div>
          <p v-if="timeError" class="form-error" role="alert">
            {{ timeError }}
          </p>
        </div>
        <footer>
          <button type="button" @click="closeAdvertisementDrawer">取消</button>
          <button class="submit">
            {{ isEditing ? "儲存修改" : "建立草稿" }}
          </button>
        </footer>
      </form>
    </div>
    <Transition name="toast"
      ><div v-if="toast" class="admin-toast" role="status">
        ✓ {{ toast }}
      </div></Transition
    >
  </section>
</template>

<style scoped src="./AdvertisementHomeView.css"></style>
