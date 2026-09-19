import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, nextTick, reactive, ref } from "vue";

import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_SHOE_SIZES,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  TAIWAN_CITIES,
  normalizeTaiwanCity,
} from "../src/constants/auctionFormOptions.js";

async function readSource(url) {
  return (await readFile(url, "utf8")).replace(/\r\n/g, "\n");
}

const editSource = await readSource(
  new URL("../src/views/auction/AuctionEditView.vue", import.meta.url),
);

function section(source, startMarker, endMarker) {
  const start =
    startMarker instanceof RegExp
      ? source.search(startMarker)
      : source.indexOf(startMarker);
  const end = source.indexOf(endMarker, start);

  assert.notEqual(start, -1, `missing ${startMarker}`);
  assert.notEqual(end, -1, `missing ${endMarker}`);

  return source.slice(start, end);
}

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

const templateSource = section(editSource, "<template>", "</template>");
const styleSource = section(
  editSource,
  /<style\b(?=[^>]*\bscoped\b)[^>]*>/,
  "</style>",
);

test("Auction Edit renders the shared required shoe-size selector", () => {
  const sizeSelect = templateSource.match(
    /<select\b(?=[^>]*\bv-model\.trim\s*=\s*["']form\.size["'])[^>]*>[\s\S]*?<\/select>/,
  )?.[0];

  assert.ok(sizeSelect, "form.size must be a native select");
  assert.match(sizeSelect, /\brequired\b/);
  assert.match(sizeSelect, /:disabled\s*=\s*["']!editable["']/);
  assert.match(sizeSelect, /:aria-invalid\s*=\s*["']Boolean\(fieldErrors\.size\)["']/);
  assert.match(sizeSelect, /v-for\s*=\s*["']sizeOption in AUCTION_SHOE_SIZES["']/);
  assert.match(sizeSelect, /:value\s*=\s*["']sizeOption["']/);
  assert.match(sizeSelect, /<option\b(?=[^>]*\bdisabled\b)[^>]*\bvalue=["']["']/);
  assert.doesNotMatch(
    templateSource,
    /<input\b(?=[^>]*\bv-model\.trim\s*=\s*["']form\.size["'])[^>]*>/,
  );
  assert.deepEqual(AUCTION_SHOE_SIZES, [
    "US 8",
    "US 8.5",
    "US 9",
    "US 9.5",
    "US 10",
    "US 10.5",
    "US 11",
  ]);
});

test("Auction Edit marks only its thirteen required controls and renders red errors", () => {
  const requiredControls =
    templateSource.match(
      /<(?:input|select|textarea)\b(?=[^>]*\brequired\b)[^>]*>/g,
    ) ?? [];
  const requiredMarks =
    templateSource.match(/class\s*=\s*["']required-mark["']/g) ?? [];

  assert.match(
    templateSource,
    /<form\b(?=[^>]*\bnovalidate\b)(?=[^>]*@submit\.prevent\s*=\s*["']submitUpdate["'])[^>]*>/,
  );
  assert.equal(requiredControls.length, 13);
  assert.equal(
    requiredMarks.length,
    15,
    "one form note, thirteen required fields and the required delivery legend",
  );
  assert.match(
    styleSource,
    /\.required-mark\s*\{[^}]*color\s*:\s*#dc3545\b/i,
  );
  assert.match(
    styleSource,
    /\.field-error\s*\{[^}]*color\s*:\s*#dc3545\b/i,
  );
  assert.match(
    styleSource,
    /\.global-error-alert\s*\{[^}]*background\s*:\s*#f8d7da\b/i,
  );
});

test("Auction Edit exposes the five shared preset selectors with inline Custom inputs", () => {
  assert.deepEqual(AUCTION_DEFECT_NOTE_OPTIONS, [
    "無明顯瑕疵",
    "有輕微使用痕跡",
    "鞋盒有輕微損傷",
    "配件不完整",
  ]);
  assert.deepEqual(AUCTION_AUTHENTICITY_NOTE_OPTIONS, [
    "品牌官方門市／專櫃購入",
    "品牌官方網站購入",
    "授權經銷商購入",
    "海外官方／授權通路購入",
    "二手平台購入",
  ]);
  assert.deepEqual(AUCTION_SHIPPING_METHOD_OPTIONS, ["宅配", "郵寄", "超商寄送"]);
  assert.deepEqual(AUCTION_PAYMENT_METHOD_OPTIONS, ["轉帳", "信用卡"]);
  assert.deepEqual(AUCTION_WARRANTY_NOTE_OPTIONS, [
    "無額外保固",
    "依原購買通路／品牌規範",
    "售後問題可與賣家聯繫協調",
  ]);

  for (const fieldName of [
    "defectNote",
    "authenticityNote",
    "shippingMethod",
    "paymentMethod",
    "warrantyNote",
  ]) {
    assert.match(
      templateSource,
      new RegExp(
        `v-model=["']selectedFormOptions\\.${fieldName}["'][\\s\\S]*?` +
          `@change=["']handleFormOptionChange\\('${fieldName}'\\)["']`,
      ),
    );
    assert.match(
      templateSource,
      new RegExp(
        `selectedFormOptions\\.${fieldName}[\\s\\S]*?CUSTOM_OPTION_VALUE`,
      ),
    );
  }

  assert.match(templateSource, /data-custom-input/);
  assert.match(
    templateSource,
    /id=["']auction-edit-shipping-method-custom["'][\s\S]*?maxlength=["']50["']/,
  );
  assert.match(
    templateSource,
    /id=["']auction-edit-payment-method-custom["'][\s\S]*?maxlength=["']50["']/,
  );
});

test("Auction Edit uses the existing read-only gallery without image upload controls", () => {
  assert.match(
    editSource,
    /import\s+AuctionImageGallery\s+from\s+["']@\/components\/auction\/AuctionImageGallery\.vue["']/,
  );

  const imageSection = section(
    templateSource,
    "<!-- 圖片 -->",
    "<!-- 交易資訊 -->",
  );
  const galleryTag = imageSection.match(/<AuctionImageGallery\b[\s\S]*?\/>/)?.[0] ?? "";

  assert.match(galleryTag, /:image-urls\s*=\s*["']currentImageUrls["']/);
  assert.match(
    galleryTag,
    /:product-name\s*=\s*["']form\.productName \|\| '競標商品'["']/,
  );
  assert.match(imageSection, /目前刊登圖片/);
  assert.match(imageSection, /商品圖片會沿用目前刊登內容/);
  assert.doesNotMatch(imageSection, /type\s*=\s*["']file["']/);
  assert.doesNotMatch(imageSection, /功能 4|尚未實作|API 尚未完成/);
});

test("Auction Edit location is a select-style searchable combobox with inline errors", () => {
  const locationArea = section(
    templateSource,
    '<!-- 商品所在地 -->',
    '<!-- 圖片 -->',
  );
  const locationInput =
    locationArea.match(
      /<input\b(?=[^>]*\bid\s*=\s*["']auction-edit-location["'])(?=[^>]*v-model\.trim\s*=\s*["']form\.location["'])[^>]*>/,
    )?.[0] ?? "";

  assert.match(locationArea, /data-validation-field\s*=\s*["']location["']/);
  assert.ok(locationInput, "location must remain bound to form.location");
  assert.match(
    locationInput,
    /class\s*=\s*["']form-select location-combobox-input["']/,
  );
  assert.match(locationInput, /role\s*=\s*["']combobox["']/);
  assert.match(locationInput, /aria-autocomplete\s*=\s*["']list["']/);
  assert.match(
    locationInput,
    /aria-controls\s*=\s*["']auction-edit-location-options["']/,
  );
  assert.match(
    locationInput,
    /:aria-expanded\s*=\s*["']locationMenuOpen["']/,
  );
  assert.match(locationInput, /:aria-activedescendant/);
  assert.match(locationInput, /autocomplete\s*=\s*["']off["']/);
  assert.match(locationInput, /:disabled\s*=\s*["']!editable["']/);
  assert.match(locationInput, /:aria-invalid\s*=\s*["']Boolean\(fieldErrors\.location\)["']/);
  assert.match(locationInput, /auction-edit-location-error/);
  assert.doesNotMatch(locationInput, /\slist\s*=/);
  assert.doesNotMatch(locationInput, /\brequired\b/);

  const locationLabel = locationArea.match(
    /<label\b[^>]*for\s*=\s*["']auction-edit-location["'][^>]*>([\s\S]*?)<\/label>/,
  );
  assert.ok(locationLabel);
  assert.doesNotMatch(locationLabel[1], /required-mark/);
  assert.match(
    locationArea,
    /<ul\b(?=[^>]*id\s*=\s*["']auction-edit-location-options["'])(?=[^>]*role\s*=\s*["']listbox["'])/,
  );
  assert.match(
    locationArea,
    /v-for\s*=\s*["']\(city, index\) in filteredTaiwanCities["']/,
  );
  assert.match(locationArea, /role\s*=\s*["']option["']/);
  assert.match(
    locationArea,
    /@pointerdown\.prevent\s*=\s*["']selectLocation\(city\)["']/,
  );
  assert.doesNotMatch(locationArea, /<datalist\b/);
  assert.doesNotMatch(locationArea, /location-combobox-arrow/);
  assert.match(locationInput, /@keydown\.down\.prevent/);
  assert.match(locationInput, /@keydown\.up\.prevent/);
  assert.match(locationInput, /@keydown\.enter\s*=\s*["']handleLocationEnter["']/);
  assert.match(locationInput, /@keydown\.esc\.prevent/);
  assert.match(
    locationArea,
    /<p\b(?=[^>]*v-if\s*=\s*["']fieldErrors\.location["'])(?=[^>]*id\s*=\s*["']auction-edit-location-error["'])[^>]*\brole\s*=\s*["']alert["']/,
  );
  assert.match(
    editSource,
    /\.location-combobox\s*\{[\s\S]*?position:\s*relative/,
  );
  assert.match(
    editSource,
    /\.location-combobox-menu\s*\{[\s\S]*?max-height:\s*15rem;[\s\S]*?overflow-y:\s*auto/,
  );
});

test("Auction Edit endAt owns the relationship error and accessible invalid state", () => {
  const endAtArea = section(
    templateSource,
    'data-validation-field="endAt"',
    "<!-- 操作 -->",
  );

  assert.match(endAtArea, /fieldErrors\.endAt/);
  assert.match(
    endAtArea,
    /:aria-invalid\s*=\s*["']Boolean\(fieldErrors\.endAt\)["']/,
  );
  assert.match(endAtArea, /class\s*=\s*["']field-error["']/);
  assert.match(endAtArea, /role\s*=\s*["']alert["']/);
  assert.match(
    scriptSetup(editSource),
    /if\s*\(\s*endAt\s*<=\s*startAt\s*\)\s*\{\s*return\s*["']競標結束時間必須晚於開始時間["']/,
  );
});

function editableAuction(overrides = {}) {
  return {
    brandId: 1,
    categoryId: 2,
    productName: "Air Jordan 1",
    model: "AJ1",
    size: "US 9",
    condition: "NEW",
    description: "商品介紹",
    defectNote: null,
    authenticityNote: null,
    location: null,
    shippingMethod: "宅配",
    shippingFee: 60,
    paymentMethod: "轉帳",
    allowMeetup: false,
    allowReturn: false,
    warrantyNote: null,
    startPrice: 5000,
    minIncrement: 100,
    startAt: "2026-09-20T10:00:00",
    endAt: "2099-09-20T11:00:00",
    canManage: true,
    hasStarted: false,
    status: "SCHEDULED",
    bidCount: 0,
    isOwner: true,
    imageUrls: ["/uploads/auctions/air-jordan-1.jpg"],
    ...overrides,
  };
}

function createEditHarness() {
  const updateRequests = [];
  const routerPushes = [];
  const selectedFields = [];
  const scrollCalls = [];
  const focusCalls = [];
  const fieldControlQueries = [];
  const renderedCustomFields = new Set();
  const focusableField = {
    disabled: false,
    focus(options) {
      focusCalls.push(options);
    },
  };
  const router = {
    async push(target) {
      routerPushes.push(target);
    },
    async replace() {},
  };
  const context = {
    computed,
    nextTick,
    reactive,
    ref,
    console,
    AUCTION_AUTHENTICITY_NOTE_OPTIONS,
    AUCTION_DEFECT_NOTE_OPTIONS,
    AUCTION_PAYMENT_METHOD_OPTIONS,
    AUCTION_SHIPPING_METHOD_OPTIONS,
    AUCTION_SHOE_SIZES,
    AUCTION_WARRANTY_NOTE_OPTIONS,
    CUSTOM_OPTION_VALUE,
    DELIVERY_OPTION_VALUES,
    TAIWAN_CITIES,
    normalizeTaiwanCity,
    document: {
      querySelector(selector) {
        selectedFields.push(selector);
        const fieldName = selector.match(
          /^\[data-validation-field="([^"]+)"\]$/,
        )?.[1];

        return {
          scrollIntoView(options) {
            scrollCalls.push(options);
          },
          querySelector(controlSelector) {
            fieldControlQueries.push([fieldName, controlSelector]);

            if (controlSelector === "[data-custom-input]") {
              return renderedCustomFields.has(fieldName)
                ? focusableField
                : null;
            }

            assert.equal(controlSelector, "input, select, textarea");
            return focusableField;
          },
        };
      },
    },
    onMounted: () => {},
    useRoute: () => ({ params: { id: 42 } }),
    useRouter: () => router,
    parseAuctionTime: (value) => new Date(value),
    getAuctionOptions: async () => ({ data: { data: {} } }),
    requireAuctionSession: async () => {},
    handleAuctionAccessError: async () => false,
    getAuctionDetail: async () => ({ data: { data: {} } }),
    updateAuction: async (auctionId, request) => {
      updateRequests.push([auctionId, request]);
    },
  };

  vm.runInNewContext(
    scriptSetup(editSource) +
      `
      globalThis.subject = {
        form,
        fieldErrors,
        validationOrder,
        editable,
        selectedFormOptions,
        deliveryOption,
        currentImageUrls,
        auctionStatus,
        bidCount,
        originalStartAt,
        fillForm,
        handleFormOptionChange,
        validateForm,
        revalidateField,
        normalizeLocation,
        locationMenuOpen,
        activeLocationIndex,
        activeLocationOptionId,
        filteredTaiwanCities,
        openLocationMenu,
        closeLocationMenu,
        handleLocationInput,
        moveLocationOption,
        selectLocation,
        handleLocationEnter,
        submitUpdate
      };
    `,
    context,
  );

  return {
    ...context.subject,
    updateRequests,
    routerPushes,
    selectedFields,
    scrollCalls,
    focusCalls,
    fieldControlQueries,
    renderedCustomFields,
  };
}

test("Edit preserves all legacy Custom values and current image URLs on initialization", () => {
  const harness = createEditHarness();
  const legacyValues = {
    defectNote: "左鞋鞋頭有一道約 1 公分刮痕",
    authenticityNote: "親友自海外實體店購入",
    shippingMethod: "黑貓宅配",
    paymentMethod: "匯款後通知",
    warrantyNote: "收到商品七日內可聯絡賣家",
  };
  const imageUrls = ["/uploads/auctions/one.jpg", "/uploads/auctions/two.jpg"];

  harness.fillForm(editableAuction({ ...legacyValues, imageUrls }));

  for (const [fieldName, value] of Object.entries(legacyValues)) {
    assert.equal(harness.selectedFormOptions[fieldName], CUSTOM_OPTION_VALUE);
    assert.equal(harness.form[fieldName], value);
  }
  assert.deepEqual(Array.from(harness.currentImageUrls.value), imageUrls);
});

test("Edit normalizes exact 台 aliases but preserves and rejects ambiguous legacy locations", async () => {
  for (const validLocation of ["", "高雄市", "臺北市"]) {
    const harness = createEditHarness();
    harness.fillForm(editableAuction({ location: validLocation }));
    assert.equal(harness.validateForm(), true);
    assert.equal(harness.fieldErrors.location, "");
  }

  const aliasHarness = createEditHarness();
  aliasHarness.fillForm(editableAuction({ location: "台北市" }));
  assert.equal(aliasHarness.form.location, "臺北市");
  assert.equal(aliasHarness.validateForm(), true);

  for (const legacyLocation of ["新北", "北部", "左營", "台北信義區"]) {
    const harness = createEditHarness();
    harness.fillForm(editableAuction({ location: legacyLocation }));
    assert.equal(harness.form.location, legacyLocation);
    assert.equal(harness.validateForm(), false);
    assert.equal(
      harness.fieldErrors.location,
      "請從台灣縣市選項中選擇商品所在地",
    );
  }

  const focusHarness = createEditHarness();
  focusHarness.fillForm(editableAuction({ location: "北部" }));

  await focusHarness.submitUpdate();

  assert.equal(focusHarness.updateRequests.length, 0);
  assert.deepEqual(focusHarness.selectedFields, [
    '[data-validation-field="location"]',
  ]);
  assert.equal(focusHarness.scrollCalls.length, 1);
  assert.equal(focusHarness.focusCalls.length, 1);
  assert.ok(
    focusHarness.validationOrder.indexOf("location") <
      focusHarness.validationOrder.indexOf("shippingMethod"),
  );

  focusHarness.form.location = "台北市";
  focusHarness.normalizeLocation();
  assert.equal(focusHarness.form.location, "臺北市");
  assert.equal(focusHarness.fieldErrors.location, "");
});

test("Edit combobox filters aliases and keeps its UI state out of the update request", async () => {
  const harness = createEditHarness();
  harness.fillForm(editableAuction({ location: "" }));

  harness.openLocationMenu();
  assert.equal(harness.locationMenuOpen.value, true);
  assert.equal(harness.filteredTaiwanCities.value.length, 22);

  harness.form.location = "台北";
  harness.fieldErrors.location =
    "請從台灣縣市選項中選擇商品所在地";
  harness.handleLocationInput();

  assert.deepEqual(
    Array.from(harness.filteredTaiwanCities.value),
    ["臺北市"],
  );

  let preventCount = 0;
  harness.handleLocationEnter({
    preventDefault() {
      preventCount += 1;
    },
  });

  assert.equal(preventCount, 1);
  assert.equal(harness.form.location, "臺北市");
  assert.equal(harness.fieldErrors.location, "");
  assert.equal(harness.locationMenuOpen.value, false);

  await harness.submitUpdate();

  assert.equal(harness.updateRequests.length, 1);
  const [, request] = harness.updateRequests[0];
  assert.equal(request.location, "臺北市");
  assert.equal(Object.hasOwn(request, "locationMenuOpen"), false);
  assert.equal(Object.hasOwn(request, "activeLocationIndex"), false);
  assert.equal(Object.hasOwn(request, "filteredTaiwanCities"), false);
});

test("Edit Custom shipping/payment errors focus the expanded input and presets clear them", async () => {
  const harness = createEditHarness();

  harness.fillForm(editableAuction());
  harness.selectedFormOptions.shippingMethod = CUSTOM_OPTION_VALUE;
  harness.handleFormOptionChange("shippingMethod");
  harness.selectedFormOptions.paymentMethod = CUSTOM_OPTION_VALUE;
  harness.handleFormOptionChange("paymentMethod");
  harness.renderedCustomFields.add("shippingMethod");
  harness.renderedCustomFields.add("paymentMethod");

  await harness.submitUpdate();

  assert.equal(
    harness.fieldErrors.shippingMethod,
    "請輸入自訂運送方式",
  );
  assert.equal(
    harness.fieldErrors.paymentMethod,
    "請輸入自訂付款方式",
  );
  assert.deepEqual(harness.selectedFields, [
    '[data-validation-field="shippingMethod"]',
  ]);
  assert.deepEqual(harness.fieldControlQueries, [
    ["shippingMethod", "[data-custom-input]"],
  ]);
  assert.equal(harness.updateRequests.length, 0);

  harness.selectedFormOptions.shippingMethod = "宅配";
  harness.handleFormOptionChange("shippingMethod");
  harness.selectedFormOptions.paymentMethod = "轉帳";
  harness.handleFormOptionChange("paymentMethod");

  assert.equal(harness.form.shippingMethod, "宅配");
  assert.equal(harness.form.paymentMethod, "轉帳");
  assert.equal(harness.fieldErrors.shippingMethod, "");
  assert.equal(harness.fieldErrors.paymentMethod, "");
});

test("Edit sends real Custom strings without UI selectors or sentinel values", async () => {
  const harness = createEditHarness();
  const customValues = {
    defectNote: "鞋帶有輕微染色",
    authenticityNote: "朋友自日本門市購入",
    shippingMethod: "黑貓宅配",
    paymentMethod: "匯款後通知",
    warrantyNote: "售後問題請先站內聯絡",
  };

  harness.fillForm(editableAuction());

  for (const fieldName of Object.keys(customValues)) {
    harness.selectedFormOptions[fieldName] = CUSTOM_OPTION_VALUE;
    harness.handleFormOptionChange(fieldName);
    harness.form[fieldName] = customValues[fieldName];
  }

  await harness.submitUpdate();

  assert.equal(harness.updateRequests.length, 1);
  const request = harness.updateRequests[0][1];

  for (const [fieldName, value] of Object.entries(customValues)) {
    assert.equal(request[fieldName], value);
  }
  assert.equal(JSON.stringify(request).includes(CUSTOM_OPTION_VALUE), false);
  assert.equal("selectedFormOptions" in request, false);
  assert.equal("deliveryOption" in request, false);
  assert.equal("currentImageUrls" in request, false);
});

test("Edit scrubs a manually entered UI sentinel before validation or request", async () => {
  const optionalHarness = createEditHarness();
  optionalHarness.fillForm(editableAuction());

  const optionalFields = [
    "defectNote",
    "authenticityNote",
    "warrantyNote",
  ];
  for (const fieldName of optionalFields) {
    optionalHarness.selectedFormOptions[fieldName] = CUSTOM_OPTION_VALUE;
    optionalHarness.handleFormOptionChange(fieldName);
    optionalHarness.form[fieldName] = CUSTOM_OPTION_VALUE;
  }

  await optionalHarness.submitUpdate();

  assert.equal(optionalHarness.updateRequests.length, 1);
  const request = optionalHarness.updateRequests[0][1];
  for (const fieldName of optionalFields) {
    assert.equal(request[fieldName], "");
  }
  assert.equal(JSON.stringify(request).includes(CUSTOM_OPTION_VALUE), false);

  const requiredHarness = createEditHarness();
  requiredHarness.fillForm(editableAuction());
  for (const fieldName of ["shippingMethod", "paymentMethod"]) {
    requiredHarness.selectedFormOptions[fieldName] = CUSTOM_OPTION_VALUE;
    requiredHarness.handleFormOptionChange(fieldName);
    requiredHarness.form[fieldName] = CUSTOM_OPTION_VALUE;
  }

  await requiredHarness.submitUpdate();

  assert.equal(requiredHarness.updateRequests.length, 0);
  assert.equal(requiredHarness.form.shippingMethod, "");
  assert.equal(requiredHarness.form.paymentMethod, "");
  assert.equal(
    requiredHarness.fieldErrors.shippingMethod,
    "請輸入自訂運送方式",
  );
  assert.equal(
    requiredHarness.fieldErrors.paymentMethod,
    "請輸入自訂付款方式",
  );
});

test("Edit delivery option maps only to allowMeetup and never offers meetup-only", async () => {
  assert.doesNotMatch(templateSource, /v-model\s*=\s*["']form\.allowMeetup["']/);
  assert.match(templateSource, /僅提供寄送/);
  assert.match(templateSource, /提供寄送，也可協調面交/);
  assert.doesNotMatch(templateSource, /MEETUP_ONLY/);

  const harness = createEditHarness();

  harness.fillForm(editableAuction({ allowMeetup: true }));
  assert.equal(
    harness.deliveryOption.value,
    DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP,
  );

  harness.deliveryOption.value = DELIVERY_OPTION_VALUES.SHIPPING_ONLY;
  assert.equal(harness.form.allowMeetup, false);

  harness.fillForm(editableAuction({ allowMeetup: false }));
  assert.equal(
    harness.deliveryOption.value,
    DELIVERY_OPTION_VALUES.SHIPPING_ONLY,
  );

  harness.deliveryOption.value = DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP;
  assert.equal(harness.form.allowMeetup, true);

  await harness.submitUpdate();

  assert.equal(harness.updateRequests.length, 1);
  assert.equal(harness.updateRequests[0][1].allowMeetup, true);
  assert.equal("deliveryOption" in harness.updateRequests[0][1], false);
});

test("Edit rejects a non-empty shoe size outside the shared allowlist", async () => {
  const harness = createEditHarness();

  harness.fillForm(editableAuction({ size: "US 12" }));
  await harness.submitUpdate();

  assert.equal(harness.fieldErrors.size, "請選擇尺寸");
  assert.deepEqual(harness.selectedFields, [
    '[data-validation-field="size"]',
  ]);
  assert.equal(harness.updateRequests.length, 0);
});

test("invalid Edit time binds endAt, scrolls and focuses it without updating", async () => {
  const harness = createEditHarness();

  harness.fillForm(
    editableAuction({
      startAt: "2026-09-20T10:00:00",
      endAt: "2026-09-20T09:00:00",
    }),
  );

  await harness.submitUpdate();

  assert.equal(
    harness.fieldErrors.endAt,
    "競標結束時間必須晚於開始時間",
  );
  assert.equal(harness.updateRequests.length, 0);
  assert.deepEqual(harness.selectedFields, [
    '[data-validation-field="endAt"]',
  ]);
  assert.equal(harness.scrollCalls.length, 1);
  assert.equal(harness.scrollCalls[0].behavior, "smooth");
  assert.equal(harness.scrollCalls[0].block, "center");
  assert.equal(harness.focusCalls.length, 1);
  assert.equal(harness.focusCalls[0].preventScroll, true);

  harness.form.endAt = "2099-09-20T11:00";
  harness.revalidateField("endAt");

  assert.equal(
    harness.fieldErrors.endAt,
    "",
    "correcting endAt must clear its own error without another submit",
  );
});

test("Edit shows all current field errors but jumps only to the first DOM field", async () => {
  const harness = createEditHarness();

  harness.fillForm(
    editableAuction({
      brandId: "",
      categoryId: "",
      productName: "",
      size: "",
    }),
  );

  await harness.submitUpdate();

  assert.equal(harness.fieldErrors.brandId, "請選擇品牌");
  assert.equal(harness.fieldErrors.categoryId, "請選擇分類");
  assert.equal(harness.fieldErrors.productName, "請輸入商品名稱");
  assert.equal(harness.fieldErrors.size, "請選擇尺寸");
  assert.deepEqual(harness.selectedFields, [
    '[data-validation-field="brandId"]',
  ]);
  assert.equal(harness.scrollCalls.length, 1);
  assert.equal(harness.focusCalls.length, 1);
  assert.equal(harness.updateRequests.length, 0);
});

test("Edit preserves lifecycle locks and ACTIVE original startAt update contract", async () => {
  const template = templateSource;
  const script = scriptSetup(editSource);

  assert.match(
    template,
    /v-model\.number\s*=\s*["']form\.brandId["'][\s\S]*?:disabled\s*=\s*["']!editable \|\| auctionStatus === 'ACTIVE'["']/,
  );
  assert.match(
    template,
    /v-model\s*=\s*["']form\.startAt["'][\s\S]*?:disabled\s*=\s*["']!editable \|\| auctionStatus === 'ACTIVE'["']/,
  );
  assert.match(script, /bidCount\.value\s*>\s*0/);
  assert.match(
    script,
    /auctionStatus\.value\s*===\s*["']SCHEDULED["']\s*\|\|\s*auctionStatus\.value\s*===\s*["']ACTIVE["']/,
  );

  const harness = createEditHarness();
  const originalStartAt = "2020-09-20T10:00:00";

  harness.fillForm(
    editableAuction({
      hasStarted: true,
      startAt: originalStartAt,
      endAt: "2099-09-20T11:00:00",
    }),
  );

  assert.equal(harness.auctionStatus.value, "ACTIVE");
  assert.equal(harness.editable.value, true);

  await harness.submitUpdate();

  assert.equal(harness.updateRequests.length, 1);
  assert.equal(harness.updateRequests[0][0], 42);
  assert.equal(harness.updateRequests[0][1].startAt, originalStartAt);
  assert.equal("fieldErrors" in harness.updateRequests[0][1], false);
  assert.equal(harness.routerPushes.length, 1);
  assert.equal(harness.routerPushes[0].name, "auction-detail");
  assert.equal(harness.routerPushes[0].params.id, 42);
});
