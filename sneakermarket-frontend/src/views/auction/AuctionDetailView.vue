<template>
  <section class="auction-detail-page">
    <!-- 背景同步警告獨立於主要頁面狀態，保留已載入的詳細資料。 -->
    <div
      v-if="auction && statusSyncMessage"
      class="alert alert-warning"
      role="status"
    >
      {{ statusSyncMessage }}
    </div>

    <div v-if="loading" class="detail-state" role="status">
      <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
      正在載入競標商品
    </div>

    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <div v-else-if="auction">
      <RouterLink :to="{ name: 'auction' }" class="back-link">
        ← 返回競標商城
      </RouterLink>

      <div class="detail-layout">
        <div class="gallery-column">
          <AuctionImageGallery
            :image-urls="auction.imageUrls"
            :product-name="auction.productName"
          />
        </div>

        <div class="auction-summary">
          <AuctionSummaryPanel
            :auction="auction"
            :format-condition="formatCondition"
          />
          <AuctionBidPanel
            :auction="auction"
            :candidate-bid="candidateBid"
            :submitting="submittingBid"
            :can-place-bid="canPlaceBid"
            :availability-message="bidAvailabilityMessage"
            :candidate-at-minimum="isCandidateAtMinimum"
            :success-message="bidSuccessMessage"
            :error-message="bidErrorMessage"
            @increase="increaseCandidateBid"
            @decrease="decreaseCandidateBid"
            @submit="handlePlaceBid"
          />
          <AuctionSellerActionPanel
            v-if="auction.isOwner"
            :auction="auction"
            :can-manage="canOwnerManageAuction"
            :manage-message="ownerManageMessage"
            :cancelling="cancelling"
            @cancel="handleCancelAuction"
          />
        </div>
      </div>

      <div class="detail-tabs" role="tablist" aria-label="競標詳細內容">
        <button
          id="detail-information-tab"
          type="button"
          role="tab"
          :aria-selected="activeDetailTab === 'information'"
          aria-controls="detail-information-panel"
          :tabindex="activeDetailTab === 'information' ? 0 : -1"
          @click="activeDetailTab = 'information'"
          @keydown="handleDetailTabKeydown"
        >
          商品與交易資訊
        </button>
        <button
          id="bid-history-tab"
          type="button"
          role="tab"
          :aria-selected="activeDetailTab === 'history'"
          aria-controls="bid-history-panel"
          :tabindex="activeDetailTab === 'history' ? 0 : -1"
          @click="activeDetailTab = 'history'"
          @keydown="handleDetailTabKeydown"
        >
          出價紀錄 <span class="detail-tab-count">{{ bids.length }}</span>
        </button>
      </div>

      <div
        v-show="activeDetailTab === 'information'"
        id="detail-information-panel"
        class="detail-tab-panel"
        role="tabpanel"
        aria-labelledby="detail-information-tab"
        tabindex="0"
      >
        <AuctionDetailInformation
          :auction="auction"
          :format-condition="formatCondition"
        />
      </div>

      <div
        v-show="activeDetailTab === 'history'"
        id="bid-history-panel"
        class="detail-tab-panel"
        role="tabpanel"
        aria-labelledby="bid-history-tab"
        tabindex="0"
      >
        <AuctionBidHistory :bids="bids" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { parseAuctionTime } from "@/utils/auctionTime";
import { formatTwd } from "@/utils/auctionMoney";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import AuctionSummaryPanel from "@/components/auction/detail/AuctionSummaryPanel.vue";
import AuctionBidPanel from "@/components/auction/detail/AuctionBidPanel.vue";
import AuctionSellerActionPanel from "@/components/auction/detail/AuctionSellerActionPanel.vue";
import AuctionDetailInformation from "@/components/auction/detail/AuctionDetailInformation.vue";
import AuctionBidHistory from "@/components/auction/detail/AuctionBidHistory.vue";
import AuctionImageGallery from "@/components/auction/AuctionImageGallery.vue";
import {
  handleAuctionAccessError,
  cancelAuction,
  getAuctionDetail,
  getAuctionStatus,
  getBidHistory,
  placeBid,
} from "@/service/auctionService";
import { createAuctionSocketClient } from "@/service/auctionSocketService";

