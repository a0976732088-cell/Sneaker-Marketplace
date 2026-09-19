import test from "node:test";
import assert from "node:assert/strict";

import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_SHOE_SIZES,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  MAX_AUCTION_IMAGE_COUNT,
  TAIWAN_CITIES,
  normalizeTaiwanCity,
} from "../src/constants/auctionFormOptions.js";

test("auction form options expose the shared shoe size allowlist", () => {
  assert.deepEqual([...AUCTION_SHOE_SIZES], [
    "US 8",
    "US 8.5",
    "US 9",
    "US 9.5",
    "US 10",
    "US 10.5",
    "US 11",
  ]);
  assert.equal(Object.isFrozen(AUCTION_SHOE_SIZES), true);
});

test("auction form options expose the exact preset copy", () => {
  assert.deepEqual([...AUCTION_DEFECT_NOTE_OPTIONS], [
    "無明顯瑕疵",
    "有輕微使用痕跡",
    "鞋盒有輕微損傷",
    "配件不完整",
  ]);
  assert.deepEqual([...AUCTION_AUTHENTICITY_NOTE_OPTIONS], [
    "品牌官方門市／專櫃購入",
    "品牌官方網站購入",
    "授權經銷商購入",
    "海外官方／授權通路購入",
    "二手平台購入",
  ]);
  assert.deepEqual([...AUCTION_SHIPPING_METHOD_OPTIONS], [
    "宅配",
    "郵寄",
    "超商寄送",
  ]);
  assert.deepEqual([...AUCTION_PAYMENT_METHOD_OPTIONS], ["轉帳", "信用卡"]);
  assert.deepEqual([...AUCTION_WARRANTY_NOTE_OPTIONS], [
    "無額外保固",
    "依原購買通路／品牌規範",
    "售後問題可與賣家聯繫協調",
  ]);

  for (const options of [
    AUCTION_DEFECT_NOTE_OPTIONS,
    AUCTION_AUTHENTICITY_NOTE_OPTIONS,
    AUCTION_SHIPPING_METHOD_OPTIONS,
    AUCTION_PAYMENT_METHOD_OPTIONS,
    AUCTION_WARRANTY_NOTE_OPTIONS,
  ]) {
    assert.equal(Object.isFrozen(options), true);
    assert.equal(options.includes(CUSTOM_OPTION_VALUE), false);
  }
});

test("auction form UI sentinels stay distinct from backend values", () => {
  assert.equal(CUSTOM_OPTION_VALUE, "**CUSTOM**");
  assert.deepEqual(DELIVERY_OPTION_VALUES, {
    SHIPPING_ONLY: "SHIPPING_ONLY",
    SHIPPING_OR_MEETUP: "SHIPPING_OR_MEETUP",
  });
  assert.equal(Object.isFrozen(DELIVERY_OPTION_VALUES), true);
  assert.equal(Object.hasOwn(DELIVERY_OPTION_VALUES, "MEETUP_ONLY"), false);
});

test("auction form options expose the exact 22 official Taiwan cities", () => {
  assert.deepEqual([...TAIWAN_CITIES], [
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
  assert.equal(TAIWAN_CITIES.length, 22);
  assert.equal(new Set(TAIWAN_CITIES).size, 22);
  assert.equal(Object.isFrozen(TAIWAN_CITIES), true);
});

test("Taiwan city normalization accepts exact 台 aliases without guessing", () => {
  assert.equal(normalizeTaiwanCity(""), "");
  assert.equal(normalizeTaiwanCity("高雄市"), "高雄市");
  assert.equal(normalizeTaiwanCity("臺北市"), "臺北市");
  assert.equal(normalizeTaiwanCity("台北市"), "臺北市");
  assert.equal(normalizeTaiwanCity("台中市"), "臺中市");
  assert.equal(normalizeTaiwanCity("台南市"), "臺南市");
  assert.equal(normalizeTaiwanCity("台東縣"), "臺東縣");

  for (const legacyValue of ["新北", "北部", "左營", "台北信義區"]) {
    assert.equal(normalizeTaiwanCity(legacyValue), legacyValue);
  }
});

test("auction create image count limit is ten", () => {
  assert.equal(MAX_AUCTION_IMAGE_COUNT, 10);
});
