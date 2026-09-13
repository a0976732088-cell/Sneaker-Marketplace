<script setup>
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { faTrash, faPencil } from "@fortawesome/free-solid-svg-icons";
import { resolveAdvertisementImageUrl } from "@/service/advertisementService";

defineProps({
  ads: {
    type: Array,
    required: true,
  },
});

const emit = defineEmits(["edit", "toggle", "delete"]);
</script>

<template>
  <div class="table-wrap">
    <table>
      <!-- 固定各欄位比例，狀態或資料筆數改變時表格不會左右跳動。 -->
      <colgroup>
        <col class="column-ad" />
        <col class="column-placement" />
        <col class="column-period" />
        <col class="column-status" />
        <!-- <col class="column-number" />
                <col class="column-number" />
                <col class="column-ctr" /> -->
        <col class="column-actions" />
      </colgroup>
      <thead>
        <tr>
          <th>廣告內容</th>
          <th>版位／類型</th>
          <th>投放期間</th>
          <th>狀態</th>
          <!-- <th class="number">曝光</th> -->
          <!-- <th class="number">點擊</th> -->
          <!-- <th class="number">CTR</th> -->
          <th class="actions-column">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ad in ads" :key="ad.id">
          <td>
            <div class="ad-identity">
              <!-- 有圖與無圖皆使用固定大小；圖片完整顯示、不裁切 -->
              <div class="ad-thumbnail">
                <!-- 有網址且圖片載入正常時顯示 -->
                <img
                  v-if="ad.imageUrl && !ad.imageLoadFailed"
                  class="ad-thumbnail-image"
                  :src="resolveAdvertisementImageUrl(ad.imageUrl)"
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
          <!-- <td class="number">{{ ad.views }}</td>
                  <td class="number">{{ ad.clicks }}</td>
                  <td class="number">
                    <strong>{{ ad.ctr }}</strong>
                  </td> -->
          <td class="actions-column">
            <div class="row-actions">
              <button
                v-if="['DRAFT', 'INACTIVE'].includes(ad.adStatus)"
                title="編輯"
                @click="emit('edit', ad)"
              >
                <FontAwesomeIcon :icon="faPencil" />
              </button>
              <!--草稿與已停用顯示上架按鈕，進行中顯示停用按鈕，已結束不允許操作，所以不顯示按鈕。-->
              <button
                v-if="['ACTIVE', 'DRAFT', 'INACTIVE'].includes(ad.adStatus)"
                type="button"
                :title="ad.adStatus === 'ACTIVE' ? '停用' : '發布/排程'"
                @click="emit('toggle', ad)"
              >
                {{ ad.adStatus === "ACTIVE" ? "Ⅱ" : "▶" }}
              </button>
              <button
                v-if="['DRAFT', 'INACTIVE'].includes(ad.adStatus)"
                title="刪除"
                @click="emit('delete', ad)"
              >
                <FontAwesomeIcon :icon="faTrash" />
              </button>
            </div>
          </td>
        </tr>
        <tr v-if="!ads.length">
          <td colspan="5" class="empty-state">
            找不到符合條件的廣告，請調整搜尋或篩選條件。
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrap {
  flex: 1;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1100px;
  table-layout: fixed;
  border-collapse: collapse;
}

.column-ad {
  width: 34%;
}

.column-placement {
  width: 16%;
}

.column-period {
  width: 22%;
}

.column-status {
  width: 14%;
}

.column-actions {
  width: 14%;
}

th {
  padding: 11px 16px;
  color: #8993a2;
  background: #fafbfc;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-align: left;
  text-transform: uppercase;
}

td {
  padding: 13px 16px;
  border-top: 1px solid #f0f1f4;
  color: #4f5c6d;
  font-size: 14px;
  vertical-align: middle;
}

tbody tr:hover {
  background: #fcfcfd;
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
  width: 160px;
  height: 120px;
  box-sizing: border-box;
  flex: 0 0 160px;
  place-items: center;
  overflow: hidden;
  border: 1px solid #dfe3e9;
  border-radius: 6px;
  background: #fff;
}

.ad-thumbnail-image {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
}

.ad-thumbnail-empty {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #7b8492;
  background: #eef0f3;
  font-size: 12px;
}

.ad-identity strong,
.ad-identity small,
.cell-main,
.cell-sub {
  display: block;
}

.ad-identity strong {
  margin-bottom: 3px;
  color: var(--ink);
  font-size: 15px;
}

.ad-identity small {
  color: #7a8595;
  font-size: 12px;
}

.cell-main {
  color: #3e4a59;
  font-size: 14px;
}

.cell-sub {
  margin-top: 3px;
  color: #939cab;
  font-size: 12px;
}

.period {
  white-space: nowrap;
  font-size: 12px;
}

.status-badge {
  display: inline-block;
  min-width: 76px;
  padding: 5px 7px;
  box-sizing: border-box;
  border-radius: 12px;
  white-space: nowrap;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
}

.status-badge.已排程 {
  color: #2563eb;
  background: #eff6ff;
}

.status-badge.已取消排程,
.status-badge.已暫停 {
  color: #d97706;
  background: #fff7ed;
}

.status-badge.進行中 {
  color: #168867;
  background: #e8f8f2;
}

.status-badge.草稿 {
  color: #687386;
  background: #f0f2f5;
}

.status-badge.已結束 {
  color: #8d94a0;
  background: #f5f5f6;
}

.actions-column {
  text-align: right;
}

.row-actions {
  display: flex;
  width: 85px;
  gap: 2px;
  justify-content: flex-end;
  margin-left: auto;
}

.row-actions button {
  width: 27px;
  height: 27px;
  border: 0;
  border-radius: 5px;
  color: #7a8493;
  background: transparent;
  font-size: 14px;
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
</style>
