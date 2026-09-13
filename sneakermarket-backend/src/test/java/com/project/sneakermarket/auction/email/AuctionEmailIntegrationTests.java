package com.project.sneakermarket.auction.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.scheduler.AuctionEmailRecoveryScheduler;
import com.project.sneakermarket.auction.service.*;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 MSSQL + Spring proxy/AFTER_COMMIT；只 mock SMTP、設定及時間，不掃歷史 Demo。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false", "auction.email.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class AuctionEmailIntegrationTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 11, 12, 0);
    @Autowired AuctionEmailService emails;
    @Autowired AuctionEmailDispatchService dispatch;
    @Autowired AuctionLifecycleService lifecycle;
    @Autowired AuctionOrderService orderService;
    @Autowired AuctionRepository auctions;
    @Autowired AuctionProductRepository products;
    @Autowired AuctionResultRepository results;
    @Autowired AuctionOrderRepository orders;
    @Autowired AuctionBidRepository bids;
    @Autowired AuctionEmailLogRepository logs;
    @Autowired MemberRepository members;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ApplicationEventPublisher events;
    @MockitoBean JavaMailSender sender;
    @MockitoBean AuctionTime time;
    // 寄送服務可測試 enabled=true，實際 Scheduler Bean 仍固定 disabled。
    @MockitoBean AuctionEmailSettings settings;
    private final List<Long> auctionIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private Long seller, winner, loserA, loserB;
    private ExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(NOW);
        when(settings.enabled()).thenReturn(true);
        when(settings.maxRetries()).thenReturn(3);
        when(settings.from()).thenReturn("sender@example.invalid");
        when(settings.frontendBaseUrl()).thenReturn("https://frontend.example.invalid");
        seller = member("Seller");
        winner = member("Winner");
        loserA = member("Loser A");
        loserB = member("Loser B");
    }

    @AfterEach
    void cleanup() throws Exception {
        if (release != null) release.countDown();
        if (executor != null) {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        for (Long id : auctionIds) {
            logs.deleteAllInBatch(logs.findByAuctionId(id));
            orders.findByAuctionId(id).ifPresent(orders::delete);
            results.findByAuctionId(id).ifPresent(results::delete);
            bids.deleteAllInBatch(bids.findByAuctionIdOrderByCreatedAtDesc(id));
            auctions.deleteById(id);
        }
        for (Long id : productIds) products.deleteById(id);
        for (Long id : memberIds) members.deleteById(id);
    }

    @Test
    void unsoldCreatesOnlySellerPendingLog() {
        Long id = closed(false, false);
        emails.ensureEmailLogsForAuction(id);
        var rows = logs.findByAuctionId(id);
        assertEquals(1, rows.size());
        var row = rows.get(0);
        assertEquals(AuctionEmailType.UNSOLD_SELLER, row.getEmailType());
        assertEquals(seller, row.getRecipientMemberId());
        assertEquals(AuctionEmailRecipientRole.SELLER, row.getRecipientRole());
        assertEquals(AuctionEmailSendStatus.PENDING, row.getSendStatus());
        assertEquals(0, row.getRetryCount());
        assertNull(row.getLastError());
        assertNull(row.getSentAt());
        assertEquals(NOW, row.getCreatedAt());
        assertEquals(NOW, row.getUpdatedAt());
        verifyNoInteractions(sender);
    }

    @Test
    void removedResultCreatesNoEmailLogAndIsNotARecoveryCandidate() {
        Long id = fixture(true);
        Auction auction = auctions.findById(id).orElseThrow();
        auction.setRemovedAt(NOW.minusMinutes(10));
        auction.setRemovedByMemberId(seller);
        auction.setRemoveReason("平台下架後到期");
        auction.setRemovedFromStatus(AuctionStatus.ACTIVE);
        auctions.saveAndFlush(auction);

        lifecycle.processAuction(id);

        AuctionResult result = results.findByAuctionId(id).orElseThrow();
        assertEquals(AuctionStatus.REMOVED, auctions.findById(id).orElseThrow().getStatus());
        assertEquals(AuctionResultStatus.REMOVED, result.getResultStatus());
        assertNull(result.getWinnerId());
        assertNull(result.getFinalPrice());
        assertTrue(emails.ensureEmailLogsForAuction(id).isEmpty());
        assertFalse(results.findEmailReadinessCandidateIds().contains(id));
        assertTrue(logs.findByAuctionId(id).isEmpty());
        verifyNoInteractions(sender);
    }

    @Test
    void soldWithoutOrderIsNormalNotReadyEvenWithMissingAddress() {
        Long id = closed(true, false);
        var buyer = members.findById(winner).orElseThrow();
        buyer.setAddress(null);
        members.saveAndFlush(buyer);
        assertThrows(IllegalStateException.class, () -> orderService.createOrderForAuction(id));
        assertTrue(emails.ensureEmailLogsForAuction(id).isEmpty());
        assertTrue(logs.findByAuctionId(id).isEmpty());
        assertFalse(results.findEmailReadinessCandidateIds().contains(id));
        verifyNoInteractions(sender);
        buyer.setAddress("補齊地址");
        members.saveAndFlush(buyer);
        orderService.createOrderForAuction(id);
        assertEquals(4, logs.findByAuctionId(id).size());
        assertTrue(logs.findByAuctionId(id).stream().allMatch(e -> e.getSendStatus() == AuctionEmailSendStatus.SENT));
    }

    @Test
    void soldHasDistinctLosersAndWinnerNeverGetsLost() {
        Long id = closed(true, true);
        emails.ensureEmailLogsForAuction(id);
        var rows = logs.findByAuctionId(id);
        assertEquals(4, rows.size());
        assertEquals(Set.of(seller, winner, loserA, loserB),
                new HashSet<>(rows.stream().map(AuctionEmailLog::getRecipientMemberId).toList()));
        assertRecipient(rows, seller, AuctionEmailType.SOLD_SELLER, AuctionEmailRecipientRole.SELLER);
        assertRecipient(rows, winner, AuctionEmailType.WON, AuctionEmailRecipientRole.WINNER);
        assertRecipient(rows, loserA, AuctionEmailType.LOST, AuctionEmailRecipientRole.BIDDER);
        assertRecipient(rows, loserB, AuctionEmailType.LOST, AuctionEmailRecipientRole.BIDDER);
        assertEquals(Set.of(loserA, loserB), new HashSet<>(bids.findDistinctLosingBidderIds(id, winner)));
    }

    @Test
    void repeatedEnsureKeepsIdsAndEmailSnapshot() {
        Long id = closed(true, true);
        var first = emails.ensureEmailLogsForAuction(id);
        var recipient = members.findById(winner).orElseThrow();
        String original = recipient.getEmail();
        recipient.setEmail(UUID.randomUUID() + "@example.invalid");
        members.saveAndFlush(recipient);
        assertEquals(first, emails.ensureEmailLogsForAuction(id));
        assertEquals(4, logs.findByAuctionId(id).size());
        assertEquals(original, logs.findByAuctionId(id).stream()
                .filter(e -> e.getEmailType() == AuctionEmailType.WON).findFirst().orElseThrow().getRecipientEmail());
    }

    @Test
    void smtpSuccessUsesSnapshotAndExpectedRoleLinks() {
        Long id = closed(true, true);
        for (Long logId : emails.ensureEmailLogsForAuction(id)) dispatch.sendEmailLog(logId);
        var captured = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(4)).send(captured.capture());
        for (var row : logs.findByAuctionId(id)) {
            assertEquals(AuctionEmailSendStatus.SENT, row.getSendStatus());
            assertEquals(NOW, row.getSentAt());
            assertEquals(0, row.getRetryCount());
            assertNull(row.getLastError());
            var message = captured.getAllValues().stream()
                    .filter(m -> m.getTo()[0].equals(row.getRecipientEmail())).findFirst().orElseThrow();
            assertEquals(1, message.getTo().length);
            assertNull(message.getCc());
            assertEquals("sender@example.invalid", message.getFrom());
            assertTrue(message.getText().contains("Email integration fixture"));
            assertTrue(message.getText().contains("6000"));
            String route = row.getEmailType() == AuctionEmailType.WON ? "/auction-orders"
                    : row.getEmailType() == AuctionEmailType.SOLD_SELLER ? "/auctions/me/listings" : "/auctions/" + id;
            assertTrue(message.getText().contains("https://frontend.example.invalid" + route));
        }
    }

    @Test
    void smtpFailureAndSuccessfulRetryNeverChangeAuctionResultOrderOrBids() {
        Long id = closed(true, true);
        var auctionBefore = jdbc.queryForMap("SELECT * FROM dbo.auctions WHERE auction_id=?", id);
        var resultBefore = jdbc.queryForMap("SELECT * FROM dbo.auction_results WHERE auction_id=?", id);
        var orderBefore = jdbc.queryForMap("SELECT * FROM dbo.auction_orders WHERE auction_id=?", id);
        var bidsBefore = jdbc.queryForList("SELECT * FROM dbo.auction_bids WHERE auction_id=? ORDER BY bid_id", id);
        Long logId = emails.ensureEmailLogsForAuction(id).get(0);
        doThrow(new MailSendException("password=secret\r\nCookie=credential " + "x".repeat(2000)))
                .when(sender).send(any(SimpleMailMessage.class));
        dispatch.sendEmailLog(logId);
        var row = logs.findById(logId).orElseThrow();
        assertEquals(AuctionEmailSendStatus.FAILED, row.getSendStatus());
        assertEquals(1, row.getRetryCount());
        assertNull(row.getSentAt());
        assertEquals("SMTP delivery failed", row.getLastError());
        assertEquals(auctionBefore, jdbc.queryForMap("SELECT * FROM dbo.auctions WHERE auction_id=?", id));
        assertEquals(resultBefore, jdbc.queryForMap("SELECT * FROM dbo.auction_results WHERE auction_id=?", id));
        assertEquals(orderBefore, jdbc.queryForMap("SELECT * FROM dbo.auction_orders WHERE auction_id=?", id));
        assertEquals(bidsBefore, jdbc.queryForList("SELECT * FROM dbo.auction_bids WHERE auction_id=? ORDER BY bid_id", id));
        doNothing().when(sender).send(any(SimpleMailMessage.class));
        dispatch.sendEmailLog(logId);
        row = logs.findById(logId).orElseThrow();
        assertEquals(AuctionEmailSendStatus.SENT, row.getSendStatus());
        assertEquals(1, row.getRetryCount());
        assertNotNull(row.getSentAt());
        assertNull(row.getLastError());
    }

    @Test
    void threeFailuresStopBothCandidateQueryAndDirectDispatch() {
        Long id = closed(false, false);
        Long logId = emails.ensureEmailLogsForAuction(id).get(0);
        doThrow(new MailSendException("fixture")).when(sender).send(any(SimpleMailMessage.class));
        for (int failure = 1; failure <= 3; failure++) {
            dispatch.sendEmailLog(logId);
            assertEquals(failure, logs.findById(logId).orElseThrow().getRetryCount());
        }
        assertFalse(logs.findDispatchCandidateIds(id, 3).contains(logId));
        dispatch.sendEmailLog(logId);
        scheduler(List.of(), List.of(logId)).recoverEmails(); // stale candidate 也必須重新檢查
        verify(sender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sentNeverResendsAcrossEventsRecoveryAndDirectCalls() {
        Long id = closed(false, false);
        Long logId = emails.ensureEmailLogsForAuction(id).get(0);
        dispatch.sendEmailLog(logId);
        for (int i = 0; i < 3; i++) {
            new TransactionTemplate(transactions).executeWithoutResult(s -> events.publishEvent(new AuctionEmailReadyEvent(id)));
            scheduler(List.of(id), List.of(logId)).recoverEmails();
            dispatch.sendEmailLog(logId);
        }
        assertTrue(logs.findDispatchCandidateIds(id, 3).isEmpty());
        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void rollbackResultHasNoEmailSideEffects() {
        Long id = fixture(false);
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            lifecycle.processAuction(id);
            assertTrue(logs.findByAuctionId(id).isEmpty());
            verifyNoInteractions(sender);
            s.setRollbackOnly();
        });
        assertTrue(results.findByAuctionId(id).isEmpty());
        assertEquals(AuctionStatus.ACTIVE, auctions.findById(id).orElseThrow().getStatus());
        assertTrue(logs.findByAuctionId(id).isEmpty());
        verifyNoInteractions(sender);
    }

    @Test
    void unsoldEmailOccursOnlyAfterResultCommit() {
        Long id = fixture(false);
        doAnswer(call -> {
            assertTrue(results.findByAuctionId(id).isPresent());
            assertEquals(AuctionStatus.UNSOLD, auctions.findById(id).orElseThrow().getStatus());
            return null;
        }).when(sender).send(any(SimpleMailMessage.class));
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            lifecycle.processAuction(id);
            assertTrue(logs.findByAuctionId(id).isEmpty());
            verifyNoInteractions(sender);
        });
        assertEquals(AuctionEmailSendStatus.SENT, logs.findByAuctionId(id).get(0).getSendStatus());
        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void soldResultCommitWaitsUntilOrderCommitAndOuterRollbackCannotUndoEmails() {
        Long id = fixture(true);
        lifecycle.processAuction(id);
        assertTrue(logs.findByAuctionId(id).isEmpty());
        verifyNoInteractions(sender);
        doAnswer(call -> {
            assertTrue(orders.existsByAuctionId(id));
            return null;
        }).when(sender).send(any(SimpleMailMessage.class));
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            orderService.createOrderForAuction(id); // REQUIRES_NEW 已提交
            s.setRollbackOnly();
        });
        assertTrue(orders.existsByAuctionId(id));
        assertEquals(4, logs.findByAuctionId(id).size());
        verify(sender, times(4)).send(any(SimpleMailMessage.class));
    }

    @Test
    void orderInsertRollbackDoesNotPublishEmailEffects() {
        Long id = closed(true, false);
        when(time.now()).thenAnswer(call -> {
            jdbc.update("DELETE FROM dbo.members WHERE member_id=?", seller);
            return NOW;
        });
        assertThrows(DataIntegrityViolationException.class, () -> orderService.createOrderForAuction(id));
        doReturn(NOW).when(time).now();
        assertFalse(orders.existsByAuctionId(id));
        assertTrue(logs.findByAuctionId(id).isEmpty());
        assertTrue(members.existsById(seller));
        verifyNoInteractions(sender);
    }

    @Test
    void missingRecipientRollsBackWholeLogBatchAndRecoveryCanRetry() {
        Long id = closed(true, true);
        Long badBidId = bid(id, -1L);
        assertThrows(NoSuchElementException.class, () -> emails.ensureEmailLogsForAuction(id));
        assertTrue(logs.findByAuctionId(id).isEmpty());
        assertTrue(results.findEmailReadinessCandidateIds().contains(id));
        bids.deleteById(badBidId);
        assertEquals(4, emails.ensureEmailLogsForAuction(id).size());
    }

    @Test
    void readinessRecoveryFindsOnlyCommittedReadyAuctionsWithoutLogs() {
        Long unsold = closed(false, false);
        Long ready = closed(true, true);
        Long waiting = closed(true, false);
        Long complete = closed(false, false);
        emails.ensureEmailLogsForAuction(complete);
        var candidates = results.findEmailReadinessCandidateIds();
        assertTrue(candidates.containsAll(List.of(unsold, ready)));
        assertFalse(candidates.contains(waiting));
        assertFalse(candidates.contains(complete));
        scheduler(List.of(unsold, ready), List.of()).recoverEmails();
        assertEquals(1, logs.findByAuctionId(unsold).size());
        assertEquals(4, logs.findByAuctionId(ready).size());
    }

    @Test
    void recoveryDispatchesNewTasksAndKeepsGoingAfterFirstSmtpFailure() {
        Long id = closed(true, true);
        var candidates = mock(AuctionResultRepository.class);
        var ownLogs = mock(AuctionEmailLogRepository.class);
        when(candidates.findEmailReadinessCandidateIds()).thenReturn(List.of(id));
        when(ownLogs.findDispatchCandidateIds(null, 3)).thenAnswer(c -> logs.findDispatchCandidateIds(id, 3));
        doThrow(new MailSendException("fixture")).doNothing().when(sender).send(any(SimpleMailMessage.class));
        new AuctionEmailRecoveryScheduler(candidates, ownLogs, emails, dispatch, settings).recoverEmails();
        var rows = logs.findByAuctionId(id).stream().sorted(Comparator.comparing(AuctionEmailLog::getEmailLogId)).toList();
        assertEquals(AuctionEmailSendStatus.FAILED, rows.get(0).getSendStatus());
        assertEquals(AuctionEmailSendStatus.SENT, rows.get(1).getSendStatus());
        assertEquals(3, rows.stream().filter(e -> e.getSendStatus() == AuctionEmailSendStatus.SENT).count());
    }

    @Test
    void duplicateLogIsRejectedByDatabaseUniqueConstraint() {
        Long id = closed(false, false);
        Long logId = emails.ensureEmailLogsForAuction(id).get(0);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO dbo.auction_email_logs
                (auction_id,recipient_member_id,recipient_email,recipient_role,email_type,subject,send_status,retry_count,created_at,updated_at)
                SELECT auction_id,recipient_member_id,recipient_email,recipient_role,email_type,subject,send_status,retry_count,created_at,updated_at
                FROM dbo.auction_email_logs WHERE email_log_id=?
                """, logId));
        assertEquals(1, logs.findByAuctionId(id).size());
    }

    @Test
    void twoCreatorsSerializeOnResultLock() throws Exception {
        Long id = closed(true, true);
        var locked = new CountDownLatch(1);
        release = new CountDownLatch(1);
        var first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(c -> {
            if (first.compareAndSet(true, false)) {
                locked.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return NOW;
        });
        executor = Executors.newFixedThreadPool(2);
        Future<?> a = executor.submit(() -> emails.ensureEmailLogsForAuction(id));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        var started = new CountDownLatch(1);
        Future<?> b = executor.submit(() -> { started.countDown(); return emails.ensureEmailLogsForAuction(id); });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> b.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        a.get(20, TimeUnit.SECONDS);
        b.get(20, TimeUnit.SECONDS);
        assertEquals(4, logs.findByAuctionId(id).size());
    }

    @Test
    void twoDispatchersSerializeOnlyOnEmailLogAndSendOnce() throws Exception {
        Long id = closed(false, false);
        Long logId = emails.ensureEmailLogsForAuction(id).get(0);
        var entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
        doAnswer(c -> {
            entered.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return null;
        }).when(sender).send(any(SimpleMailMessage.class));
        executor = Executors.newFixedThreadPool(3);
        Future<?> a = executor.submit(() -> dispatch.sendEmailLog(logId));
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        var started = new CountDownLatch(1);
        Future<?> b = executor.submit(() -> { started.countDown(); dispatch.sendEmailLog(logId); });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> b.get(200, TimeUnit.MILLISECONDS));
        executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(s ->
                assertTrue(auctions.findByIdForUpdate(id).isPresent()))).get(5, TimeUnit.SECONDS);
        release.countDown();
        a.get(20, TimeUnit.SECONDS);
        b.get(20, TimeUnit.SECONDS);
        verify(sender).send(any(SimpleMailMessage.class));
        assertEquals(AuctionEmailSendStatus.SENT, logs.findById(logId).orElseThrow().getSendStatus());
    }

    @Test
    void disabledFeatureDoesNotCreateOrSendTasks() {
        Long id = closed(false, false);
        Long logId = emails.ensureEmailLogsForAuction(id).get(0);
        when(settings.enabled()).thenReturn(false);
        assertTrue(emails.ensureEmailLogsForAuction(id).isEmpty());
        dispatch.sendEmailLog(logId);
        scheduler(List.of(id), List.of(logId)).recoverEmails();
        verifyNoInteractions(sender);
        assertEquals(AuctionEmailSendStatus.PENDING, logs.findById(logId).orElseThrow().getSendStatus());
    }

    @Test
    void dispatchCandidateQueryIncludesPendingAndRetryableFailedOnly() {
        Long id = closed(true, true);
        var ids = emails.ensureEmailLogsForAuction(id);
        jdbc.update("UPDATE dbo.auction_email_logs SET send_status='FAILED',retry_count=1 WHERE email_log_id=?", ids.get(1));
        jdbc.update("UPDATE dbo.auction_email_logs SET send_status='FAILED',retry_count=3 WHERE email_log_id=?", ids.get(2));
        jdbc.update("UPDATE dbo.auction_email_logs SET send_status='SENT',sent_at=? WHERE email_log_id=?", NOW, ids.get(3));
        assertEquals(List.of(ids.get(0), ids.get(1)), logs.findDispatchCandidateIds(id, 3));
    }

    private AuctionEmailRecoveryScheduler scheduler(List<Long> readyIds, List<Long> retryIds) {
        var source = mock(AuctionResultRepository.class);
        var pending = mock(AuctionEmailLogRepository.class);
        when(source.findEmailReadinessCandidateIds()).thenReturn(readyIds);
        when(pending.findDispatchCandidateIds(null, 3)).thenReturn(retryIds);
        return new AuctionEmailRecoveryScheduler(source, pending, emails, dispatch, settings);
    }

    private void assertRecipient(List<AuctionEmailLog> rows, Long id, AuctionEmailType type, AuctionEmailRecipientRole role) {
        var matches = rows.stream().filter(e -> e.getRecipientMemberId().equals(id)).toList();
        assertEquals(1, matches.size());
        assertEquals(type, matches.get(0).getEmailType());
        assertEquals(role, matches.get(0).getRecipientRole());
    }

    private Long closed(boolean sold, boolean withOrder) {
        Long id = fixture(sold);
        when(settings.enabled()).thenReturn(false);
        lifecycle.processAuction(id);
        if (withOrder) orderService.createOrderForAuction(id);
        when(settings.enabled()).thenReturn(true);
        return id;
    }

    private Long member(String name) {
        var member = new Member();
        String token = UUID.randomUUID().toString();
        member.setAccount("email-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setPhone("0900000000");
        member.setAddress("台北市測試地址");
        Long id = members.saveAndFlush(member).getMemberId();
        memberIds.add(id);
        return id;
    }

    private Long fixture(boolean sold) {
        var product = new AuctionProduct();
        product.setSellerId(seller);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Email integration fixture");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Owned by email integration test");
        product.setShippingMethod("Delivery");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(NOW.minusHours(2));
        product.setUpdatedAt(NOW.minusHours(2));
        product = products.saveAndFlush(product);
        productIds.add(product.getAuctionProductId());
        var auction = new Auction();
        auction.setAuctionProductId(product.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000"));
        auction.setCurrentPrice(new BigDecimal(sold ? "6000" : "5000"));
        auction.setMinIncrement(new BigDecimal("100"));
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBidCount(sold ? 8 : 0);
        auction.setHighestBidderId(sold ? winner : null);
        auction.setCreatedAt(NOW.minusHours(2));
        auction.setUpdatedAt(NOW.minusHours(2));
        Long id = auctions.saveAndFlush(auction).getAuctionId();
        auctionIds.add(id);
        if (sold) {
            for (int i = 0; i < 5; i++) bid(id, loserA);
            bid(id, loserB);
            bid(id, winner);
            bid(id, winner);
        }
        return id;
    }

    private Long bid(Long id, Long bidder) {
        var bid = new AuctionBid();
        bid.setAuctionId(id);
        bid.setBidderId(bidder);
        bid.setBidAmount(new BigDecimal("6000"));
        bid.setCreatedAt(NOW.minusMinutes(1));
        return bids.saveAndFlush(bid).getBidId();
    }
}
