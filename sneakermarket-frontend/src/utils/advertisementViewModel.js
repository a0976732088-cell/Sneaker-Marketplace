// 傳入後端廣告資料-轉換格式-回傳新物件
const TYPE_LABELS = {
  HOT_PRODUCT: "熱門商品",
  NEW_PRODUCT: "新品推薦",
  AUCTION: "競標活動",
  DISCOUNT: "折扣優惠",
  PLATFORM_EVENT: "行事曆活動",
};

const PLACEMENT_LABELS = {
  BANNER: "首頁橫幅",
  POPUP: "首頁彈窗",
  SMALL_BLOCK: "商品頁側欄",
  CATEGORY_CARD: "分類卡片",
};

function resolveDisplayStatus(ad) {
  if (ad.adStatus === "DRAFT") return "草稿";

  if (ad.adStatus === "INACTIVE") {
    const now = Date.now();
    const startAt = new Date(ad.startAt).getTime();

    return now < startAt ? "已取消排程" : "已暫停";
  }

  if (ad.adStatus === "EXPIRED") return "已結束";

  if (ad.adStatus === "ACTIVE") {
    const now = Date.now();
    const startAt = new Date(ad.startAt).getTime();
    const endAt = new Date(ad.endAt).getTime();

    if (now < startAt) return "已排程";
    if (now >= endAt) return "已結束";

    return "進行中";
  }

  return ad.adStatus;
}

function formatDateTime(value) {
  if (!value) return "尚未設定";

  return new Intl.DateTimeFormat("zh-TW", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

export function toAdvertisementViewModel(ad) {
  return {
    id: ad.adId,
    name: ad.adName,
    desc: ad.adDescription || "",
    imageUrl: ad.imageUrl,
    imageLoadFailed: false,

    type: TYPE_LABELS[ad.adType] || ad.adType,
    place: PLACEMENT_LABELS[ad.adPlacement] || ad.adPlacement,
    status: resolveDisplayStatus(ad),

    adStatus: ad.adStatus,
    adType: ad.adType,
    adPlacement: ad.adPlacement,
    startAt: ad.startAt,
    endAt: ad.endAt,
    targetType: ad.targetType,
    targetId: ad.targetId,
    targetUrl: ad.targetUrl,
    sortOrder: ad.sortOrder,

    period:
      ad.startAt || ad.endAt
        ? `${formatDateTime(ad.startAt)} — ${formatDateTime(ad.endAt)}`
        : "尚未設定",

    views: 0,
    clicks: 0,
    ctr: "0.00%",
  };
}
