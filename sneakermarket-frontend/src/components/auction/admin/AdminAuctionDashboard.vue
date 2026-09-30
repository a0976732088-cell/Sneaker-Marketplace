<template>
  <section class="auction-dashboard mb-4" aria-labelledby="auction-dashboard-title" :aria-busy="loading">
    <div class="dashboard-heading">
      <div>
        <h2 id="auction-dashboard-title">競標營運總覽</h2>
        <p>全部競標與商城可見筆數分開統計，不受下方篩選影響。</p>
      </div>
      <div class="dashboard-refresh">
        <span class="update-time" role="status">
          {{ loading ? "統計更新中…" : updatedAt ? `更新於 ${formattedUpdatedAt}` : "尚未取得統計" }}
        </span>
        <button class="btn btn-sm btn-outline-secondary" type="button" :disabled="loading || disabled" @click="refresh">
          <FontAwesomeIcon :icon="faRotateRight" aria-hidden="true" />
          {{ errorMessage ? "重試" : "更新總覽" }}
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="alert alert-warning py-2 small" role="alert">{{ errorMessage }}</div>

    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.key" class="metric-tile" :class="`metric-${metric.tone}`">
        <div class="metric-heading">
          <h3>{{ metric.label }}<span v-if="metric.qualifier" class="metric-qualifier">{{ metric.qualifier }}</span></h3>
          <span class="metric-icon"><FontAwesomeIcon :icon="metric.icon" aria-hidden="true" /></span>
        </div>
        <div class="metric-value">{{ formatCount(counts?.[metric.key]) }}<span>筆</span></div>
        <p>{{ metric.description }}</p>
      </article>
    </div>

    <div class="analysis-grid">
      <article class="analysis-panel">
        <div class="panel-heading">
          <h3>競標狀態分布</h3>
          <span>以全部競標計算筆數與占比</span>
        </div>
        <div class="distribution-track" aria-hidden="true">
          <span v-for="status in statusRows" :key="status.key" :style="{ width: `${status.percent}%`, background: status.color }"></span>
        </div>
        <p v-if="!counts" class="distribution-note">{{ loading ? "正在讀取全部競標統計…" : "統計尚未取得" }}</p>
        <p v-else-if="statusTotal === 0" class="distribution-note">目前尚無競標資料。</p>
        <ul class="status-grid">
          <li v-for="status in statusRows" :key="status.key">
            <span class="status-label"><i :style="{ background: status.color }" aria-hidden="true"></i>{{ status.label }}</span>
            <span class="status-value">{{ formatCount(status.count) }}<small>{{ counts ? `${status.percent.toFixed(1)}%` : "—" }}</small></span>
          </li>
        </ul>
      </article>

      <article class="analysis-panel management-panel">
        <div class="panel-heading">
          <h3>管理狀態</h3>
          <span>平台顯示與處置</span>
        </div>
        <dl class="management-list">
          <div v-for="state in managementStates" :key="state.key">
            <dt><span class="management-dot" :style="{ background: state.color }" aria-hidden="true"></span>{{ state.label }}</dt>
            <dd>{{ formatCount(counts?.[state.key]) }}<span>筆</span></dd>
          </div>
        </dl>
        <p class="management-note">「未下架／刪除」仍可能包含已取消或草稿，與商城可見筆數不同。「廢標」是競標結果，「已下架」是管理狀態。</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, watch } from "vue";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { faGavel, faLayerGroup, faStore, faTrophy, faWallet, faRotateRight } from "@fortawesome/free-solid-svg-icons";
import { useAdminAuctionOverview } from "@/composables/useAdminAuctionOverview";
import { formatMemberAuctionStatus } from "@/utils/auctionMemberStatus";

const props = defineProps({
  refreshKey: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false },
});
const { counts, loading, errorMessage, updatedAt, refresh } = useAdminAuctionOverview();
const numberFormatter = new Intl.NumberFormat("zh-TW");
const timeFormatter = new Intl.DateTimeFormat("zh-TW", {
  month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false,
});
const formattedUpdatedAt = computed(() => updatedAt.value ? timeFormatter.format(updatedAt.value) : "");
const metrics = [
  { key: "total", label: "全部競標", qualifier: "（含非公開）", description: "含取消、草稿、下架及刪除", icon: faLayerGroup, tone: "total" },
  { key: "storefrontVisible", label: "商城可見", description: "商城未套用篩選時的商品總數", icon: faStore, tone: "storefront" },
  { key: "ACTIVE", label: "競標中", description: "依全部競標資料統計", icon: faGavel, tone: "active" },
  { key: "SOLD", label: "已結標", description: "有得標者，尚不代表完成付款", icon: faTrophy, tone: "sold" },
  { key: "pendingPayment", label: "待付款", description: "得標後等待買家付款的訂單", icon: faWallet, tone: "pending" },
];
const statusDefinitions = [
  { key: "ACTIVE", color: "#56836a" },
  { key: "SCHEDULED", color: "#6485b3" },
  { key: "SOLD", color: "#424c5d" },
  { key: "UNSOLD", color: "#b28c52" },
  { key: "CANCELLED", color: "#a58488" },
  { key: "REMOVED", color: "#c46555" },
  { key: "DRAFT", color: "#98a2b3" },
];
const managementStates = [
  { key: "managementVISIBLE", label: "未下架／刪除", color: "#56836a" },
  { key: "managementREMOVED", label: "已下架", color: "#b28c52" },
  { key: "managementDELETED", label: "已刪除", color: "#c46555" },
];
const statusTotal = computed(() => statusDefinitions.reduce((sum, status) => sum + (counts.value?.[status.key] ?? 0), 0));
const statusRows = computed(() => statusDefinitions.map((status) => ({
  ...status,
  label: formatMemberAuctionStatus(status.key),
  count: counts.value?.[status.key],
  percent: statusTotal.value > 0 ? counts.value[status.key] / statusTotal.value * 100 : 0,
})));

