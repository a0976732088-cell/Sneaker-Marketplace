/**
 * 結標 Email：AFTER_COMMIT readiness、獨立工作交易與 SMTP 寄送。
 * SOLD 必須已有提交的 AuctionOrder；失敗由 Recovery Scheduler 重試。
 */
package com.project.sneakermarket.auction.email;