const STATUS_POLL_INTERVAL_MS = 12000;
const TERMINAL_STATUSES = new Set(["SOLD", "UNSOLD", "CANCELLED", "REMOVED"]);
const REALTIME_EVENT_TYPES = new Set([
  "BID_PLACED",
  "AUCTION_STARTED",
  "AUCTION_SOLD",
  "AUCTION_UNSOLD",
  "AUCTION_CANCELLED",
  "AUCTION_REMOVED",
]);

// 取得目前路由資訊
const route = useRoute();
const router = useRouter();

// 競標詳細資料
const auction = ref(null);

// 出價紀錄
const bids = ref([]);

// 詳細內容頁籤
const activeDetailTab = ref("information");

function handleDetailTabKeydown(event) {
  const tabs = Array.from(
    event.currentTarget.parentElement.querySelectorAll('[role="tab"]'),
  );
  const currentIndex = tabs.indexOf(event.currentTarget);
  let nextIndex;

  if (event.key === "ArrowRight") nextIndex = (currentIndex + 1) % tabs.length;
  else if (event.key === "ArrowLeft") nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
  else if (event.key === "Home") nextIndex = 0;
  else if (event.key === "End") nextIndex = tabs.length - 1;
  else return;

  event.preventDefault();
  activeDetailTab.value = nextIndex === 0 ? "information" : "history";
  tabs[nextIndex].focus();
}

// 是否正在讀取資料
const loading = ref(false);

// 頁面錯誤訊息
const errorMessage = ref("");

// 是否正在執行取消競標
const cancelling = ref(false);

// 出價表單狀態
const candidateBid = ref(null);
const submittingBid = ref(false);
const bidSuccessMessage = ref("");
const bidErrorMessage = ref("");

// 背景狀態同步失敗不取代主要頁面錯誤。
const statusSyncMessage = ref("");

let statusPollingTimerId = null;
let statusRefreshPromise = null;
let statusRefreshConnectionVersion = null;
let bidHistorySyncPending = false;
let bidHistorySyncPromise = null;
let auctionSocketClient = null;
let unsubscribeAuctionSocket = null;
let socketConnected = false;
let socketConnectionVersion = 0;
let removalNavigationPending = false;
let viewUnmounted = false;

/**
 * 讀取競標詳細資料與出價紀錄。
 */
async function loadAuctionDetail() {
  loading.value = true;
  errorMessage.value = "";

  try {
    // 從 /auctions/:id 取得競標 ID
    const auctionId = route.params.id;

    // 詳細資料與出價紀錄彼此獨立，
    // 因此可同時向後端發出兩個 GET Request。
    const [detailResponse, bidsResponse] = await Promise.all([
      getAuctionDetail(auctionId),
      getBidHistory(auctionId),
    ]);

    auction.value = detailResponse.data.data;
    bids.value = bidsResponse.data.data;
    candidateBid.value = Number(auction.value.nextMinimumBid);
    statusSyncMessage.value = "";
    bidHistorySyncPending = false;

    if (TERMINAL_STATUSES.has(auction.value.status)) {
      stopStatusPolling();
    }
  } catch (error) {
    console.error("取得競標詳細資料失敗：", error);

    // auctionId 不存在時，後端目前會回 HTTP 404
    if (error.response?.status === 404) {
      errorMessage.value = "找不到指定的競標資料";
    } else {
      errorMessage.value = "無法取得競標詳細資料";
    }
  } finally {
    loading.value = false;
  }
}

/**
 * 避免較早的 REST Response 或 WebSocket Event 覆蓋畫面上的新狀態。
 */
function isOlderStatus(latestStatus) {
  if (!auction.value || !latestStatus) {
    return true;
  }

  if (
    TERMINAL_STATUSES.has(auction.value?.status) &&
    !TERMINAL_STATUSES.has(latestStatus.status)
  ) {
    return true;
  }

  const latestBidCount = Number(latestStatus.bidCount);
  const currentBidCount = Number(auction.value?.bidCount);

  if (
    Number.isFinite(latestBidCount) &&
    Number.isFinite(currentBidCount) &&
    latestBidCount < currentBidCount
  ) {
    return true;
  }

  const latestUpdatedAt = parseAuctionTime(latestStatus.updatedAt).getTime();
  const currentUpdatedAt = parseAuctionTime(auction.value?.updatedAt).getTime();

  return (
    Number.isFinite(latestUpdatedAt) &&
    Number.isFinite(currentUpdatedAt) &&
    latestUpdatedAt < currentUpdatedAt
  );
}

