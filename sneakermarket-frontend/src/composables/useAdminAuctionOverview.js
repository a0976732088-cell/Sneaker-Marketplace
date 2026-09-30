import { onBeforeUnmount, ref } from "vue";
import { getAdminAuctions } from "@/service/auctionAdminService";
import { getAuctions } from "@/service/auctionService";

const countQueries = [
  ["total", {}],
  ["storefrontVisible", {}],
  ...["DRAFT", "SCHEDULED", "ACTIVE", "SOLD", "UNSOLD", "CANCELLED", "REMOVED"].map(
    (status) => [status, { status }],
  ),
  ...["VISIBLE", "REMOVED", "DELETED"].map((managementState) => [
    `management${managementState}`,
    { managementState },
  ]),
  ["pendingPayment", { orderStatus: "PENDING_PAYMENT" }],
];

export function useAdminAuctionOverview() {
  const counts = ref(null);
  const loading = ref(false);
  const errorMessage = ref("");
  const updatedAt = ref(null);
  let requestSequence = 0;

  async function refresh() {
    const sequence = ++requestSequence;
    loading.value = true;
    errorMessage.value = "";
    const nextCounts = {};

    try {
      // Reuse paginated totals without downloading every auction. Limit concurrent reads.
      for (let offset = 0; offset < countQueries.length; offset += 3) {
        if (sequence !== requestSequence) return;
        const results = await Promise.allSettled(
          countQueries.slice(offset, offset + 3).map(async ([key, filters]) => {
            // The storefront applies its own visibility rules, beyond managementState=VISIBLE.
            const query = key === "storefrontVisible" ? getAuctions : getAdminAuctions;
            const response = await query({ ...filters, page: 0, size: 1 });
            const total = response.data?.data?.totalElements;
            if (!Number.isSafeInteger(total) || total < 0) {
              throw new Error("Invalid auction total");
            }
            return [key, total];
          }),
        );
        if (sequence !== requestSequence) return;
        for (const result of results) {
          if (result.status === "rejected") throw result.reason;
          const [key, total] = result.value;
          nextCounts[key] = total;
        }
      }
      // Publish one complete snapshot; a failed request must never look like zero auctions.
      counts.value = nextCounts;
      updatedAt.value = new Date();
    } catch {
      if (sequence !== requestSequence) return;
      errorMessage.value = counts.value
        ? "總覽更新失敗，目前保留上次成功載入的統計。請稍後重試。"
        : "暫時無法載入競標總覽，請稍後重試。下方列表仍可獨立查詢。";
    } finally {
      if (sequence === requestSequence) loading.value = false;
    }
  }

  onBeforeUnmount(() => { requestSequence += 1; });

  return { counts, loading, errorMessage, updatedAt, refresh };
}
