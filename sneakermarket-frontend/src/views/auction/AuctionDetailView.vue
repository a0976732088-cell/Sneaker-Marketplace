<template>
  <div class="container py-4">
    <!-- 背景同步警告獨立於主要頁面狀態，保留已載入的詳細資料。 -->
    <div
      v-if="auction && statusSyncMessage"
      class="alert alert-warning"
      role="status"
    >
      {{ statusSyncMessage }}
    </div>

    <!-- 資料載入中 -->
    <div v-if="loading">載入中...</div>

    <!-- API 發生錯誤 -->
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <!-- 競標詳細資料 -->
    <div v-else-if="auction">
      <h1 class="mb-4">
        {{ auction.productName }}
      </h1>

      <!-- 賣家競標操作 -->
      <div v-if="auction.isOwner" class="card mb-4">
        <div class="card-header">我的競標操作</div>

        <div class="card-body">
          <!-- 尚可編輯／取消 -->
          <div v-if="canOwnerManageAuction" class="d-flex gap-2">
            <RouterLink
              :to="{
                name: 'auction-edit',
                params: {
                  id: auction.auctionId,
                },
              }"
              class="btn btn-primary"
            >
              編輯競標
            </RouterLink>

            <button
              type="button"
              class="btn btn-outline-danger"
              :disabled="cancelling"
              @click="handleCancelAuction"
            >
              {{ cancelling ? "取消中..." : "取消競標" }}
            </button>
          </div>

          <!-- 本人的競標，但現在不能再操作 -->
          <div v-else class="text-muted">
            {{ ownerManageMessage }}
          </div>
        </div>
      </div>

      <!-- 商品圖片 -->
      <div class="mb-4">
        <h5>商品圖片</h5>

        <div v-if="auction.imageUrls?.length">
          <div class="row g-3">
            <div
              v-for="(imageUrl, index) in auction.imageUrls"
              :key="`${imageUrl}-${index}`"
              class="col-md-4"
            >
              <img
                :src="resolveImageUrl(imageUrl)"
                :alt="`${auction.productName} 圖片 ${index + 1}`"
                class="img-fluid border rounded"
              />
            </div>
          </div>
        </div>

        <div v-else class="text-muted">目前沒有商品圖片。</div>
      </div>

      <p>品牌：{{ auction.brandName ?? "品牌資料未提供" }} ／ 分類：{{ auction.categoryName ?? "分類資料未提供" }}</p>
      <!-- 商品資訊 -->
      <div class="card mb-4">
        <div class="card-header">商品資訊</div>

        <div class="card-body">
          <p>型號：{{ auction.model || "-" }}</p>

          <p>尺寸：{{ auction.size }}</p>

          <p>
            商品狀況：
            {{ formatCondition(auction.condition) }}
          </p>

          <p>商品描述：{{ auction.description }}</p>

          <p>
            瑕疵說明：
            {{ auction.defectNote || "無" }}
          </p>

          <p>
            真偽／來源說明：
            {{ auction.authenticityNote || "-" }}
          </p>

          <p>
            商品所在地：
            {{ auction.location || "-" }}
          </p>
        </div>
      </div>

      <!-- 競標資訊 -->
      <div class="card mb-4">
        <div class="card-header">競標資訊</div>

        <div class="card-body">
          <p>賣家：{{ auction.sellerName || "會員資料未提供" }}</p>

          <p>起標價：{{ formatTwd(auction.startPrice) }}</p>

          <p>目前價格：{{ formatTwd(auction.currentPrice) }}</p>

          <p>最低加價：{{ formatTwd(auction.minIncrement) }}</p>

          <p>下一次最低出價：{{ formatTwd(auction.nextMinimumBid) }}</p>

          <p>
            出價次數：
            {{ auction.bidCount }}
          </p>

          <p>
            開始時間：
            {{ formatDateTime(auction.startAt) }}
          </p>

          <p>
            結束時間：
            {{ formatDateTime(auction.endAt) }}
          </p>

          <p>
            競標狀態：
            {{ formatStatus(auction.status) }}
          </p>
        </div>
      </div>

      <!-- 出價操作 -->
      <div class="card mb-4">
        <div class="card-header">我要出價</div>

        <div class="card-body">
          <p class="mb-2">目前價格：<strong>{{ formatTwd(auction.currentPrice) }}</strong></p>

          <p class="mb-2">最低加價：<strong>{{ formatTwd(auction.minIncrement) }}</strong></p>

          <p class="mb-3">
            目前最低可出價：
            <strong>{{ formatTwd(auction.nextMinimumBid) }}</strong>
          </p>

          <div v-if="auction.isOwner" class="alert alert-secondary mb-0">
            賣家不可對自己的競標出價。
          </div>

          <form v-else @submit.prevent="handlePlaceBid">
            <div class="d-flex align-items-center gap-3 mb-3">
              <button
                type="button"
                class="btn btn-outline-secondary"
                :disabled="!canPlaceBid || submittingBid || isCandidateAtMinimum"
                aria-label="減少一個最低加價級距"
                @click="decreaseCandidateBid"
              >
                −
              </button>

              <strong class="fs-4" aria-live="polite">
                {{ formatTwd(candidateBid) }}
              </strong>

              <button
                type="button"
                class="btn btn-outline-secondary"
                :disabled="!canPlaceBid || submittingBid"
                aria-label="增加一個最低加價級距"
                @click="increaseCandidateBid"
              >
                +
              </button>
            </div>

            <button
              type="submit"
              class="btn btn-success"
              :disabled="!canPlaceBid || submittingBid"
            >
              {{
                submittingBid
                  ? "出價中..."
                  : `確認出價 ${formatTwd(candidateBid)}`
              }}
            </button>

            <div v-if="!canPlaceBid" class="text-muted mt-2">
              {{ bidAvailabilityMessage }}
            </div>

            <div v-if="bidSuccessMessage" class="alert alert-success mt-3 mb-0">
              {{ bidSuccessMessage }}
            </div>

            <div v-if="bidErrorMessage" class="alert alert-danger mt-3 mb-0">
              {{ bidErrorMessage }}
            </div>
          </form>
        </div>
      </div>

      <!-- 交易資訊 -->
      <div class="card mb-4">
        <div class="card-header">交易資訊</div>

        <div class="card-body">
          <p>
            運送方式：
            {{ auction.shippingMethod }}
          </p>

          <p>運費：{{ formatTwd(auction.shippingFee) }}</p>

          <p>
            付款方式：
            {{ auction.paymentMethod }}
          </p>

          <p>
            允許面交：
            {{ formatBoolean(auction.allowMeetup) }}
          </p>

          <p>
            允許退貨：
            {{ formatBoolean(auction.allowReturn) }}
          </p>

          <p>
            保固／售後：
            {{ auction.warrantyNote || "-" }}
          </p>
        </div>
      </div>

      <!-- 出價紀錄 -->
      <div class="card">
        <div class="card-header">出價紀錄</div>

        <div class="card-body">
          <div v-if="bids.length === 0">目前尚無出價紀錄。</div>

          <div v-else class="table-responsive">
            <table class="table">
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

                  <td>
                    {{ formatDateTime(bid.createdAt) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { parseAuctionTime, formatAuctionTime } from "@/utils/auctionTime";
import { formatTwd } from "@/utils/auctionMoney";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import {
  handleAuctionAccessError,
  cancelAuction,
  getAuctionDetail,
  getAuctionStatus,
  getBidHistory,
  placeBid,
} from "@/service/auctionService";

const STATUS_POLL_INTERVAL_MS = 12000;
const TERMINAL_STATUSES = new Set([
  "SOLD",
  "UNSOLD",
  "CANCELLED",
  "REMOVED",
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
let refreshingStatus = false;
let bidHistorySyncPending = false;
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
 * 避免較早發出的 polling Response 覆蓋剛完成出價後的新狀態。
 */
function isOlderStatus(latestStatus) {
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
 * 重新取得競標最新狀態；只有出價次數改變時才同步出價紀錄。
 */
async function refreshAuctionStatus() {
  if (!auction.value || refreshingStatus || viewUnmounted) {
    return;
  }

  refreshingStatus = true;

  try {
    const oldBidCount = auction.value.bidCount;
    const response = await getAuctionStatus(auction.value.auctionId);

    if (viewUnmounted) {
      return;
    }

    const latestStatus = response.data.data;

    if (isOlderStatus(latestStatus)) {
      return;
    }

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

    if (latestStatus.bidCount !== oldBidCount) {
      bidHistorySyncPending = true;
    }

    if (bidHistorySyncPending) {
      try {
        const bidsResponse = await getBidHistory(auction.value.auctionId);

        if (!viewUnmounted) {
          bids.value = bidsResponse.data.data;
          bidHistorySyncPending = false;
          bidErrorMessage.value = "";
        }
      } catch (historyError) {
        console.error("同步出價紀錄失敗：", historyError);
        statusSyncMessage.value =
          "最新競標狀態已更新，但出價紀錄暫時同步失敗，將自動重試。";
      }
    }

    if (
      TERMINAL_STATUSES.has(latestStatus.status) &&
      !bidHistorySyncPending
    ) {
      stopStatusPolling();
    }
  } catch (error) {
    console.error("同步競標最新狀態失敗：", error);
    statusSyncMessage.value =
      "最新競標狀態暫時同步失敗，將自動重試。";
  } finally {
    refreshingStatus = false;
  }
}

function stopStatusPolling() {
  if (statusPollingTimerId !== null) {
    clearInterval(statusPollingTimerId);
    statusPollingTimerId = null;
  }
}

function startStatusPolling() {
  stopStatusPolling();

  statusPollingTimerId = setInterval(
    refreshAuctionStatus,
    STATUS_POLL_INTERVAL_MS,
  );
}

/**
 * 判斷目前賣家是否仍可編輯／取消競標。
 *
 * 前端只負責顯示控制；
 * 最終權限與 bidCount 判斷仍由後端重新確認。
 */
const canOwnerManageAuction = computed(() => {
  if (!auction.value?.canManage || parseAuctionTime(auction.value.endAt) <= new Date()) {
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

  if (parseAuctionTime(auction.value.endAt) <= new Date()) return "此競標已到截止時間，無法修改或取消。";

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
    Object.assign(auction.value, latestStatus);
    candidateBid.value = Number(latestStatus.nextMinimumBid);
    bidSuccessMessage.value = `已成功出價 ${formatTwd(submittedBid)}`;

    try {
      const bidsResponse = await getBidHistory(auction.value.auctionId);
      bids.value = bidsResponse.data.data;
      bidHistorySyncPending = false;
    } catch (historyError) {
      console.error("重新取得出價紀錄失敗：", historyError);
      bidHistorySyncPending = true;
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
    SOLD: "已成交",
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

  if (
    !viewUnmounted &&
    auction.value &&
    !TERMINAL_STATUSES.has(auction.value.status)
  ) {
    startStatusPolling();
  }
});

onUnmounted(() => {
  viewUnmounted = true;
  stopStatusPolling();
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
