import test from "node:test";
import assert from "node:assert/strict";

import { formatTwd } from "../src/utils/auctionMoney.js";

test("競標金額以整數新台幣顯示", () => {
  assert.equal(formatTwd(3000), "NT$ 3,000");
  assert.equal(formatTwd("3500.00"), "NT$ 3,500");
  assert.equal(formatTwd(0), "NT$ 0");
});

test("缺少或無效金額時不顯示 NaN", () => {
  assert.equal(formatTwd(null), "NT$ -");
  assert.equal(formatTwd("invalid"), "NT$ -");
});
