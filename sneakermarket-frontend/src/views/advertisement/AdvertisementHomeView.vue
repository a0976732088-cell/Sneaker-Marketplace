<script setup>
import { computed, ref, reactive, onMounted } from "vue";
import { VueDatePicker } from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import { zhTW } from "date-fns/locale";
import {
  createAdvertisement,
  getAllAdvertisements,
} from "@/service/advertisementService";

const tabs = ["全部廣告", "進行中", "草稿", "已停用", "已結束"];
const activeTab = ref("全部廣告");
const keyword = ref("");
const placement = ref("全部版位");
const showCreate = ref(false);
const toast = ref("");

const ads = ref([]);
const loadingAds = ref(false);
const loadError = ref("");

const typeLabels = {
  HOT_PRODUCT: "熱門商品",
  NEW_PRODUCT: "新品推薦",
  AUCTION: "競標活動",
  DISCOUNT: "折扣優惠",
};
const placementLabels = {
  BANNER: "首頁橫幅",
  POPUP: "首頁彈窗",
  SMALL_BLOCK: "商品頁側欄",
};
const statusLabels = {
  DRAFT: "草稿",
  ACTIVE: "進行中",
  INACTIVE: "已停用",
  EXPIRED: "已結束",
};

const filteredAds = computed(() =>
  ads.value.filter((ad) => {
    const term = keyword.value.trim().toLowerCase();
    return (
      (activeTab.value === "全部廣告" || ad.status === activeTab.value) &&
      (placement.value === "全部版位" || ad.place === placement.value) &&
      (!term || `${ad.name} ${ad.id} ${ad.desc}`.toLowerCase().includes(term))
    );
  }),
);

const form = reactive({
  // 未來登入功能完成後應由登入者資料取得
  createdBy: 1,
  adName: "",
  imageUrl: "",
  adDescription: "",
  adType: "NEW_PRODUCT",
  adPlacement: "BANNER",
  startAt: "",
  endAt: "",
  targetType: "URL",
  targetId: null,
  targetUrl: "",
  sortOrder: 0,
});

function resetForm() {
  Object.assign(form, {
    createdBy: 1,
    adName: "",
    imageUrl: "",
    adDescription: "",
    adType: "NEW_PRODUCT",
    adPlacement: "BANNER",
    startAt: "",
    endAt: "",
    targetType: "URL",
    targetId: null,
    targetUrl: "",
    sortOrder: 0,
  });
}

function notify(message) {
  let timer = null;
  toast.value = message;

  if (timer) clearTimeout(timer);

  timer = window.setTimeout(() => {
    toast.value = "";
  }, 2400);
}

function toggle(ad) {
  ad.status = ad.status === "進行中" ? "草稿" : "進行中";
  notify(`「${ad.name}」已${ad.status === "進行中" ? "開始投放" : "暫停投放"}`);
}

async function createDraft() {
  const payload = {
    createdBy: form.createdBy,
    adName: form.adName,
    imageUrl: form.imageUrl,
    adDescription: form.adDescription || null,
    adType: form.adType,
    adPlacement: form.adPlacement,
    startAt: form.startAt || null,
    endAt: form.endAt || null,
    targetType: form.targetType,
    targetId: form.targetType === "URL" ? null : form.targetId || null,
    targetUrl: form.targetType === "URL" ? form.targetUrl || null : null,
    sortOrder: form.sortOrder,
  };
  try {
    const result = await createAdvertisement(payload);

    resetForm();
    showCreate.value = false;

    notify(result.message);

    await loadAdvertisements();

    console.log("新增成功");
  } catch (error) {
    console.error("新增失敗");

    notify(error.response?.data?.message || "請確認後端與資料庫");
  }
}

