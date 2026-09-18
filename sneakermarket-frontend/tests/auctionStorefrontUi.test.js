import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { ref } from "vue";

const listSource = await readFile(
  new URL("../src/views/auction/AuctionListView.vue", import.meta.url),
  "utf8",
);
const detailSource = await readFile(
  new URL("../src/views/auction/AuctionDetailView.vue", import.meta.url),
  "utf8",
);
const gallerySource = await readFile(
  new URL("../src/components/auction/AuctionImageGallery.vue", import.meta.url),
  "utf8",
);
const countdownSource = await readFile(
  new URL("../src/components/auction/AuctionCountdown.vue", import.meta.url),
  "utf8",
);

function scriptSetup(source) {
  const match = source.match(/<script setup>([\s\S]*?)<\/script>/);
  assert.ok(match, "expected an exact <script setup> block");
  return match[1].replace(/^import[\s\S]*?from\s+["'][^"']+["'];/gm, "");
}

function section(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker);
  const end = source.indexOf(endMarker, start);
  assert.notEqual(start, -1, `missing ${startMarker}`);
  assert.notEqual(end, -1, `missing ${endMarker}`);
  return source.slice(start, end);
}

function assertIncludesAll(source, values, area) {
  for (const value of values) {
    assert.ok(source.includes(value), `${area} must retain ${value}`);
  }
}

function createListHarness() {
  const requests = [];
  const context = {
    ref,
    console,
    parseAuctionTime: (value) => new Date(value),
    formatAuctionTime: (value) => value,
    formatTwd: (value) => String(value),
    onMounted: () => {},
    onUnmounted: () => {},
    setInterval: () => 1,
    clearInterval: () => {},
    getAuctions: async (params) => {
      requests.push({ ...params });
      return {
        data: {
          data: {
            items: [],
            page: params.page,
            size: params.size,
            totalElements: 0,
            totalPages: 6,
          },
        },
      };
    },
    getAuctionOptions: async () => ({
      data: { data: { brands: [], categories: [] } },
    }),
  };

  vm.runInNewContext(
    scriptSetup(listSource) +
      `
      globalThis.subject = {
        keyword, brandId, categoryId, status, condition, shoeSize,
        minPrice, maxPrice, currentPage, pageSize, totalPages, sort,
        loadAuctions, searchAuctions, clearFilters, changePage
      };
    `,
    context,
  );

  return { requests, subject: context.subject };
}

test("public auction list sends every supported filter through server-side paging and sorting", async () => {
  const { requests, subject } = createListHarness();

  subject.keyword.value = "  Air Jordan  ";
  subject.brandId.value = 11;
  subject.categoryId.value = 22;
  subject.status.value = "ACTIVE";
  subject.condition.value = "NEW";
  subject.shoeSize.value = "US 9";
  subject.minPrice.value = "4000";
  subject.maxPrice.value = "9000";
  subject.currentPage.value = 3;
  subject.sort.value = "currentPrice,desc";

  await subject.loadAuctions();

  assert.deepEqual(requests[0], {
    keyword: "Air Jordan",
    brandId: 11,
    categoryId: 22,
    status: "ACTIVE",
    condition: "NEW",
    shoeSize: "US 9",
    minPrice: 4000,
    maxPrice: 9000,
    page: 3,
    size: 9,
    sort: "currentPrice,desc",
  });

  subject.currentPage.value = 5;
  await subject.searchAuctions();
  assert.equal(requests[1].page, 0, "a new search must return to the first server page");
  assert.equal(requests[1].size, 9);
  assert.equal(requests[1].sort, "currentPrice,desc");

  await subject.clearFilters();
  assert.deepEqual(requests[2], {
    keyword: undefined,
    brandId: undefined,
    categoryId: undefined,
    status: undefined,
    condition: undefined,
    shoeSize: undefined,
    minPrice: undefined,
    maxPrice: undefined,
    page: 0,
    size: 9,
    sort: "endAt,asc",
  });

  const requestCount = requests.length;
  await subject.changePage(-1);
  await subject.changePage(subject.totalPages.value);
  assert.equal(requests.length, requestCount, "invalid pages must not call the API");

  await subject.changePage(2);
  assert.equal(requests.at(-1).page, 2);
  assert.equal(requests.at(-1).size, 9);
});

