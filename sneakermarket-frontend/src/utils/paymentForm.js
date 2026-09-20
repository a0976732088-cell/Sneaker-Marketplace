export const AUCTION_RECEIVER_LIMITS = Object.freeze({
  receiverName: 50,
  receiverPhone: 20,
  shippingAddress: 300,
});

function text(value) {
  return value === null || value === undefined ? "" : String(value);
}

export function formatCardNumber(value) {
  const digits = text(value).replace(/\D/g, "").slice(0, 16);
  return (digits.match(/.{1,4}/g) ?? []).join(" ");
}

export function formatCardExpiry(value) {
  const digits = text(value).replace(/\D/g, "").slice(0, 4);
  return digits.length <= 2
    ? digits
    : `${digits.slice(0, 2)}/${digits.slice(2)}`;
}

export function formatCardCvv(value) {
  return text(value).replace(/\D/g, "").slice(0, 4);
}

export function formatAccountLastFive(value) {
  return text(value).replace(/\D/g, "").slice(0, 5);
}

export function validateShippingFormValues(values) {
  const errors = {};
  const receiverName = text(values?.receiverName).trim();
  const receiverPhone = text(values?.receiverPhone).trim();
  const shippingAddress = text(values?.shippingAddress).trim();

  if (!receiverName) {
    errors.receiverName = "請輸入收件人姓名";
  } else if (receiverName.length > AUCTION_RECEIVER_LIMITS.receiverName) {
    errors.receiverName = "收件人姓名不可超過 50 個字";
  }

  if (!receiverPhone) {
    errors.receiverPhone = "請輸入收件人電話";
  } else if (receiverPhone.length > AUCTION_RECEIVER_LIMITS.receiverPhone) {
    errors.receiverPhone = "收件人電話不可超過 20 個字";
  }

  if (!shippingAddress) {
    errors.shippingAddress = "請輸入配送地址";
  } else if (
    shippingAddress.length > AUCTION_RECEIVER_LIMITS.shippingAddress
  ) {
    errors.shippingAddress = "配送地址不可超過 300 個字";
  }

  return errors;
}

export function validateCreditCardFormValues(values, now = new Date()) {
  const errors = {};
  const cardHolderName = text(values?.cardHolderName).trim();
  const cardNumber = text(values?.cardNumber).replace(/\s/g, "");
  const cardExpiry = text(values?.cardExpiry);
  const cardCvv = text(values?.cardCvv);

  if (!cardHolderName) {
    errors.cardHolderName = "請輸入持卡人姓名";
  }

  if (!/^\d{13,16}$/.test(cardNumber)) {
    errors.cardNumber = "卡號格式不正確";
  }

  const expiryMatch = cardExpiry.match(/^(\d{2})\/(\d{2})$/);
  if (!expiryMatch) {
    errors.cardExpiry = "有效期限格式不正確，請輸入 MM/YY";
  } else {
    const month = Number(expiryMatch[1]);
    const year = Number(expiryMatch[2]) + 2000;

    if (month < 1 || month > 12) {
      errors.cardExpiry = "有效期限月份不正確";
    } else {
      const expiryBoundary = new Date(year, month, 1);
      if (expiryBoundary.getTime() <= now.getTime()) {
        errors.cardExpiry = "這張卡已經過期";
      }
    }
  }

  if (!/^\d{3,4}$/.test(cardCvv)) {
    errors.cardCvv = "安全碼格式不正確";
  }

  return errors;
}

export function validateTransferFormValues(values) {
  const errors = {};

  if (!text(values?.remitterName).trim()) {
    errors.remitterName = "請輸入匯款人姓名";
  }

  if (!/^\d{5}$/.test(text(values?.accountLastFive))) {
    errors.accountLastFive = "請輸入轉帳帳號末五碼";
  }

  return errors;
}
