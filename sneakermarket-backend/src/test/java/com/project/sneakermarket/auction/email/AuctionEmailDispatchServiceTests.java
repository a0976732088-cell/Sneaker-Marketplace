package com.project.sneakermarket.auction.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;

class AuctionEmailDispatchServiceTests {
    static Stream<RuntimeException> failures() {
        String untrusted = "password=fixture-secret\r\nCookie=fixture-cookie " + "x".repeat(2000);
        return Stream.of(new MailAuthenticationException(untrusted), new MailSendException(untrusted),
                new MailParseException(untrusted), new IllegalStateException(untrusted));
    }

    static Stream<Arguments> auctionMessages() {
        return Stream.of(
                Arguments.of(AuctionEmailType.SOLD_SELLER, "/auctions/me/listings", "查看成交商品"),
                Arguments.of(AuctionEmailType.UNSOLD_SELLER, "/auctions/me/listings", "查看我的上架"),
                Arguments.of(AuctionEmailType.WON, "/auction-orders", "查看競標訂單"),
                Arguments.of(AuctionEmailType.LOST, "/auctions/1", "查看競標詳情"),
                Arguments.of(AuctionEmailType.ORDER_ABANDONED_BUYER, "/auction-orders", "查看棄標訂單"),
                Arguments.of(AuctionEmailType.ORDER_ABANDONED_SELLER, "/auction-orders", "查看訂單詳情"));
    }

    static Stream<Arguments> abandonedMessages() {
        return Stream.of(
                Arguments.of(AuctionEmailType.ORDER_ABANDONED_BUYER, true),
                Arguments.of(AuctionEmailType.ORDER_ABANDONED_SELLER, false));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void errorsAreSafeBoundedSingleLineSummaries(RuntimeException failure) {
        var fixture = new Fixture(AuctionEmailType.WON, "shoe", "subject");
        doThrow(failure).when(fixture.sender).send(any(MimeMessage.class));

        fixture.dispatch.sendEmailLog(1L);

        assertEquals(AuctionEmailSendStatus.FAILED, fixture.row.getSendStatus());
        assertEquals(1, fixture.row.getRetryCount());
        assertFalse(fixture.row.getLastError().isBlank());
        assertTrue(fixture.row.getLastError().length() <= 1000);
        assertFalse(fixture.row.getLastError().matches("(?s).*(fixture|password|Cookie|\\r|\\n).*"));
        verify(fixture.logs).saveAndFlush(fixture.row);
        verify(fixture.auctions, never()).findByIdForUpdate(any());
        verify(fixture.results, never()).findByAuctionIdForUpdate(any());
    }

    @Test
    void missingLogDoesNotSend() {
        var logs = mock(AuctionEmailLogRepository.class);
        var sender = mock(JavaMailSender.class);
        new AuctionEmailDispatchService(logs, mock(AuctionResultRepository.class), mock(AuctionRepository.class),
                mock(AuctionProductRepository.class), mock(AuctionOrderRepository.class), sender, new AuctionTime(),
                new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid"))
                .sendEmailLog(1L);
        verifyNoInteractions(sender);
    }

    @ParameterizedTest
    @MethodSource("auctionMessages")
    void allTypesSendMimeHtmlAndPlainTextWithExpectedCtaAndRoute(
            AuctionEmailType type, String route, String cta) throws Exception {
        String productName = "限量 <script>alert(\"x\")</script> & 球鞋";
        String subject = "SneakerMarket <測試> & " + type;
        var fixture = new Fixture(type, productName, subject);

        fixture.dispatch.sendEmailLog(1L);

        MimeMessage message = fixture.sentMessage();
        String plainText = body(message, "text/plain");
        String html = body(message, "text/html");
        String detailsHtml = detailTable(html);
        assertNotNull(plainText);
        assertNotNull(html);
        assertFalse(detailsHtml.contains("<p"));
        assertEquals(subject, message.getSubject());
        assertEquals("snapshot@example.invalid", message.getAllRecipients()[0].toString());
        assertTrue(plainText.contains(productName));
        assertTrue(plainText.contains("https://example.invalid" + route));
        assertTrue(plainText.contains("※ 防詐騙提醒 ※"));
        assertTrue(html.contains("background:#f5f7fa"));
        assertTrue(html.contains("background:#ffffff"));
        assertTrue(html.contains("background:#26313d"));
        assertTrue(html.contains("cid:brandLogo"));
        assertTrue(html.contains("※ 防詐騙提醒 ※"));
        assertTrue(html.contains(cta));
        assertTrue(html.contains("https://example.invalid" + route));
        assertTrue(html.contains("SneakerMarket &lt;測試&gt; &amp; " + type));
        assertTrue(html.contains("限量 &lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; 球鞋"));
        assertFalse(html.contains("<script>"));
        assertTrue(hasContentId(message, "<brandLogo>"));

        if (type == AuctionEmailType.UNSOLD_SELLER) {
            assertTrue(plainText.contains("本次無成功出價。"));
            assertTrue(html.contains("本次無成功出價。"));
            assertDetailNote(detailsHtml, "本次無成功出價。");
            assertFalse(plainText.contains("最終成交價：6000"));
        } else if (type == AuctionEmailType.ORDER_ABANDONED_BUYER
                || type == AuctionEmailType.ORDER_ABANDONED_SELLER) {
            assertTrue(plainText.contains("訂單編號：AO-EMAIL-001"));
            assertTrue(html.contains("AO-EMAIL-001"));
        } else {
            assertTrue(plainText.contains("最終成交價：6000"));
            assertTrue(html.contains("6000"));
        }
        assertEquals(AuctionEmailSendStatus.SENT, fixture.row.getSendStatus());
    }

    @ParameterizedTest
    @MethodSource("abandonedMessages")
    void abandonedMessagesContainOrderDeadlineAndRoleSpecificExplanation(
            AuctionEmailType type, boolean buyerMessage) throws Exception {
        var fixture = new Fixture(type, "限量球鞋", "棄標通知");

        fixture.dispatch.sendEmailLog(1L);

        MimeMessage message = fixture.sentMessage();
        String plainText = body(message, "text/plain");
        String html = body(message, "text/html");
        String detailsHtml = detailTable(html);
        assertFalse(detailsHtml.contains("<p"));
        for (String expected : new String[]{"AO-EMAIL-001", "限量球鞋", "6000",
                "2026/09/16 20:00:00（Asia/Taipei）", "2026/09/16 20:00:10（Asia/Taipei）"}) {
            assertTrue(plainText.contains(expected));
            assertTrue(html.contains(expected));
        }
        if (buyerMessage) {
            assertTrue(plainText.contains("棄標原因：逾期未付款"));
            assertTrue(plainText.contains("目前無法再完成付款"));
            assertTrue(html.contains("棄標原因：逾期未付款"));
            assertTrue(html.contains("目前無法再完成付款"));
            assertDetailNote(detailsHtml, "棄標原因：逾期未付款");
        } else {
            assertTrue(plainText.contains("買家未在付款期限內完成付款"));
            assertTrue(plainText.contains("已標記為「已棄標」"));
            assertTrue(html.contains("買家未在付款期限內完成付款"));
            assertTrue(html.contains("已標記為「已棄標」"));
            assertDetailNote(detailsHtml, "此競標訂單的買家未在付款期限內完成付款");
        }
        assertFalse(plainText.contains("重新上架"));
        assertFalse(plainText.contains("第二順位"));
        assertFalse(html.contains("重新上架"));
        assertFalse(html.contains("第二順位"));
        assertEquals(AuctionEmailSendStatus.SENT, fixture.row.getSendStatus());
    }

    private static MimeMessage mimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static String detailTable(String html) {
        String marker = "style=\"width:100%;font-size:15px;line-height:1.7;\">";
        int start = html.indexOf(marker);
        assertTrue(start >= 0);
        start += marker.length();
        int end = html.indexOf("</table>", start);
        assertTrue(end >= 0);
        return html.substring(start, end);
    }

    private static void assertDetailNote(String detailsHtml, String expectedText) {
        String noteRow = "(?s)<tr>\\s*<td colspan=\"2\"[^>]*>.*?"
                + java.util.regex.Pattern.quote(expectedText)
                + ".*?</td>\\s*</tr>";
        assertTrue(java.util.regex.Pattern.compile(noteRow).matcher(detailsHtml).find());
    }

    private static String body(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) return (String) part.getContent();
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = body(multipart.getBodyPart(i), mimeType);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasContentId(Part part, String expected) throws Exception {
        String[] contentIds = part.getHeader("Content-ID");
        if (contentIds != null && Stream.of(contentIds).anyMatch(expected::equals)) return true;
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                if (hasContentId(multipart.getBodyPart(i), expected)) return true;
            }
        }
        return false;
    }

