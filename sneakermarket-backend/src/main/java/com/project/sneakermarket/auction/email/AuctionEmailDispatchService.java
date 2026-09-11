package com.project.sneakermarket.auction.email;

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
    private final AuctionEmailLogRepository logs;
    private final AuctionResultRepository results;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
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
                case WON -> "/auction-orders";
                case SOLD_SELLER, UNSOLD_SELLER -> "/auctions/me/listings";
                case LOST -> "/auctions/" + log.getAuctionId();
            };
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(settings.from());
            message.setTo(log.getRecipientEmail());
            message.setSubject(log.getSubject());
            message.setText("商品：" + product.getProductName() + "\n結果："
                    + AuctionEmailService.outcome(log.getEmailType())
                    + (log.getEmailType() == AuctionEmailType.UNSOLD_SELLER ? "\n本次無成功出價。"
                        : "\n最終成交價：" + result.getFinalPrice().toPlainString())
                    + "\n查看詳情：" + settings.frontendBaseUrl() + route);
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

    private String safeError(Exception ex) {
        if (ex instanceof MailAuthenticationException) return "SMTP authentication failed";
        if (ex instanceof MailSendException) return "SMTP delivery failed";
        if (ex instanceof MailException) return "Mail preparation or transport failed";
        return "Email preparation or delivery failed";
    }
}
