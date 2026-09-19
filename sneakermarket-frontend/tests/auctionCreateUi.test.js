import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, nextTick, reactive, ref } from "vue";

import {
  AUCTION_SHOE_SIZES,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  MAX_AUCTION_IMAGE_COUNT,
  TAIWAN_CITIES,
  normalizeTaiwanCity,
} from "../src/constants/auctionFormOptions.js";
import { parseAuctionTime } from "../src/utils/auctionTime.js";

async function readSource(url) {
  return (await readFile(url, "utf8")).replace(/\r\n/g, "\n");
}

const createSource = await readSource(
  new URL("../src/views/auction/AuctionCreateView.vue", import.meta.url),
);
const auctionServiceSource = await readSource(
  new URL("../src/service/auctionService.js", import.meta.url),
);

const templateMatch = createSource.match(/<template\b[^>]*>([\s\S]*?)<\/template\s*>/);
assert.ok(templateMatch, "AuctionCreateView must retain a template");
const templateSource = templateMatch[1];

function classTokens(source) {
  return [...source.matchAll(/\bclass\s*=\s*["']([^"']*)["']/g)].flatMap(
    (match) => match[1].split(/\s+/).filter(Boolean),
  );
}

function assertModelBinding(field, modifier = "") {
  const directive = modifier ? `v-model\\.${modifier}` : "v-model";
  assert.match(
    templateSource,
    new RegExp(`${directive}\\s*=\\s*["']form\\.${field}["']`),
    `expected ${directive} to keep form.${field}`,
  );
}

function scriptSetup(source) {
  const match = source.match(
    /<script\b(?=[^>]*\bsetup\b)[^>]*>([\s\S]*?)<\/script\s*>/,
  );
  assert.ok(match, "AuctionCreateView must retain a script setup block");

  return match[1].replace(
    /^[ \t]*import\b[\s\S]*?from\s+["'][^"']+["']\s*;?/gm,
    "",
  );
}

function createValidationHarness() {
  const createCalls = [];
  const routerPushes = [];
  const router = {
    push: async (route) => {
      routerPushes.push(route);
    },
    replace: async () => {},
  };
  const context = {
    ref,
    reactive,
    computed,
    nextTick,
    console,
    AUCTION_SHOE_SIZES,
    CUSTOM_OPTION_VALUE,
    DELIVERY_OPTION_VALUES,
    MAX_AUCTION_IMAGE_COUNT,
    TAIWAN_CITIES,
    normalizeTaiwanCity,
    parseAuctionTime,
    onMounted: () => {},
    useRouter: () => router,
    createAuction: async (...args) => {
      createCalls.push(args);
      return { data: { data: 123 } };
    },
    getAuctionOptions: async () => ({
      data: { data: { brands: [], categories: [] } },
    }),
    requireAuctionSession: async () => {},
    handleAuctionAccessError: async () => false,
  };

  vm.runInNewContext(
    scriptSetup(createSource) +
      "globalThis.subject = {" +
      " form, selectedFormOptions, deliveryOption," +
      " images, ready, fieldErrors, validationOrder," +
      " handleImageChange, validateForm, submitAuction," +
      " setFieldElement, revalidateErroredField, revalidateTimeFields," +
      " handleFormOptionChange, normalizeLocation," +
      " locationMenuOpen, activeLocationIndex, activeLocationOptionId," +
      " filteredTaiwanCities," +
      " openLocationMenu, closeLocationMenu, handleLocationInput," +
      " moveLocationOption, selectLocation, handleLocationEnter" +
      " };",
    context,
  );

  const subject = context.subject;
  subject.ready.value = true;
  Object.assign(subject.form, {
    brandId: 1,
    categoryId: 2,
    productName: "Air Test",
    size: "US 9",
    condition: "NEW",
    description: "Test description",
    shippingMethod: "宅配",
    shippingFee: 0,
    paymentMethod: "轉帳",
    startPrice: 5000,
    minIncrement: 100,
    startAt: "2099-09-20T10:00",
    endAt: "2099-09-20T12:00",
  });
  Object.assign(subject.selectedFormOptions, {
    shippingMethod: "宅配",
    paymentMethod: "轉帳",
  });
  subject.images.value = [{ name: "shoe.jpg" }];

  function registerTarget(field) {
    const scrollCalls = [];
    const focusCalls = [];
    const element = {
      scrollIntoView(options) {
        scrollCalls.push(options);
      },
      focus(options) {
        focusCalls.push(options);
      },
    };
    subject.setFieldElement(field)(element);
    return { scrollCalls, focusCalls };
  }

  return {
    subject,
    createCalls,
    routerPushes,
    registerTarget,
  };
}

