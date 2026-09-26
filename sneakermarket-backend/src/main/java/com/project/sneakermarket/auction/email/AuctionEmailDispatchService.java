package com.project.sneakermarket.auction.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

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
            String destinationUrl = settings.frontendBaseUrl() + route;
            EmailContent content = messageContent(log, result, product, destinationUrl);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(settings.from());
            helper.setTo(log.getRecipientEmail());
            helper.setSubject(log.getSubject());
            helper.setText(content.plainText(), content.html());
            helper.addInline("brandLogo", new ClassPathResource("logoPhoto/newLogo_rectangle.png"), "image/png");
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

    private EmailContent messageContent(
            AuctionEmailLog log, AuctionResult result, AuctionProduct product, String destinationUrl) {
        String details;
        String htmlDetails;
        if (log.getEmailType() == AuctionEmailType.ORDER_ABANDONED_BUYER
                || log.getEmailType() == AuctionEmailType.ORDER_ABANDONED_SELLER) {
            AuctionOrder order = orders.findByAuctionId(log.getAuctionId()).orElseThrow();
            details = "訂單編號：" + order.getOrderNumber()
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
            details += explanation;
            htmlDetails = detailRow("訂單編號", order.getOrderNumber())
                    + detailRow("商品", product.getProductName())
                    + detailRow("成交價格", order.getFinalPrice().toPlainString())
                    + detailRow("付款期限", formatTaipei(order.getPaymentDueAt()))
                    + detailRow("棄標時間", formatTaipei(order.getAbandonedAt()))
                    + detailNote(log.getEmailType() == AuctionEmailType.ORDER_ABANDONED_BUYER
                        ? "棄標原因：逾期未付款<br><br>您的競標訂單未於付款期限前完成付款。<br>"
                            + "因此訂單已標記為「已棄標」，目前無法再完成付款。"
                        : "此競標訂單的買家未在付款期限內完成付款，<br>"
                            + "訂單目前已標記為「已棄標」。");
        } else {
            String outcome = AuctionEmailService.outcome(log.getEmailType());
            details = "商品：" + product.getProductName() + "\n結果：" + outcome
                    + (log.getEmailType() == AuctionEmailType.UNSOLD_SELLER
                        ? "\n本次無成功出價。"
                        : "\n最終成交價：" + result.getFinalPrice().toPlainString());
            htmlDetails = detailRow("商品", product.getProductName())
                    + detailRow("結果", outcome)
                    + (log.getEmailType() == AuctionEmailType.UNSOLD_SELLER
                        ? detailNote("本次無成功出價。")
                        : detailRow("最終成交價", result.getFinalPrice().toPlainString()));
        }

        String cta = ctaLabel(log.getEmailType());
        String plainText = details + "\n查看詳情：" + destinationUrl
                + "\n\n※ 防詐騙提醒 ※"
                + "\n酷客 Kicks 不會透過 Email 要求您提供密碼、驗證碼或匯款。"
                + "\n如收到可疑訊息，請勿點擊不明連結。";
        return new EmailContent(plainText, htmlMessage(log.getSubject(), htmlDetails, destinationUrl, cta));
    }

    private String detailRow(String label, String value) {
        return """
                <tr>
                  <td style="padding:8px 14px 8px 0;color:#6b7280;white-space:nowrap;vertical-align:top;">%s</td>
                  <td style="padding:8px 0;color:#1f2937;font-weight:bold;vertical-align:top;">%s</td>
                </tr>
                """.formatted(escape(label), escape(value));
    }

    private String detailNote(String html) {
        return """
                <tr>
                  <td colspan="2"
                      style="padding:20px 0 0;color:#1f2937;font-size:15px;line-height:1.8;">
                    %s
                  </td>
                </tr>
                """.formatted(html);
    }

    private String ctaLabel(AuctionEmailType type) {
        return switch (type) {
            case SOLD_SELLER -> "查看成交商品";
            case UNSOLD_SELLER -> "查看我的上架";
            case WON -> "查看競標訂單";
            case LOST -> "查看競標詳情";
            case ORDER_ABANDONED_BUYER -> "查看棄標訂單";
            case ORDER_ABANDONED_SELLER -> "查看訂單詳情";
        };
    }

    private String htmlMessage(String subject, String htmlDetails, String destinationUrl, String cta) {
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;padding:32px 12px;background:#f5f7fa;
                               font-family:Arial,'Microsoft JhengHei',sans-serif;color:#1f2937;">
                    <table role="presentation" cellpadding="0" cellspacing="0"
                           style="width:100%%;max-width:600px;margin:0 auto;background:#ffffff;border-radius:14px;">
                      <tr>
                        <td style="padding:36px 38px;">
                          <h1 style="margin:0 0 28px;font-size:24px;line-height:1.4;">%s</h1>
                          <table role="presentation" cellpadding="0" cellspacing="0"
                                 style="width:100%%;font-size:15px;line-height:1.7;">
                            %s
                          </table>
                          <p style="margin:28px 0 12px;font-size:15px;font-weight:bold;">查看詳情：</p>
                          <a href="%s"
                             style="display:inline-block;padding:12px 20px;background:#26313d;color:#ffffff;
                                    text-decoration:none;border-radius:6px;font-size:14px;font-weight:bold;">%s</a>
                          <div style="margin-top:38px;border-top:1px solid #dfe3e8;padding-top:20px;">
                            <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%%;">
                              <tr>
                                <td style="width:48%%;padding-right:22px;vertical-align:middle;">
                                  <img src="cid:brandLogo" alt="酷客 Kicks" width="300"
                                       style="display:block;width:100%%;max-width:220px;height:auto;border:0;">
                                </td>
                                <td style="vertical-align:middle;text-align:center;color:#111827;">
                                  <p style="margin:0 0 14px;font-size:18px;font-weight:bold;line-height:1.5;">
                                    ※ 防詐騙提醒 ※
                                  </p>
                                  <p style="margin:0;font-size:14px;line-height:1.8;color:#374151;">
                                    酷客 Kicks 不會透過 Email 要求您提供密碼、驗證碼或匯款。<br>
                                    如收到可疑訊息，請勿點擊不明連結。
                                  </p>
                                </td>
                              </tr>
                            </table>
                          </div>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(escape(subject), htmlDetails, escape(destinationUrl), escape(cta));
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
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

    private record EmailContent(String plainText, String html) {}
}
