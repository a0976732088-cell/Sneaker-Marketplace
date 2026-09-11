import test from "node:test";
import assert from "node:assert/strict";
import { parseAuctionTime, formatAuctionTime } from "../src/utils/auctionTime.js";

test("台北表單時間在不同主機時區代表相同時刻", () => {
  assert.equal(parseAuctionTime("2026-09-07T12:00").toISOString(), "2026-09-07T04:00:00.000Z");
});
test("含 offset 的時間不會重複加八小時", () => {
  assert.equal(parseAuctionTime("2026-09-07T04:00:00Z").toISOString(), "2026-09-07T04:00:00.000Z");
  assert.equal(parseAuctionTime("2026-09-07T12:00:00+08:00").toISOString(), "2026-09-07T04:00:00.000Z");
});
test("顯示跨日時間採台北日期", () => {
  assert.match(formatAuctionTime("2026-09-07T20:30:00Z"), /2026\/09\/08.*04:30/);
});
test("空白或錯誤時間不產生有效日期", () => {
  assert.equal(formatAuctionTime(null), "-");
  assert.equal(formatAuctionTime("invalid"), "-");
});
