package com.project.sneakermarket.auction.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionEmailDispatchService {
    private static final DateTimeFormatter TAIPEI_DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final AuctionEmailLogRepository logs;
    private final AuctionResultRepository results;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final AuctionOrderRepository orders;
    private final JavaMailSender sender;
    private final AuctionTime time;
    private final AuctionEmailSettings settings;

    /** 只鎖 Email Log；SMTP 不持有 Auction/Result 寫入鎖。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmailLog(Long emailLogId) {
        if (!settings.enabled()) return;
        AuctionEmailLog log = logs.findByIdForUpdate(emailLogId).orElse(null);
        if (log == null || log.getSendStatus() == AuctionEmailSendStatus.SENT
                || log.getRetryCount() >= settings.maxRetries()) return;
        try {
            var result = results.findByAuctionId(log.getAuctionId()).orElseThrow();
            var auction = auctions.findById(log.getAuctionId()).orElseThrow();
            var product = products.findById(auction.getAuctionProductId()).orElseThrow();
            String route = switch (log.getEmailType()) {
                case WON, ORDER_ABANDONED_BUYER, ORDER_ABANDONED_SELLER -> "/auction-orders";
                case SOLD_SELLER, UNSOLD_SELLER -> "/auctions/me/listings";
                case LOST -> "/auctions/" + log.getAuctionId();
            };
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(settings.from());
            message.setTo(log.getRecipientEmail());
            message.setSubject(log.getSubject());
            message.setText(messageText(log, result, product, route));
            sender.send(message);
            log.setSendStatus(AuctionEmailSendStatus.SENT);
            log.setSentAt(time.now());
            log.setLastError(null);
        } catch (Exception ex) {
            log.setSendStatus(AuctionEmailSendStatus.FAILED);
            log.setRetryCount(log.getRetryCount() + 1);
            // 不使用例外原文、cause 或 stack trace，避免 SMTP 憑證及收件資料洩漏。
            log.setLastError(safeError(ex));
        }
        log.setUpdatedAt(time.now());
        logs.saveAndFlush(log);
    }

    private String messageText(AuctionEmailLog log, AuctionResult result, AuctionProduct product, String route) {
        if (log.getEmailType() == AuctionEmailType.ORDER_ABANDONED_BUYER
                || log.getEmailType() == AuctionEmailType.ORDER_ABANDONED_SELLER) {
            AuctionOrder order = orders.findByAuctionId(log.getAuctionId()).orElseThrow();
            String details = "訂單編號：" + order.getOrderNumber()
                    + "\n商品：" + product.getProductName()
                    + "\n成交價格：" + order.getFinalPrice().toPlainString()
                    + "\n付款期限：" + formatTaipei(order.getPaymentDueAt())
                    + "\n棄標時間：" + formatTaipei(order.getAbandonedAt());
            String explanation = log.getEmailType() == AuctionEmailType.ORDER_ABANDONED_BUYER
                    ? "\n棄標原因：逾期未付款"
                        + "\n\n您的競標訂單未於付款期限前完成付款。"
                        + "\n因此訂單已標記為「已棄標」，目前無法再完成付款。"
                    : "\n\n此競標訂單的買家未在付款期限內完成付款，"
                        + "\n訂單目前已標記為「已棄標」。";
            return details + explanation + "\n查看詳情：" + settings.frontendBaseUrl() + route;
        }
        return "商品：" + product.getProductName() + "\n結果："
                + AuctionEmailService.outcome(log.getEmailType())
                + (log.getEmailType() == AuctionEmailType.UNSOLD_SELLER ? "\n本次無成功出價。"
                    : "\n最終成交價：" + result.getFinalPrice().toPlainString())
                + "\n查看詳情：" + settings.frontendBaseUrl() + route;
    }

    private String formatTaipei(LocalDateTime value) {
        return value == null ? "—" : TAIPEI_DATE_TIME.format(value) + "（Asia/Taipei）";
    }

    private String safeError(Exception ex) {
        if (ex instanceof MailAuthenticationException) return "SMTP authentication failed";
        if (ex instanceof MailSendException) return "SMTP delivery failed";
        if (ex instanceof MailException) return "Mail preparation or transport failed";
        return "Email preparation or delivery failed";
    }
}
