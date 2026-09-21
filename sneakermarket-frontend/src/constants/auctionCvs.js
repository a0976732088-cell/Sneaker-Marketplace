export const AUCTION_CVS_STORE_TYPE = Object.freeze({
  UNIMART: "UNIMART",
  FAMI: "FAMI",
  HILIFE: "HILIFE",
});

export const AUCTION_CVS_STORE_OPTIONS = Object.freeze([
  Object.freeze({ value: AUCTION_CVS_STORE_TYPE.UNIMART, label: "7-ELEVEN" }),
  Object.freeze({ value: AUCTION_CVS_STORE_TYPE.FAMI, label: "全家" }),
  Object.freeze({ value: AUCTION_CVS_STORE_TYPE.HILIFE, label: "萊爾富" }),
]);

export const AUCTION_CVS_RESULT_LIMIT = 100;
