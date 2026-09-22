// KPI 摘要
import { reactive } from "vue";
import { getAdvertisementDashboardSummary } from "@/service/advertisementService";

export function useAdvertisementDashboard() {
  // 四個值屬同一份摘要資料
  const dashboardSummary = reactive({
    activeAdvertisements: 0,
    views: 0,
    clicks: 0,
    ctr: 0,
  });

  async function loadDashboardSummary() {
    try {
      const result = await getAdvertisementDashboardSummary();
      const summary = result.data || {};

      dashboardSummary.activeAdvertisements = summary.activeAdvertisements ?? 0;
      dashboardSummary.views = summary.views ?? 0;
      dashboardSummary.clicks = summary.clicks ?? 0;
      dashboardSummary.ctr = summary.ctr ?? 0;
    } catch (error) {
      console.error("廣告摘要載入失敗", error);
    }
  }

  return {
    dashboardSummary,
    loadDashboardSummary,
  };
}
