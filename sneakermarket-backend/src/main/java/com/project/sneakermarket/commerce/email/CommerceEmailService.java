package com.project.sneakermarket.commerce.email;

import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.entity.OrderItem;
import com.project.sneakermarket.commerce.entity.ProductImage;
import com.project.sneakermarket.commerce.repository.OrderItemRepository;
import com.project.sneakermarket.commerce.repository.ProductImageRepository;
import com.project.sneakermarket.member.dto.MemberResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class CommerceEmailService {

    private final JavaMailSender sender;
    private final OrderItemRepository orderItemRepository;
    private final ProductImageRepository productImageRepository;

    // 商品照片放在後端 src/main/resources/static/images/products/
    private static final String PRODUCT_IMAGE_FOLDER = "static/images/products/";

    // 沿用通知模組的寄件地址設定，全站寄件人一致
    @Value("${notification.email.from:}")
    private String mailFrom;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    // 在背景執行緒寄信時保持資料庫連線，查商品主圖（延遲載入）才不會失敗
    @Transactional(readOnly = true)
    public void sendPaymentSuccessEmail(
            MemberResponse member,
            Order order
    ) {
        // 寄信失敗只記 log，不往外丟例外，避免連帶回滾付款／訂單狀態
        try {
            List<OrderItem> orderItems =
                    orderItemRepository.findByOrderOrderId(order.getOrderId());

            // ------------------------------------------------------
            // 商品明細：同一個迴圈同時組純文字版與 HTML 版
            // ------------------------------------------------------

            StringBuilder itemText = new StringBuilder("【訂購商品】\n");
            StringBuilder itemRows = new StringBuilder();

            BigDecimal subtotal = BigDecimal.ZERO;

            // 要夾帶在信裡的商品圖片（cid → 圖片檔）
            Map<String, ClassPathResource> productImages = new LinkedHashMap<>();
            int imageIndex = 0;

            for (OrderItem item : orderItems) {

                itemText.append("商品名稱：").append(item.getProductName()).append("\n")
                        .append("規格：").append(item.getSpecification()).append("\n")
                        .append("數量：").append(item.getQuantity()).append("\n")
                        .append("小計：NT$").append(money(item.getLineTotal())).append("\n\n");

                String imageCid = null;
                ClassPathResource image = findPrimaryImage(item);
                if (image != null) {
                    imageCid = "product" + (++imageIndex);
                    productImages.put(imageCid, image);
                }

                itemRows.append(itemRow(
                        imageCid,
                        item.getProductName(),
                        item.getSpecification(),
                        item.getQuantity(),
                        item.getLineTotal()
                ));

                subtotal = subtotal.add(item.getLineTotal());
            }

            // ------------------------------------------------------
            // 金額：商品小計 / 運費 / 優惠折抵（有折抵才顯示）/ 訂單金額
            // ------------------------------------------------------

            boolean hasDiscount = order.getDiscountAmount() != null
                    && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;

            StringBuilder totalsText = new StringBuilder();
            StringBuilder totalsRows = new StringBuilder();

            totalsText.append("商品小計：NT$").append(money(subtotal)).append("\n");
            totalsText.append("運費：NT$").append(money(order.getShippingFee())).append("\n");

            totalsRows.append(amountRow("商品小計", "NT$" + money(subtotal), false));
            totalsRows.append(amountRow("運費", "NT$" + money(order.getShippingFee()), false));

            if (hasDiscount) {
                totalsText.append("優惠折抵：-NT$")
                        .append(money(order.getDiscountAmount())).append("\n");
                totalsRows.append(amountRow(
                        "優惠折抵", "-NT$" + money(order.getDiscountAmount()), false));
            }

            totalsRows.append(amountRow(
                    "訂單金額", "NT$" + money(order.getTotalAmount()), true));

            // ------------------------------------------------------
            // 收件資訊
            // ------------------------------------------------------

            String shippingText;
            StringBuilder shippingRows = new StringBuilder();

            shippingRows.append(infoRow("收件人", order.getReceiverName()));
            shippingRows.append(infoRow("收件電話", order.getReceiverPhone()));

            if ("CVS".equals(order.getShippingMethod())) {

                shippingText = "收件方式：超商取貨\n"
                        + "取貨門市：" + order.getCvsStoreName() + "\n"
                        + "門市地址：" + order.getCvsAddress();

                shippingRows.append(infoRow("收件方式", "超商取貨"));
                shippingRows.append(infoRow("取貨門市", order.getCvsStoreName()));
                shippingRows.append(infoRow("門市地址", order.getCvsAddress()));

            } else {

                shippingText = "收件方式：宅配到府\n"
                        + "收件地址：" + order.getShippingAddress();

                shippingRows.append(infoRow("收件方式", "宅配到府"));
                shippingRows.append(infoRow("收件地址", order.getShippingAddress()));
            }

            String orderUrl = frontendBaseUrl.replaceAll("/+$", "")
                    + "/orders/" + order.getOrderId();

            // ------------------------------------------------------
            // HTML 版（外框、標題、按鈕、頁尾與通知模組一致）
            // 注意：.formatted() 會把 % 當格式符號，CSS 的 % 要寫成 %%
            // ------------------------------------------------------

            String html = """
        <!doctype html>
        <html>
          <body style="margin:0;padding:32px 12px;background:#f5f7fa;
                       font-family:Arial,'Microsoft JhengHei',sans-serif;
                       color:#1f2937;">

            <table role="presentation"
                   cellpadding="0"
                   cellspacing="0"
                   style="width:100%%;max-width:600px;margin:0 auto;
                          background:#ffffff;border-radius:14px;">

              <tr>
                <td style="padding:36px 38px;">

                  <h1 style="margin:0 0 28px;
                             font-size:24px;
                             line-height:1.4;">
                    酷客 Kicks - 付款成功通知
                  </h1>

                  <p style="margin:0 0 20px;
                            font-size:16px;
                            line-height:1.8;">
                    %s 您好：<br>
                    您的訂單已付款成功，感謝您的購買！
                  </p>

                  <p style="margin:0 0 28px;
                            font-size:15px;
                            line-height:1.8;">
                    <strong>訂單編號：</strong>%s
                  </p>

                  <p style="margin:0 0 8px;
                            font-size:15px;
                            font-weight:bold;">
                    訂購商品
                  </p>

                  <table role="presentation"
                         cellpadding="0"
                         cellspacing="0"
                         style="width:100%%;">
                    %s
                  </table>

                  <table role="presentation"
                         cellpadding="0"
                         cellspacing="0"
                         style="width:100%%;margin-top:12px;">
                    %s
                  </table>

                  <p style="margin:32px 0 8px;
                            font-size:15px;
                            font-weight:bold;">
                    收件資訊
                  </p>

                  <table role="presentation"
                         cellpadding="0"
                         cellspacing="0"
                         style="width:100%%;">
                    %s
                  </table>

                  <p style="margin:32px 0 12px;
                            font-size:15px;
                            font-weight:bold;">
                    查看訂單：
                  </p>

                  <a href="%s"
                     style="display:inline-block;
                            padding:12px 20px;
                            background:#26313d;
                            color:#ffffff;
                            text-decoration:none;
                            border-radius:6px;
                            font-size:14px;
                            font-weight:bold;">
                    查看訂單
                  </a>

                  <div style="margin-top:38px;
                              border-top:1px solid #dfe3e8;
                              padding-top:20px;">

                    <table role="presentation"
                           cellpadding="0"
                           cellspacing="0"
                           style="width:100%%;">

                      <tr>
                        <td style="width:48%%;
                                   padding-right:22px;
                                   vertical-align:middle;">

                          <img src="cid:brandLogo"
                               alt="酷客 Kicks"
                               width="300"
                               style="display:block;
                                      width:100%%;
                                      max-width:220px;
                                      height:auto;
                                      border:0;">
                        </td>

                        <td style="vertical-align:middle;
                                   text-align:center;
                                   color:#111827;">

                          <p style="margin:0 0 14px;
                                    font-size:18px;
                                    font-weight:bold;
                                    line-height:1.5;">
                            ※ 防詐騙提醒 ※
                          </p>

                          <p style="margin:0;
                                    font-size:14px;
                                    line-height:1.8;
                                    color:#374151;">
                            酷客 Kicks 不會透過 Email
                            要求您提供密碼、驗證碼或匯款。<br>
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
        """.formatted(
                    esc(member.getName()),
                    esc(order.getOrderNumber()),
                    itemRows,
                    totalsRows,
                    shippingRows,
                    esc(orderUrl)
            );

            // ------------------------------------------------------
            // 純文字版（收件端不支援 HTML 時顯示）
            // ------------------------------------------------------

            String plainText =
                    "您好，" + member.getName() + "！\n\n"
                            + "您的訂單已付款成功。\n\n"
                            + "訂單編號：" + order.getOrderNumber() + "\n"
                            + "訂單金額：NT$" + money(order.getTotalAmount()) + "\n\n"
                            + itemText
                            + totalsText + "\n"
                            + "【收件資訊】\n"
                            + "收件人：" + order.getReceiverName() + "\n"
                            + "收件電話：" + order.getReceiverPhone() + "\n"
                            + shippingText + "\n\n"
                            + "查看訂單：" + orderUrl + "\n\n"
                            + "感謝您的購買！\n"
                            + "酷客 Kicks\n\n"
                            + "※ 防詐騙提醒 ※\n"
                            + "酷客 Kicks 不會透過 Email 要求您提供密碼、驗證碼或匯款。\n"
                            + "如收到可疑訊息，請勿點擊不明連結。";

            // ------------------------------------------------------
            // 寄出
            // ------------------------------------------------------

            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(member.getEmail());
            helper.setSubject("酷客 Kicks - 付款成功通知");
            helper.setText(plainText, html);

            helper.addInline(
                    "brandLogo",
                    new ClassPathResource("logoPhoto/newLogo_rectangle.png"),
                    "image/png"
            );

            // 商品圖片（addInline 必須在 setText 之後）
            for (Map.Entry<String, ClassPathResource> entry : productImages.entrySet()) {
                helper.addInline(entry.getKey(), entry.getValue());
            }

            sender.send(mimeMessage);

        } catch (Exception exception) {
            log.error("付款成功通知信寄送失敗，orderId={}", order.getOrderId(), exception);
        }
    }

    // ==========================================================
    // HTML 小工具
    // ==========================================================

    /** 商品明細的一列：左邊圖片，中間名稱＋規格×數量，右邊小計 */
    private static String itemRow(
            String imageCid,
            String productName,
            String specification,
            Integer quantity,
            BigDecimal lineTotal
    ) {
        // 沒圖片時放同尺寸灰底方框，讓每列欄位對齊
        String imageHtml = imageCid == null
                ? "<div style=\"width:64px;height:64px;border-radius:8px;background:#F1F1F1;\"></div>"
                : """
                  <img src="cid:%s" alt="%s" width="64" height="64"
                       style="display:block;width:64px;height:64px;border:0;
                              border-radius:8px;background:#F1F1F1;">
                  """.formatted(imageCid, esc(productName));

        return """
                <tr>
                  <td style="padding:12px 14px 12px 0;border-bottom:1px solid #eef0f3;
                             width:64px;vertical-align:top;">
                    %s
                  </td>
                  <td style="padding:12px 0;border-bottom:1px solid #eef0f3;
                             font-size:15px;line-height:1.6;vertical-align:top;">
                    <div style="font-weight:bold;">%s</div>
                    <div style="font-size:13px;color:#6b7280;">%s ／ 數量 %s</div>
                  </td>
                  <td style="padding:12px 0;border-bottom:1px solid #eef0f3;
                             font-size:15px;text-align:right;white-space:nowrap;
                             vertical-align:top;">
                    NT$%s
                  </td>
                </tr>
                """.formatted(
                imageHtml,
                esc(productName),
                esc(specification),
                esc(quantity),
                money(lineTotal)
        );
    }

    /** 查商品主圖並從後端 static 資料夾讀檔；任何問題都回傳 null，不影響寄信 */
    private ClassPathResource findPrimaryImage(OrderItem item) {
        try {
            if (item.getVariant() == null) {
                return null; // 規格已被刪除
            }

            Long productId = item.getVariant().getProduct().getProductId();
            List<ProductImage> images =
                    productImageRepository.findByProductProductIdAndIsPrimaryTrue(productId);

            if (images.isEmpty() || images.get(0).getImageUrl() == null) {
                return null;
            }

            // 只取網址最後的檔名，不論存的是完整網址或相對路徑都適用
            String url = images.get(0).getImageUrl();
            String fileName = URLDecoder.decode(
                    url.substring(url.lastIndexOf('/') + 1),
                    StandardCharsets.UTF_8
            );

            ClassPathResource resource =
                    new ClassPathResource(PRODUCT_IMAGE_FOLDER + fileName);

            return resource.exists() ? resource : null;

        } catch (Exception exception) {
            log.warn("訂單信商品圖片讀取失敗，商品名稱={}", item.getProductName(), exception);
            return null;
        }
    }

    /** 金額區的一列；bold = true 時為最後的訂單金額 */
    private static String amountRow(String label, String value, boolean bold) {
        String style = bold
                ? "padding:12px 0 0;border-top:1px solid #dfe3e8;font-size:17px;font-weight:bold;"
                : "padding:4px 0;font-size:14px;color:#374151;";

        return """
                <tr>
                  <td style="%s">%s</td>
                  <td style="%s text-align:right;white-space:nowrap;">%s</td>
                </tr>
                """.formatted(style, esc(label), style, esc(value));
    }

    /** 收件資訊的一列：左欄標籤、右欄內容 */
    private static String infoRow(String label, String value) {
        return """
                <tr>
                  <td style="padding:6px 0;width:90px;font-size:14px;
                             color:#6b7280;vertical-align:top;">%s</td>
                  <td style="padding:6px 0;font-size:14px;line-height:1.6;">%s</td>
                </tr>
                """.formatted(esc(label), esc(value));
    }

    /** HTML 跳脫；null 顯示為空字串 */
    private static String esc(Object value) {
        return value == null ? "" : HtmlUtils.htmlEscape(String.valueOf(value));
    }

    /** 金額轉字串；null 顯示為 0 */
    private static String money(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }
}