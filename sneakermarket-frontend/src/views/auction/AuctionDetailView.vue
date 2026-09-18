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
          <!-- 競標資訊 -->
          <div class="summary-heading">
            <span
              class="status-badge"
              :class="'status-' + auction.status.toLowerCase()"
            >
              {{ formatStatus(auction.status) }}
            </span>
            <h1>{{ auction.productName }}</h1>
            <p class="brand-category">
              {{ auction.brandName ?? "品牌資料未提供" }}
              <span aria-hidden="true">／</span>
              {{ auction.categoryName ?? "分類資料未提供" }}
            </p>
          </div>

          <dl class="product-meta-grid">
            <div>
              <dt>型號</dt>
              <dd>{{ auction.model || "-" }}</dd>
            </div>
            <div>
              <dt>尺寸</dt>
              <dd>{{ auction.size }}</dd>
            </div>
            <div>
              <dt>商品狀況</dt>
              <dd>{{ formatCondition(auction.condition) }}</dd>
            </div>
            <div>
              <dt>刊登會員</dt>
              <dd>賣家：{{ auction.sellerName || "會員資料未提供" }}</dd>
            </div>
          </dl>

          <div class="current-price-block">
            <span>目前價格</span>
            <strong>{{ formatTwd(auction.currentPrice) }}</strong>
            <small>起標價 {{ formatTwd(auction.startPrice) }}</small>
          </div>

          <dl class="auction-metrics">
            <div>
              <dt>出價次數</dt>
              <dd>{{ auction.bidCount }} 次</dd>
            </div>
            <div>
              <dt>最低加價</dt>
              <dd>{{ formatTwd(auction.minIncrement) }}</dd>
            </div>
            <div>
              <dt>下次最低出價</dt>
              <dd>{{ formatTwd(auction.nextMinimumBid) }}</dd>
            </div>
          </dl>

          <div class="auction-time-panel">
            <div>
              <span>開始時間</span>
              <strong>{{ formatDateTime(auction.startAt) }}</strong>
            </div>
            <div>
              <span>結束時間</span>
              <strong>{{ formatDateTime(auction.endAt) }}</strong>
            </div>
            <div class="remaining-row">
              <span>剩餘時間</span>
              <strong>
                <AuctionCountdown :end-at="auction.endAt" :status="auction.status" />
              </strong>
            </div>
          </div>

          <!-- 出價操作 -->
          <section class="bid-panel" aria-labelledby="bid-panel-title">
            <div class="section-title-row">
              <div>
                <p>限時競價</p>
                <h2 id="bid-panel-title">準備出價</h2>
              </div>
              <span>最低 {{ formatTwd(auction.nextMinimumBid) }}</span>
            </div>

            <div v-if="auction.isOwner" class="owner-bid-notice">
              賣家不可對自己的競標出價。
            </div>

            <form v-else @submit.prevent="handlePlaceBid">
              <div class="bid-stepper">
                <button
                  type="button"
                  :disabled="
                    !canPlaceBid || submittingBid || isCandidateAtMinimum
                  "
                  aria-label="減少一個最低加價級距"
                  @click="decreaseCandidateBid"
                >
                  −
                </button>

                <strong aria-live="polite">
                  {{ formatTwd(candidateBid) }}
                </strong>

                <button
                  type="button"
                  :disabled="!canPlaceBid || submittingBid"
                  aria-label="增加一個最低加價級距"
                  @click="increaseCandidateBid"
                >
                  +
                </button>
              </div>

              <button
                type="submit"
                class="place-bid-button"
                :disabled="!canPlaceBid || submittingBid"
              >
                {{
                  submittingBid
                    ? "出價中..."
                    : `確認出價 ${formatTwd(candidateBid)}`
                }}
              </button>

              <p v-if="!canPlaceBid" class="bid-availability-message">
                {{ bidAvailabilityMessage }}
              </p>

              <div v-if="bidSuccessMessage" class="alert alert-success mt-3 mb-0">
                {{ bidSuccessMessage }}
              </div>

              <div v-if="bidErrorMessage" class="alert alert-danger mt-3 mb-0">
                {{ bidErrorMessage }}
              </div>
            </form>
          </section>

          <!-- 賣家競標操作 -->
          <section v-if="auction.isOwner" class="seller-panel">
            <h2>我的競標操作</h2>

            <div v-if="canOwnerManageAuction" class="seller-actions">
              <RouterLink
                :to="{
                  name: 'auction-edit',
                  params: {
                    id: auction.auctionId,
                  },
                }"
                class="seller-edit-button"
              >
                編輯競標
              </RouterLink>

              <button
                type="button"
                class="seller-cancel-button"
                :disabled="cancelling"
                @click="handleCancelAuction"
              >
                {{ cancelling ? "取消中..." : "取消競標" }}
              </button>
            </div>

            <p v-else class="seller-message">
              {{ ownerManageMessage }}
            </p>
          </section>
        </div>
      </div>

      <div class="detail-information">
        <section class="information-section" aria-labelledby="product-info-title">
          <header>
            <p>Product</p>
            <h2 id="product-info-title">商品資訊</h2>
          </header>

          <dl class="information-list">
            <div class="full-row">
              <dt>商品描述</dt>
              <dd>{{ auction.description || "-" }}</dd>
            </div>
            <div>
              <dt>商品狀況</dt>
              <dd>{{ formatCondition(auction.condition) }}</dd>
            </div>
            <div>
              <dt>商品所在地</dt>
              <dd>{{ auction.location || "-" }}</dd>
            </div>
            <div class="full-row">
              <dt>瑕疵說明</dt>
              <dd>{{ auction.defectNote || "無" }}</dd>
            </div>
            <div class="full-row">
              <dt>真偽／來源說明</dt>
              <dd>{{ auction.authenticityNote || "-" }}</dd>
            </div>
          </dl>
        </section>

        <!-- 交易資訊 -->
        <section class="information-section" aria-labelledby="transaction-info-title">
          <header>
            <p>Transaction</p>
            <h2 id="transaction-info-title">交易資訊</h2>
          </header>

          <dl class="information-list">
            <div>
              <dt>運送方式</dt>
              <dd>{{ auction.shippingMethod }}</dd>
            </div>
            <div>
              <dt>運費</dt>
              <dd>{{ formatTwd(auction.shippingFee) }}</dd>
            </div>
            <div>
              <dt>付款方式</dt>
              <dd>{{ auction.paymentMethod }}</dd>
            </div>
            <div>
              <dt>允許面交</dt>
              <dd>{{ formatBoolean(auction.allowMeetup) }}</dd>
            </div>
            <div>
              <dt>允許退貨</dt>
              <dd>{{ formatBoolean(auction.allowReturn) }}</dd>
            </div>
            <div class="full-row">
              <dt>保固／售後</dt>
              <dd>{{ auction.warrantyNote || "-" }}</dd>
            </div>
          </dl>
        </section>
      </div>

      <!-- 出價紀錄 -->
      <section class="bid-history-section" aria-labelledby="bid-history-title">
        <header>
          <div>
            <p>Bid history</p>
            <h2 id="bid-history-title">出價紀錄</h2>
          </div>
          <span>{{ bids.length }} 筆</span>
        </header>

        <div v-if="bids.length === 0" class="empty-bid-history">
          目前尚無出價紀錄。
        </div>

        <div v-else class="table-responsive">
          <table class="table bid-history-table">
            <thead>
              <tr>
                <th>出價者</th>
                <th>出價金額</th>
                <th>出價時間</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="(bid, index) in bids"
                :key="`${bid.createdAt}-${index}`"
              >
                <td>
                  {{ bid.bidderName || "會員資料未提供" }}
                </td>
                <td>{{ formatTwd(bid.bidAmount) }}</td>
                <td>{{ formatDateTime(bid.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { parseAuctionTime, formatAuctionTime } from "@/utils/auctionTime";
import { formatTwd } from "@/utils/auctionMoney";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import AuctionCountdown from "@/components/auction/AuctionCountdown.vue";
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
    SOLD: "此競標已成交，無法再修改或取消。",
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
 * 將競標狀態轉換為中文。
 */
function formatStatus(status) {
  const statusMap = {
    SCHEDULED: "尚未開始",
    ACTIVE: "競標中",
    SOLD: "已結標",
    UNSOLD: "流標",
    CANCELLED: "已取消",
    REMOVED: "廢標",
  };

  return statusMap[status] ?? status;
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

/**
 * 將時間轉成前端顯示格式。
 */
const formatDateTime = formatAuctionTime;

/**
 * 將 Boolean 轉成較容易閱讀的中文。
 */
function formatBoolean(value) {
  return value ? "是" : "否";
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
  color: #6c757d;
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

.summary-heading {
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #e9ecef;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  margin-bottom: 0.85rem;
  padding: 0.2rem 0.68rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.status-active {
  background: #111;
  color: #fff;
}

.status-scheduled {
  background: #f1f3f5;
  color: #343a40;
}

.status-sold {
  background: #e6f4ea;
  color: #1e7e34;
}

.status-unsold,
.status-cancelled,
.status-removed {
  background: #f1f1f1;
  color: #6c757d;
}

.summary-heading h1 {
  margin: 0;
  color: #111;
  font-size: clamp(1.75rem, 3.3vw, 2.6rem);
  font-weight: 800;
  line-height: 1.13;
  letter-spacing: -0.035em;
}

.brand-category {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin: 0.8rem 0 0;
  color: #6c757d;
  font-size: 0.92rem;
}

.product-meta-grid,
.auction-metrics,
.information-list {
  margin: 0;
}

.product-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  padding: 1.25rem 0;
  border-bottom: 1px solid #e9ecef;
}

.product-meta-grid div,
.auction-metrics div,
.information-list div {
  min-width: 0;
}

.product-meta-grid dt,
.auction-metrics dt,
.information-list dt {
  margin-bottom: 0.25rem;
  color: #6c757d;
  font-size: 0.72rem;
  font-weight: 600;
}

.product-meta-grid dd,
.auction-metrics dd,
.information-list dd {
  margin: 0;
  color: #212529;
  overflow-wrap: anywhere;
}

.product-meta-grid dd {
  font-size: 0.88rem;
  font-weight: 600;
}

.current-price-block {
  display: flex;
  flex-direction: column;
  padding: 1.5rem 0 1.25rem;
}

.current-price-block > span {
  color: #6c757d;
  font-size: 0.78rem;
  font-weight: 600;
}

.current-price-block > strong {
  margin-top: 0.15rem;
  color: #111;
  font-size: clamp(2rem, 4vw, 2.8rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.current-price-block > small {
  margin-top: 0.3rem;
  color: #8a9096;
}

.auction-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid #e9ecef;
}

.auction-metrics div {
  padding: 0.85rem;
}

.auction-metrics div + div {
  border-left: 1px solid #e9ecef;
}

.auction-metrics dd {
  font-size: 0.83rem;
  font-weight: 700;
}

.auction-time-panel {
  margin-top: 1.25rem;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
}

.auction-time-panel > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 0;
}

.auction-time-panel > div + div {
  border-top: 1px solid #f1f3f5;
}

.auction-time-panel span {
  color: #6c757d;
  font-size: 0.78rem;
}

.auction-time-panel strong {
  color: #343a40;
  font-size: 0.82rem;
  text-align: right;
}

.auction-time-panel .remaining-row strong {
  color: #111;
  font-size: 1rem;
}

.bid-panel {
  margin-top: 1.5rem;
  padding: 1.25rem;
  background: #f8f9fa;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.section-title-row p,
.information-section header p,
.bid-history-section header p {
  margin: 0 0 0.25rem;
  color: #8a9096;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.66rem;
  font-weight: 700;
}

.section-title-row h2,
.seller-panel h2,
.information-section h2,
.bid-history-section h2 {
  margin: 0;
  color: #111;
  font-size: 1.15rem;
  font-weight: 800;
}

.section-title-row > span {
  color: #6c757d;
  font-size: 0.75rem;
  white-space: nowrap;
}

.owner-bid-notice,
.seller-message,
.bid-availability-message {
  margin: 0;
  color: #6c757d;
  font-size: 0.82rem;
  line-height: 1.6;
}

.bid-stepper {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.9rem;
}

.bid-stepper button {
  width: 44px;
  height: 44px;
  border: 1px solid #ced4da;
  border-radius: 50%;
  background: #fff;
  color: #111;
  font-size: 1.25rem;
  line-height: 1;
}

.bid-stepper button:hover:not(:disabled) {
  border-color: #111;
}

.bid-stepper button:disabled {
  color: #adb5bd;
  cursor: not-allowed;
}

.bid-stepper strong {
  min-width: 0;
  color: #111;
  font-size: clamp(1.35rem, 3vw, 1.8rem);
  text-align: center;
  white-space: nowrap;
}

.place-bid-button,
.seller-edit-button,
.seller-cancel-button {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 2rem;
  font-size: 0.88rem;
  font-weight: 700;
  text-decoration: none;
}

.place-bid-button {
  width: 100%;
  padding: 0.7rem 1.5rem;
  border: 1px solid #111;
  background: #111;
  color: #fff;
}

.place-bid-button:hover:not(:disabled) {
  background: #2c2c2c;
}

.place-bid-button:disabled {
  border-color: #adb5bd;
  background: #adb5bd;
  cursor: not-allowed;
}

.bid-availability-message {
  margin-top: 0.65rem;
  text-align: center;
}

.seller-panel {
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e9ecef;
}

.seller-actions {
  display: flex;
  gap: 0.65rem;
  margin-top: 0.85rem;
}

.seller-edit-button,
.seller-cancel-button {
  flex: 1;
  padding: 0.65rem 1rem;
}

.seller-edit-button {
  border: 1px solid #111;
  background: #111;
  color: #fff;
}

.seller-edit-button:hover {
  color: #fff;
  background: #2c2c2c;
}

.seller-cancel-button {
  border: 1px solid #ced4da;
  background: #fff;
  color: #495057;
}

.seller-cancel-button:hover:not(:disabled) {
  border-color: #dc3545;
  color: #dc3545;
}

.seller-message {
  margin-top: 0.65rem;
}

.detail-information {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3rem;
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid #dee2e6;
}

.information-section header,
.bid-history-section > header {
  margin-bottom: 1.25rem;
}

.information-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
}

.information-list div {
  padding: 0.85rem 0;
  border-bottom: 1px solid #e9ecef;
}

.information-list div:nth-child(odd):not(.full-row) {
  padding-right: 1rem;
}

.information-list div:nth-child(even):not(.full-row) {
  padding-left: 1rem;
}

.information-list .full-row {
  grid-column: 1 / -1;
}

.information-list dd {
  font-size: 0.88rem;
  line-height: 1.65;
  white-space: pre-line;
}

.bid-history-section {
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid #dee2e6;
}

.bid-history-section > header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.bid-history-section > header > span {
  color: #6c757d;
  font-size: 0.78rem;
}

.empty-bid-history {
  padding: 2rem;
  background: #f8f9fa;
  color: #6c757d;
  text-align: center;
}

.bid-history-table {
  min-width: 620px;
  margin-bottom: 0;
}

.bid-history-table th {
  padding: 0.8rem 0.5rem;
  border-bottom-color: #212529;
  color: #6c757d;
  font-size: 0.72rem;
  font-weight: 700;
}

.bid-history-table td {
  padding: 1rem 0.5rem;
  color: #343a40;
  font-size: 0.86rem;
  vertical-align: middle;
}

.bid-history-table td:nth-child(2) {
  color: #111;
  font-weight: 700;
}

@media (max-width: 991.98px) {
  .detail-layout {
    grid-template-columns: minmax(0, 1fr) minmax(340px, 0.95fr);
    gap: 2rem;
  }

  .auction-metrics {
    grid-template-columns: minmax(0, 1fr);
  }

  .auction-metrics div + div {
    border-top: 1px solid #e9ecef;
    border-left: 0;
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

  .summary-heading h1 {
    font-size: 1.8rem;
  }

  .auction-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .auction-metrics div + div {
    border-top: 0;
    border-left: 1px solid #e9ecef;
  }

  .detail-information {
    grid-template-columns: minmax(0, 1fr);
    gap: 2.5rem;
    margin-top: 3rem;
  }

  .bid-history-section {
    margin-top: 3rem;
  }
}

@media (max-width: 479.98px) {
  .product-meta-grid,
  .information-list {
    grid-template-columns: minmax(0, 1fr);
  }

  .information-list .full-row {
    grid-column: auto;
  }

  .information-list div:nth-child(odd):not(.full-row),
  .information-list div:nth-child(even):not(.full-row) {
    padding-right: 0;
    padding-left: 0;
  }

  .auction-metrics {
    grid-template-columns: minmax(0, 1fr);
  }

  .auction-metrics div + div {
    border-top: 1px solid #e9ecef;
    border-left: 0;
  }

  .auction-time-panel > div {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.2rem;
  }

  .auction-time-panel strong {
    text-align: left;
  }

  .bid-panel {
    margin-right: -0.25rem;
    margin-left: -0.25rem;
    padding: 1rem;
  }

  .bid-stepper {
    grid-template-columns: 40px minmax(0, 1fr) 40px;
  }

  .bid-stepper button {
    width: 40px;
    height: 40px;
  }

  .bid-stepper strong {
    font-size: 1.25rem;
  }

  .seller-actions {
    flex-direction: column;
  }
}
</style>
