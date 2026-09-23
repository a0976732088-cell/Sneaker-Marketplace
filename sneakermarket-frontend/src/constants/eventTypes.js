export const EVENT_TYPES = [
  { key: "RELEASE", label: "新品發售", color: "#a9c4b8" },
  { key: "SALE", label: "優惠活動", color: "#afbed2" },
  { key: "EVENT", label: "品牌活動", color: "#c7adb9" },
  { key: "NOTICE", label: "公告", color: "#d9bdac" },
  { key: "OTHER", label: "其他", color: "#bcc2c9" },
];

const EVENT_TYPE_ALIASES = {
  PROMOTION: "SALE",
  CAMPAIGN: "EVENT",
  SYSTEM: "NOTICE",
};

export const getEventType = (key) => {
  const normalizedKey = EVENT_TYPE_ALIASES[key] || key;
  return EVENT_TYPES.find((type) => type.key === normalizedKey) ?? EVENT_TYPES[4];
};
