# 模組與資料表責任（BASE）

| 模組 | package | 主要資料表責任 |
|---|---|---|
| 會員 | `member` | `members`, `coupons` |
| 商城 | `commerce` | `brands`, `categories`, `products`, `product_variants`, `product_images`, `carts`, `cart_items`, `orders`, `order_items` |
| 廣告 | `advertisement` | `advertisements` |
| 行事曆 | `calendar` | `calendar_events`, `event_follows` |
| 通知 | `notification` | `notifications`, `notification_preferences`, `notification_logs` |
| 競標 | `auction` | `auction_products`, `auction_product_images`, `auctions`, `auction_bids`, `auction_results`, `auction_orders`, `auction_email_logs` |
| 管理員 | `admin` | 後台協調層；不重複建立各模組主資料表 |

`brands` 與 `categories` 雖由商城組維護，但一般商城與競標商城共用。
