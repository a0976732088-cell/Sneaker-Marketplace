// 競標 API 的無 offset LocalDateTime 固定表示 Asia/Taipei。
// 明確補 +08:00，避免依瀏覽器／部署主機時區解析。
export function parseAuctionTime(value) {
  if (!value) return new Date(NaN);
  const text = String(value).replace(" ", "T");
  return new Date(/(?:Z|[+-]\d{2}:?\d{2})$/i.test(text) ? text : text + "+08:00");
}

export function formatAuctionTime(value) {
  const date = parseAuctionTime(value);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("zh-TW", {
    timeZone: "Asia/Taipei", year: "numeric", month: "2-digit",
    day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).format(date);
}