/**
 * 依序同步出價紀錄。同步期間若又收到新 bidCount，完成後會再抓一次。
 */
function syncBidHistory() {
  if (!bidHistorySyncPending || viewUnmounted) {
    return Promise.resolve(true);
  }

  if (bidHistorySyncPromise) {
    return bidHistorySyncPromise;
  }

  bidHistorySyncPromise = (async () => {
    while (bidHistorySyncPending && !viewUnmounted && auction.value) {
      bidHistorySyncPending = false;

      try {
        const bidsResponse = await getBidHistory(auction.value.auctionId);

        if (viewUnmounted) {
          return false;
        }

        bids.value = bidsResponse.data.data;
        bidErrorMessage.value = "";
      } catch (historyError) {
        console.error("同步出價紀錄失敗：", historyError);
        bidHistorySyncPending = true;
        const canRetryWithPolling = !TERMINAL_STATUSES.has(
          auction.value.status,
        );
        statusSyncMessage.value = canRetryWithPolling
          ? "最新競標狀態已更新，但出價紀錄暫時同步失敗，將自動重試。"
          : "最新競標狀態已更新，但出價紀錄暫時同步失敗。";

        if (canRetryWithPolling) {
          startStatusPolling();
        }
        return false;
      }
    }

    return true;
  })().finally(() => {
    bidHistorySyncPromise = null;
  });

  return bidHistorySyncPromise;
}

/**
 * REST polling 與 WebSocket 共用同一套狀態套用規則。
 */
async function applyLatestAuctionStatus(latestStatus) {
  if (viewUnmounted || isOlderStatus(latestStatus)) {
    return false;
  }

  const oldBidCount = Number(auction.value.bidCount);
  const latestBidCount = Number(latestStatus.bidCount);
  const latestMinimumBid = Number(latestStatus.nextMinimumBid);

  Object.assign(auction.value, latestStatus);

  if (
    Number.isFinite(latestMinimumBid) &&
    (!Number.isFinite(candidateBid.value) ||
      candidateBid.value < latestMinimumBid)
  ) {
    candidateBid.value = latestMinimumBid;
  }

  statusSyncMessage.value = "";

  if (
    Number.isFinite(latestBidCount) &&
    Number.isFinite(oldBidCount) &&
    latestBidCount !== oldBidCount
  ) {
    bidHistorySyncPending = true;
  }

  if (bidHistorySyncPending) {
    await syncBidHistory();
  }

  if (TERMINAL_STATUSES.has(latestStatus.status) && !bidHistorySyncPending) {
    stopStatusPolling();
  }

  return true;
}

/**
 * 重新取得競標最新狀態。共享進行中的 Promise，讓 reconnect 一定能等待
 * REST reconciliation 完成後才決定是否停止 fallback polling。
 */
function refreshAuctionStatus() {
  if (!auction.value || viewUnmounted) {
    return Promise.resolve(false);
  }

  if (statusRefreshPromise) {
    return statusRefreshPromise;
  }

  const refreshConnectionVersion = socketConnected
    ? socketConnectionVersion
    : null;
  let trackedRefreshPromise;

  trackedRefreshPromise = (async () => {
    try {
      const response = await getAuctionStatus(auction.value.auctionId);

      if (viewUnmounted) {
        return false;
      }

      const latestStatus = response.data.data;

      if (!isOlderStatus(latestStatus)) {
        await applyLatestAuctionStatus(latestStatus);
      }

      if (
        socketConnected &&
        refreshConnectionVersion !== null &&
        refreshConnectionVersion === socketConnectionVersion &&
        !bidHistorySyncPending
      ) {
        stopStatusPolling();
      }

      return true;
    } catch (error) {
      if (error.response?.status === 404) {
        await handleAuctionRemoved();
        return false;
      }

      console.error("同步競標最新狀態失敗：", error);
      statusSyncMessage.value = "最新競標狀態暫時同步失敗，將自動重試。";
      return false;
    }
  })().finally(() => {
    if (statusRefreshPromise === trackedRefreshPromise) {
      statusRefreshPromise = null;
      statusRefreshConnectionVersion = null;
    }
  });

  statusRefreshConnectionVersion = refreshConnectionVersion;
  statusRefreshPromise = trackedRefreshPromise;

  return trackedRefreshPromise;
}

