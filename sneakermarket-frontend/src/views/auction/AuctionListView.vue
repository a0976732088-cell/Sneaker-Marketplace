<template>
  <div class="container py-4">
    <h1 class="mb-4">競標商城</h1>
    <nav class="d-flex flex-wrap gap-2 mb-4" aria-label="我的競標導覽">
      <RouterLink class="btn btn-outline-primary" to="/auctions/me/bids">我的出價</RouterLink>
      <RouterLink class="btn btn-outline-primary" to="/auctions/me/listings">我的刊登</RouterLink>
      <RouterLink class="btn btn-outline-primary" to="/auction-orders">我的競標訂單</RouterLink>
    </nav>

    <form class="row g-2 mb-4" @submit.prevent="searchAuctions">
      <!-- 關鍵字搜尋 -->
      <div class="col-md-4">
        <input
          v-model="keyword"
          type="text"
          class="form-control"
          placeholder="搜尋商品名稱或型號"
        />
      </div>

      <!-- 排序 -->
      <div class="row g-2 mb-4">
        <div class="col-md-3">
          <select v-model="sort" class="form-select">
            <option value="endAt,asc">即將結束</option>

            <option value="createdAt,desc">最新上架</option>

            <option value="currentPrice,asc">價格：低到高</option>

            <option value="currentPrice,desc">價格：高到低</option>

            <option value="bidCount,desc">出價次數：多到少</option>
          </select>
        </div>

        <div class="col-md-2">
          <button
            type="button"
            class="btn btn-outline-secondary w-100"
            :disabled="loading"
            @click="searchAuctions"
          >
            套用排序
          </button>
        </div>

        <div class="col-md-2">
          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="loading"
            @click="clearFilters"
          >
            清除條件
          </button>
        </div>
      </div>

      <div class="col-md-2">
        <select v-model="brandId" class="form-select" aria-label="品牌篩選">
          <option value="">全部品牌</option>
          <option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option>
        </select>
      </div>
      <div class="col-md-2">
        <select v-model="categoryId" class="form-select" aria-label="分類篩選">
          <option value="">全部分類</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
        </select>
      </div>
      <p v-if="optionsError" class="text-danger">{{ optionsError }}</p>
      <!-- 競標狀態 -->
      <div class="col-md-2">
        <select v-model="status" class="form-select">
          <option value="">全部狀態</option>
          <option value="ACTIVE">競標中</option>
          <option value="SCHEDULED">尚未開始</option>
          <option value="SOLD">已成交</option>
          <option value="UNSOLD">流標</option>
        </select>
      </div>

      <!-- 商品狀況 -->
      <div class="col-md-2">
        <select v-model="condition" class="form-select">
          <option value="">全部狀況</option>
          <option value="NEW">全新</option>
          <option value="LIKE_NEW">近全新</option>
          <option value="USED">二手</option>
        </select>
      </div>

      <!-- 球鞋尺寸 -->
      <div class="col-md-2">
        <select v-model="shoeSize" class="form-select">
          <option value="">全部尺寸</option>
          <option value="US 8">US 8</option>
          <option value="US 8.5">US 8.5</option>
          <option value="US 9">US 9</option>
          <option value="US 9.5">US 9.5</option>
          <option value="US 10">US 10</option>
          <option value="US 10.5">US 10.5</option>
        </select>
      </div>

      <!-- 價格範圍 -->
      <div class="row g-2 mb-3">
        <div class="col-md-3">
          <input
            v-model="minPrice"
            type="number"
            min="0"
            step="1"
            class="form-control"
            placeholder="最低價格（NT$）"
          />
        </div>

        <div class="col-md-3">
          <input
            v-model="maxPrice"
            type="number"
            min="0"
            step="1"
            class="form-control"
            placeholder="最高價格（NT$）"
          />
        </div>

        <div class="col-md-2">
          <button
            type="button"
            class="btn btn-outline-primary w-100"
            :disabled="loading"
            @click="searchAuctions"
          >
            套用價格
          </button>
        </div>
      </div>

      <!-- 搜尋按鈕 -->
      <div class="col-md-2">
        <button type="submit" class="btn btn-primary w-100" :disabled="loading">
          搜尋
        </button>
      </div>
    </form>

    <!-- 資料載入中 -->
    <div v-if="loading">載入中...</div>

    <!-- API 發生錯誤 -->
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <!-- 查不到競標資料 -->
    <div v-else-if="auctions.length === 0">目前沒有符合條件的競標商品。</div>

    <!-- 競標列表 -->
    <div v-else>
      <div
        v-for="auction in auctions"
        :key="auction.auctionId"
        class="card mb-3"
      >
        <div class="card-body">
          <h5 class="card-title">
            {{ auction.productName }}
          </h5>

          <p class="card-text">品牌：{{ auction.brandName ?? "品牌資料未提供" }} ／ 分類：{{ auction.categoryName ?? "分類資料未提供" }}</p>
          <p class="card-text">型號：{{ auction.model }}</p>

          <p class="card-text">尺寸：{{ auction.size }}</p>

          <p class="card-text">
            商品狀況：{{ formatCondition(auction.condition) }}
          </p>

          <p class="card-text">目前價格：{{ formatTwd(auction.currentPrice) }}</p>

          <p class="card-text">出價次數：{{ auction.bidCount }}</p>

          <p class="card-text">
            上架時間：{{ formatDateTime(auction.createdAt) }}
          </p>

          <p class="card-text">結束時間：{{ formatDateTime(auction.endAt) }}</p>

          <p class="card-text">剩餘時間：{{ formatRemainingTime(auction) }}</p>

          <p class="card-text">競標狀態：{{ formatStatus(auction.status) }}</p>

          <!-- 進入競標詳細頁 -->
          <RouterLink
            :to="{
              name: 'auction-detail',
              params: { id: auction.auctionId },
            }"
            class="btn btn-primary"
          >
            查看詳細
          </RouterLink>
        </div>
      </div>

      <!-- 分頁 -->
      <div
        v-if="totalPages > 0"
        class="d-flex justify-content-between align-items-center mt-4"
      >
        <div>
          共 {{ totalElements }} 筆， 第 {{ currentPage + 1 }} /
          {{ totalPages }} 頁
        </div>

        <div class="d-flex gap-2">
          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="loading || currentPage === 0"
            @click="changePage(currentPage - 1)"
          >
            上一頁
          </button>

          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="loading || currentPage >= totalPages - 1"
            @click="changePage(currentPage + 1)"
          >
            下一頁
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { parseAuctionTime, formatAuctionTime } from "@/utils/auctionTime";
import { formatTwd } from "@/utils/auctionMoney";
import { onMounted, onUnmounted, ref } from "vue";
import { getAuctions, getAuctionOptions } from "@/service/auctionService";

