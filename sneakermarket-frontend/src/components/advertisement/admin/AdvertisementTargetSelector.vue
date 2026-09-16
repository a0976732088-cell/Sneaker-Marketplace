<script setup>
import { ref, watch } from "vue";
import httpClient from "@/service/httpClient";
import { getAuctions } from "@/service/auctionService";

const props = defineProps({
  targetType: {
    type: String,
    required: true,
  },
});

const emit = defineEmits(["select", "close"]);

const keyword = ref("");
const results = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const hasSearched = ref(false);

const CATEGORY_OPTIONS = [
  {
    id: 1,
    name: "籃球鞋",
    detail: "籃球鞋分類商品",
    imageUrl: null,
  },
  {
    id: 2,
    name: "慢跑鞋",
    detail: "慢跑鞋分類商品",
    imageUrl: null,
  },
  {
    id: 3,
    name: "休閒鞋",
    detail: "休閒鞋分類商品",
    imageUrl: null,
  },
  {
    id: 4,
    name: "滑板鞋",
    detail: "滑板鞋分類商品",
    imageUrl: null,
  },
];

watch(
  () => props.targetType,
  (targetType) => {
    keyword.value = "";
    results.value = [];
    errorMessage.value = "";
    hasSearched.value = false;

    if (targetType === "CATEGORY") {
      results.value = [...CATEGORY_OPTIONS];
      hasSearched.value = true;
    }
  },
  { immediate: true },
);

async function searchTargets() {
  loading.value = true;
  errorMessage.value = "";
  results.value = [];
  hasSearched.value = true;

  try {
    if (props.targetType === "PRODUCT") {
      const response = await httpClient.get("/products", {
        params: {
          keyword: keyword.value.trim() || undefined,
        },
      });

      results.value = response.data.map((product) => ({
        id: product.productId,
        name: product.productName,
        detail: `${product.brandName}・NT$ ${product.price}`,
        imageUrl: product.imageUrl,
      }));
    }

    if (props.targetType === "AUCTION") {
      const response = await getAuctions({
        keyword: keyword.value.trim() || undefined,
        page: 0,
        size: 10,
      });

      results.value = (response.data.data.items || []).map((auction) => ({
        id: auction.auctionId,
        name: auction.productName,
        detail: `${auction.model || "未提供型號"}・目前 NT$ ${auction.currentPrice}`,
        imageUrl: null,
      }));
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = "搜尋資料失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
}

function selectTarget(target) {
  emit("select", target);
}

function getTargetTitle(targetType) {
  const titles = {
    PRODUCT: "一般商品",
    AUCTION: "競標商品",
    CATEGORY: "商品分類",
  };

  return titles[targetType] || "導向目標";
}

function getSearchPlaceholder(targetType) {
  const placeholders = {
    PRODUCT: "搜尋商品名稱",
    AUCTION: "搜尋競標商品名稱或型號",
    CATEGORY: "搜尋分類名稱",
  };

  return placeholders[targetType] || "搜尋導向目標";
}
</script>

<template>
  <aside class="target-drawer">
    <header class="target-drawer-header">
      <div>
        <span>SELECT TARGET</span>
        <h2>{{ getTargetTitle(props.targetType) }}</h2>
        <p>
          {{
            props.targetType === "CATEGORY"
              ? "直接選擇一個商品分類，作為廣告導向目標。"
              : "搜尋後選擇一筆資料，作為廣告導向目標。"
          }}
        </p>
      </div>

      <button type="button" aria-label="關閉" @click="emit('close')">×</button>
    </header>

    <div class="target-drawer-body">
      <div v-if="props.targetType !== 'CATEGORY'" class="search-row">
        <input
          v-model.trim="keyword"
          type="search"
          :placeholder="getSearchPlaceholder(props.targetType)"
          @keyup.enter="searchTargets"
        />

        <button type="button" @click="searchTargets">搜尋</button>
      </div>

      <p v-if="loading" class="hint">搜尋中……</p>
      <p v-else-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </p>

      <ul v-else-if="results.length" class="target-results">
        <li v-for="target in results" :key="target.id">
          <img
            v-if="target.imageUrl"
            :src="target.imageUrl"
            :alt="target.name"
          />
          <div v-else class="image-placeholder">暫無圖片</div>

          <div class="target-info">
            <strong>{{ target.name }}</strong>
            <small>{{ target.detail }}</small>
          </div>

          <button type="button" @click="selectTarget(target)">選擇</button>
        </li>
      </ul>
      <p v-else-if="hasSearched" class="hint">
        查無符合條件的{{ getTargetTitle(props.targetType) }}
      </p>
    </div>
  </aside>
</template>

<style scoped>
.target-drawer {
  position: absolute;
  z-index: 2;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: min(520px, 46vw);
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  box-shadow: 20px 0 50px #00000029;
  animation: target-slide-in 0.25s ease-out;
}

.target-drawer-header {
  display: flex;
  justify-content: space-between;
  padding: 30px;
  border-bottom: 1px solid #e2e6ec;
}

.target-drawer-header span {
  color: var(--coral);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.target-drawer-header h2 {
  margin: 6px 0;
  color: var(--ink);
  font-size: 25px;
}

.target-drawer-header p {
  margin: 0;
  color: #748092;
  font-size: 12px;
}

.target-drawer-header button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: #f4f5f7;
  font-size: 21px;
  cursor: pointer;
}

.target-drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.search-row {
  display: flex;
  gap: 8px;
}

.search-row input {
  min-width: 0;
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #cfd6df;
  border-radius: 7px;
}

.search-row button,
.target-results button {
  padding: 8px 12px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: #2563eb;
  cursor: pointer;
}

.target-results {
  display: grid;
  gap: 8px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.target-results li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 1px solid #e2e6ec;
  border-radius: 7px;
  background: #fff;
}

.target-results img,
.image-placeholder {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 5px;
}

.image-placeholder {
  display: grid;
  flex: 0 0 52px;
  place-items: center;
  color: #8b95a5;
  background: #edf0f4;
  font-size: 10px;
}

.target-info {
  min-width: 0;
  flex: 1;
}

.target-info strong,
.target-info small {
  display: block;
}

.target-info small,
.hint {
  margin-top: 3px;
  color: #748092;
  font-size: 12px;
}

.error-message {
  color: #dc2626;
  font-size: 12px;
}

@keyframes target-slide-in {
  from {
    transform: translateX(-100%);
  }
}

@media (max-width: 900px) {
  .target-drawer {
    width: min(520px, 94vw);
  }
}
</style>
