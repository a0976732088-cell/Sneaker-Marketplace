export const AUCTION_PAYMENT_METHOD = Object.freeze({
  CREDIT_CARD: "信用卡",
  TRANSFER: "轉帳",
  LINE_PAY: "LINE Pay",
});

/**
 * Demo only. This account is displayed as a simulation instruction and is
 * never persisted with an auction order.
 */
export const AUCTION_TRANSFER_ACCOUNT = Object.freeze({
  bankName: "酷客 Kicks 模擬銀行",
  bankCode: "999",
  accountName: "酷客 Kicks",
  accountNumber: "123456789012",
});
