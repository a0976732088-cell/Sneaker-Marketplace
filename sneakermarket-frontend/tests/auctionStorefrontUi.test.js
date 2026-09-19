import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { ref } from "vue";

async function readSource(url) {
  return (await readFile(url, "utf8")).replace(/\r\n/g, "\n");
}

const listSource = await readSource(
  new URL("../src/views/auction/AuctionListView.vue", import.meta.url),
);
const detailSource = await readSource(
  new URL("../src/views/auction/AuctionDetailView.vue", import.meta.url),
);
const gallerySource = await readSource(
  new URL("../src/components/auction/AuctionImageGallery.vue", import.meta.url),
);
const countdownSource = await readSource(
  new URL("../src/components/auction/AuctionCountdown.vue", import.meta.url),
);

function scriptSetup(source) {
  const match = source.match(
    /<script\b(?=[^>]*\bsetup\b)[^>]*>([\s\S]*?)<\/script\s*>/,
  );
  assert.ok(match, "expected an exact <script setup> block");
  return match[1].replace(
    /^[ \t]*import\b[\s\S]*?from\s+["'][^"']+["']\s*;?/gm,
    "",
  );
}

function section(source, startMarker, endMarker) {
  const start =
    startMarker instanceof RegExp ? source.search(startMarker) : source.indexOf(startMarker);
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
    /<RouterLink\b[^>]*\bv-for\s*=\s*["']auction\s+in\s+auctions["']/,
    "<!-- 分頁 -->",
  );

  assert.match(card, /\bv-for\s*=\s*["']auction\s+in\s+auctions["']/);
  assert.match(
    card,
    /auction\.imageUrl\s*&&\s*!failedAuctionImageUrls\.has\(auction\.imageUrl\)/,
  );
  assert.match(card, /:src\s*=\s*["']resolveAuctionImageUrl\(auction\.imageUrl\)["']/);
  assert.match(card, /@error\s*=\s*["']markAuctionImageFailed\(auction\.imageUrl\)["']/);

  const cardPlaceholderTag =
    card.match(
      /<div\b(?=[^>]*\bclass\s*=\s*["']product-image product-image-empty["'])[^>]*>/,
    )?.[0] ?? "";
  assert.match(cardPlaceholderTag, /\bv-else\b/);
  assert.match(card, /尚無圖片/);
  assert.match(card, /name\s*:\s*["']auction-detail["']/);
  assert.match(card, /id\s*:\s*auction\.auctionId/);
  assert.match(
    card,
    /<RouterLink\b[^>]*>[\s\S]*?<article\b(?=[^>]*\bclass\s*=\s*["']auction-product-card["'])[^>]*>[\s\S]*?<\/article>\s*<\/RouterLink>/,
  );
  assert.doesNotMatch(
    listSource,
    /\bgetAuctionDetail\s*\(/,
    "the list must not fetch one detail response per card",
  );
});

test("auction grid keeps three desktop, two tablet and one mobile columns", () => {
  const style = section(
    listSource,
    /<style\b(?=[^>]*\bscoped\b)[^>]*>/,
    "</style>",
  );
  const tabletStart = style.search(
    /@media\s*\(\s*max-width\s*:\s*1199\.98px\s*\)/,
  );
  const mobileStart = style.search(
    /@media\s*\(\s*max-width\s*:\s*767\.98px\s*\)/,
  );
  const compactStart = style.search(
    /@media\s*\(\s*max-width\s*:\s*479\.98px\s*\)/,
  );

  assert.ok(tabletStart > 0, "missing tablet breakpoint");
  assert.ok(mobileStart > tabletStart, "missing mobile breakpoint after tablet");

  const desktopCss = style.slice(0, tabletStart);
  const tabletCss = style.slice(tabletStart, mobileStart);
  const mobileCss = style.slice(mobileStart, compactStart > mobileStart ? compactStart : undefined);

  assert.match(
    desktopCss,
    /\.auction-grid\s*\{[^}]*grid-template-columns\s*:\s*repeat\s*\(\s*3\s*,\s*minmax\s*\(\s*0\s*,\s*1fr\s*\)\s*\)/,
  );
  assert.match(
    tabletCss,
    /\.auction-grid\s*\{[^}]*grid-template-columns\s*:\s*repeat\s*\(\s*2\s*,\s*minmax\s*\(\s*0\s*,\s*1fr\s*\)\s*\)/,
  );
  assert.match(
    mobileCss,
    /\.auction-grid\s*\{[^}]*grid-template-columns\s*:\s*minmax\s*\(\s*0\s*,\s*1fr\s*\)/,
  );
});

test("auction detail retains gallery, countdown and complete public commerce sections", () => {
  assert.match(detailSource, /import\s+AuctionImageGallery\s+from\s+["'][^"']+["']/);
  assert.match(detailSource, /import\s+AuctionCountdown\s+from\s+["'][^"']+["']/);

  const galleryTag = detailSource.match(/<AuctionImageGallery\b[^>]*>/)?.[0] ?? "";
  assert.match(galleryTag, /:image-urls\s*=\s*["']auction\.imageUrls["']/);
  assert.match(galleryTag, /:product-name\s*=\s*["']auction\.productName["']/);

  const countdownTag = detailSource.match(/<AuctionCountdown\b[^>]*>/)?.[0] ?? "";
  assert.match(countdownTag, /:end-at\s*=\s*["']auction\.endAt["']/);
  assert.match(countdownTag, /:status\s*=\s*["']auction\.status["']/);

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
  assert.match(detailSource, /@submit\.prevent\s*=\s*["']handlePlaceBid["']/);
  assertIncludesAll(
    detailSource,
    [
      "ownerManageMessage",
    ],
    "seller controls",
  );
  assert.match(detailSource, /v-if\s*=\s*["']auction\.isOwner["']/);
  assert.match(detailSource, /v-if\s*=\s*["']canOwnerManageAuction["']/);
  assert.match(detailSource, /name\s*:\s*["']auction-edit["']/);
  assert.match(detailSource, /@click\s*=\s*["']handleCancelAuction["']/);
});

test("image gallery keeps thumbnail selection, backend URL resolution and placeholders", () => {
  assert.match(gallerySource, /const\s+selectedIndex\s*=\s*ref\s*\(\s*0\s*\)/);
  assert.match(gallerySource, /v-for\s*=\s*["']\(imageUrl, index\) in images["']/);
  assert.match(gallerySource, /@click\s*=\s*["']selectImage\(index\)["']/);
  assert.match(gallerySource, /selectedIndex\.value\s*=\s*index/);
  assert.match(gallerySource, /:src\s*=\s*["']resolveAuctionImageUrl\(selectedImage\)["']/);

  const galleryPlaceholderTag =
    gallerySource.match(
      /<div\b(?=[^>]*\bclass\s*=\s*["']gallery-placeholder["'])[^>]*>/,
    )?.[0] ?? "";
  assert.match(galleryPlaceholderTag, /\bv-else\b/);
  assert.match(gallerySource, /尚無商品圖片/);
  assert.match(gallerySource, /@error\s*=\s*["']markImageFailed\(selectedIndex\)["']/);
  assert.match(gallerySource, /@error\s*=\s*["']markImageFailed\(index\)["']/);

  const thumbnailPlaceholderTag =
    gallerySource.match(
      /<span\b(?=[^>]*\bclass\s*=\s*["']thumbnail-placeholder["'])[^>]*>/,
    )?.[0] ?? "";
  assert.match(thumbnailPlaceholderTag, /\bv-else\b/);

  assert.match(countdownSource, /parseAuctionTime\s*\(\s*props\.endAt\s*\)/);
  const terminalStatuses =
    countdownSource.match(
      /terminalStatuses\s*=\s*new\s+Set\s*\(\s*\[([\s\S]*?)\]\s*\)/,
    )?.[1] ?? "";
  for (const status of ["SOLD", "UNSOLD", "CANCELLED", "REMOVED"]) {
    assert.match(terminalStatuses, new RegExp(`["']${status}["']`));
  }
  assert.match(countdownSource, /return\s*["']已到截止時間["']/);
});