test("auction create uses the storefront page, hero and four-step section presentation", () => {
  const tokens = classTokens(templateSource);

  for (const className of ["auction-create-page", "create-hero", "create-section"]) {
    assert.ok(tokens.includes(className), `missing ${className}`);
  }

  const createSections = (templateSource.match(/<section\b[^>]*>/g) ?? []).filter(
    (tag) => classTokens(tag).includes("create-section"),
  );
  assert.equal(createSections.length, 4, "expected the four create form sections");

  const styleMatch = createSource.match(
    /<style\b(?=[^>]*\bscoped\b)[^>]*>([\s\S]*?)<\/style\s*>/,
  );
  assert.ok(styleMatch, "AuctionCreateView must use scoped presentation styles");

  const mobileBreakpoints = [
    ...styleMatch[1].matchAll(
      /@media\s*\(\s*max-width\s*:\s*(\d+(?:\.\d+)?)(px|rem|em)\s*\)/g,
    ),
  ].map((match) => Number(match[1]) * (match[2] === "px" ? 1 : 16));

  assert.ok(
    mobileBreakpoints.some((width) => width <= 768),
    "expected at least one mobile max-width breakpoint",
  );
});

test("auction create retains submit, loading, error and success-navigation behavior", () => {
  const formTag = templateSource.match(
    /<form\b(?=[^>]*\bv-if\s*=\s*["']ready["'])(?=[^>]*@submit\.prevent\s*=\s*["']submitAuction["'])[^>]*>/,
  );
  assert.ok(formTag, "ready form must continue to submit through submitAuction");

  assert.match(templateSource, /v-if\s*=\s*["']errorMessage["']/);
  assert.match(templateSource, /v-if\s*=\s*["']!ready\s*&&\s*!errorMessage["']/);
  assert.match(templateSource, /正在確認登入與載入選項\.\.\./);
  assert.match(templateSource, /:disabled\s*=\s*["']submitting["']/);
  assert.match(templateSource, /建立中\.\.\./);

  assert.match(createSource, /\bcreateAuction\s*\(/);
  assert.match(createSource, /\brequireAuctionSession\s*\(\s*\)/);
  assert.match(createSource, /\bgetAuctionOptions\s*\(\s*\)/);
  assert.match(createSource, /const\s+auctionId\s*=\s*response\.data\.data/);
  assert.match(createSource, /name\s*:\s*["']auction-detail["']/);
  assert.match(createSource, /id\s*:\s*auctionId/);

  for (const stateName of ["submitting", "ready", "errorMessage"]) {
    assert.match(createSource, new RegExp(`const\\s+${stateName}\\s*=\\s*ref\\s*\\(`));
  }
});

test("auction create retains the native multi-image input and one-to-ten image rules", () => {
  const imageInput =
    templateSource.match(
      /<input\b(?=[^>]*\btype\s*=\s*["']file["'])(?=[^>]*@change\s*=\s*["']handleImageChange["'])[^>]*>/,
    )?.[0] ?? "";

  assert.ok(imageInput, "missing native file input wired to handleImageChange");
  assert.match(imageInput, /\bmultiple(?:\s|\/?>)/);

  const acceptValue = imageInput.match(/\baccept\s*=\s*["']([^"']+)["']/)?.[1] ?? "";
  const acceptedTypes = acceptValue
    .toLowerCase()
    .split(",")
    .map((value) => value.trim());

  for (const extension of [".jpg", ".jpeg", ".png", ".webp"]) {
    assert.ok(acceptedTypes.includes(extension), `image accept must retain ${extension}`);
  }

  assert.match(templateSource, /images\.length/);
  assert.match(templateSource, /image\.name/);
  assert.match(
    createSource,
    /selectedFiles\.length\s*>\s*MAX_AUCTION_IMAGE_COUNT/,
  );
  assert.match(createSource, /images\.value\.length\s*===\s*0/);
  assert.match(templateSource, /最多\s*\{\{\s*MAX_AUCTION_IMAGE_COUNT\s*\}\}\s*張/);
  assert.equal(MAX_AUCTION_IMAGE_COUNT, 10);
});

test("auction create retains every form binding and does not add draft or identity fields", () => {
  for (const field of [
    "brandId",
    "categoryId",
    "shippingFee",
    "startPrice",
    "minIncrement",
  ]) {
    assertModelBinding(field, "number");
  }

  for (const field of [
    "productName",
    "model",
    "size",
    "description",
    "defectNote",
    "authenticityNote",
    "location",
    "shippingMethod",
    "paymentMethod",
    "warrantyNote",
  ]) {
    assertModelBinding(field, "trim");
  }

  for (const field of [
    "condition",
    "allowReturn",
    "startAt",
    "endAt",
  ]) {
    assertModelBinding(field);
  }

  assert.doesNotMatch(
    templateSource,
    /v-model(?:\.(?:trim|number))?\s*=\s*["']form\.(?:sellerId|status)["']/i,
  );

  const formState = createSource.match(
    /const\s+form\s*=\s*reactive\s*\(\s*\{([\s\S]*?)\}\s*\)\s*;/,
  );
  assert.ok(formState, "missing reactive create form state");
  assert.doesNotMatch(formState[1], /^\s*(?:sellerId|status)\s*:/im);
  assert.doesNotMatch(createSource, /Save\s+Draft|儲存草稿|自動出價|一口價/i);
});

test("auction create size select exposes only the supported sizes in order", () => {
  const sizeSelect = templateSource.match(
    /<select\b(?=[^>]*v-model\.trim\s*=\s*"form\.size")([^>]*)>([\s\S]*?)<\/select\s*>/,
  );
  assert.ok(sizeSelect, "form.size must remain a native select with v-model.trim");
  assert.match(sizeSelect[1], /\brequired\b/, "form.size select must remain required");
  assert.match(
    sizeSelect[2],
    /<option\b(?=[^>]*\bdisabled\b)[^>]*\bvalue\s*=\s*["']["'][^>]*>\s*請選擇尺寸\s*<\/option>/,
  );
  assert.match(sizeSelect[2], /v-for\s*=\s*["']size in AUCTION_SHOE_SIZES["']/);
  assert.match(sizeSelect[2], /:value\s*=\s*["']size["']/);
  assert.deepEqual([...AUCTION_SHOE_SIZES], [
    "US 8",
    "US 8.5",
    "US 9",
    "US 9.5",
    "US 10",
    "US 10.5",
    "US 11",
  ]);
  assert.doesNotMatch(
    templateSource,
    /<input\b(?=[^>]*v-model\.trim\s*=\s*["']form\.size["'])[^>]*>/,
  );
});

test("auction create location is a select-style searchable combobox with inline errors", () => {
  const locationInput =
    templateSource.match(
      /<input\b(?=[^>]*\bid\s*=\s*["']auction-location["'])(?=[^>]*v-model\.trim\s*=\s*["']form\.location["'])[^>]*>/,
    )?.[0] ?? "";
  assert.ok(locationInput, "location must remain bound to form.location");
  assert.match(
    locationInput,
    /class\s*=\s*["']form-select location-combobox-input["']/,
  );
  assert.match(locationInput, /setFieldElement\('location'\)/);
  assert.match(locationInput, /role\s*=\s*["']combobox["']/);
  assert.match(locationInput, /aria-autocomplete\s*=\s*["']list["']/);
  assert.match(
    locationInput,
    /aria-controls\s*=\s*["']auction-create-location-options["']/,
  );
  assert.match(
    locationInput,
    /:aria-expanded\s*=\s*["']locationMenuOpen["']/,
  );
  assert.match(locationInput, /:aria-activedescendant/);
  assert.match(locationInput, /autocomplete\s*=\s*["']off["']/);
  assert.match(locationInput, /:aria-invalid\s*=\s*["']Boolean\(fieldErrors\.location\)["']/);
  assert.match(locationInput, /auction-location-error/);
  assert.doesNotMatch(locationInput, /\slist\s*=/);
  assert.doesNotMatch(locationInput, /\brequired\b/);

  const locationLabel = templateSource.match(
    /<label\b[^>]*for\s*=\s*["']auction-location["'][^>]*>([\s\S]*?)<\/label>/,
  );
  assert.ok(locationLabel);
  assert.doesNotMatch(locationLabel[1], /required-mark/);

  const listbox = templateSource.match(
    /<ul\b(?=[^>]*id\s*=\s*["']auction-create-location-options["'])(?=[^>]*role\s*=\s*["']listbox["'])[^>]*>([\s\S]*?)<\/ul>/,
  );
  assert.ok(listbox, "location must render a dropdown listbox");
  assert.match(
    listbox[1],
    /v-for\s*=\s*["']\(city, index\) in filteredTaiwanCities["']/,
  );
  assert.match(listbox[1], /role\s*=\s*["']option["']/);
  assert.match(
    listbox[1],
    /@pointerdown\.prevent\s*=\s*["']selectLocation\(city\)["']/,
  );
  assert.doesNotMatch(templateSource, /<datalist\b/);
  assert.doesNotMatch(templateSource, /location-combobox-arrow/);
  assert.match(locationInput, /@keydown\.down\.prevent/);
  assert.match(locationInput, /@keydown\.up\.prevent/);
  assert.match(locationInput, /@keydown\.enter\s*=\s*["']handleLocationEnter["']/);
  assert.match(locationInput, /@keydown\.esc\.prevent/);
  assert.match(
    templateSource,
    /<p\b(?=[^>]*v-if\s*=\s*["']fieldErrors\.location["'])(?=[^>]*id\s*=\s*["']auction-location-error["'])[^>]*\brole\s*=\s*["']alert["']/,
  );
  assert.match(
    createSource,
    /\.location-combobox\s*\{[\s\S]*?position:\s*relative/,
  );
  assert.match(
    createSource,
    /\.location-combobox-menu\s*\{[\s\S]*?max-height:\s*15rem;[\s\S]*?overflow-y:\s*auto/,
  );
});

test("auction create uses UI-only preset controllers and inline custom fields", () => {
  const fields = [
    ["defectNote", "AUCTION_DEFECT_NOTE_OPTIONS", "textarea", "1000", false],
    [
      "authenticityNote",
      "AUCTION_AUTHENTICITY_NOTE_OPTIONS",
      "textarea",
      "1000",
      false,
    ],
    [
      "shippingMethod",
      "AUCTION_SHIPPING_METHOD_OPTIONS",
      "input",
      "50",
      true,
    ],
    ["paymentMethod", "AUCTION_PAYMENT_METHOD_OPTIONS", "input", "50", true],
    ["warrantyNote", "AUCTION_WARRANTY_NOTE_OPTIONS", "textarea", "500", false],
  ];

  for (const [field, optionsName, tagName, maxlength, required] of fields) {
    const selection = templateSource.match(
      new RegExp(
        `<select\\b(?=[^>]*v-model=["']selectedFormOptions\\.${field}["'])[^>]*>([\\s\\S]*?)<\\/select\\s*>`,
      ),
    );
    assert.ok(selection, `${field} must use a UI-only selection controller`);
    assert.match(selection[1], new RegExp(`v-for=["']option in ${optionsName}["']`));
    assert.match(
      selection[1],
      /<option\b[^>]*:value=["']CUSTOM_OPTION_VALUE["'][^>]*>其他（自行填寫）<\/option>/,
    );

    assert.match(
      templateSource,
      new RegExp(
        `v-if=["']selectedFormOptions\\.${field} === CUSTOM_OPTION_VALUE["']`,
      ),
    );
    const customControl =
      templateSource.match(
        new RegExp(
          `<${tagName}\\b(?=[^>]*v-model\\.trim=["']form\\.${field}["'])[^>]*>`,
        ),
      )?.[0] ?? "";
    assert.ok(customControl, `${field} custom control must write to the original form field`);
    assert.match(customControl, new RegExp(`maxlength=["']${maxlength}["']`));
    if (required) {
      assert.match(customControl, /\brequired\b/);
    } else {
      assert.doesNotMatch(customControl, /\brequired\b/);
    }
  }

  assert.doesNotMatch(
    templateSource,
    /v-model\s*=\s*["']form\.allowMeetup["']/,
    "allowMeetup must be mapped from deliveryOption instead of a standalone checkbox",
  );
  assert.match(templateSource, /v-model\s*=\s*["']deliveryOption["']/);
  assert.match(templateSource, /DELIVERY_OPTION_VALUES\.SHIPPING_ONLY/);
  assert.match(templateSource, /DELIVERY_OPTION_VALUES\.SHIPPING_OR_MEETUP/);
  assertModelBinding("allowReturn");
});

test("auction create marks only required controls and exposes inline error semantics", () => {
  const formTag =
    templateSource.match(
      /<form\b(?=[^>]*@submit\.prevent\s*=\s*["']submitAuction["'])[^>]*>/,
    )?.[0] ?? "";
  assert.match(formTag, /\bnovalidate\b/);

  const requiredLabelIds = [
    "auction-brand",
    "auction-category",
    "auction-product-name",
    "auction-size",
    "auction-condition",
    "auction-description",
    "auction-images",
    "auction-shipping-method",
    "auction-shipping-fee",
    "auction-payment-method",
    "auction-start-price",
    "auction-min-increment",
    "auction-start-at",
    "auction-end-at",
  ];

  for (const id of requiredLabelIds) {
    const label = templateSource.match(
      new RegExp(
        "<label\\b[^>]*for=[\"']" +
          id +
          "[\"'][^>]*>([\\s\\S]*?)<\\/label>",
      ),
    );
    assert.ok(label, "missing label for " + id);
    assert.match(label[1], /\brequired-mark\b/, id + " must show a required mark");
  }

  assert.match(
    templateSource,
    /<legend\b[^>]*\bclass\s*=\s*["'][^"']*\bform-label\b[^"']*["'][^>]*>[\s\S]*?交付選項[\s\S]*?\brequired-mark\b[\s\S]*?<\/legend>/,
    "delivery option legend must show a required mark",
  );

  const requiredMarks =
    templateSource.match(/\bclass\s*=\s*["'][^"']*\brequired-mark\b[^"']*["']/g) ?? [];
  assert.equal(
    requiredMarks.length,
    requiredLabelIds.length + 2,
    "only required labels, delivery legend and the single form hint should show a required mark",
  );

  const endAtInput =
    templateSource.match(
      /<input\b(?=[^>]*v-model\s*=\s*["']form\.endAt["'])[^>]*>/,
    )?.[0] ?? "";
  assert.match(endAtInput, /:aria-invalid\s*=\s*["']Boolean\(fieldErrors\.endAt\)["']/);
  assert.match(endAtInput, /auction-end-at-error/);
  assert.match(endAtInput, /is-invalid-field/);
  assert.match(
    templateSource,
    /<p\b(?=[^>]*v-if\s*=\s*["']fieldErrors\.endAt["'])(?=[^>]*id\s*=\s*["']auction-end-at-error["'])[^>]*\brole\s*=\s*["']alert["']/,
  );

  const imageInput =
    templateSource.match(
      /<input\b(?=[^>]*\btype\s*=\s*["']file["'])(?=[^>]*@change\s*=\s*["']handleImageChange["'])[^>]*>/,
    )?.[0] ?? "";
  assert.match(imageInput, /:aria-invalid\s*=\s*["']Boolean\(fieldErrors\.images\)["']/);
  assert.match(imageInput, /auction-images-error/);

  const styleSource = createSource.match(
    /<style\b(?=[^>]*\bscoped\b)[^>]*>([\s\S]*?)<\/style\s*>/,
  )?.[1];
  assert.ok(styleSource, "missing scoped validation styles");
  assert.match(styleSource, /\.required-mark\s*\{[^}]*color\s*:\s*#dc3545/i);
  assert.match(styleSource, /\.field-error\s*\{[^}]*color\s*:\s*#dc3545/i);
  assert.match(styleSource, /\.is-invalid-field/);
  assert.match(
    styleSource,
    /\.create-alert\s*\{[^}]*background\s*:\s*#f8d7da[^}]*border-left\s*:\s*4px\s+solid\s+#dc3545/i,
  );
});

test("auction create assigns an invalid time range to endAt and scrolls and focuses it", async () => {
  const { subject, createCalls, registerTarget } = createValidationHarness();
  const endAtTarget = registerTarget("endAt");
  subject.form.startAt = "2026-09-20T10:00";
  subject.form.endAt = "2026-09-20T09:00";

  await subject.submitAuction();

  assert.equal(
    subject.fieldErrors.endAt,
    "競標結束時間必須晚於開始時間",
  );
  assert.equal(createCalls.length, 0, "invalid form must not call createAuction");
  assert.equal(endAtTarget.scrollCalls.length, 1);
  assert.equal(endAtTarget.scrollCalls[0].behavior, "smooth");
  assert.equal(endAtTarget.scrollCalls[0].block, "center");
  assert.equal(endAtTarget.focusCalls.length, 1);
  assert.equal(endAtTarget.focusCalls[0].preventScroll, true);
});

test("auction create reports and navigates to a missing or unsupported size", async () => {
  const { subject, createCalls, registerTarget } = createValidationHarness();
  const sizeTarget = registerTarget("size");
  subject.form.size = "";

  await subject.submitAuction();

  assert.equal(subject.fieldErrors.size, "請選擇尺寸");
  assert.equal(createCalls.length, 0);
  assert.equal(sizeTarget.scrollCalls.length, 1);
  assert.equal(sizeTarget.focusCalls.length, 1);

  const unsupportedHarness = createValidationHarness();
  const unsupportedTarget = unsupportedHarness.registerTarget("size");
  unsupportedHarness.subject.form.size = "US 12";

  await unsupportedHarness.subject.submitAuction();

  assert.equal(unsupportedHarness.subject.fieldErrors.size, "請選擇尺寸");
  assert.equal(unsupportedHarness.createCalls.length, 0);
  assert.equal(unsupportedTarget.scrollCalls.length, 1);
  assert.equal(unsupportedTarget.focusCalls.length, 1);
});

test("auction create accepts one and ten images but rejects zero and eleven", async () => {
  const missingHarness = createValidationHarness();
  const missingTarget = missingHarness.registerTarget("images");
  missingHarness.subject.images.value = [];

  await missingHarness.subject.submitAuction();

  assert.equal(
    missingHarness.subject.fieldErrors.images,
    "請至少選擇一張商品圖片",
  );
  assert.equal(missingHarness.createCalls.length, 0);
  assert.equal(missingTarget.scrollCalls.length, 1);
  assert.equal(missingTarget.focusCalls.length, 1);

  for (const acceptedCount of [1, 10]) {
    const acceptedHarness = createValidationHarness();
    const acceptedInput = {
      files: Array.from({ length: acceptedCount }, (_, index) => ({
        name: "shoe-" + index + ".jpg",
      })),
      value: "accepted-files",
    };

    acceptedHarness.subject.handleImageChange({ target: acceptedInput });

    assert.equal(acceptedHarness.subject.images.value.length, acceptedCount);
    assert.equal(acceptedHarness.subject.validateForm(), true);
    assert.equal(acceptedHarness.subject.fieldErrors.images, "");
    assert.equal(acceptedInput.value, "accepted-files");
  }

  const excessHarness = createValidationHarness();
  const excessTarget = excessHarness.registerTarget("images");
  const input = {
    files: Array.from({ length: 11 }, (_, index) => ({
      name: "shoe-" + index + ".jpg",
    })),
    value: "eleven-files",
  };

  excessHarness.subject.handleImageChange({ target: input });
  await new Promise((resolve) => setImmediate(resolve));

  assert.equal(
    excessHarness.subject.fieldErrors.images,
    "競標商品最多只能選擇 10 張圖片",
  );
  assert.equal(excessHarness.subject.images.value.length, 0);
  assert.equal(input.value, "");
  assert.equal(excessTarget.scrollCalls.length, 1);
  assert.equal(excessTarget.focusCalls.length, 1);
});

test("auction create normalizes exact 台 aliases and rejects unsupported locations inline", async () => {
  for (const validLocation of ["", "高雄市", "臺北市"]) {
    const harness = createValidationHarness();
    harness.subject.form.location = validLocation;
    assert.equal(harness.subject.validateForm(), true);
    assert.equal(harness.subject.fieldErrors.location, "");
  }

  const aliasHarness = createValidationHarness();
  aliasHarness.subject.form.location = "台北市";
  assert.equal(aliasHarness.subject.validateForm(), true);
  assert.equal(aliasHarness.subject.form.location, "臺北市");

  for (const invalidLocation of ["北部", "左營"]) {
    const harness = createValidationHarness();
    harness.subject.form.location = invalidLocation;
    assert.equal(harness.subject.validateForm(), false);
    assert.equal(
      harness.subject.fieldErrors.location,
      "請從台灣縣市選項中選擇商品所在地",
    );
    assert.equal(harness.subject.form.location, invalidLocation);
  }

  const focusHarness = createValidationHarness();
  const locationTarget = focusHarness.registerTarget("location");
  focusHarness.subject.form.location = "北部";

  await focusHarness.subject.submitAuction();

  assert.equal(focusHarness.createCalls.length, 0);
  assert.equal(locationTarget.scrollCalls.length, 1);
  assert.equal(locationTarget.focusCalls.length, 1);
  assert.ok(
    focusHarness.subject.validationOrder.indexOf("location") <
      focusHarness.subject.validationOrder.indexOf("images"),
  );

  focusHarness.subject.form.location = "台北市";
  focusHarness.subject.normalizeLocation();
  assert.equal(focusHarness.subject.form.location, "臺北市");
  assert.equal(focusHarness.subject.fieldErrors.location, "");
});

test("auction create combobox filters aliases and writes only the selected official city", () => {
  const harness = createValidationHarness();
  const { subject } = harness;

  subject.openLocationMenu();
  assert.equal(subject.locationMenuOpen.value, true);
  assert.equal(subject.filteredTaiwanCities.value.length, 22);

  subject.form.location = "台北";
  subject.fieldErrors.location =
    "請從台灣縣市選項中選擇商品所在地";
  subject.handleLocationInput();

  assert.deepEqual(
    Array.from(subject.filteredTaiwanCities.value),
    ["臺北市"],
  );
  assert.equal(subject.activeLocationIndex.value, 0);

  let preventCount = 0;
  subject.handleLocationEnter({
    preventDefault() {
      preventCount += 1;
    },
  });

  assert.equal(preventCount, 1);
  assert.equal(subject.form.location, "臺北市");
  assert.equal(subject.fieldErrors.location, "");
  assert.equal(subject.locationMenuOpen.value, false);

  subject.form.location = "左營";
  subject.handleLocationInput();
  assert.equal(subject.filteredTaiwanCities.value.length, 0);

  subject.handleLocationEnter({
    preventDefault() {
      preventCount += 1;
    },
  });

  assert.equal(preventCount, 1, "no-result Enter must not block form submit");
  assert.equal(subject.form.location, "左營");
});

test("auction create shows every error but navigates only to the first visual field", async () => {
  const { subject, registerTarget } = createValidationHarness();
  const brandTarget = registerTarget("brandId");
  const sizeTarget = registerTarget("size");
  const imageTarget = registerTarget("images");
  subject.form.brandId = "";
  subject.form.size = "";
  subject.images.value = [];

  await subject.submitAuction();

  assert.equal(subject.fieldErrors.brandId, "請選擇品牌");
  assert.equal(subject.fieldErrors.size, "請選擇尺寸");
  assert.equal(subject.fieldErrors.images, "請至少選擇一張商品圖片");
  assert.equal(brandTarget.scrollCalls.length, 1);
  assert.equal(brandTarget.focusCalls.length, 1);
  assert.equal(sizeTarget.scrollCalls.length, 0);
  assert.equal(sizeTarget.focusCalls.length, 0);
  assert.equal(imageTarget.scrollCalls.length, 0);
  assert.equal(imageTarget.focusCalls.length, 0);
});

test("auction create clears a field error as soon as that field becomes valid", () => {
  const { subject } = createValidationHarness();
  subject.form.startAt = "2099-09-20T10:00";
  subject.form.endAt = "2099-09-20T09:00";
  assert.equal(subject.validateForm(), false);
  assert.equal(
    subject.fieldErrors.endAt,
    "競標結束時間必須晚於開始時間",
  );

  subject.form.endAt = "2099-09-20T11:00";
  subject.revalidateTimeFields();
  assert.equal(subject.fieldErrors.endAt, "");

  subject.form.size = "";
  assert.equal(subject.validateForm(), false);
  assert.equal(subject.fieldErrors.size, "請選擇尺寸");
  subject.form.size = "US 10";
  subject.revalidateErroredField("size");
  assert.equal(subject.fieldErrors.size, "");
});

test("auction create preset controllers write values without leaking the custom sentinel", () => {
  const { subject } = createValidationHarness();
  const presets = {
    defectNote: "無明顯瑕疵",
    authenticityNote: "品牌官方網站購入",
    shippingMethod: "超商寄送",
    paymentMethod: "信用卡",
    warrantyNote: "無額外保固",
  };

  for (const [field, value] of Object.entries(presets)) {
    subject.selectedFormOptions[field] = value;
    subject.handleFormOptionChange(field);
    assert.equal(subject.form[field], value);

    subject.selectedFormOptions[field] = CUSTOM_OPTION_VALUE;
    subject.handleFormOptionChange(field);
    assert.equal(subject.form[field], "", `${field} must not receive the sentinel`);
  }
});

test("auction create scrubs a manually entered UI sentinel before validation or request", async () => {
  const optionalHarness = createValidationHarness();
  const optionalFields = [
    "defectNote",
    "authenticityNote",
    "warrantyNote",
  ];

  for (const field of optionalFields) {
    optionalHarness.subject.selectedFormOptions[field] = CUSTOM_OPTION_VALUE;
    optionalHarness.subject.handleFormOptionChange(field);
    optionalHarness.subject.form[field] = CUSTOM_OPTION_VALUE;
  }

  await optionalHarness.subject.submitAuction();

  assert.equal(optionalHarness.createCalls.length, 1);
  const request = optionalHarness.createCalls[0][0];
  for (const field of optionalFields) {
    assert.equal(request[field], "");
  }
  assert.doesNotMatch(JSON.stringify(request), /\*\*CUSTOM\*\*/);

  const requiredHarness = createValidationHarness();
  for (const field of ["shippingMethod", "paymentMethod"]) {
    requiredHarness.subject.selectedFormOptions[field] = CUSTOM_OPTION_VALUE;
    requiredHarness.subject.handleFormOptionChange(field);
    requiredHarness.subject.form[field] = CUSTOM_OPTION_VALUE;
  }

  await requiredHarness.subject.submitAuction();

  assert.equal(requiredHarness.createCalls.length, 0);
  assert.equal(requiredHarness.subject.form.shippingMethod, "");
  assert.equal(requiredHarness.subject.form.paymentMethod, "");
  assert.equal(
    requiredHarness.subject.fieldErrors.shippingMethod,
    "請輸入自訂運送方式",
  );
  assert.equal(
    requiredHarness.subject.fieldErrors.paymentMethod,
    "請輸入自訂付款方式",
  );
});

test("auction create custom shipping and payment errors focus their expanded inputs and clear on preset", async () => {
  const shippingHarness = createValidationHarness();
  const shippingCustomTarget = shippingHarness.registerTarget(
    "shippingMethodCustom",
  );
  shippingHarness.subject.selectedFormOptions.shippingMethod =
    CUSTOM_OPTION_VALUE;
  shippingHarness.subject.handleFormOptionChange("shippingMethod");

  await shippingHarness.subject.submitAuction();

  assert.equal(
    shippingHarness.subject.fieldErrors.shippingMethod,
    "請輸入自訂運送方式",
  );
  assert.equal(shippingHarness.createCalls.length, 0);
  assert.equal(shippingCustomTarget.scrollCalls.length, 1);
  assert.equal(shippingCustomTarget.focusCalls.length, 1);

  shippingHarness.subject.selectedFormOptions.shippingMethod = "宅配";
  shippingHarness.subject.handleFormOptionChange("shippingMethod");
  assert.equal(shippingHarness.subject.form.shippingMethod, "宅配");
  assert.equal(shippingHarness.subject.fieldErrors.shippingMethod, "");

  const paymentHarness = createValidationHarness();
  const paymentCustomTarget = paymentHarness.registerTarget("paymentMethodCustom");
  paymentHarness.subject.selectedFormOptions.paymentMethod = CUSTOM_OPTION_VALUE;
  paymentHarness.subject.handleFormOptionChange("paymentMethod");

  await paymentHarness.subject.submitAuction();

  assert.equal(
    paymentHarness.subject.fieldErrors.paymentMethod,
    "請輸入自訂付款方式",
  );
  assert.equal(paymentHarness.createCalls.length, 0);
  assert.equal(paymentCustomTarget.scrollCalls.length, 1);
  assert.equal(paymentCustomTarget.focusCalls.length, 1);

  paymentHarness.subject.form.paymentMethod = "到店付款";
  paymentHarness.subject.revalidateErroredField("paymentMethod");
  assert.equal(paymentHarness.subject.fieldErrors.paymentMethod, "");

  paymentHarness.subject.form.paymentMethod = "";
  assert.equal(paymentHarness.subject.validateForm(), false);
  assert.equal(
    paymentHarness.subject.fieldErrors.paymentMethod,
    "請輸入自訂付款方式",
  );
  paymentHarness.subject.selectedFormOptions.paymentMethod = "信用卡";
  paymentHarness.subject.handleFormOptionChange("paymentMethod");
  assert.equal(paymentHarness.subject.form.paymentMethod, "信用卡");
  assert.equal(paymentHarness.subject.fieldErrors.paymentMethod, "");
});

test("auction create delivery option maps allowMeetup and UI-only state stays out of the request", async () => {
  const { subject, createCalls } = createValidationHarness();

  assert.equal(
    subject.deliveryOption.value,
    DELIVERY_OPTION_VALUES.SHIPPING_ONLY,
  );
  assert.equal(subject.form.allowMeetup, false);

  subject.deliveryOption.value = DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP;
  assert.equal(subject.form.allowMeetup, true);

  Object.assign(subject.selectedFormOptions, {
    defectNote: CUSTOM_OPTION_VALUE,
    authenticityNote: "品牌官方門市／專櫃購入",
    shippingMethod: CUSTOM_OPTION_VALUE,
    paymentMethod: "信用卡",
    warrantyNote: "依原購買通路／品牌規範",
  });
  for (const field of Object.keys(subject.selectedFormOptions)) {
    subject.handleFormOptionChange(field);
  }
  subject.form.defectNote = "鞋側有輕微摺痕";
  subject.form.shippingMethod = "黑貓宅配";

  await subject.submitAuction();

  assert.equal(createCalls.length, 1);
  const request = createCalls[0][0];
  assert.equal(request.defectNote, "鞋側有輕微摺痕");
  assert.equal(request.authenticityNote, "品牌官方門市／專櫃購入");
  assert.equal(request.shippingMethod, "黑貓宅配");
  assert.equal(request.paymentMethod, "信用卡");
  assert.equal(request.warrantyNote, "依原購買通路／品牌規範");
  assert.equal(request.allowMeetup, true);
  assert.equal(Object.hasOwn(request, "selectedFormOptions"), false);
  assert.equal(Object.hasOwn(request, "deliveryOption"), false);
  assert.doesNotMatch(JSON.stringify(request), /\*\*CUSTOM\*\*/);
  assert.equal(createCalls[0][1], subject.images.value);
});

test("auction service retains JSON request and repeated images multipart parts", () => {
  const createStart = auctionServiceSource.indexOf("export function createAuction");
  const createEnd = auctionServiceSource.indexOf("export const updateAuction", createStart);

  assert.notEqual(createStart, -1, "missing createAuction service function");
  assert.notEqual(createEnd, -1, "missing createAuction service boundary");

  const createAuctionSource = auctionServiceSource.slice(createStart, createEnd);

  assert.match(
    createAuctionSource,
    /export\s+function\s+createAuction\s*\(\s*request\s*,\s*images\s*\)/,
  );
  assert.match(createAuctionSource, /new\s+FormData\s*\(\s*\)/);
  assert.match(
    createAuctionSource,
    /new\s+Blob\s*\(\s*\[\s*JSON\.stringify\s*\(\s*request\s*\)\s*\]\s*,\s*\{[\s\S]*?type\s*:\s*["']application\/json["']/,
  );
  assert.match(
    createAuctionSource,
    /formData\.append\s*\(\s*["']request["']\s*,\s*requestBlob\s*\)/,
  );
  assert.match(
    createAuctionSource,
    /images\.forEach\s*\(\s*\(\s*image\s*\)\s*=>\s*\{[\s\S]*?formData\.append\s*\(\s*["']images["']\s*,\s*image\s*\)/,
  );
  assert.match(
    createAuctionSource,
    /httpClient\.post\s*\(\s*["']\/auctions["']\s*,\s*formData\s*\)/,
  );
});