function formatCount(value) {
  return value == null ? "—" : numberFormatter.format(value);
}

onMounted(refresh);
watch(() => props.refreshKey, refresh);
</script>

<style scoped>
.auction-dashboard { color: #202733; }
.dashboard-heading, .dashboard-refresh, .metric-heading, .panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.dashboard-heading { margin-bottom: 16px; flex-wrap: wrap; }
.dashboard-heading h2 { margin: 0 0 6px; font-size: 18px; font-weight: 750; }
.dashboard-heading p, .metric-tile p, .management-note, .distribution-note {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.7;
}
.dashboard-refresh { flex-wrap: wrap; justify-content: flex-end; }
.update-time { color: #667085; font-size: 12px; }
.dashboard-refresh button { display: inline-flex; align-items: center; gap: 7px; white-space: nowrap; }
.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-bottom: 14px; }
.metric-tile { padding: 18px 20px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
.metric-heading { min-height: 40px; }
.metric-heading h3 { margin: 0; font-size: 13px; font-weight: 650; color: #596273; }
.metric-qualifier { display: block; margin-top: 3px; font-size: 11px; font-weight: 400; white-space: nowrap; }
.metric-icon { display: grid; width: 32px; height: 32px; flex-shrink: 0; place-items: center; border-radius: 9px; background: #f0f3f6; color: #53657d; }
.metric-value { display: flex; align-items: baseline; gap: 8px; margin: 9px 0 5px; font-size: 30px; line-height: 1.15; font-weight: 750; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.metric-value > span { font-size: 12px; font-weight: 400; color: #667085; flex-shrink: 0; }
.metric-total { background: #202733; border-color: #202733; color: #fff; }
.metric-total h3, .metric-total p, .metric-total .metric-value > span { color: #d0d5dd; }
.metric-total .metric-icon { color: #fff; background: #394250; }
.metric-active .metric-icon { color: #426a52; background: #edf5ef; }
.metric-pending .metric-icon { color: #8c692f; background: #faf2e3; }
.analysis-grid { display: grid; grid-template-columns: minmax(0, 1.8fr) minmax(0, 1fr); gap: 14px; }
.analysis-panel { min-width: 0; padding: 20px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
.panel-heading h3 { margin: 0; font-size: 14px; font-weight: 700; }
.panel-heading > span { font-size: 11px; color: #667085; }
.distribution-track { display: flex; height: 12px; margin: 22px 0 18px; overflow: hidden; border-radius: 6px; background: #f0f2f5; }
.distribution-track span { height: 100%; }
.distribution-note { margin-bottom: 12px; }
.status-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px 12px; margin: 0; padding: 0; list-style: none; }
.status-label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #596273; white-space: nowrap; }
.status-label i, .management-dot { display: inline-block; width: 7px; height: 7px; flex-shrink: 0; border-radius: 50%; }
.status-value { display: flex; flex-wrap: wrap; align-items: baseline; gap: 7px; padding-left: 13px; margin-top: 5px; font-size: 17px; font-weight: 650; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.status-value small { font-size: 10px; color: #667085; font-weight: 400; }
.management-list { margin: 12px 0 10px; }
.management-list > div { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; padding: 9px 0; border-bottom: 1px solid #f0f2f5; }
.management-list dt { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 500; }
.management-list dd { margin: 0; font-size: 16px; font-weight: 650; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.management-list dd span { margin-left: 7px; font-size: 11px; font-weight: 400; color: #667085; }
.management-note { font-size: 11px; }
@media (max-width: 1199px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metric-tile:last-child { grid-column: 1 / -1; }
  .status-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 767px) {
  .analysis-grid { grid-template-columns: minmax(0, 1fr); }
  .dashboard-refresh { justify-content: flex-start; }
}
@media (max-width: 480px) {
  .metric-grid { gap: 10px; }
  .metric-tile { padding: 14px; }
  .metric-icon { width: 26px; height: 26px; font-size: 12px; }
  .metric-value { font-size: 26px; }
  .metric-tile p { font-size: 11px; }
  .analysis-panel { padding: 16px; }
  .status-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