function stopStatusPolling() {
  if (statusPollingTimerId !== null) {
    clearInterval(statusPollingTimerId);
    statusPollingTimerId = null;
  }
}

function startStatusPolling() {
  if (
    viewUnmounted ||
    removalNavigationPending ||
    !auction.value ||
    TERMINAL_STATUSES.has(auction.value.status) ||
    statusPollingTimerId !== null
  ) {
    return;
  }

  statusPollingTimerId = setInterval(
    refreshAuctionStatus,
    STATUS_POLL_INTERVAL_MS,
  );
}

function cleanupAuctionRealtime() {
  const unsubscribe = unsubscribeAuctionSocket;
  const client = auctionSocketClient;

  unsubscribeAuctionSocket = null;
  auctionSocketClient = null;
  socketConnected = false;
  socketConnectionVersion++;

  try {
    unsubscribe?.();
  } catch (error) {
    console.error("取消競標即時訂閱失敗：", error);
  }

  if (client) {
    try {
      Promise.resolve(client.deactivate()).catch((error) => {
        console.error("關閉競標即時連線失敗：", error);
      });
    } catch (error) {
      console.error("關閉競標即時連線失敗：", error);
    }
  }
}

async function handleAuctionRemoved() {
  if (viewUnmounted || removalNavigationPending) {
    return;
  }

  removalNavigationPending = true;
  stopStatusPolling();
  cleanupAuctionRealtime();

  try {
    await router.replace({ name: "auction" });
  } catch (error) {
    removalNavigationPending = false;
    console.error("返回競標列表失敗：", error);
    startStatusPolling();
  }
}

async function handleAuctionSocketConnected() {
  if (viewUnmounted || removalNavigationPending) {
    return;
  }

  if (!socketConnected) {
    socketConnected = true;
    socketConnectionVersion++;
  }

  const connectionVersion = socketConnectionVersion;

  // connect 前由 fallback polling 發出的 Request 不能充當訂閱後 reconciliation。
  // 先讓它結束，再強制發出一次 subscribe 完成後的新 REST Request。
  const refreshStartedBeforeConnection = statusRefreshPromise;

  if (
    refreshStartedBeforeConnection &&
    statusRefreshConnectionVersion !== connectionVersion
  ) {
    await refreshStartedBeforeConnection;

    if (
      viewUnmounted ||
      !socketConnected ||
      connectionVersion !== socketConnectionVersion
    ) {
      return;
    }
  }

  const reconciled = await refreshAuctionStatus();

  if (
    !viewUnmounted &&
    socketConnected &&
    connectionVersion === socketConnectionVersion &&
    reconciled &&
    !bidHistorySyncPending
  ) {
    stopStatusPolling();
  }
}

function handleAuctionSocketDisconnected() {
  socketConnected = false;
  socketConnectionVersion++;

  if (
    !viewUnmounted &&
    !removalNavigationPending &&
    auction.value &&
    !TERMINAL_STATUSES.has(auction.value.status)
  ) {
    startStatusPolling();
  }
}

function handleAuctionSocketError(error) {
  console.error("競標即時連線失敗，改用定時同步：", error);
  handleAuctionSocketDisconnected();
}

async function handleAuctionRealtimeMessage(message) {
  if (
    viewUnmounted ||
    !auction.value ||
    !REALTIME_EVENT_TYPES.has(message?.eventType) ||
    !message.data ||
    String(message.data.auctionId) !== String(auction.value.auctionId)
  ) {
    return;
  }

  if (isOlderStatus(message.data)) {
    return;
  }

  if (message.eventType === "AUCTION_REMOVED") {
    await handleAuctionRemoved();
    return;
  }

  await applyLatestAuctionStatus(message.data);
}

function startAuctionRealtime() {
  if (
    viewUnmounted ||
    !auction.value ||
    auctionSocketClient
  ) {
    return;
  }

  try {
    const client = createAuctionSocketClient({
      onConnected: handleAuctionSocketConnected,
      onDisconnected: handleAuctionSocketDisconnected,
      onError: handleAuctionSocketError,
    });

    auctionSocketClient = client;
    unsubscribeAuctionSocket = client.subscribeAuction(
      auction.value.auctionId,
      {
        onMessage: handleAuctionRealtimeMessage,
      },
    );
    client.activate();
  } catch (error) {
    handleAuctionSocketError(error);
  }
}

