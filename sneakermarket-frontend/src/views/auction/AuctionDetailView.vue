<template>
  <div class="container py-4">
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
          <p>起標價： NT$ {{ auction.startPrice }}</p>

          <p>目前價格： NT$ {{ auction.currentPrice }}</p>

          <p>最低加價： NT$ {{ auction.minIncrement }}</p>

          <p>下一次最低出價： NT$ {{ auction.nextMinimumBid }}</p>

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

      <!-- 交易資訊 -->
      <div class="card mb-4">
        <div class="card-header">交易資訊</div>

        <div class="card-body">
          <p>
            運送方式：
            {{ auction.shippingMethod }}
          </p>

          <p>運費： NT$ {{ auction.shippingFee }}</p>

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
                    {{ bid.maskedBidder }}
                  </td>

                  <td>NT$ {{ bid.bidAmount }}</td>

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
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";

import {
  cancelAuction,
  getAuctionDetail,
  getBidHistory,
} from "@/service/auctionService";

// 取得目前路由資訊
const route = useRoute();

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
 * 判斷目前賣家是否仍可編輯／取消競標。
 *
 * 前端只負責顯示控制；
 * 最終權限與 bidCount 判斷仍由後端重新確認。
 */
const canOwnerManageAuction = computed(() => {
  if (!auction.value?.isOwner) {
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
 * 本人競標目前不能修改／取消時的提示。
 */
const ownerManageMessage = computed(() => {
  if (!auction.value) {
    return "";
  }

  if (auction.value.bidCount > 0) {
    return "此競標已有出價紀錄，無法再修改或自行取消。";
  }

  const statusMessageMap = {
    SOLD: "此競標已成交，無法再修改或取消。",
    UNSOLD: "此競標已流標，無法再修改或取消。",
    CANCELLED: "此競標已取消。",
    REMOVED: "此競標已被下架。",
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
 * 將競標狀態轉換為中文。
 */
function formatStatus(status) {
  const statusMap = {
    DRAFT: "草稿",
    SCHEDULED: "尚未開始",
    ACTIVE: "競標中",
    SOLD: "已成交",
    UNSOLD: "流標",
    CANCELLED: "已取消",
    REMOVED: "已下架",
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
function formatDateTime(dateTime) {
  if (!dateTime) {
    return "-";
  }

  return new Intl.DateTimeFormat("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(dateTime));
}

/**
 * 將 Boolean 轉成較容易閱讀的中文。
 */
function formatBoolean(value) {
  return value ? "是" : "否";
}

// 頁面載入後取得資料
onMounted(() => {
  loadAuctionDetail();
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
