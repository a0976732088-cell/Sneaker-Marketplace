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
  bankName: "SneakerMarket 模擬銀行",
  bankCode: "999",
  accountName: "SneakerMarket",
  accountNumber: "123456789012",
});