    private static final class Fixture {
        final AuctionEmailLogRepository logs = mock(AuctionEmailLogRepository.class);
        final AuctionResultRepository results = mock(AuctionResultRepository.class);
        final AuctionRepository auctions = mock(AuctionRepository.class);
        final AuctionProductRepository products = mock(AuctionProductRepository.class);
        final AuctionOrderRepository orders = mock(AuctionOrderRepository.class);
        final JavaMailSender sender = mock(JavaMailSender.class);
        final AuctionEmailLog row = new AuctionEmailLog();
        final AuctionEmailDispatchService dispatch;

        Fixture(AuctionEmailType type, String productName, String subject) {
            row.setAuctionId(1L);
            row.setRecipientEmail("snapshot@example.invalid");
            row.setSubject(subject);
            row.setEmailType(type);
            row.setSendStatus(AuctionEmailSendStatus.PENDING);
            row.setRetryCount(0);
            when(logs.findByIdForUpdate(1L)).thenReturn(Optional.of(row));

            var result = new AuctionResult();
            result.setFinalPrice(new BigDecimal("6000"));
            when(results.findByAuctionId(1L)).thenReturn(Optional.of(result));
            var auction = new Auction();
            auction.setAuctionProductId(2L);
            when(auctions.findById(1L)).thenReturn(Optional.of(auction));
            var product = new AuctionProduct();
            product.setProductName(productName);
            when(products.findById(2L)).thenReturn(Optional.of(product));
            var order = new AuctionOrder();
            order.setOrderNumber("AO-EMAIL-001");
            order.setFinalPrice(new BigDecimal("6000"));
            order.setPaymentDueAt(LocalDateTime.of(2026, 9, 16, 20, 0));
            order.setAbandonedAt(LocalDateTime.of(2026, 9, 16, 20, 0, 10));
            when(orders.findByAuctionId(1L)).thenReturn(Optional.of(order));
            when(sender.createMimeMessage()).thenAnswer(call -> mimeMessage());
            dispatch = new AuctionEmailDispatchService(logs, results, auctions, products, orders, sender,
                    new AuctionTime(),
                    new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid"));
        }

        MimeMessage sentMessage() throws Exception {
            var captured = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
            verify(sender).send(captured.capture());
            MimeMessage message = captured.getValue();
            message.saveChanges();
            return message;
        }
    }
}
