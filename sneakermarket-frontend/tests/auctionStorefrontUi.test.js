import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import axios from "axios";
import { ref } from "vue";

async function readSource(url) {
  return (await readFile(url, "utf8")).replace(/\r\n/g, "\n");
}

const listSource = await readSource(
  new URL("../src/views/auction/AuctionListView.vue", import.meta.url),
);
const auctionServiceSource = await readSource(
  new URL("../src/service/auctionService.js", import.meta.url),
);
const commerceSource = await readSource(
  new URL("../src/views/commerce/CommerceHomeView.vue", import.meta.url),
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

function createListHarness({ getAuctionsImpl } = {}) {
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
      const request = Object.fromEntries(
        Object.entries(params).map(([key, value]) => [
          key,
          Array.isArray(value) ? [...value] : value,
        ]),
      );
      requests.push(request);

      if (getAuctionsImpl) {
        return getAuctionsImpl(request, requests.length - 1);
      }

      return {
        data: {
          data: {
            items: [],
            page: request.page,
            size: request.size,
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
        auctions, loading, errorMessage,
        keyword, appliedKeyword,
        selectedBrandIds, selectedCategoryIds, selectedStatuses,
        selectedConditions, selectedShoeSizes,
        minPrice, maxPrice, appliedMinPrice, appliedMaxPrice,
        currentPage, pageSize, totalPages, sort,
        loadAuctions, refreshAuctionsFromFirstPage, searchAuctions,
        toggleFacetSelection, clearFacetSelection, applyPriceFilters,
        clearFilters, changePage
      };
    `,
    context,
  );

  return { requests, subject: context.subject };
}

test("public auction list sends every supported filter through server-side paging and sorting", async () => {
  const { requests, subject } = createListHarness();

  subject.appliedKeyword.value = "Air Jordan";
  subject.selectedBrandIds.value = [11, 12];
  subject.selectedCategoryIds.value = [22, 23];
  subject.selectedStatuses.value = ["ACTIVE", "SCHEDULED"];
  subject.selectedConditions.value = ["NEW", "LIKE_NEW"];
  subject.selectedShoeSizes.value = ["US 9", "US 9.5"];
  subject.minPrice.value = "4000";
  subject.maxPrice.value = "9000";
  subject.appliedMinPrice.value = "4000";
  subject.appliedMaxPrice.value = "9000";
  subject.currentPage.value = 3;
  subject.sort.value = "currentPrice,desc";

  await subject.loadAuctions();

  assert.deepEqual(requests[0], {
    keyword: "Air Jordan",
    brandIds: [11, 12],
    categoryIds: [22, 23],
    statuses: ["ACTIVE", "SCHEDULED"],
    conditions: ["NEW", "LIKE_NEW"],
    shoeSizes: ["US 9", "US 9.5"],
    minPrice: 4000,
    maxPrice: 9000,
    page: 3,
    size: 9,
    sort: "currentPrice,desc",
  });

  subject.keyword.value = "  Air Max  ";
  subject.currentPage.value = 5;
  await subject.searchAuctions();
  assert.equal(requests[1].page, 0, "a new search must return to the first server page");
  assert.equal(requests[1].keyword, "Air Max");
  assert.equal(requests[1].size, 9);
  assert.equal(requests[1].sort, "currentPrice,desc");

  await subject.clearFilters();
  assert.deepEqual(requests[2], {
    keyword: undefined,
    brandIds: undefined,
    categoryIds: undefined,
    statuses: undefined,
    conditions: undefined,
    shoeSizes: undefined,
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

test("public status filter excludes CANCELLED and keeps the four public statuses", () => {
  const statusFilter = section(
    listSource,
    "<!-- 狀態 -->",
    "<!-- 商品狀況 -->",
  );

  for (const status of ["ACTIVE", "SCHEDULED", "SOLD", "UNSOLD"]) {
    assert.match(statusFilter, new RegExp(`selectedStatuses\\.includes\\('${status}'\\)`));
  }
  assert.doesNotMatch(statusFilter, /CANCELLED/);
  assert.doesNotMatch(statusFilter, /已取消/);
  assert.match(listSource, /const\s+sort\s*=\s*ref\s*\(\s*["']endAt,asc["']\s*\)/);
  assert.match(listSource, /CANCELLED\s*:\s*["']已取消["']/);
});

test("facet selections query immediately while price remains explicit and multi-value", async () => {
  const { requests, subject } = createListHarness();

  subject.currentPage.value = 4;
  subject.keyword.value = "not submitted";
  subject.minPrice.value = "4000";
  subject.maxPrice.value = "9000";

  await subject.toggleFacetSelection(subject.selectedBrandIds.value, 11);
  assert.deepEqual(requests[0].brandIds, [11]);
  assert.equal(requests[0].keyword, undefined);
  assert.equal(requests[0].minPrice, undefined);
  assert.equal(requests[0].maxPrice, undefined);
  assert.equal(requests[0].page, 0);

  await subject.toggleFacetSelection(subject.selectedBrandIds.value, 12);
  assert.deepEqual(requests[1].brandIds, [11, 12]);

  await subject.toggleFacetSelection(subject.selectedCategoryIds.value, 22);
  await subject.toggleFacetSelection(subject.selectedCategoryIds.value, 23);
  assert.deepEqual(requests[3].categoryIds, [22, 23]);

  await subject.applyPriceFilters();
  assert.equal(requests[4].minPrice, 4000);
  assert.equal(requests[4].maxPrice, 9000);

  await subject.toggleFacetSelection(subject.selectedBrandIds.value, 11);
  assert.deepEqual(requests[5].brandIds, [12]);
  assert.equal(requests[5].minPrice, 4000);
  assert.equal(requests[5].maxPrice, 9000);

  await subject.clearFacetSelection(subject.selectedCategoryIds.value);
  assert.equal(requests[6].categoryIds, undefined);
});

test("a slower obsolete facet response cannot overwrite the latest result", async () => {
  const pending = [];
  const { subject } = createListHarness({
    getAuctionsImpl: (params) =>
      new Promise((resolve) => {
        pending.push({ params, resolve });
      }),
  });

  const firstRequest = subject.toggleFacetSelection(
    subject.selectedBrandIds.value,
    11,
  );
  const latestRequest = subject.toggleFacetSelection(
    subject.selectedBrandIds.value,
    12,
  );

  assert.equal(pending.length, 2);
  pending[1].resolve({
    data: {
      data: {
        items: [{ auctionId: 2 }],
        page: 0,
        size: 9,
        totalElements: 1,
        totalPages: 1,
      },
    },
  });
  await latestRequest;

  pending[0].resolve({
    data: {
      data: {
        items: [{ auctionId: 1 }],
        page: 0,
        size: 9,
        totalElements: 1,
        totalPages: 1,
      },
    },
  });
  await firstRequest;

  assert.equal(subject.auctions.value[0].auctionId, 2);
  assert.equal(subject.loading.value, false);
});

test("auction service serializes plural filters as repeated Spring query parameters", () => {
  const serviceMethod = section(
    auctionServiceSource,
    "export function getAuctions",
    "/**\n * 查詢指定競標的完整詳細資料。",
  );

  assert.match(serviceMethod, /paramsSerializer\s*:\s*\{/);
  assert.match(serviceMethod, /indexes\s*:\s*null/);

  const requestUrl = axios.getUri({
    url: "/auctions",
    params: {
      brandIds: [11, 12],
      statuses: ["ACTIVE", "SOLD"],
    },
    paramsSerializer: {
      indexes: null,
    },
  });

  assert.match(requestUrl, /brandIds=11&brandIds=12/);
  assert.match(requestUrl, /statuses=ACTIVE&statuses=SOLD/);
  assert.doesNotMatch(requestUrl, /%5B%5D|\[\]/);
});

test("auction list size checkboxes expose the supported US sizes in order", () => {
  const sizeFilter = section(listSource, "<!-- 尺寸 -->", "<!-- 價格 -->");
  const checkboxTags = [
    ...sizeFilter.matchAll(/<input\b([^>]*)\/>/g),
  ].map((match) => match[1]);
  const supportedSizes = [
    "US 8",
    "US 8.5",
    "US 9",
    "US 9.5",
    "US 10",
    "US 10.5",
    "US 11",
  ];

  assert.equal(checkboxTags.length, supportedSizes.length);
  assert.deepEqual(
    checkboxTags.map(
      (attributes) =>
        attributes.match(/selectedShoeSizes\.includes\('([^']+)'\)/)?.[1],
    ),
    supportedSizes,
  );
  for (const attributes of checkboxTags) {
    assert.match(attributes, /\btype\s*=\s*["']checkbox["']/);
    assert.match(
      attributes,
      /toggleFacetSelection\s*\(\s*selectedShoeSizes\s*,/,
    );
  }
  assert.deepEqual(
    [...sizeFilter.matchAll(/<span>(US [^<]+)<\/span>/g)].map(
      (match) => match[1],
    ),
    supportedSizes,
  );
  assert.doesNotMatch(sizeFilter, /<select\b/);
});

test("auction storefront matches the general commerce typography, search and compact filter language", () => {
  const auctionStyle = section(
    listSource,
    /<style\b(?=[^>]*\bscoped\b)[^>]*>/,
    "</style>",
  );
  const commerceStyle = section(
    commerceSource,
    /<style\b(?=[^>]*\bscoped\b)[^>]*>/,
    "</style>",
  );
  const auctionHero = section(listSource, "<!-- Hero -->", "<!-- 競標商品區 -->");

  assert.match(auctionHero, /<h1\s+class=["'][^"']*\bdisplay-5\b[^"']*\bfw-bold\b/);
  assert.match(auctionHero, /class=["'][^"']*\bd-flex\b[^"']*\bgap-2\b[^"']*\bhero-search\b/);
  assert.match(auctionHero, /class=["'][^"']*\bbtn-light\b[^"']*\bpx-4\b[^"']*\bhero-search-btn\b/);

  for (const style of [commerceStyle, auctionStyle]) {
    assert.match(style, /\.hero-search\s*\{[^}]*max-width\s*:\s*380px/);
    assert.match(style, /\.hero-search input\s*\{[^}]*font-size\s*:\s*0\.95rem/);
    assert.match(style, /\.filter-sidebar\s*\{[^}]*font-size\s*:\s*0\.82rem/);
    assert.match(
      style,
      /\.sort-select\s*\{[^}]*border\s*:\s*(?:none|0)[^}]*background-color\s*:\s*transparent[^}]*font-weight\s*:\s*500/,
    );
  }

  assert.match(listSource, /<h2\s+class=["'][^"']*\bh4\b[^"']*\bfw-bold\b/);
  assert.match(listSource, /« 隱藏篩選條件/);
  assert.match(listSource, /» 顯示篩選條件/);
  assert.match(
    listSource,
    /id=["']auction-sort["'][\s\S]*?class=["'][^"']*\bform-select-sm\b[^"']*\bw-auto\b/,
  );

  const checkboxGroups = [
    ["<!-- 品牌 -->", "<!-- 狀態 -->", "selectedBrandIds"],
    ["<!-- 狀態 -->", "<!-- 商品狀況 -->", "selectedStatuses"],
    ["<!-- 商品狀況 -->", "<!-- 尺寸 -->", "selectedConditions"],
    ["<!-- 尺寸 -->", "<!-- 價格 -->", "selectedShoeSizes"],
  ];
  for (const [start, end, model] of checkboxGroups) {
    const filter = section(listSource, start, end);
    assert.match(filter, /type=["']checkbox["']/);
    assert.match(filter, new RegExp(`:checked=["']${model}\\.includes\\(`));
    assert.match(
      filter,
      new RegExp(`toggleFacetSelection\\s*\\(\\s*${model}\\s*,`),
    );
    assert.doesNotMatch(filter, /<select\b/);
  }

  const sidebar = section(
    listSource,
    "<!-- 左側篩選 -->",
    "<!-- 右側商品列表 -->",
  );
  assert.doesNotMatch(sidebar, /<select\b/);

  assert.match(listSource, /class=["'][^"']*\brow\b[^"']*\bg-4\b[^"']*\bcatalog-layout\b/);
  assert.match(listSource, /<aside[\s\S]*?class=["'][^"']*\bcol-md-2\b/);
  assert.match(listSource, /<main[\s\S]*?col-12 col-md-10 auction-results/);
  assert.match(listSource, /class=["']category-list["']/);
  assert.match(listSource, /v-for=["']category in categories["']/);
  assert.match(listSource, /selectedCategoryIds\.includes\(category\.id\)/);
  assert.match(
    listSource,
    /toggleFacetSelection\s*\(\s*selectedCategoryIds\s*,\s*category\.id\s*\)/,
  );
  assert.match(listSource, /<details\s+class=["']filter-group(?: price-filter)?["']/);
  assert.match(listSource, /<form\s+@submit\.prevent=["']applyPriceFilters["']/);
  assert.match(listSource, /套用金額篩選/);
  assert.match(listSource, /清除全部篩選/);
  assert.match(listSource, /@click=["']clearFilters["']/);

  assert.match(
    auctionStyle,
    /\.filter-group\s*\{[^}]*padding\s*:\s*0\s+0\s+0\.5rem[^}]*border-bottom\s*:\s*1px\s+solid\s+#dee2e6/,
  );
  assert.match(
    auctionStyle,
    /\.apply-filter-button\s*\{[^}]*border-radius\s*:\s*0\.25rem/,
  );
  assert.match(
    auctionStyle,
    /\.filter-option\s*\{[^}]*display\s*:\s*flex[^}]*font-size\s*:\s*0\.82rem/,
  );
  assert.match(
    auctionStyle,
    /\.filter-option input\[type=["']checkbox["']\]\s*\{[^}]*accent-color\s*:\s*#111/,
  );
  assert.match(
    auctionStyle,
    /\.product-brand\s*\{[^}]*font-size\s*:\s*0\.875rem[^}]*font-weight\s*:\s*400/,
  );
  assert.match(
    auctionStyle,
    /\.product-card-body h3\s*\{[^}]*font-size\s*:\s*1rem[^}]*font-weight\s*:\s*500/,
  );
  assert.match(
    auctionStyle,
    /\.price-block strong\s*\{[^}]*font-size\s*:\s*1rem[^}]*font-weight\s*:\s*600/,
  );
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

test("auction detail combines shipping and meetup into one delivery presentation", () => {
  const transactionSection = section(
    detailSource,
    "<!-- 交易資訊 -->",
    "</section>",
  );

  assert.match(transactionSection, /<dt>交付方式<\/dt>/);
  assert.match(
    transactionSection,
    /formatDeliveryMethod\s*\(\s*auction\.shippingMethod\s*,\s*auction\.allowMeetup\s*,?\s*\)/,
  );
  assert.doesNotMatch(transactionSection, /<dt>允許面交<\/dt>/);
  assert.match(transactionSection, /auction\.shippingFee/);
  assert.match(transactionSection, /auction\.allowReturn/);

  const detailScript = scriptSetup(detailSource);
  const formatterStart = detailScript.indexOf("function formatDeliveryMethod");
  const formatterEnd = detailScript.indexOf("\n}\n", formatterStart);
  assert.notEqual(formatterStart, -1, "missing delivery formatter");
  assert.notEqual(formatterEnd, -1, "missing delivery formatter boundary");

  const context = {};
  vm.runInNewContext(
    detailScript.slice(formatterStart, formatterEnd + 2) +
      "\nglobalThis.formatDeliveryMethod = formatDeliveryMethod;",
    context,
  );

  assert.equal(context.formatDeliveryMethod("宅配", false), "宅配");
  assert.equal(
    context.formatDeliveryMethod("宅配", true),
    "宅配／可協調面交",
  );
  assert.equal(
    context.formatDeliveryMethod("黑貓宅配", true),
    "黑貓宅配／可協調面交",
  );
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