function formatDateTime(value) {
  if (!value) return "尚未設定";

  return new Intl.DateTimeFormat("zh-TW", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function toAdvertisementViewModel(ad) {
  return {
    id: ad.adId,
    name: ad.adName,
    desc: ad.adDescription || "",
    imageUrl: ad.imageUrl,
    // 圖片載入失敗時切換為「尚無圖片」提示
    imageLoadFailed: false,

    type: typeLabels[ad.adType] || ad.adType,
    place: placementLabels[ad.adPlacement] || ad.adPlacement,
    status: statusLabels[ad.adStatus] || ad.adStatus,

    period:
      ad.startAt || ad.endAt
        ? `${formatDateTime(ad.startAt)} — ${formatDateTime(ad.endAt)}`
        : "尚未設定",

    targetType: ad.targetType,
    targetId: ad.targetId,
    targetUrl: ad.targetUrl,

    // 後端目前沒有成效統計欄位
    views: "—",
    clicks: "—",
    ctr: "—",
    color: "blue",
  };
}

async function loadAdvertisements() {
  loadingAds.value = true;
  loadError.value = "";

  try {
    const result = await getAllAdvertisements();

    ads.value = (result.data || []).map(toAdvertisementViewModel);
  } catch (error) {
    console.error("查詢廣告失敗");

    loadError.value = "廣告列表載入失敗";
  } finally {
    loadingAds.value = false;
  }
}

onMounted(() => {
  loadAdvertisements();
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
          <button class="create-button" @click="showCreate = true">
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
            {{ tab }}<span v-if="tab === '全部廣告'">{{ ads.length }}</span>
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
                      <!-- 左側廣告預覽依照原始圖片比例顯示，避免裁切內容 -->
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
                      <button
                        title="編輯"
                        @click="notify(`正在開啟「${ad.name}」`)"
                      >
                        ✎</button
                      ><button
                        v-if="['進行中', '草稿'].includes(ad.status)"
                        :title="ad.status === '進行中' ? '暫停' : '上架'"
                        @click="toggle(ad)"
                      >
                        {{ ad.status === "進行中" ? "Ⅱ" : "▶" }}</button
                      ><button title="更多" @click="notify('已開啟更多操作')">
                        •••
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
            <span
              >顯示 1–{{ filteredAds.length }}，共
              {{ filteredAds.length }} 筆</span
            >
            <div>
              <button disabled>‹</button><button class="current">1</button
              ><button disabled>›</button>
            </div>
          </footer>
        </template>
      </div>
    </main>

    <div
      v-if="showCreate"
      class="drawer-backdrop"
      @click.self="showCreate = false"
    >
      <form class="create-drawer" @submit.prevent="createDraft">
        <header>
          <div>
            <span>NEW CAMPAIGN</span>
            <h2>建立新廣告</h2>
            <p>先建立草稿，稍後可補上圖片與導向設定。</p>
          </div>
          <button type="button" aria-label="關閉" @click="showCreate = false">
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
                :formats="{ input: 'yyyy/MM/dd HH:mm' }"
                :time-config="{ is24: true, timePickerInline: true }"
                placeholder="選擇日期與時間"
              />
            </label>
          </div>
        </div>
        <footer>
          <button type="button" @click="showCreate = false">取消</button
          ><button class="submit">建立草稿</button>
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

<style scoped>
.ad-admin-shell {
  --ink: #111827;
  --muted: #718096;
  --line: #e8ebf0;
  --coral: #ff5c4d;
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  min-height: calc(100vh - 128px);
  /* 抵銷外層 container 的上下間距，讓廣告版面銜接導覽列與頁尾。 */
  margin: -1.5rem calc((100vw - 100%) / -2);
  margin-bottom: -1.5rem;
  background: #f6f7f9;
  color: var(--ink);
  font-family: Inter, "Noto Sans TC", system-ui, sans-serif;
}
.admin-sidebar {
  display: flex;
  flex-direction: column;
  padding: 28px 18px 22px;
  color: #fff;
  background: #111827;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px 28px;
  border-bottom: 1px solid #273142;
}
.brand-mark {
  display: grid;
  width: 39px;
  height: 39px;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: var(--coral);
  font-size: 12px;
  font-weight: 900;
  font-style: italic;
  transform: skew(-7deg);
}
.sidebar-brand strong {
  display: block;
  font-size: 13px;
  line-height: 1.05;
  letter-spacing: 0.12em;
}
.sidebar-brand small {
  display: block;
  margin-top: 5px;
  color: #7e8ba2;
  font-size: 8px;
  letter-spacing: 0.16em;
}
.sidebar-nav {
  flex: 1;
  padding-top: 20px;
}
.sidebar-nav p {
  margin: 18px 10px 7px;
  color: #667085;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.sidebar-nav a {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  margin: 3px 0;
  border-radius: 7px;
  color: #9ca7b9;
  font-size: 13px;
  font-weight: 600;
  transition: 0.18s;
}
.sidebar-nav a:hover {
  color: #fff;
  background: #1d2737;
}
.sidebar-nav a.active {
  color: #fff;
  background: #273142;
  box-shadow: inset 3px 0 var(--coral);
}
.sidebar-nav i {
  display: inline-grid;
  width: 19px;
  place-items: center;
  color: #8b98ab;
  font-size: 17px;
  font-style: normal;
}
.sidebar-nav .active i {
  color: var(--coral);
}
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 16px 6px 0;
  border-top: 1px solid #273142;
}
.avatar {
  display: grid;
  width: 35px;
  height: 35px;
  place-items: center;
  flex: 0 0 35px;
  border-radius: 50%;
  background: #ffdbb5;
  color: #823f13;
  font-size: 12px;
  font-weight: 800;
}
.sidebar-user strong,
.sidebar-user small {
  display: block;
}
.sidebar-user strong {
  font-size: 11px;
}
.sidebar-user small {
  margin-top: 2px;
  color: #748197;
  font-size: 9px;
}
.sidebar-user button {
  margin-left: auto;
  border: 0;
  color: #778397;
  background: none;
}
.ad-workspace {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 32px clamp(24px, 3vw, 48px) 46px;
}
.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 26px;
}
.eyebrow {
  margin-bottom: 7px;
  color: var(--coral);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.2em;
}
.workspace-header h1 {
  margin: 0;
  font-size: clamp(27px, 3vw, 36px);
  font-weight: 800;
  letter-spacing: -0.045em;
}
.workspace-header p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.bell {
  position: relative;
  width: 42px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  font-size: 20px;
}
.bell b {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 6px;
  height: 6px;
  border: 1px solid #fff;
  border-radius: 50%;
  background: var(--coral);
}
.create-button {
  padding: 11px 17px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: var(--coral);
  box-shadow: 0 8px 20px #ff5c4d33;
  font-size: 12px;
  font-weight: 800;
}
.create-button span {
  font-size: 17px;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 19px;
}
.metric-grid article {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 15px #11182708;
}
.metric-grid i {
  display: grid;
  width: 39px;
  height: 39px;
  flex: 0 0 39px;
  place-items: center;
  border-radius: 9px;
  font-size: 18px;
  font-style: normal;
}
.metric-grid i.coral {
  color: #ef5549;
  background: #fff0ee;
}
.metric-grid i.blue {
  color: #3975e9;
  background: #edf4ff;
}
.metric-grid i.violet {
  color: #8557df;
  background: #f2edff;
}
.metric-grid i.mint {
  color: #109f79;
  background: #e6f8f3;
}
.metric-grid span {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
  font-size: 10px;
  font-weight: 600;
}
.metric-grid strong {
  display: block;
  font-size: 23px;
  line-height: 1;
  letter-spacing: -0.03em;
}
.metric-grid strong small {
  display: inline;
  color: var(--muted);
  font-size: 10px;
}
.metric-grid em {
  position: absolute;
  top: 18px;
  right: 14px;
  color: #19a778;
  font-size: 8px;
  font-style: normal;
  font-weight: 700;
}
.ad-panel {
  /* 廣告列表填滿工作區剩餘高度，資料較少時版面也不會縮在上方。 */
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fff;
  box-shadow: 0 4px 18px #11182708;
}
.panel-tabs {
  display: flex;
  gap: 23px;
  padding: 0 21px;
  border-bottom: 1px solid var(--line);
}
.panel-tabs button {
  position: relative;
  padding: 17px 1px 13px;
  border: 0;
  color: #8490a1;
  background: none;
  font-size: 11px;
  font-weight: 700;
}
.panel-tabs button.active {
  color: var(--ink);
}
.panel-tabs button.active:after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: var(--coral);
  content: "";
}
.panel-tabs button span {
  display: inline-grid;
  min-width: 17px;
  height: 17px;
  margin-left: 4px;
  place-items: center;
  border-radius: 8px;
  background: #f0f2f5;
  color: #7d8797;
  font-size: 8px;
}
.filter-row {
  display: flex;
  gap: 9px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}
.search-box {
  display: flex;
  align-items: center;
  width: min(310px, 100%);
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}
.search-box span {
  padding-left: 11px;
  color: #97a1b0;
  font-size: 18px;
}
.search-box input {
  width: 100%;
  padding: 9px;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 11px;
}
.filter-row select,
.outline-button {
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 7px;
  color: #536071;
  background: #fff;
  font-size: 10px;
  font-weight: 600;
}
.outline-button {
  margin-left: auto;
  min-height: 36px;
}
.table-wrap {
  flex: 1;
  overflow-x: auto;
}
table {
  width: 100%;
  min-width: 1100px;
  /* 搭配 colgroup 固定欄寬，避免內容長度改變時重新排版。 */
  table-layout: fixed;
  border-collapse: collapse;
}
.column-ad {
  width: 31%;
}
.column-placement {
  width: 13%;
}
.column-period {
  width: 18%;
}
.column-status {
  width: 10%;
}
.column-number {
  width: 7%;
}
.column-ctr {
  width: 6%;
}
.column-actions {
  width: 8%;
}
th {
  padding: 11px 16px;
  color: #8993a2;
  background: #fafbfc;
  font-size: 8px;
  letter-spacing: 0.08em;
  text-align: left;
  text-transform: uppercase;
}
td {
  padding: 13px 16px;
  border-top: 1px solid #f0f1f4;
  color: #4f5c6d;
  font-size: 10px;
  vertical-align: middle;
}
tbody tr:hover {
  background: #fcfcfd;
}
.number {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.ad-identity {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 310px;
}
.ad-thumbnail {
  position: relative;
  display: grid;
  /* 只固定寬度，高度交給圖片依原始比例自動撐開。 */
  width: 160px;
  height: auto;
  flex: 0 0 160px;
  place-items: center;
  overflow: visible;
  border-radius: 6px;
  color: #fff;
}
.ad-thumbnail:after {
  position: absolute;
  width: 70px;
  height: 70px;
  border: 1px solid #ffffff40;
  border-radius: 50%;
  content: "";
}
.ad-thumbnail.mint {
  background: linear-gradient(135deg, #0e554b, #5ad2b5);
}
.ad-thumbnail.orange {
  background: linear-gradient(135deg, #b5411a, #ffb04c);
}
.ad-thumbnail.violet {
  background: linear-gradient(135deg, #34204f, #a26fde);
}
.ad-thumbnail.blue {
  background: linear-gradient(135deg, #175b93, #6cc6ed);
}
.ad-thumbnail.pink {
  background: linear-gradient(135deg, #b32755, #ff8f98);
}
.ad-thumbnail > span {
  z-index: 1;
  font-size: 25px;
  font-weight: 900;
  transform: rotate(-10deg);
}
.ad-thumbnail > small {
  position: absolute;
  right: 4px;
  bottom: 3px;
  z-index: 2;
  font-size: 5px;
  font-weight: 800;
}
.ad-identity strong,
.ad-identity small,
.ad-identity em,
.cell-main,
.cell-sub {
  display: block;
}
.ad-identity strong {
  margin-bottom: 3px;
  color: var(--ink);
  font-size: 11px;
}
.ad-identity small {
  color: #7a8595;
  font-size: 8px;
}
.ad-identity em {
  margin-top: 3px;
  color: #adb5c0;
  font-size: 7px;
  font-style: normal;
}
.cell-main {
  color: #3e4a59;
  font-size: 9px;
}
.cell-sub {
  margin-top: 3px;
  color: #939cab;
  font-size: 8px;
}
.period {
  white-space: nowrap;
  font-size: 8px;
}
.status-badge {
  display: inline-block;
  /* 每種狀態使用相同寬度，從草稿切換到進行中也不會推動欄位。 */
  min-width: 76px;
  padding: 5px 7px;
  box-sizing: border-box;
  border-radius: 12px;
  white-space: nowrap;
  text-align: center;
  font-size: 8px;
  font-weight: 700;
}
.status-badge.進行中 {
  color: #168867;
  background: #e8f8f2;
}
.status-badge.已排程 {
  color: #376fd2;
  background: #edf4ff;
}
.status-badge.草稿 {
  color: #687386;
  background: #f0f2f5;
}
.status-badge.已結束 {
  color: #8d94a0;
  background: #f5f5f6;
}
.row-actions {
  display: flex;
  width: 85px;
  gap: 2px;
  justify-content: flex-end;
}
.row-actions button {
  width: 27px;
  height: 27px;
  border: 0;
  border-radius: 5px;
  color: #7a8493;
  background: transparent;
  font-size: 11px;
}
.row-actions button:hover {
  color: var(--coral);
  background: #fff0ee;
}
.empty-state {
  padding: 44px;
  color: #8c96a5;
  text-align: center;
}
.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid var(--line);
  color: #9099a7;
  font-size: 9px;
}
.panel-footer div {
  display: flex;
  gap: 4px;
}
.panel-footer button {
  width: 25px;
  height: 25px;
  border: 1px solid var(--line);
  border-radius: 5px;
  color: #788394;
  background: #fff;
  font-size: 10px;
}
.panel-footer button.current {
  border-color: var(--coral);
  color: #fff;
  background: var(--coral);
}
.drawer-backdrop {
  position: fixed;
  z-index: 1090;
  inset: 0;
  display: flex;
  justify-content: flex-end;
  background: #0c121c7a;
  backdrop-filter: blur(2px);
}
.create-drawer {
  display: flex;
  flex-direction: column;
  width: min(460px, 94vw);
  height: 100%;
  background: #fff;
  box-shadow: -20px 0 50px #00000029;
  animation: slide-in 0.25s ease-out;
}
.create-drawer header {
  display: flex;
  justify-content: space-between;
  padding: 30px 30px 22px;
  border-bottom: 1px solid var(--line);
}
.create-drawer header span {
  color: var(--coral);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.18em;
}
.create-drawer h2 {
  margin: 6px 0 5px;
  font-size: 25px;
  font-weight: 800;
}
.create-drawer header p {
  margin: 0;
  color: var(--muted);
  font-size: 11px;
}
.create-drawer header button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: #f4f5f7;
  font-size: 21px;
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 25px 30px;
}
.drawer-body label {
  display: block;
  margin-bottom: 17px;
  color: #495567;
  font-size: 10px;
  font-weight: 700;
}
.drawer-body input,
.drawer-body select,
.drawer-body textarea {
  display: block;
  width: 100%;
  margin-top: 7px;
  padding: 11px 12px;
  border: 1px solid #dfe3e9;
  border-radius: 7px;
  outline: none;
  color: var(--ink);
  background: #fff;
  font: inherit;
  font-weight: 500;
}
.drawer-body input:focus,
.drawer-body select:focus,
.drawer-body textarea:focus {
  border-color: var(--coral);
  box-shadow: 0 0 0 3px #ff5c4d1a;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.create-drawer footer {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  padding: 18px 30px;
  border-top: 1px solid var(--line);
}
.create-drawer footer button {
  padding: 10px 17px;
  border: 1px solid var(--line);
  border-radius: 7px;
  color: #596577;
  background: #fff;
  font-size: 10px;
  font-weight: 700;
}
.create-drawer footer .submit {
  border-color: var(--coral);
  color: #fff;
  background: var(--coral);
}
.admin-toast {
  position: fixed;
  z-index: 1100;
  right: 24px;
  bottom: 24px;
  padding: 12px 17px;
  border-radius: 8px;
  color: #fff;
  background: #111827;
  box-shadow: 0 10px 30px #0003;
  font-size: 11px;
  font-weight: 700;
}
.toast-enter-active,
.toast-leave-active {
  transition: 0.2s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
@keyframes slide-in {
  from {
    transform: translateX(100%);
  }
}
@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .ad-admin-shell {
    grid-template-columns: 190px minmax(0, 1fr);
  }
}
@media (max-width: 760px) {
  .ad-admin-shell {
    display: block;
    margin: -1.5rem -12px;
  }
  .admin-sidebar {
    display: none;
  }
  .ad-workspace {
    padding: 24px 15px 36px;
  }
  .workspace-header {
    align-items: flex-end;
  }
  .workspace-header p,
  .eyebrow,
  .bell {
    display: none;
  }
  .metric-grid {
    grid-template-columns: 1fr 1fr;
    gap: 9px;
  }
  .metric-grid article {
    padding: 13px;
  }
  .metric-grid i {
    display: none;
  }
  .metric-grid strong {
    font-size: 20px;
  }
  .metric-grid em {
    top: 13px;
  }
  .panel-tabs {
    gap: 17px;
    overflow-x: auto;
  }
  .panel-tabs button {
    white-space: nowrap;
  }
  .filter-row {
    flex-wrap: wrap;
  }
  .search-box {
    width: 100%;
  }
  .filter-row select {
    min-height: 36px;
  }
  .outline-button {
    margin-left: 0;
  }
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
/* 保留網址圖片的完整比例，不以固定高度裁切上下或左右內容。 */
.ad-thumbnail-image {
  display: block;
  width: 100%;
  height: auto;
  object-fit: contain;
  object-position: center;
  border-radius: 6px;
}

/* 沒有圖片或圖片網址失效時的預設狀態 */
.ad-thumbnail-empty {
  display: grid;
  width: 100%;
  min-height: 90px;
  place-items: center;
  border-radius: 6px;
  color: #7b8492;
  background: #eef0f3;
  font-size: 8px;
}

/* 移除舊假縮圖使用的圓形裝飾，避免蓋住真實圖片 */
.ad-thumbnail::after {
  display: none;
}

/* 側欄已隱藏，廣告管理主內容改為單欄滿寬 */
.ad-admin-shell {
  grid-template-columns: minmax(0, 1fr);
}

.ad-workspace {
  width: 100%;
}

/*
 * 廣告管理頁字體尺寸統一放大。
 * 集中放在樣式最後方，方便日後直接從這一區調整整頁文字大小。
 */
.eyebrow,
.metric-grid em,
.panel-tabs button span,
th,
.ad-identity small,
.ad-identity em,
.cell-sub,
.period,
.status-badge,
.create-drawer header span,
.ad-thumbnail-empty {
  font-size: 12px;
}

.workspace-header p,
.create-button,
.metric-grid article div > span,
.metric-grid strong small,
.panel-tabs button,
.search-box input,
.filter-row select,
.outline-button,
td,
.cell-main,
.row-actions button,
.panel-footer,
.panel-footer button,
.create-drawer p,
.drawer-body label,
.secondary-button,
.primary-button,
.admin-toast {
  font-size: 14px;
}

.ad-identity strong {
  font-size: 15px;
}

.metric-grid strong {
  font-size: 27px;
}

.workspace-header h1 {
  font-size: clamp(32px, 3vw, 42px);
}

.create-drawer h2 {
  font-size: 29px;
}
</style>
