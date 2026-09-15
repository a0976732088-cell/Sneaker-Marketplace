<template>
  <div class="container py-4">
    <h1 class="mb-4">編輯競標</h1>

    <!-- 資料載入中 -->
    <div v-if="loading">載入中...</div>

    <!-- API / 表單錯誤 -->
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <template v-else>
      <!-- 目前競標狀態提示 -->
      <div class="alert alert-info">
        目前狀態：{{ formatStatus(auctionStatus) }} ／ 出價次數：{{ bidCount }}
      </div>

      <!-- 不允許編輯 -->
      <div v-if="!editable" class="alert alert-warning">
        {{ editableMessage }}
      </div>

      <form @submit.prevent="submitUpdate">
        <!-- 商品基本資料 -->
        <div class="card mb-4">
          <div class="card-header">商品基本資料</div>

          <div class="card-body">
            <div class="row g-3">
              <!-- 品牌 -->
              <div class="col-md-6">
                <label class="form-label"> 品牌 </label>

                <select
                  v-model.number="form.brandId"
                  class="form-select"
                  required
                  :disabled="!editable || auctionStatus === 'ACTIVE'"
                >
                  <option disabled value="">請選擇品牌</option>

                  <option
                    v-for="brand in brands"
                    :key="brand.id"
                    :value="brand.id"
                  >
                    {{ brand.name }}
                  </option>
                </select>

                <div v-if="auctionStatus === 'ACTIVE'" class="form-text">
                  競標開始後不可修改品牌／分類。
                </div>
              </div>

              <!-- 分類 -->
              <div class="col-md-6">
                <label class="form-label"> 分類 </label>

                <select
                  v-model.number="form.categoryId"
                  class="form-select"
                  required
                  :disabled="!editable || auctionStatus === 'ACTIVE'"
                >
                  <option disabled value="">請選擇分類</option>

                  <option
                    v-for="category in categories"
                    :key="category.id"
                    :value="category.id"
                  >
                    {{ category.name }}
                  </option>
                </select>
              </div>

              <!-- 商品名稱 -->
              <div class="col-md-8">
                <label class="form-label"> 商品名稱 </label>

                <input
                  v-model.trim="form.productName"
                  type="text"
                  maxlength="150"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 型號 -->
              <div class="col-md-4">
                <label class="form-label"> 型號／系列 </label>

                <input
                  v-model.trim="form.model"
                  type="text"
                  maxlength="100"
                  class="form-control"
                  :disabled="!editable"
                />
              </div>

              <!-- 尺寸 -->
              <div class="col-md-6">
                <label class="form-label"> 尺寸 </label>

                <input
                  v-model.trim="form.size"
                  type="text"
                  maxlength="20"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 商品狀況 -->
              <div class="col-md-6">
                <label class="form-label"> 商品狀況 </label>

                <select
                  v-model="form.condition"
                  class="form-select"
                  required
                  :disabled="!editable"
                >
                  <option value="NEW">全新</option>

                  <option value="LIKE_NEW">近全新</option>

                  <option value="USED">二手</option>
                </select>
              </div>

              <!-- 商品介紹 -->
              <div class="col-12">
                <label class="form-label"> 商品介紹 </label>

                <textarea
                  v-model.trim="form.description"
                  class="form-control"
                  rows="4"
                  required
                  :disabled="!editable"
                ></textarea>
              </div>

              <!-- 瑕疵說明 -->
              <div class="col-md-6">
                <label class="form-label"> 瑕疵說明 </label>

                <textarea
                  v-model.trim="form.defectNote"
                  class="form-control"
                  rows="3"
                  maxlength="1000"
                  :disabled="!editable"
                ></textarea>
              </div>

              <!-- 真偽來源 -->
              <div class="col-md-6">
                <label class="form-label"> 真偽／來源說明 </label>

                <textarea
                  v-model.trim="form.authenticityNote"
                  class="form-control"
                  rows="3"
                  maxlength="1000"
                  :disabled="!editable"
                ></textarea>
              </div>

              <!-- 商品所在地 -->
              <div class="col-md-6">
                <label class="form-label"> 商品所在地 </label>

                <input
                  v-model.trim="form.location"
                  type="text"
                  maxlength="100"
                  class="form-control"
                  :disabled="!editable"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 圖片 -->
        <div class="card mb-4">
          <div class="card-header">商品圖片</div>

          <div class="card-body">
            <div class="text-muted">
              功能 4 目前不提供圖片修改；既有圖片會保持不變。
            </div>
          </div>
        </div>

        <!-- 交易資訊 -->
        <div class="card mb-4">
          <div class="card-header">交易資訊</div>

          <div class="card-body">
            <div class="row g-3">
              <!-- 運送方式 -->
              <div class="col-md-6">
                <label class="form-label"> 運送方式 </label>

                <input
                  v-model.trim="form.shippingMethod"
                  type="text"
                  maxlength="50"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 運費 -->
              <div class="col-md-6">
                <label class="form-label"> 運費（NT$） </label>

                <input
                  v-model.number="form.shippingFee"
                  type="number"
                  min="0"
                  step="1"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 付款方式 -->
              <div class="col-md-6">
                <label class="form-label"> 付款方式 </label>

                <input
                  v-model.trim="form.paymentMethod"
                  type="text"
                  maxlength="50"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 面交 -->
              <div class="col-md-3">
                <div class="form-check mt-4">
                  <input
                    id="allowMeetup"
                    v-model="form.allowMeetup"
                    type="checkbox"
                    class="form-check-input"
                    :disabled="!editable"
                  />

                  <label for="allowMeetup" class="form-check-label">
                    允許面交
                  </label>
                </div>
              </div>

              <!-- 退貨 -->
              <div class="col-md-3">
                <div class="form-check mt-4">
                  <input
                    id="allowReturn"
                    v-model="form.allowReturn"
                    type="checkbox"
                    class="form-check-input"
                    :disabled="!editable"
                  />

                  <label for="allowReturn" class="form-check-label">
                    允許退貨
                  </label>
                </div>
              </div>

              <!-- 保固 -->
              <div class="col-12">
                <label class="form-label"> 保固／售後說明 </label>

                <textarea
                  v-model.trim="form.warrantyNote"
                  class="form-control"
                  rows="2"
                  maxlength="500"
                  :disabled="!editable"
                ></textarea>
              </div>
            </div>
          </div>
        </div>

        <!-- 競標設定 -->
        <div class="card mb-4">
          <div class="card-header">競標設定</div>

          <div class="card-body">
            <div class="row g-3">
              <!-- 起標價 -->
              <div class="col-md-6">
                <label class="form-label"> 起標價（NT$） </label>

                <input
                  v-model.number="form.startPrice"
                  type="number"
                  min="1"
                  step="1"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 最低加價 -->
              <div class="col-md-6">
                <label class="form-label"> 最低加價（NT$） </label>

                <input
                  v-model.number="form.minIncrement"
                  type="number"
                  min="1"
                  step="1"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>

              <!-- 開始時間 -->
              <div class="col-md-6">
                <label class="form-label"> 開始時間 </label>

                <input
                  v-model="form.startAt"
                  type="datetime-local" title="台北時間（UTC+8）"
                  class="form-control"
                  required
                  :disabled="!editable || auctionStatus === 'ACTIVE'"
                />

                <div v-if="auctionStatus === 'ACTIVE'" class="form-text">
                  競標開始後不可修改開始時間。
                </div>
              </div>

              <!-- 結束時間 -->
              <div class="col-md-6">
                <label class="form-label"> 結束時間 </label>

                <input
                  v-model="form.endAt"
                  type="datetime-local" title="台北時間（UTC+8）"
                  class="form-control"
                  required
                  :disabled="!editable"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 操作 -->
        <div class="d-flex gap-2">
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="submitting || !editable"
          >
            {{ submitting ? "儲存中..." : "儲存修改" }}
          </button>

          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="submitting"
            @click="goBackToDetail"
          >
            返回競標詳細頁
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { parseAuctionTime } from "@/utils/auctionTime";
import { getAuctionOptions, requireAuctionSession, handleAuctionAccessError } from "@/service/auctionService";
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { getAuctionDetail, updateAuction } from "@/service/auctionService";