// 競標列表資料
const auctions = ref([]);
const brands = ref([]);
const categories = ref([]);
const brandId = ref("");
const categoryId = ref("");
const optionsError = ref("");

// 搜尋關鍵字
const keyword = ref("");

// 競標狀態篩選
const status = ref("");

// 商品狀況篩選
const condition = ref("");

// 球鞋尺寸篩選
const shoeSize = ref("");

// 競標列表排序方式
const sort = ref("endAt,asc");

// 是否正在讀取資料
const loading = ref(false);

// 錯誤訊息
const errorMessage = ref("");

// 目前頁碼
const currentPage = ref(0);

// 每頁資料筆數
const pageSize = ref(5);

// 符合條件的總資料筆數
const totalElements = ref(0);

// 總頁數
const totalPages = ref(0);

// 最低價格篩選
const minPrice = ref("");

// 最高價格篩選
const maxPrice = ref("");

// 前端目前時間，用來更新競標剩餘時間
const currentTime = ref(Date.now());

let countdownTimer = null;

/**
 * 從後端取得競標列表。
 */
async function loadAuctions() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getAuctions({
      keyword: keyword.value.trim() || undefined,
      brandId: brandId.value || undefined,
      categoryId: categoryId.value || undefined,
      status: status.value || undefined,
      condition: condition.value || undefined,
      shoeSize: shoeSize.value || undefined,
      minPrice: minPrice.value !== "" ? Number(minPrice.value) : undefined,
      maxPrice: maxPrice.value !== "" ? Number(maxPrice.value) : undefined,
      page: currentPage.value,
      size: pageSize.value,
      sort: sort.value,
    });

    const pageData = response.data.data;

    auctions.value = pageData.items;
    currentPage.value = pageData.page;
    pageSize.value = pageData.size;
    totalElements.value = pageData.totalElements;
    totalPages.value = pageData.totalPages;
  } catch (error) {
    console.error("取得競標列表失敗：", error);

    errorMessage.value = "無法取得競標列表";
  } finally {
    loading.value = false;
  }
}

/**
 * 將競標狀態轉成前端顯示文字。
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
 * 將商品狀況轉成前端顯示文字。
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
 * 將後端時間轉成前端顯示格式。
 */
const formatDateTime = formatAuctionTime;

/**
 * 計算競標剩餘時間。
 */
function formatRemainingTime(auction) {
  // 已進入終態的競標不再顯示倒數
  if (["SOLD", "UNSOLD", "CANCELLED", "REMOVED"].includes(auction.status)) {
    return "已結束";
  }

  if (!auction.endAt) {
    return "-";
  }

  const endTime = parseAuctionTime(auction.endAt).getTime();
  const diff = endTime - currentTime.value;

  // 前端倒數歸零不代表後端已完成結標
  if (diff <= 0) {
    return "已到截止時間";
  }

  const totalMinutes = Math.floor(diff / 1000 / 60);

  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  if (days > 0) {
    return `${days} 天 ${hours} 小時 ${minutes} 分`;
  }

  if (hours > 0) {
    return `${hours} 小時 ${minutes} 分`;
  }

  return `${minutes} 分`;
}

/**
 * 執行關鍵字搜尋。
 */
async function searchAuctions() {
  // 搜尋條件改變時回到第一頁
  currentPage.value = 0;

  await loadAuctions();
}

/**
 * 清除競標列表搜尋與篩選條件。
 */
async function clearFilters() {
  keyword.value = "";
  brandId.value = "";
  categoryId.value = "";
  status.value = "";
  condition.value = "";
  shoeSize.value = "";
  minPrice.value = "";
  maxPrice.value = "";

  // 恢復預設排序
  sort.value = "endAt,asc";

  // 清除條件後回到第一頁
  currentPage.value = 0;

  await loadAuctions();
}

/**
 * 切換競標列表頁面。
 */
async function changePage(page) {
  // 不允許超出有效頁碼
  if (page < 0 || page >= totalPages.value) {
    return;
  }

  currentPage.value = page;

  await loadAuctions();
}

// 頁面載入完成後自動取得競標列表
onMounted(() => {
  loadAuctions();
  getAuctionOptions().then((response) => {
    brands.value = response.data.data.brands;
    categories.value = response.data.data.categories;
  }).catch(() => {
    optionsError.value = "品牌與分類選項暫時無法載入，仍可使用其他搜尋條件。";
  });

  // 每分鐘更新一次列表上的剩餘時間
  countdownTimer = setInterval(() => {
    currentTime.value = Date.now();
  }, 60000);
});

// 頁面卸載時清除訂閱
onUnmounted(() => {
  // 在這裡可以清除任何訂閱或清理工作
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});
</script>
