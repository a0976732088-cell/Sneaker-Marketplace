import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { ref } from "vue";

async function readSource(path) {
  return (await readFile(new URL(path, import.meta.url), "utf8")).replace(/\r\n/g, "\n");
}

function styleBlock(source) {
  const match = source.match(/<style\b[^>]*>([\s\S]*?)<\/style>/);
  assert.ok(match, "expected style block");
  return match[1];
}

function routeBlock(source, path) {
  const start = source.indexOf(`path: "${path}"`);
  assert.notEqual(start, -1, `route ${path}`);
  const end = source.indexOf("\n  },", start);
  assert.notEqual(end, -1, `route boundary ${path}`);
  return source.slice(start, end);
}

function memberNavBlock(source) {
  const match = source.match(/<nav class="member-nav"[^>]*>([\s\S]*?)<\/nav>/);
  assert.ok(match, "expected inline member navigation");
  return match[1];
}

const memberHomeSource = await readSource("../src/views/member/MemberHomeView.vue");
const memberOrdersSource = await readSource("../src/views/member/MemberOrderListView.vue");
const memberPageSource = await readSource("../src/components/auction/AuctionMemberPage.vue");
const recordSource = await readSource("../src/components/auction/AuctionRecordItem.vue");
const bidsSource = await readSource("../src/views/auction/AuctionMyBidsView.vue");
const listingsSource = await readSource("../src/views/auction/AuctionMyListingsView.vue");
const ordersSource = await readSource("../src/views/auction/AuctionOrderListView.vue");
const routerSource = await readSource("../src/router/index.js");