test("auction cards use the summary image, placeholder and whole-card detail navigation without detail N+1", () => {
  const card = section(
    listSource,
    '<RouterLink\n                v-for="auction in auctions"',
    "<!-- 分頁 -->",
  );

  assert.match(
    card,
    /auction\.imageUrl\s*&&\s*!failedAuctionImageUrls\.has\(auction\.imageUrl\)/,
  );
  assert.match(card, /:src="resolveAuctionImageUrl\(auction\.imageUrl\)"/);
  assert.match(card, /@error="markAuctionImageFailed\(auction\.imageUrl\)"/);
  assert.match(card, /v-else class="product-image product-image-empty"/);
  assert.match(card, /尚無圖片/);
  assert.match(
    card,
    /<RouterLink[\s\S]*?v-for="auction in auctions"[\s\S]*?name: 'auction-detail'[\s\S]*?id: auction\.auctionId[\s\S]*?<article class="auction-product-card">[\s\S]*?<\/article>\s*<\/RouterLink>/,
  );
  assert.doesNotMatch(
    listSource,
    /\bgetAuctionDetail\s*\(/,
    "the list must not fetch one detail response per card",
  );
});

test("auction grid keeps three desktop, two tablet and one mobile columns", () => {
  const style = section(listSource, "<style scoped>", "</style>");
  const tabletStart = style.indexOf("@media (max-width: 1199.98px)");
  const mobileStart = style.indexOf("@media (max-width: 767.98px)");
  const compactStart = style.indexOf("@media (max-width: 479.98px)");

  assert.ok(tabletStart > 0, "missing tablet breakpoint");
  assert.ok(mobileStart > tabletStart, "missing mobile breakpoint after tablet");

  const desktopCss = style.slice(0, tabletStart);
  const tabletCss = style.slice(tabletStart, mobileStart);
  const mobileCss = style.slice(mobileStart, compactStart > mobileStart ? compactStart : undefined);

  assert.match(
    desktopCss,
    /\.auction-grid\s*\{[^}]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/,
  );
  assert.match(
    tabletCss,
    /\.auction-grid\s*\{[^}]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/,
  );
  assert.match(
    mobileCss,
    /\.auction-grid\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/,
  );
});

test("auction detail retains gallery, countdown and complete public commerce sections", () => {
  assert.match(detailSource, /import AuctionImageGallery from/);
  assert.match(detailSource, /import AuctionCountdown from/);
  assert.match(
    detailSource,
    /<AuctionImageGallery\s+:image-urls="auction\.imageUrls"\s+:product-name="auction\.productName"/,
  );
  assert.match(
    detailSource,
    /<AuctionCountdown\s+:end-at="auction\.endAt"\s+:status="auction\.status"/,
  );

  assertIncludesAll(
    detailSource,
    [
      "auction.productName",
      "auction.brandName",
      "auction.categoryName",
      "auction.model",
      "auction.size",
      "auction.condition",
      "auction.currentPrice",
      "auction.startPrice",
      "auction.bidCount",
      "auction.minIncrement",
      "auction.nextMinimumBid",
      "auction.startAt",
      "auction.endAt",
      "auction.description",
      "auction.defectNote",
      "auction.authenticityNote",
      "auction.location",
    ],
    "product and auction information",
  );
  assertIncludesAll(
    detailSource,
    [
      "auction.shippingMethod",
      "auction.shippingFee",
      "auction.paymentMethod",
      "auction.allowMeetup",
      "auction.allowReturn",
      "auction.warrantyNote",
    ],
    "transaction information",
  );
  assertIncludesAll(
    detailSource,
    [
      '@submit.prevent="handlePlaceBid"',
      "candidateBid",
      "canPlaceBid",
      "submittingBid",
      "bidSuccessMessage",
      "bidErrorMessage",
      "bid.bidderName",
      "bid.bidAmount",
      "bid.createdAt",
    ],
    "bid controls and history",
  );
  assertIncludesAll(
    detailSource,
    [
      'v-if="auction.isOwner"',
      'v-if="canOwnerManageAuction"',
      "name: 'auction-edit'",
      '@click="handleCancelAuction"',
      "ownerManageMessage",
    ],
    "seller controls",
  );
});

test("image gallery keeps thumbnail selection, backend URL resolution and placeholders", () => {
  assert.match(gallerySource, /const selectedIndex = ref\(0\)/);
  assert.match(gallerySource, /v-for="\(imageUrl, index\) in images"/);
  assert.match(gallerySource, /@click="selectImage\(index\)"/);
  assert.match(gallerySource, /selectedIndex\.value = index/);
  assert.match(gallerySource, /:src="resolveAuctionImageUrl\(selectedImage\)"/);
  assert.match(gallerySource, /v-else class="gallery-placeholder"/);
  assert.match(gallerySource, /尚無商品圖片/);
  assert.match(gallerySource, /@error="markImageFailed\(selectedIndex\)"/);
  assert.match(gallerySource, /@error="markImageFailed\(index\)"/);
  assert.match(gallerySource, /v-else class="thumbnail-placeholder"/);

  assert.match(countdownSource, /parseAuctionTime\(props\.endAt\)/);
  assert.match(countdownSource, /new Set\(\["SOLD", "UNSOLD", "CANCELLED", "REMOVED"\]\)/);
  assert.match(countdownSource, /return "已到截止時間"/);
});
