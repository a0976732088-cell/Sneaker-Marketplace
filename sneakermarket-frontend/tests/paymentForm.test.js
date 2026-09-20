import test from "node:test";
import assert from "node:assert/strict";

import {
  formatAccountLastFive,
  formatCardCvv,
  formatCardExpiry,
  formatCardNumber,
  validateCreditCardFormValues,
  validateShippingFormValues,
  validateTransferFormValues,
} from "../src/utils/paymentForm.js";

test("payment field formatters keep only the supported numeric shapes", () => {
  assert.equal(formatCardNumber("4111x1111 1111-1111"), "4111 1111 1111 1111");
  assert.equal(formatCardExpiry("12-30"), "12/30");
  assert.equal(formatCardCvv("12x34z"), "1234");
  assert.equal(formatAccountLastFive("12x3456"), "12345");
});

test("shipping validation trims required values and enforces entity lengths", () => {
  assert.deepEqual(validateShippingFormValues({}), {
    receiverName: "請輸入收件人姓名",
    receiverPhone: "請輸入收件人電話",
    shippingAddress: "請輸入配送地址",
  });
  assert.deepEqual(
    validateShippingFormValues({
      receiverName: "王小明",
      receiverPhone: "0912345678",
      shippingAddress: "台北市測試路 1 號",
    }),
    {},
  );
  assert.ok(validateShippingFormValues({ receiverName: "名".repeat(51) }).receiverName);
  assert.ok(validateShippingFormValues({ receiverPhone: "1".repeat(21) }).receiverPhone);
  assert.ok(validateShippingFormValues({ shippingAddress: "址".repeat(301) }).shippingAddress);
});

test("credit-card validation matches the existing simulated checkout rules", () => {
  const now = new Date(2026, 8, 20, 12, 0, 0);
  assert.deepEqual(
    validateCreditCardFormValues(
      {
        cardHolderName: "王小明",
        cardNumber: "4111 1111 1111 1111",
        cardExpiry: "12/30",
        cardCvv: "123",
      },
      now,
    ),
    {},
  );

  const invalid = validateCreditCardFormValues(
    {
      cardHolderName: " ",
      cardNumber: "4111-letters",
      cardExpiry: "13/20",
      cardCvv: "12x",
    },
    now,
  );
  assert.deepEqual(Object.keys(invalid).sort(), [
    "cardCvv",
    "cardExpiry",
    "cardHolderName",
    "cardNumber",
  ]);

  assert.ok(
    validateCreditCardFormValues(
      {
        cardHolderName: "王小明",
        cardNumber: "4111111111111111",
        cardExpiry: "08/26",
        cardCvv: "1234",
      },
      now,
    ).cardExpiry,
  );
});

test("transfer validation requires a trimmed name and exactly five digits", () => {
  assert.deepEqual(
    validateTransferFormValues({ remitterName: "王小明", accountLastFive: "12345" }),
    {},
  );
  assert.ok(validateTransferFormValues({ remitterName: " ", accountLastFive: "123" }).remitterName);
  assert.ok(validateTransferFormValues({ remitterName: "王小明", accountLastFive: "ABCDE" }).accountLastFive);
});