test("member profile, commerce orders and auction pages keep the same inline member navigation", () => {
  for (const source of [memberHomeSource, memberOrdersSource, memberPageSource]) {
    assert.doesNotMatch(source, /MemberCenterNav/);

    const nav = memberNavBlock(source);
    for (const destination of [
      /(?:name:\s*['"]member['"]|to=["']\/member["'])/,
      /(?:name:\s*['"]coupons['"]|to=["']\/coupons["'])/,
      /(?:name:\s*['"]member-orders['"]|to=["']\/member\/orders["'])/,
      /name:\s*['"]auction-my-bids['"]/,
    ]) {
      assert.match(nav, destination);
    }

    const css = styleBlock(source);
    const navRule = css.match(/\.member-nav\s*\{([^}]*)\}/)?.[1] ?? "";
    assert.match(navRule, /display:\s*flex/);
    assert.match(navRule, /justify-content:\s*center/);
    assert.match(navRule, /gap:\s*35px/);
    assert.match(navRule, /padding:\s*30px 20px/);
    assert.match(navRule, /border-bottom:\s*1px solid #eee/);
    assert.match(css, /\.member-nav a\s*\{[\s\S]*?color:\s*#111[\s\S]*?font-weight:\s*600/);
    assert.match(css, /\.member-nav[^{}]*\.router-link-active\s*\{[\s\S]*?text-decoration:\s*underline[\s\S]*?text-underline-offset:\s*6px/);
    const mobile = css.slice(css.indexOf("@media (max-width: 768px)"));
    assert.match(mobile, /\.member-nav\s*\{[\s\S]*?overflow-x:\s*auto/);
  }

  const auctionNav = memberNavBlock(memberPageSource);
  assert.match(
    auctionNav,
    /name:\s*['"]auction-my-bids['"][\s\S]*?class="router-link-active"[\s\S]*?aria-current="page"/,
  );
});

test("member auction canonical routes use the member namespace without changing names or views", () => {
  const unchangedRoutes = [
    ["/member", "member", "@/views/member/MemberHomeView.vue"],
    ["/member/orders", "member-orders", "@/views/member/MemberOrderListView.vue"],
  ];
  const canonicalRoutes = [
    ["/member/auctions/bids", "auction-my-bids", "@/views/auction/AuctionMyBidsView.vue"],
    ["/member/auctions/listings", "auction-my-listings", "@/views/auction/AuctionMyListingsView.vue"],
    ["/member/auctions/orders", "auction-orders", "@/views/auction/AuctionOrderListView.vue"],
  ];

  for (const [path, name, component] of [...unchangedRoutes, ...canonicalRoutes]) {
    const block = routeBlock(routerSource, path);
    assert.match(block, new RegExp(`name:\\s*"${name}"`));
    assert.ok(block.includes(`import("${component}")`), component);
  }

  const topLevelNames = [...routerSource.matchAll(/^ {4}name:\s*"([^"]+)",/gm)]
    .map((match) => match[1]);
  for (const [, name] of canonicalRoutes) {
    assert.equal(topLevelNames.filter((candidate) => candidate === name).length, 1, name);
  }
});

test("legacy auction member URLs redirect by name to the canonical routes", () => {
  const redirects = [
    ["/auctions/me/bids", "auction-my-bids"],
    ["/auctions/me/listings", "auction-my-listings"],
    ["/auction-orders", "auction-orders"],
  ];

  for (const [path, name] of redirects) {
    const block = routeBlock(routerSource, path);
    assert.match(block, new RegExp(`redirect:\\s*\\{\\s*name:\\s*"${name}"\\s*\\}`));
    assert.doesNotMatch(block, /^ {4}name:/m);
    assert.doesNotMatch(block, /\bcomponent\s*:/);
  }
});

test("auction order checkout and detail routes keep a parameterized legacy payment redirect", () => {
  const checkout = routeBlock(routerSource, "/member/auctions/orders/:orderNumber/checkout");
  assert.match(checkout, /name:\s*"auction-order-checkout"/);
  assert.match(checkout, /AuctionOrderCheckoutView\.vue/);

  const detail = routeBlock(routerSource, "/member/auctions/orders/:orderNumber");
  assert.match(detail, /name:\s*"auction-order-detail"/);
  assert.match(detail, /AuctionOrderDetailView\.vue/);

  const legacy = routeBlock(routerSource, "/auction-orders/:orderNumber/payment");
  assert.match(legacy, /name:\s*"auction-order-payment"/);
  assert.match(legacy, /redirect:\s*\(to\)\s*=>/);
  assert.match(legacy, /name:\s*"auction-order-checkout"/);
  assert.match(legacy, /orderNumber:\s*to\.params\.orderNumber/);
  assert.doesNotMatch(legacy, /\bcomponent\s*:/);
});

test("auction member shell uses a page heading and underline second-level navigation", () => {
  assert.match(memberPageSource, /<h1>競標紀錄<\/h1>/);
  assert.match(memberPageSource, /管理你的出價、刊登商品與競標訂單/);
  assert.match(memberPageSource, /aria-label="競標紀錄分類"/);
  for (const [route, label] of [
    ["auction-my-bids", "出價紀錄"],
    ["auction-my-listings", "刊登紀錄"],
    ["auction-orders", "競標訂單"],
  ]) {
    assert.match(memberPageSource, new RegExp(`name: '${route}'[\\s\\S]*?>\\s*${label}\\s*</RouterLink>`));
  }
  assert.equal((memberPageSource.match(/active-class="is-active"/g) ?? []).length, 3);
  assert.match(memberPageSource, /\.auction-member-tabs\s*\{[\s\S]*?border-bottom:\s*1px solid #dee2e6/);
  assert.match(memberPageSource, /\.auction-member-tab\.is-active::after\s*\{[\s\S]*?opacity:\s*1/);
  assert.doesNotMatch(memberPageSource, /btn-group|btn-outline-primary/);
});

test("AuctionRecordItem provides one reusable commerce row with resilient image fallback", () => {
  assert.match(recordSource, /<article class="auction-record-item">/);
  assert.match(recordSource, /<header class="record-header">/);
  assert.match(recordSource, /<section class="record-product">/);
  assert.match(recordSource, /<section v-if="\$slots\.default" class="record-content">/);
  assert.match(recordSource, /<footer v-if="\$slots\.actions" class="record-actions">/);
  assert.match(recordSource, /v-if="imageUrl && !imageFailed"/);
  assert.match(recordSource, /:src="resolveAuctionImageUrl\(imageUrl\)"/);
  assert.match(recordSource, /@error="imageFailed = true"/);
  assert.match(recordSource, /v-else class="record-image record-image--fallback"/);
  assert.match(recordSource, />暫無圖片</);

  let resetImageFailure;
  const recordScript = recordSource.match(/<script setup>([\s\S]*?)<\/script>/)[1]
    .replace(/^import[^\n]+\n/gm, "");
  const context = {
    defineProps: () => ({ imageUrl: "/uploads/auctions/item.jpg" }),
    ref,
    watch: (_source, callback) => {
      resetImageFailure = callback;
    },
    resolveAuctionImageUrl: (url) => url,
  };
  vm.runInNewContext(
    recordScript + "\nglobalThis.subject = { imageFailed };",
    context,
  );
  context.subject.imageFailed.value = true;
  resetImageFailure();
  assert.equal(context.subject.imageFailed.value, false);
});

test("all auction record removal notices stay on one line", () => {
  for (const source of [bidsSource, listingsSource, ordersSource]) {
    assert.match(
      source,
      /class="record-action-notice record-action-notice--single-line"/,
    );
  }
  assert.match(
    recordSource,
    /\.record-actions :deep\(\.record-action-notice--single-line\)\s*\{[\s\S]*?max-width:\s*none;[\s\S]*?white-space:\s*nowrap;/,
  );
});

test("record list stays single-column and reflows at tablet and mobile widths", () => {
  const listRule = memberPageSource.match(/\.auction-member-page :deep\(\.auction-record-list\)\s*\{([^}]*)\}/)?.[1] ?? "";
  assert.match(listRule, /display:\s*grid/);
  assert.match(listRule, /gap:\s*16px/);
  assert.doesNotMatch(listRule, /grid-template-columns/);

  const css = styleBlock(recordSource);
  const tabletStart = css.indexOf("@media (max-width: 992px)");
  const mobileStart = css.indexOf("@media (max-width: 576px)");
  assert.ok(tabletStart > 0, "tablet breakpoint");
  assert.ok(mobileStart > tabletStart, "mobile breakpoint");
  const desktop = css.slice(0, tabletStart);
  const tablet = css.slice(tabletStart, mobileStart);
  const mobile = css.slice(mobileStart);

  assert.match(desktop, /\.record-body\s*\{[\s\S]*?grid-template-columns:\s*138px minmax\(190px, 1\.2fr\) minmax\(220px, 1fr\) auto/);
  assert.match(desktop, /\.record-body\s*\{[\s\S]*?min-width:\s*0/);
  assert.match(tablet, /\.record-body\s*\{[\s\S]*?grid-template-columns:\s*122px minmax\(0, 1fr\)/);
  assert.match(tablet, /\.record-content,[\s\S]*?\.record-actions\s*\{[\s\S]*?grid-column:\s*1 \/ -1/);
  assert.match(mobile, /\.record-body\s*\{[\s\S]*?grid-template-columns:\s*92px minmax\(0, 1fr\)/);
  assert.match(mobile, /\.record-content :deep\(\.record-metrics\)\s*\{[\s\S]*?grid-template-columns:\s*1fr/);
  assert.match(mobile, /\.record-actions\s*\{[\s\S]*?flex-direction:\s*column/);
  assert.match(mobile, /\.record-actions :deep\(\.record-action\)\s*\{[\s\S]*?width:\s*100%/);

  for (const source of [bidsSource, listingsSource, ordersSource]) {
    assert.match(source, /class="auction-record-list"/);
    assert.doesNotMatch(source, /class="row\b|row g-|col-md-|col-lg-/);
  }
});
