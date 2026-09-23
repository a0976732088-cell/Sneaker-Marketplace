import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, nextTick, reactive, ref } from "vue";

import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DESCRIPTION_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  AUCTION_MODEL_OPTIONS_BY_BRAND,
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_SHOE_SIZES,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  TAIWAN_CITIES,
} from "../src/constants/auctionFormOptions.js";

async function readSource(url) {
  return (await readFile(url, "utf8")).replace(/\r\n/g, "\n");
}

const editSource = await readSource(
  new URL("../src/views/auction/AuctionEditView.vue", import.meta.url),
);
const productSource = await readSource(
  new URL("../src/components/auction/edit/AuctionEditProductSection.vue", import.meta.url),
);
const descriptionSource = await readSource(
  new URL("../src/components/auction/edit/AuctionEditDescriptionSection.vue", import.meta.url),
);
const imageSource = await readSource(
  new URL("../src/components/auction/edit/AuctionEditImageSection.vue", import.meta.url),
);
const transactionSource = await readSource(
  new URL("../src/components/auction/edit/AuctionEditTransactionSection.vue", import.meta.url),
);
const settingsSource = await readSource(
  new URL("../src/components/auction/edit/AuctionEditSettingsSection.vue", import.meta.url),
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
const productTemplate = section(productSource, "<template>", "</template>");
const descriptionTemplate = section(descriptionSource, "<template>", "</template>");
const imageTemplate = section(imageSource, "<template>", "</template>");
const transactionTemplate = section(transactionSource, "<template>", "</template>");
const settingsTemplate = section(settingsSource, "<template>", "</template>");
const fieldTemplates = [
  productTemplate,
  descriptionTemplate,
  transactionTemplate,
  settingsTemplate,
];

function componentTag(name) {
  const tag = templateSource.match(new RegExp("<" + name + "\\b[\\s\\S]*?\\/>"))?.[0];
  assert.ok(tag, "missing " + name + " in Edit Parent");
  return tag;
}

test("Auction Edit renders the shared required shoe-size selector", () => {
  const sizeSelect = productTemplate.match(
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
    productTemplate,
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
  const controls = fieldTemplates.join("\n");
  const requiredControls =
    controls.match(
      /<(?:input|select|textarea)\b(?=[^>]*\brequired\b)[^>]*>/g,
    ) ?? [];
  const requiredMarks =
    [templateSource, ...fieldTemplates].join("\n")
      .match(/class\s*=\s*["']required-mark["']/g) ?? [];

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
  for (const childSource of [
    productSource,
    descriptionSource,
    transactionSource,
    settingsSource,
  ]) {
    const childStyle = section(
      childSource,
      /<style\b(?=[^>]*\bscoped\b)[^>]*>/,
      "</style>",
    );
    assert.match(childStyle, /\.required-mark\s*\{[^}]*color\s*:\s*#dc3545\b/i);
    assert.match(childStyle, /\.field-error\s*\{[^}]*color\s*:\s*#dc3545\b/i);
  }
  assert.match(
    styleSource,
    /\.global-error-alert\s*\{[^}]*background\s*:\s*#f8d7da\b/i,
  );
});

test("Auction Edit mirrors Create model choices and editable description templates", () => {
  assert.ok(AUCTION_MODEL_OPTIONS_BY_BRAND.Nike.includes("Dunk Low"));
  assert.ok(AUCTION_MODEL_OPTIONS_BY_BRAND.Jordan.includes("Air Jordan 4"));
  assert.equal(AUCTION_DESCRIPTION_OPTIONS.length, 4);

  const modelSelect = productTemplate.match(
    /<select\b(?=[^>]*id=["']auction-edit-model-option["'])(?=[^>]*v-model=["']selectedModelOption["'])[^>]*>([\s\S]*?)<\/select>/,
  );
  assert.ok(modelSelect);
  assert.match(modelSelect[0], /:disabled=["']!editable \|\| !form\.brandId["']/);
  assert.match(modelSelect[1], /option in availableModelOptions/);
  assert.match(modelSelect[1], /CUSTOM_OPTION_VALUE/);
  assert.match(
    productTemplate,
    /id=["']auction-edit-model-custom["'][\s\S]*?v-model\.trim=["']form\.model["']/,
  );
  assert.match(
    productSource,
    /set:\s*\(value\)\s*=>\s*emit\("update:selectedModelOption", value\)/,
  );
  assert.match(
    modelSelect[0],
    /@change=["']emit\('model-option-change'\)["']/,
  );

  assert.match(
    descriptionTemplate,
    /v-for=["']\(option, index\) in AUCTION_DESCRIPTION_OPTIONS["']/,
  );
  assert.match(descriptionTemplate, /@click=["']emit\('apply-description-template', option\)["']/);
  assert.match(descriptionTemplate, /@click=["']emit\('start-custom-description'\)["']/);
  assert.match(
    descriptionTemplate,
    /v-model\.trim=["']form\.description["'][\s\S]*?@input=["']emit\('description-input'\)["']/,
  );
  assert.match(descriptionTemplate, /id=["']auction-edit-description["']/);
});

test("Auction Edit keeps optional custom fields but fixes shipping and payment choices", () => {
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
  assert.deepEqual(AUCTION_PAYMENT_METHOD_OPTIONS, ["轉帳", "信用卡", "LINE Pay"]);
  assert.deepEqual(AUCTION_WARRANTY_NOTE_OPTIONS, [
    "無額外保固",
    "依原購買通路／品牌規範",
    "售後問題可與賣家聯繫協調",
  ]);

  for (const fieldName of ["defectNote", "authenticityNote", "warrantyNote"]) {
    const ownerTemplate =
      fieldName === "warrantyNote" ? transactionTemplate : descriptionTemplate;
    assert.match(
      ownerTemplate,
      new RegExp(
        "v-model=[\"']selectedFormOptions\\." + fieldName + "[\"'][\\s\\S]*?" +
          "CUSTOM_OPTION_VALUE",
      ),
    );
  }

  for (const [fieldName, legacyOption] of [
    ["shippingMethod", "legacyShippingMethodOption"],
    ["paymentMethod", "legacyPaymentMethodOption"],
  ]) {
    const select = transactionTemplate.match(
      new RegExp(
        "<select\\b(?=[^>]*v-model=[\"']selectedFormOptions\\." + fieldName + "[\"'])[^>]*>([\\s\\S]*?)<\\/select>",
      ),
    )?.[0] ?? "";
    assert.ok(select);
    assert.doesNotMatch(select, /CUSTOM_OPTION_VALUE|其他（自行填寫）/);
    assert.match(select, new RegExp(legacyOption));
  }

  assert.doesNotMatch(transactionTemplate, /auction-edit-shipping-method-custom/);
  assert.doesNotMatch(transactionTemplate, /auction-edit-payment-method-custom/);
});

test("Auction Edit uses the existing read-only gallery without image upload controls", () => {
  assert.match(
    imageSource,
    /import\s+AuctionImageGallery\s+from\s+["']@\/components\/auction\/AuctionImageGallery\.vue["']/,
  );

  const galleryTag = imageTemplate.match(/<AuctionImageGallery\b[\s\S]*?\/>/)?.[0] ?? "";
  const imageTag = componentTag("AuctionEditImageSection");

  assert.match(imageTag, /:image-urls\s*=\s*["']currentImageUrls["']/);
  assert.match(imageTag, /:product-name\s*=\s*["']form\.productName["']/);
  assert.match(galleryTag, /:image-urls\s*=\s*["']imageUrls["']/);
  assert.match(
    galleryTag,
    /:product-name\s*=\s*["']productName \|\| '競標商品'["']/,
  );
  assert.match(imageTemplate, /目前刊登圖片/);
  assert.match(imageTemplate, /商品圖片會沿用目前刊登內容/);
  assert.doesNotMatch(imageTemplate, /type\s*=\s*["']file["']/);
  assert.doesNotMatch(imageTemplate, /功能 4|尚未實作|API 尚未完成/);
});

test("Auction Edit uses the shared 22-city select and renders a legacy option", () => {
  const locationArea = section(
    descriptionSource,
    '<!-- 商品所在地 -->',
    '</template>',
  );
  const locationSelect = locationArea.match(
    /<select\b(?=[^>]*id=["']auction-edit-location["'])(?=[^>]*v-model\.trim=["']form\.location["'])[^>]*>([\s\S]*?)<\/select>/,
  );

  assert.ok(locationSelect, "location must be a native select");
  assert.match(locationSelect[0], /:disabled=["']!editable["']/);
  assert.match(locationSelect[0], /Boolean\(fieldErrors\.location\)/);
  assert.doesNotMatch(locationSelect[0], /\brequired\b/);
  assert.match(locationSelect[1], /city in TAIWAN_CITIES/);
  assert.match(locationSelect[1], /legacyLocationOption/);
  assert.match(locationSelect[1], /（舊資料）/);
  assert.equal(TAIWAN_CITIES.length, 22);
  assert.doesNotMatch(locationArea, /role=["']combobox["']|location-combobox/);
  assert.doesNotMatch(
    locationArea,
    /<input\b(?=[^>]*v-model\.trim\s*=\s*["']form\.location["'])/,
  );
  assert.match(
    locationArea,
    /<p\b(?=[^>]*v-if=["']fieldErrors\.location["'])(?=[^>]*role=["']alert["'])/,
  );
});

test("Auction Edit endAt owns the relationship error and accessible invalid state", () => {
  const endAtArea = section(
    settingsSource,
    'data-validation-field="endAt"',
    "</template>",
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

test("Auction Edit wires five UI sections and preserves every validation field container", () => {
  const components = [
    "AuctionEditProductSection",
    "AuctionEditDescriptionSection",
    "AuctionEditImageSection",
    "AuctionEditTransactionSection",
    "AuctionEditSettingsSection",
  ];
  for (const component of components) {
    assert.match(
      editSource,
      new RegExp("import\\s+" + component + "\\s+from\\s+[\"']@/components/auction/edit/" + component + "\\.vue[\"']"),
    );
  }

  const wiring = [
    ["AuctionEditProductSection", [
      'v-model:selected-model-option="selectedModelOption"',
      ':form="form"', ':brands="brands"', ':categories="categories"',
      ':editable="editable"', ':auction-status="auctionStatus"',
      ':available-model-options="availableModelOptions"',
      ':field-errors="fieldErrors"', '@brand-change="handleBrandChange"',
      '@model-option-change="handleModelOptionChange"',
      '@revalidate-field="revalidateField"',
    ]],
    ["AuctionEditDescriptionSection", [
      ':form="form"', ':editable="editable"', ':field-errors="fieldErrors"',
      ':selected-description-option="selectedDescriptionOption"',
      ':selected-form-options="selectedFormOptions"',
      ':legacy-location-option="legacyLocationOption"',
      '@apply-description-template="applyDescriptionTemplate"',
      '@start-custom-description="startCustomDescription"',
      '@description-input="handleDescriptionInput"',
      '@form-option-change="handleFormOptionChange"',
      '@revalidate-field="revalidateField"',
    ]],
    ["AuctionEditImageSection", [
      ':image-urls="currentImageUrls"', ':product-name="form.productName"',
    ]],
    ["AuctionEditTransactionSection", [
      'v-model:delivery-option="deliveryOption"', ':form="form"',
      ':editable="editable"', ':field-errors="fieldErrors"',
      ':selected-form-options="selectedFormOptions"',
      ':legacy-shipping-method-option="legacyShippingMethodOption"',
      ':legacy-payment-method-option="legacyPaymentMethodOption"',
      '@form-option-change="handleFormOptionChange"',
      '@revalidate-field="revalidateField"',
    ]],
    ["AuctionEditSettingsSection", [
      ':form="form"', ':editable="editable"',
      ':auction-status="auctionStatus"', ':field-errors="fieldErrors"',
      '@revalidate-field="revalidateField"',
    ]],
  ];
  for (const [name, attributes] of wiring) {
    const tag = componentTag(name);
    for (const attribute of attributes) {
      assert.ok(tag.includes(attribute), name + " missing " + attribute);
    }
  }

  const fieldGroups = [
    [productTemplate, ["brandId", "categoryId", "productName", "size", "condition"]],
    [descriptionTemplate, ["description", "location"]],
    [transactionTemplate, ["shippingMethod", "shippingFee", "paymentMethod"]],
    [settingsTemplate, ["startPrice", "minIncrement", "startAt", "endAt"]],
  ];
  for (const [childTemplate, expectedFields] of fieldGroups) {
    const actualFields = Array.from(
      childTemplate.matchAll(/data-validation-field="([^"]+)"/g),
      (match) => match[1],
    );
    assert.deepEqual(actualFields, expectedFields);
  }
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
    AUCTION_DESCRIPTION_OPTIONS,
    AUCTION_MODEL_OPTIONS_BY_BRAND,
    AUCTION_AUTHENTICITY_NOTE_OPTIONS,
    AUCTION_DEFECT_NOTE_OPTIONS,
    AUCTION_PAYMENT_METHOD_OPTIONS,
    AUCTION_SHIPPING_METHOD_OPTIONS,
    AUCTION_SHOE_SIZES,
    AUCTION_WARRANTY_NOTE_OPTIONS,
    CUSTOM_OPTION_VALUE,
    DELIVERY_OPTION_VALUES,
    TAIWAN_CITIES,
    document: {
      getElementById(id) {
        assert.equal(id, "auction-edit-description");
        return focusableField;
      },
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
        brands,
        fieldErrors,
        validationOrder,
        editable,
        selectedFormOptions,
        selectedModelOption,
        availableModelOptions,
        selectedDescriptionOption,
        legacyLocationOption,
        legacyShippingMethodOption,
        legacyPaymentMethodOption,
        deliveryOption,
        currentImageUrls,
        auctionStatus,
        bidCount,
        originalStartAt,
        originalLocation,
        fillForm,
        handleFormOptionChange,
        handleBrandChange,
        handleModelOptionChange,
        applyDescriptionTemplate,
        startCustomDescription,
        handleDescriptionInput,
        validateForm,
        revalidateField,
        submitUpdate
      };
    `,
    context,
  );
  context.subject.brands.value = [
    { id: 1, name: "Nike" },
    { id: 2, name: "Jordan" },
    { id: 3, name: "adidas" },
    { id: 4, name: "New Balance" },
    { id: 5, name: "Vans" },
  ];

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

test("Edit model options follow brand changes and support a new custom value", () => {
  const harness = createEditHarness();
  harness.fillForm(editableAuction({ brandId: 1, model: "Dunk Low" }));

  assert.ok(harness.availableModelOptions.value.includes("Dunk Low"));
  assert.equal(harness.selectedModelOption.value, "Dunk Low");

  harness.form.brandId = 2;
  harness.handleBrandChange();
  assert.equal(harness.form.model, "");
  assert.equal(harness.selectedModelOption.value, "");
  assert.ok(harness.availableModelOptions.value.includes("Air Jordan 4"));

  harness.selectedModelOption.value = CUSTOM_OPTION_VALUE;
  harness.handleModelOptionChange();
  harness.form.model = "Custom Sneaker X";
  assert.equal(harness.form.model, "Custom Sneaker X");
});

test("Edit preserves legacy model and custom description until the user applies a template", async () => {
  const harness = createEditHarness();
  const description = "會員自己寫的舊商品介紹";
  harness.fillForm(
    editableAuction({
      brandId: 1,
      model: "Old Legacy Model",
      description,
    }),
  );

  assert.equal(harness.selectedModelOption.value, CUSTOM_OPTION_VALUE);
  assert.equal(harness.form.model, "Old Legacy Model");
  assert.equal(harness.selectedDescriptionOption.value, CUSTOM_OPTION_VALUE);
  assert.equal(harness.form.description, description);

  await harness.submitUpdate();
  assert.equal(harness.updateRequests[0][1].model, "Old Legacy Model");
  assert.equal(harness.updateRequests[0][1].description, description);

  const template = AUCTION_DESCRIPTION_OPTIONS[0];
  harness.applyDescriptionTemplate(template);
  assert.equal(harness.form.description, template);
  assert.equal(harness.selectedDescriptionOption.value, template);

  await harness.startCustomDescription();
  assert.equal(harness.selectedDescriptionOption.value, CUSTOM_OPTION_VALUE);
  assert.ok(harness.focusCalls.length > 0);
});

test("Edit preserves an untouched legacy location and accepts only official new choices", async () => {
  for (const location of ["", "高雄市", "臺北市", "台北市", "高雄左營"]) {
    const harness = createEditHarness();
    harness.fillForm(editableAuction({ location }));

    assert.equal(harness.form.location, location);
    assert.equal(harness.validateForm(), true);
    assert.equal(harness.fieldErrors.location, "");

    await harness.submitUpdate();
    assert.equal(harness.updateRequests[0][1].location, location);
  }

  const switchHarness = createEditHarness();
  switchHarness.fillForm(editableAuction({ location: "高雄左營" }));
  assert.equal(switchHarness.legacyLocationOption.value, "高雄左營");

  switchHarness.form.location = "高雄市";
  switchHarness.revalidateField("location");
  await switchHarness.submitUpdate();
  assert.equal(switchHarness.updateRequests[0][1].location, "高雄市");

  const invalidHarness = createEditHarness();
  invalidHarness.fillForm(editableAuction({ location: "臺北市" }));
  invalidHarness.form.location = "新輸入的非縣市值";
  assert.equal(invalidHarness.validateForm(), false);
  assert.equal(
    invalidHarness.fieldErrors.location,
    "請從台灣縣市選項中選擇商品所在地",
  );
});

test("Edit preserves optional custom notes, fixed legacy methods, and image URLs", () => {
  const harness = createEditHarness();
  const optionalLegacyValues = {
    defectNote: "左鞋鞋頭有一道約 1 公分刮痕",
    authenticityNote: "親友自海外實體店購入",
    warrantyNote: "收到商品七日內可聯絡賣家",
  };
  const imageUrls = ["/uploads/auctions/one.jpg", "/uploads/auctions/two.jpg"];

  harness.fillForm(
    editableAuction({
      ...optionalLegacyValues,
      shippingMethod: "黑貓宅配",
      paymentMethod: "面交付款",
      imageUrls,
    }),
  );

  for (const [fieldName, value] of Object.entries(optionalLegacyValues)) {
    assert.equal(harness.selectedFormOptions[fieldName], CUSTOM_OPTION_VALUE);
    assert.equal(harness.form[fieldName], value);
  }
  assert.equal(harness.selectedFormOptions.shippingMethod, "黑貓宅配");
  assert.equal(harness.selectedFormOptions.paymentMethod, "面交付款");
  assert.equal(harness.legacyShippingMethodOption.value, "黑貓宅配");
  assert.equal(harness.legacyPaymentMethodOption.value, "面交付款");
  assert.deepEqual(Array.from(harness.currentImageUrls.value), imageUrls);
});

test("Edit preserves legacy shipping/payment until the user selects fixed presets", async () => {
  const legacyHarness = createEditHarness();
  legacyHarness.fillForm(
    editableAuction({
      shippingMethod: "黑貓宅配",
      paymentMethod: "面交付款",
    }),
  );

  await legacyHarness.submitUpdate();
  assert.equal(legacyHarness.updateRequests[0][1].shippingMethod, "黑貓宅配");
  assert.equal(legacyHarness.updateRequests[0][1].paymentMethod, "面交付款");

  const switchHarness = createEditHarness();
  switchHarness.fillForm(
    editableAuction({
      shippingMethod: "黑貓宅配",
      paymentMethod: "面交付款",
    }),
  );
  switchHarness.selectedFormOptions.shippingMethod = "宅配";
  switchHarness.handleFormOptionChange("shippingMethod");
  switchHarness.selectedFormOptions.paymentMethod = "信用卡";
  switchHarness.handleFormOptionChange("paymentMethod");

  await switchHarness.submitUpdate();
  assert.equal(switchHarness.updateRequests[0][1].shippingMethod, "宅配");
  assert.equal(switchHarness.updateRequests[0][1].paymentMethod, "信用卡");
  assert.equal(switchHarness.legacyShippingMethodOption.value, "");
  assert.equal(switchHarness.legacyPaymentMethodOption.value, "");
});

test("Edit sends optional custom notes and fixed legacy methods without UI state", async () => {
  const harness = createEditHarness();
  const customValues = {
    defectNote: "鞋帶有輕微染色",
    authenticityNote: "朋友自日本門市購入",
    warrantyNote: "售後問題請先站內聯絡",
  };

  harness.fillForm(
    editableAuction({
      shippingMethod: "黑貓宅配",
      paymentMethod: "匯款後通知",
    }),
  );

  for (const [fieldName, value] of Object.entries(customValues)) {
    harness.selectedFormOptions[fieldName] = CUSTOM_OPTION_VALUE;
    harness.handleFormOptionChange(fieldName);
    harness.form[fieldName] = value;
  }

  await harness.submitUpdate();

  assert.equal(harness.updateRequests.length, 1);
  const request = harness.updateRequests[0][1];
  for (const [fieldName, value] of Object.entries(customValues)) {
    assert.equal(request[fieldName], value);
  }
  assert.equal(request.shippingMethod, "黑貓宅配");
  assert.equal(request.paymentMethod, "匯款後通知");
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
    "請選擇運送方式",
  );
  assert.equal(
    requiredHarness.fieldErrors.paymentMethod,
    "請選擇付款方式",
  );
});

test("Edit delivery option maps only to allowMeetup and never offers meetup-only", async () => {
  assert.doesNotMatch(transactionTemplate, /v-model\s*=\s*["']form\.allowMeetup["']/);
  assert.match(transactionTemplate, /僅提供寄送/);
  assert.match(transactionTemplate, /提供寄送，也可協調面交/);
  assert.doesNotMatch(transactionTemplate, /MEETUP_ONLY/);
  assert.match(transactionTemplate, /v-model="deliveryOptionModel"/);
  assert.match(
    transactionSource,
    /set:\s*\(value\)\s*=>\s*emit\("update:deliveryOption", value\)/,
  );

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
  const script = scriptSetup(editSource);

  assert.match(
    productTemplate,
    /<select\b(?=[^>]*v-model\.number\s*=\s*["']form\.brandId["'])(?=[^>]*:disabled\s*=\s*["']!editable \|\| auctionStatus === 'ACTIVE'["'])[^>]*>/,
  );
  assert.match(
    productTemplate,
    /<select\b(?=[^>]*v-model\.number\s*=\s*["']form\.categoryId["'])(?=[^>]*:disabled\s*=\s*["']!editable \|\| auctionStatus === 'ACTIVE'["'])[^>]*>/,
  );
  assert.match(
    settingsTemplate,
    /<input\b(?=[^>]*v-model\s*=\s*["']form\.startAt["'])(?=[^>]*:disabled\s*=\s*["']!editable \|\| auctionStatus === 'ACTIVE'["'])[^>]*>/,
  );
  assert.match(productTemplate, /競標開始後不可修改品牌／分類。/);
  assert.match(settingsTemplate, /競標開始後不可修改開始時間。/);
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