// Router
const route = useRoute();
const router = useRouter();

// 資料載入狀態
const loading = ref(true);
const canManage = ref(false);

// 是否正在送出修改 Request
const submitting = ref(false);

// 頁面錯誤
const errorMessage = ref("");

// 目前競標狀態
const auctionStatus = ref("");

// 目前成功出價次數
const bidCount = ref(0);

// 目前瀏覽會員是否為此競標的賣家本人
const isOwner = ref(false);

/*
 * 保存後端原始 startAt。
 *
 * ACTIVE 競標的 startAt 不允許修改，
 * 因此送 PUT 時要使用後端原始值，
 * 不依賴 datetime-local 轉換後的字串。
 */
const originalStartAt = ref("");

const brands = ref([]);
const categories = ref([]);

// 編輯表單
const form = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  model: "",
  size: "",
  condition: "",
  description: "",
  defectNote: "",
  authenticityNote: "",
  location: "",

  shippingMethod: "",
  shippingFee: 0,
  paymentMethod: "",
  allowMeetup: false,
  allowReturn: false,
  warrantyNote: "",

  startPrice: null,
  minIncrement: null,
  startAt: "",
  endAt: "",
});

/**
 * 判斷目前畫面是否允許操作編輯表單。
 *
 * 前端只負責 UX；
 * 真正權限仍由後端 updateAuction()
 * 在 Transaction 中重新驗證。
 */