/**
 * 判斷目前賣家是否仍可編輯／取消競標。
 *
 * 前端只負責顯示控制；
 * 最終權限與 bidCount 判斷仍由後端重新確認。
 */
const canOwnerManageAuction = computed(() => {
  if (
    !auction.value?.canManage ||
    parseAuctionTime(auction.value.endAt) <= new Date()
  ) {
    return false;
  }

  if (auction.value.bidCount > 0) {
    return false;
  }

  return (
    auction.value.status === "SCHEDULED" || auction.value.status === "ACTIVE"
  );
});

/**
 * 前端只改善操作體驗，最終狀態、時間與價格仍由後端鎖定資料後判定。
 */
const canPlaceBid = computed(() => {
  if (!auction.value || auction.value.isOwner) {
    return false;
  }

  return (
    auction.value.status === "ACTIVE" &&
    parseAuctionTime(auction.value.startAt) <= new Date() &&
    parseAuctionTime(auction.value.endAt) > new Date()
  );
});

const bidAvailabilityMessage = computed(() => {
  if (!auction.value) {
    return "";
  }

  if (auction.value.status !== "ACTIVE") {
    return "目前競標狀態不允許出價。";
  }

  if (parseAuctionTime(auction.value.startAt) > new Date()) {
    return "競標尚未開始。";
  }

  if (parseAuctionTime(auction.value.endAt) <= new Date()) {
    return "競標已截止。";
  }

  return "目前無法出價。";
});

const isCandidateAtMinimum = computed(() => {
  if (!auction.value || !Number.isFinite(candidateBid.value)) {
    return true;
  }

  return candidateBid.value <= Number(auction.value.nextMinimumBid);
});

function increaseCandidateBid() {
  if (!auction.value || submittingBid.value) {
    return;
  }

  candidateBid.value += Number(auction.value.minIncrement);
}

function decreaseCandidateBid() {
  if (!auction.value || submittingBid.value || isCandidateAtMinimum.value) {
    return;
  }

  const minimumBid = Number(auction.value.nextMinimumBid);
  candidateBid.value = Math.max(
    minimumBid,
    candidateBid.value - Number(auction.value.minIncrement),
  );
}

/**
 * 本人競標目前不能修改／取消時的提示。
 */
const ownerManageMessage = computed(() => {
  if (!auction.value) {
    return "";
  }

  if (auction.value.bidCount > 0) {
    return "此競標已有出價紀錄，無法再修改或自行取消。";
  }

  if (parseAuctionTime(auction.value.endAt) <= new Date())
    return "此競標已到截止時間，無法修改或取消。";

  const statusMessageMap = {
    SOLD: "此競標已結標，無法再修改或取消。",
    UNSOLD: "此競標已流標，無法再修改或取消。",
    CANCELLED: "此競標已取消。",
    REMOVED: "此競標已廢標。",
  };

  return statusMessageMap[auction.value.status] ?? "目前競標狀態不允許操作。";
});

/**
 * 賣家取消自己的競標。
 *
 * 實際後端不會刪除資料，
 * 而是將競標狀態改為 CANCELLED。
 */
async function handleCancelAuction() {
  if (!auction.value) {
    return;
  }

  const confirmed = window.confirm(
    "確定要取消這場競標嗎？取消後將無法再恢復。",
  );

  if (!confirmed) {
    return;
  }

  cancelling.value = true;
  errorMessage.value = "";

  try {
    await cancelAuction(auction.value.auctionId);

    /*
     * 取消成功後重新讀取詳細資料，
     * 讓畫面立即取得最新的 CANCELLED 狀態。
     */
    await loadAuctionDetail();
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("取消競標失敗：", error);

    /*
     * 例如剛好有人先完成出價，
     * 後端會重新檢查 bidCount 並拒絕取消。
     */
    errorMessage.value =
      error.response?.data?.message ?? "取消競標失敗，請稍後再試";
  } finally {
    cancelling.value = false;
  }
}

/**
 * 送出一筆出價，成功後套用後端最新狀態並重新取得出價紀錄。
 */
