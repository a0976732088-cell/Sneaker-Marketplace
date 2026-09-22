import test from "node:test";
import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import vm from "node:vm";

const routerSource = await readFile(new URL("../src/router/index.js", import.meta.url), "utf8");
const serviceSource = await readFile(new URL("../src/service/auctionService.js", import.meta.url), "utf8");
const listSource = await readFile(new URL("../src/views/auction/AuctionOrderListView.vue", import.meta.url), "utf8");
const checkoutSource = await readFile(new URL("../src/views/auction/AuctionOrderCheckoutView.vue", import.meta.url), "utf8");
const detailSource = await readFile(new URL("../src/views/auction/AuctionOrderDetailView.vue", import.meta.url), "utf8");

function evaluateAuctionService() {
  const requests = [];
  const source = serviceSource.replace(/^import[^\n]+\n/gm, "").replace(/^export /gm, "");
  const context = {
    httpClient: {
      get: (...args) => record("GET", args),
      patch: (...args) => record("PATCH", args),
      post: (...args) => record("POST", args),
      put: (...args) => record("PUT", args),
      delete: () => {},
    },
  };
  function record(method, args) {
    const request = { method, url: args[0], body: args[1], args };
    requests.push(request);
    return request;
  }
  vm.runInNewContext(
    source +
      "\nglobalThis.subject = { getAuctionOrderDetail, updateAuctionOrderCheckout, payAuctionOrder, " +
      "getAuctionCvsSelection, getAuctionCvsStores, selectAuctionCvsStore, requestAuctionLinePay, " +
      "getAuctionLinePayStatus };",
    context,
  );
  return { ...context.subject, requests };
}

function routeBlock(path) {
  const start = routerSource.indexOf(`path: "${path}"`);
  assert.notEqual(start, -1, path);
  const end = routerSource.indexOf("\n  },", start);
  assert.notEqual(end, -1, `${path} boundary`);
  return routerSource.slice(start, end);
}

test("canonical checkout and detail routes use separate views", () => {
  const checkout = routeBlock("/member/auctions/orders/:orderNumber/checkout");
  assert.match(checkout, /name:\s*"auction-order-checkout"/);
  assert.match(checkout, /AuctionOrderCheckoutView\.vue/);

  const detail = routeBlock("/member/auctions/orders/:orderNumber");
  assert.match(detail, /name:\s*"auction-order-detail"/);
  assert.match(detail, /AuctionOrderDetailView\.vue/);
});

test("legacy named payment route redirects with its order number", () => {
  const legacy = routeBlock("/auction-orders/:orderNumber/payment");
  assert.match(legacy, /name:\s*"auction-order-payment"/);
  assert.match(legacy, /redirect:\s*\(to\)\s*=>/);
  assert.match(legacy, /name:\s*"auction-order-checkout"/);
  assert.match(legacy, /orderNumber:\s*to\.params\.orderNumber/);
  assert.doesNotMatch(legacy, /\bcomponent\s*:/);
});

test("old payment view no longer exists or has production references", async () => {
  await assert.rejects(
    access(new URL("../src/views/auction/AuctionOrderPaymentView.vue", import.meta.url)),
  );
  assert.doesNotMatch(routerSource, /AuctionOrderPaymentView/);
});

test("auction service GETs detail, PATCHes only receiver fields and POSTs pay with no body", () => {
  const service = evaluateAuctionService();
  const detail = service.getAuctionOrderDetail("AO/測試");
  assert.equal(detail.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6");

  const update = service.updateAuctionOrderCheckout("AO/測試", {
    receiverName: " 王小明 ",
    receiverPhone: "0912345678",
    shippingAddress: "台北市測試路 1 號",
    finalPrice: 1,
    paymentMethod: "其他",
    cardNumber: "not-sent",
    cardCvv: "not-sent",
    remitterName: "not-sent",
    accountLastFive: "not-sent",
  });
  assert.equal(update.method, "PATCH");
  assert.equal(update.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/checkout");
  assert.deepEqual(JSON.parse(JSON.stringify(update.body)), {
    receiverName: " 王小明 ",
    receiverPhone: "0912345678",
    shippingAddress: "台北市測試路 1 號",
  });

  const pay = service.payAuctionOrder("AO/測試");
  assert.equal(pay.method, "POST");
  assert.equal(pay.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/pay");
  assert.equal(pay.args.length, 1);
  assert.equal(pay.body, undefined);
});

test("auction service keeps in-app CVS list, selection and LINE Pay scoped to auction endpoints", () => {
  const service = evaluateAuctionService();

  const selection = service.getAuctionCvsSelection("AO/測試");
  assert.equal(selection.method, "GET");
  assert.equal(selection.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/cvs-selection");

  const stores = service.getAuctionCvsStores("AO/測試", "UNIMART");
  assert.equal(stores.method, "GET");
  assert.equal(stores.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/cvs/stores");
  assert.deepEqual(JSON.parse(JSON.stringify(stores.body)), {
    params: { storeType: "UNIMART" },
  });

  const selected = service.selectAuctionCvsStore("AO/測試", {
    storeType: "FAMI",
    storeId: "000123",
    storeName: "不可送出",
    storeAddress: "不可送出",
  });
  assert.equal(selected.method, "PUT");
  assert.equal(selected.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/cvs-selection");
  assert.deepEqual(JSON.parse(JSON.stringify(selected.body)), {
    storeType: "FAMI",
    storeId: "000123",
  });

  const linePay = service.requestAuctionLinePay("AO/測試");
  assert.equal(linePay.method, "POST");
  assert.equal(linePay.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/line-pay/request");
  assert.equal(linePay.args.length, 1);
  assert.equal(linePay.body, undefined);

  const linePayStatus = service.getAuctionLinePayStatus("AO/測試", "ALP-STRING-ID");
  assert.equal(linePayStatus.method, "GET");
  assert.equal(
    linePayStatus.url,
    "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/line-pay/status",
  );
  assert.deepEqual(JSON.parse(JSON.stringify(linePayStatus.body)), {
    params: { merchantOrderId: "ALP-STRING-ID" },
  });
});

test("buyer list routes payable orders to checkout and all other orders to detail", () => {
  assert.match(listSource, /v-if="canPayAuctionOrder\(order\)"[\s\S]*?name:\s*'auction-order-checkout'/);
  assert.match(listSource, /v-else[\s\S]*?name:\s*'auction-order-detail'/);
  assert.match(listSource, />\s*立即付款\s*</);
  assert.match(listSource, />\s*查看訂單\s*</);
});

test("seller rows link every order to the read-only detail page", () => {
  const sellerStart = listSource.indexOf('<div v-else class="auction-record-list">');
  const sellerSource = listSource.slice(sellerStart, listSource.lastIndexOf("</AuctionMemberPage>"));
  assert.match(sellerSource, /#actions/);
  assert.match(sellerSource, /name:\s*'auction-order-detail'/);
  assert.match(sellerSource, />\s*查看訂單\s*</);
  assert.doesNotMatch(sellerSource, /立即付款|auction-order-checkout/);
});

test("checkout and detail keep money time image and member access utilities scoped to auctions", () => {
  for (const source of [checkoutSource, detailSource]) {
    assert.match(source, /formatTwd/);
    assert.match(source, /formatAuctionTime/);
    assert.match(source, /resolveAuctionImageUrl/);
    assert.match(source, /handleAuctionAccessError/);
  }
  assert.match(checkoutSource, /parseAuctionTime/);
  assert.doesNotMatch(detailSource, /payAuctionOrder|updateAuctionOrderCheckout/);
});
