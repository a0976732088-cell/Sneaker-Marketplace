export const CUSTOM_OPTION_VALUE = "**CUSTOM**";

export const AUCTION_SHOE_SIZES = Object.freeze([
  "US 8",
  "US 8.5",
  "US 9",
  "US 9.5",
  "US 10",
  "US 10.5",
  "US 11",
]);

export const TAIWAN_CITIES = Object.freeze([
  "臺北市",
  "新北市",
  "桃園市",
  "臺中市",
  "臺南市",
  "高雄市",
  "基隆市",
  "新竹市",
  "嘉義市",
  "新竹縣",
  "苗栗縣",
  "彰化縣",
  "南投縣",
  "雲林縣",
  "嘉義縣",
  "屏東縣",
  "宜蘭縣",
  "花蓮縣",
  "臺東縣",
  "澎湖縣",
  "金門縣",
  "連江縣",
]);

export const MAX_AUCTION_IMAGE_COUNT = 10;

export const AUCTION_MODEL_OPTIONS_BY_BRAND = Object.freeze({
  Nike: Object.freeze([
    "Air Force 1",
    "Dunk Low",
    "Air Max 1",
    "Air Max 90",
    "Air Max 97",
    "Blazer Mid '77",
    "Cortez",
    "Pegasus",
  ]),
  Jordan: Object.freeze([
    "Air Jordan 1",
    "Air Jordan 3",
    "Air Jordan 4",
    "Air Jordan 5",
    "Air Jordan 11",
    "Jordan 1 Low",
    "Jordan 1 Mid",
  ]),
  adidas: Object.freeze([
    "Samba OG",
    "Gazelle",
    "Superstar",
    "Stan Smith",
    "Campus 00s",
    "Ultraboost",
    "Yeezy Boost 350 V2",
  ]),
  "New Balance": Object.freeze([
    "327",
    "530",
    "574",
    "9060",
    "990",
    "2002R",
    "1906R",
  ]),
  Vans: Object.freeze([
    "Old Skool",
    "Authentic",
    "Sk8-Hi",
    "Classic Slip-On",
    "Knu Skool",
    "Era",
  ]),
});

export const AUCTION_DESCRIPTION_OPTIONS = Object.freeze([
  "全新未落地，僅開盒檢查，鞋款與配件狀況良好。",
  "近全新，僅短暫試穿，鞋底與鞋面無明顯使用痕跡。",
  "正常二手使用狀況，已完成基本清潔，詳細鞋況請參考照片。",
  "收藏釋出，平時妥善保存，盒裝與配件內容以照片為準。",
]);

export function normalizeTaiwanCity(value) {
  if (typeof value !== "string") return "";

  const candidate = value.trim();
  if (candidate === "") return "";
  if (TAIWAN_CITIES.includes(candidate)) return candidate;

  if (candidate.startsWith("台")) {
    const officialCandidate = `臺${candidate.slice(1)}`;
    if (TAIWAN_CITIES.includes(officialCandidate)) return officialCandidate;
  }

  return value;
}

export const AUCTION_DEFECT_NOTE_OPTIONS = Object.freeze([
  "無明顯瑕疵",
  "有輕微使用痕跡",
  "鞋盒有輕微損傷",
  "配件不完整",
]);

export const AUCTION_AUTHENTICITY_NOTE_OPTIONS = Object.freeze([
  "品牌官方門市／專櫃購入",
  "品牌官方網站購入",
  "授權經銷商購入",
  "海外官方／授權通路購入",
  "二手平台購入",
]);

export const AUCTION_SHIPPING_METHOD_OPTIONS = Object.freeze([
  "宅配",
  "郵寄",
  "超商寄送",
]);

export const AUCTION_PAYMENT_METHOD_OPTIONS = Object.freeze([
  "轉帳",
  "信用卡",
  "LINE Pay",
]);

export const AUCTION_WARRANTY_NOTE_OPTIONS = Object.freeze([
  "無額外保固",
  "依原購買通路／品牌規範",
  "售後問題可與賣家聯繫協調",
]);

export const DELIVERY_OPTION_VALUES = Object.freeze({
  SHIPPING_ONLY: "SHIPPING_ONLY",
  SHIPPING_OR_MEETUP: "SHIPPING_OR_MEETUP",
});