async function handlePlaceBid() {
  if (!auction.value || !canPlaceBid.value || submittingBid.value) {
    return;
  }

  bidSuccessMessage.value = "";
  bidErrorMessage.value = "";

  const submittedBid = candidateBid.value;

  if (!Number.isFinite(submittedBid)) {
    bidErrorMessage.value = "無法取得目前可出價金額，請重新整理頁面。";
    return;
  }

  submittingBid.value = true;

  try {
    const response = await placeBid(auction.value.auctionId, {
      bidAmount: submittedBid,
    });

    const latestStatus = response.data.data;
    await applyLatestAuctionStatus(latestStatus);
    bidSuccessMessage.value = `已成功出價 ${formatTwd(submittedBid)}`;

    if (bidHistorySyncPending) {
      bidErrorMessage.value = "出價已成功，但暫時無法更新出價紀錄。";
    }
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("出價失敗：", error);
    bidErrorMessage.value =
      error.response?.data?.message ?? "出價失敗，請稍後再試";
  } finally {
    submittingBid.value = false;
  }
}

/**
 * 將商品狀況轉換為中文。
 */
function formatCondition(condition) {
  const conditionMap = {
    NEW: "全新",
    LIKE_NEW: "近全新",
    USED: "二手",
  };

  return conditionMap[condition] ?? condition;
}

// 頁面載入後先取得完整詳細資料，再開始同步最新狀態。
onMounted(async () => {
  await loadAuctionDetail();

  if (!viewUnmounted && auction.value) {
    if (!TERMINAL_STATUSES.has(auction.value.status)) {
      startStatusPolling();
    }

    startAuctionRealtime();
  }
});

onUnmounted(() => {
  viewUnmounted = true;
  stopStatusPolling();
  cleanupAuctionRealtime();
});

/**
 * 將後端回傳的圖片相對路徑，
 * 組合成瀏覽器可以實際存取的完整 Backend URL。
 *
 * API Base：
 * http://localhost:8080/api
 *
 * 圖片路徑：
 * /uploads/auctions/...
 *
 * 最後：
 * http://localhost:8080/uploads/auctions/...
 */
function resolveImageUrl(imageUrl) {
  if (!imageUrl) {
    return "";
  }

  // 如果未來 Storage 直接回傳完整 http/https URL，
  // 就直接使用，不另外加工。
  if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
    return imageUrl;
  }

  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

  // 將最後的 /api 移除，取得 Backend 根網址。
  const backendBaseUrl = apiBaseUrl.replace(/\/api\/?$/, "");

  return `${backendBaseUrl}${imageUrl}`;
}
</script>

<style scoped>
.auction-detail-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 1rem 0 3rem;
  color: #212529;
}

.detail-state {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  color: #495057;
}

.back-link {
  display: inline-block;
  margin-bottom: 1.5rem;
  color: #495057;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
}

.back-link:hover {
  color: #111;
  text-decoration: underline;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(380px, 0.95fr);
  gap: clamp(2rem, 5vw, 4.5rem);
  align-items: start;
}

.gallery-column {
  position: sticky;
  top: 1rem;
  min-width: 0;
}

.auction-summary {
  min-width: 0;
}

.detail-tabs {
  display: flex;
  gap: clamp(1.25rem, 4vw, 2.5rem);
  margin-top: clamp(3rem, 5vw, 4rem);
  border-bottom: 1px solid #dee2e6;
  overflow-x: auto;
}

.detail-tabs button {
  position: relative;
  flex: 0 0 auto;
  min-height: 3.5rem;
  padding: 0.75rem 0.15rem;
  border: 0;
  background: transparent;
  color: #495057;
  font-size: 1.05rem;
  font-weight: 700;
  white-space: nowrap;
}

.detail-tabs button:hover,
.detail-tabs button[aria-selected="true"] {
  color: #111;
}

.detail-tabs button[aria-selected="true"]::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  background: #111;
  content: "";
}

.detail-tabs button:focus-visible {
  outline: 2px solid #111;
  outline-offset: -4px;
}

.detail-tab-count {
  display: inline-block;
  min-width: 1.5rem;
  margin-left: 0.2rem;
  padding: 0.05rem 0.35rem;
  border-radius: 1rem;
  background: #f1f3f5;
  font-size: 0.8rem;
  text-align: center;
}

.detail-tab-panel {
  padding-top: clamp(1.75rem, 3vw, 2.25rem);
}

@media (max-width: 991.98px) {
  .detail-layout {
    grid-template-columns: minmax(0, 1fr) minmax(340px, 0.95fr);
    gap: 2rem;
  }
}

@media (max-width: 767.98px) {
  .auction-detail-page {
    padding-top: 0;
  }

  .detail-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .gallery-column {
    position: static;
  }
}
</style>