const editable = computed(() => {
  // 不是刊登者本人，不提供編輯操作
  if (!isOwner.value || !canManage.value) {
    return false;
  }

  // 已經有人成功出價，不可修改
  if (bidCount.value > 0) {
    return false;
  }

  // 目前只允許 SCHEDULED / ACTIVE 編輯
  return (
    auctionStatus.value === "SCHEDULED" || auctionStatus.value === "ACTIVE"
  );
});

/**
 * 不允許編輯時顯示的原因。
 */
const editableMessage = computed(() => {
  if (!isOwner.value) {
    return "你無權修改此競標";
  }

  if (bidCount.value > 0) {
    return "此競標已有出價紀錄，無法修改";
  }

  if (
    ["SOLD", "UNSOLD", "CANCELLED", "REMOVED"].includes(auctionStatus.value)
  ) {
    return "目前競標狀態不允許修改";
  }

  return "目前競標無法修改";
});

/**
 * 將後端 LocalDateTime 轉成 datetime-local 可使用的格式。
 *
 * 例如：
 * 2026-09-07T10:00:00
 * →
 * 2026-09-07T10:00
 */
function toDateTimeLocal(value) {
  if (!value) {
    return "";
  }

  return value.slice(0, 16);
}

/**
 * 將詳細資料填入編輯表單。
 */
function fillForm(auction) {
  form.brandId = auction.brandId;
  form.categoryId = auction.categoryId;
  form.productName = auction.productName;
  form.model = auction.model ?? "";
  form.size = auction.size;
  form.condition = auction.condition;
  form.description = auction.description;
  form.defectNote = auction.defectNote ?? "";
  form.authenticityNote = auction.authenticityNote ?? "";
  form.location = auction.location ?? "";

  form.shippingMethod = auction.shippingMethod;
  form.shippingFee = auction.shippingFee;
  form.paymentMethod = auction.paymentMethod;
  form.allowMeetup = auction.allowMeetup;
  form.allowReturn = auction.allowReturn;
  form.warrantyNote = auction.warrantyNote ?? "";

  form.startPrice = auction.startPrice;
  form.minIncrement = auction.minIncrement;
  form.startAt = toDateTimeLocal(auction.startAt);
  form.endAt = toDateTimeLocal(auction.endAt);

  canManage.value = auction.canManage;
  auctionStatus.value = auction.hasStarted && auction.status === "SCHEDULED"
    ? "ACTIVE" : auction.status;
  bidCount.value = auction.bidCount;
  isOwner.value = auction.isOwner;

  // 保存未轉換過的後端原始開始時間
  originalStartAt.value = auction.startAt;
}

