/**
 * 將競標金額顯示為不含小數的整數新台幣。
 *
 * 後端維持 BigDecimal numeric contract，前端只負責顯示格式。
 */
export function formatTwd(value) {
  if (value === null || value === undefined || value === "") {
    return "NT$ -";
  }

  const amount = Number(value);

  if (!Number.isFinite(amount)) {
    return "NT$ -";
  }

  return `NT$ ${amount.toLocaleString("zh-TW", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  })}`;
}
