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