/**
 * 讀取目前競標資料。
 *
 * 非賣家本人不可進入競標編輯畫面，
 * 取得詳細資料確認 isOwner=false 後，
 * 直接導向共用 403 權限不足頁。
 */
async function loadAuction() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const auctionId = route.params.id;

    await requireAuctionSession();
    const [response, optionsResponse] = await Promise.all([
      getAuctionDetail(auctionId), getAuctionOptions(),
    ]);
    brands.value = optionsResponse.data.data.brands;
    categories.value = optionsResponse.data.data.categories;

    const auction = response.data.data;

    /*
     * 編輯頁屬於賣家本人操作區。
     *
     * 非本人即使直接輸入 /auctions/{id}/edit，
     * 也不顯示編輯表單，而是導向權限不足頁。
     *
     * 後端 PUT API 仍會再次驗證 sellerId，
     * 這裡只是前端的頁面存取控制。
     */
    if (!auction.isOwner) {
      await router.replace({
        name: "forbidden",
      });

      return;
    }

    fillForm(auction);
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("取得競標編輯資料失敗：", error);

    if (error.response?.status === 404) {
      errorMessage.value = "找不到指定的競標資料";
    } else {
      errorMessage.value = "無法取得競標資料";
    }
  } finally {
    loading.value = false;
  }
}

/**
 * 修改前的基本前端檢查。
 *
 * 後端仍是最終商業規則判定者。
 */
function validateForm() {
  if (!form.startAt || !form.endAt) {
    errorMessage.value = "請設定競標開始與結束時間";

    return false;
  }

  const startAt =
    auctionStatus.value === "ACTIVE"
      ? parseAuctionTime(originalStartAt.value)
      : parseAuctionTime(form.startAt);

  const endAt = parseAuctionTime(form.endAt);

  if (!Number.isFinite(startAt.getTime()) || !Number.isFinite(endAt.getTime()) || endAt <= startAt) {
    errorMessage.value = "競標結束時間必須晚於開始時間";

    return false;
  }

  if (endAt <= new Date()) {
    errorMessage.value = "競標結束時間必須晚於目前時間";

    return false;
  }

  return true;
}

/**
 * 提交修改。
 */
async function submitUpdate() {
  errorMessage.value = "";

  if (!editable.value || submitting.value) {
    return;
  }

  if (!validateForm()) {
    return;
  }

  submitting.value = true;

  try {
    const auctionId = route.params.id;

    /*
     * ACTIVE 的 startAt 必須使用原始後端值，
     * 確保前端不會意外改動已經發生的開始時間。
     */
    const request = {
      ...form,

      startAt:
        auctionStatus.value === "ACTIVE" ? originalStartAt.value : form.startAt,
    };

    await updateAuction(auctionId, request);

    // 修改成功後回詳細頁重新讀取最新資料
    await router.push({
      name: "auction-detail",
      params: {
        id: auctionId,
      },
    });
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("修改競標失敗：", error);

    /*
     * 例如：
     * - 剛好有人先完成出價
     * - 非本人
     * - 狀態已改變
     * - 時間規則錯誤
     *
     * 都以後端最新判定為準。
     */
    errorMessage.value =
      error.response?.data?.message ?? "修改競標失敗，請稍後再試";
  } finally {
    submitting.value = false;
  }
}

/**
 * 返回目前競標詳細頁。
 */
function goBackToDetail() {
  router.push({
    name: "auction-detail",
    params: {
      id: route.params.id,
    },
  });
}

/**
 * 競標狀態中文顯示。
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

// 頁面載入後讀取目前競標
onMounted(() => {
  loadAuction();
});
</script>
