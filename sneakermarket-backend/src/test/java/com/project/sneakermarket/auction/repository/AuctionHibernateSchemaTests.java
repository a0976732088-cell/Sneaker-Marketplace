package com.project.sneakermarket.auction.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionResult;
import com.project.sneakermarket.auction.entity.AuctionResultStatus;
import com.project.sneakermarket.member.entity.Member;

/**
 * 真實 MSSQL Schema 驗證。Spring context 先以 update 啟動既有開發 DB；
 * 另建 UUID 命名的空白測試 DB，只用正式 Entity + Hibernate update 建表
 * finally 僅刪除此方法成功建立的專屬測試 DB，絕不 DROP 使用者 DB 或 table。
 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
class AuctionHibernateSchemaTests {
    @Autowired private DataSource dataSource;
    @Autowired private Environment environment;

    @Test
    void existingDevelopmentSchemaRemainsCompatible() {
        assertSchema(new JdbcTemplate(dataSource));
    }

    @Test
    void updateBuildsFreshSchemaAndCanRestartWithScalarIds() {
        JdbcTemplate admin = new JdbcTemplate(dataSource);
        String database = "AuctionSchemaTest_" + UUID.randomUUID().toString().replace("-", "");
        assertTrue(database.matches("AuctionSchemaTest_[a-f0-9]{32}"));
        assertNotEquals(database, admin.queryForObject("SELECT DB_NAME()", String.class));

        // CREATE 成功後才進入 finally；不清除既有同名 DB。
        admin.execute("CREATE DATABASE [" + database + "]");
        try {
            String url = environment.getRequiredProperty("spring.datasource.url");
            assertTrue(url.matches("(?is).*;databaseName=[^;]+.*"),
                    "Schema test requires an explicit databaseName in the configured MSSQL URL");
            String isolatedUrl = url.replaceFirst("(?i)(;databaseName=)[^;]+", "$1" + database);
            DriverManagerDataSource isolated = new DriverManagerDataSource(
                    isolatedUrl,
                    environment.getProperty("spring.datasource.username"),
                    environment.getProperty("spring.datasource.password"));
            JdbcTemplate jdbc = new JdbcTemplate(isolated);
            assertEquals(database, jdbc.queryForObject("SELECT DB_NAME()", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys.tables", Integer.class));

            Long resultId;
            try (SessionFactory factory = updateSchema(isolated)) {
                assertSchema(jdbc);
                assertColumnTypes(jdbc);
                long auctionId = jdbc.queryForObject("""
                        INSERT INTO dbo.auctions
                        (auction_product_id, start_price, current_price, min_increment,
                         start_at, end_at, status, bid_count, version, created_at, updated_at)
                        OUTPUT inserted.auction_id
                        VALUES (1, 5000, 5500, 500, '2026-09-10T11:00:00', '2026-09-10T12:00:00',
                                'SOLD', 1, 0, '2026-09-10T11:00:00', '2026-09-10T12:00:00')
                        """, Long.class);
                long winnerId = jdbc.queryForObject("""
                        INSERT INTO dbo.members (account, password_hash, name, email, phone, role)
                        OUTPUT inserted.member_id
                        VALUES ('schema-test', 'not-a-login', 'Schema test',
                                'schema@example.invalid', '0900000000', 'Role_Member')
                        """, Long.class);

                // 正式 scalar setters 仍能單獨寫入；不設定／fetch association。
                AuctionResult result = new AuctionResult();
                result.setAuctionId(auctionId);
                result.setWinnerId(winnerId);
                result.setFinalPrice(new BigDecimal("5500.00"));
                result.setResultStatus(AuctionResultStatus.SOLD);
                result.setEndedAt(LocalDateTime.of(2026, 9, 10, 12, 0));
                result.setCreatedAt(result.getEndedAt());
                try (var session = factory.openSession()) {
                    var transaction = session.beginTransaction();
                    session.persist(result);
                    transaction.commit();
                }
                resultId = result.getResultId();
                persistScalarOrder(factory, auctionId, winnerId);
                assertNotNull(resultId);
                assertEquals(winnerId, jdbc.queryForObject(
                        "SELECT winner_id FROM dbo.auction_results WHERE result_id = ?", Long.class, resultId));

                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                        "UPDATE dbo.auction_results SET winner_id = -1 WHERE result_id = ?", resultId));
                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                        "UPDATE dbo.auction_results SET auction_id = -1 WHERE result_id = ?", resultId));
                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                        "UPDATE dbo.auction_results SET result_status = 'INVALID' WHERE result_id = ?", resultId));
                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                        INSERT INTO dbo.auction_results (auction_id, result_status, ended_at, created_at)
                        SELECT auction_id, result_status, ended_at, created_at FROM dbo.auction_results
                        WHERE result_id = ?
                        """, resultId));

                // 四個 enum 值皆為 CHECK 允許的正式值。
                for (AuctionResultStatus status : AuctionResultStatus.values()) {
                    assertEquals(1, jdbc.update(
                            "UPDATE dbo.auction_results SET result_status = ? WHERE result_id = ?",
                            status.name(), resultId));
                }
                // winner association 允許 NULL。
                jdbc.update("UPDATE dbo.auction_results SET winner_id = NULL, final_price = NULL,"
                        + " result_status = 'UNSOLD' WHERE result_id = ?", resultId);
            }

            // 再次 update 不重建 table、不丟掉既有 Result / Order，索引不重複。
            try (SessionFactory factory = updateSchema(isolated)) {
                assertSchema(jdbc);
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.auction_results", Integer.class));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.auction_orders", Integer.class));
                assertEquals("收件快照", jdbc.queryForObject(
                        "SELECT receiver_name FROM dbo.auction_orders WHERE order_number = 'AO-schema'", String.class));
                try (var session = factory.openSession()) {
                    AuctionResult persisted = session.find(AuctionResult.class, resultId);
                    assertEquals(AuctionResultStatus.UNSOLD, persisted.getResultStatus());
                    assertNull(persisted.getWinnerId());
                    assertNull(persisted.getWinner());
                }
            }
        } finally {
            // 名稱是本方法產生的 UUID，且 CREATE 已成功；不接受環境提供的清理目標。
            admin.execute("ALTER DATABASE [" + database + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
            admin.execute("DROP DATABASE [" + database + "]");
        }
    }

    private SessionFactory updateSchema(DataSource isolated) {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(Auction.class)
                .addAnnotatedClass(AuctionResult.class)
                .addAnnotatedClass(com.project.sneakermarket.auction.entity.AuctionOrder.class)
                .addAnnotatedClass(Member.class);
        configuration.getProperties().put("hibernate.connection.datasource", isolated);
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        configuration.setProperty("hibernate.hbm2ddl.halt_on_error", "true");
        configuration.setProperty("hibernate.default_schema", "dbo");
        configuration.setProperty("hibernate.physical_naming_strategy",
                environment.getRequiredProperty("spring.jpa.hibernate.naming.physical-strategy"));
        return configuration.buildSessionFactory();
    }

    private void assertSchema(JdbcTemplate jdbc) {
        assertOrderSchema(jdbc);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys.tables WHERE object_id = OBJECT_ID('dbo.auction_results')", Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.indexes i
                WHERE i.object_id = OBJECT_ID('dbo.auction_results') AND i.is_unique = 1
                  AND (SELECT COUNT(*) FROM sys.index_columns ic
                       WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0) = 1
                  AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c
                              ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
                                AND ic.key_ordinal = 1 AND c.name = 'auction_id')
                """, Integer.class));
        assertForeignKey(jdbc, "FK_auction_results_auctions", "auction_id", "auctions", "auction_id");
        assertForeignKey(jdbc, "FK_auction_results_members", "winner_id", "members", "member_id");
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.check_constraints
                WHERE parent_object_id = OBJECT_ID('dbo.auction_results')
                  AND name = 'CK_auction_results_status' AND is_disabled = 0 AND is_not_trusted = 0
                """, Integer.class));
        assertIndex(jdbc, "IX_auctions_status_start_at", List.of("status", "start_at"));
        assertIndex(jdbc, "IX_auctions_status_end_at", List.of("status", "end_at"));
    }

    private void assertForeignKey(JdbcTemplate jdbc, String name, String source, String targetTable, String target) {
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.foreign_keys fk
                JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
                JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
                JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
                WHERE fk.parent_object_id = OBJECT_ID('dbo.auction_results') AND fk.name = ?
                  AND pc.name = ? AND OBJECT_NAME(fk.referenced_object_id) = ? AND rc.name = ?
                  AND fk.is_disabled = 0 AND fk.is_not_trusted = 0
                """, Integer.class, name, source, targetTable, target));
    }

    private void assertIndex(JdbcTemplate jdbc, String name, List<String> columns) {
        assertEquals(columns, jdbc.queryForList("""
                SELECT c.name FROM sys.indexes i
                JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                WHERE i.object_id = OBJECT_ID('dbo.auctions') AND i.name = ? AND i.is_disabled = 0
                  AND ic.key_ordinal > 0 ORDER BY ic.key_ordinal
                """, String.class, name));
    }

    private void assertColumnTypes(JdbcTemplate jdbc) {
        assertEquals("decimal:12:2", jdbc.queryForObject("""
                SELECT CONCAT(TYPE_NAME(user_type_id), ':', precision, ':', scale) FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_results') AND name = 'final_price'
                """, String.class));
        for (String column : List.of("ended_at", "created_at")) {
            assertEquals("datetime2:0", jdbc.queryForObject("""
                    SELECT CONCAT(TYPE_NAME(user_type_id), ':', scale) FROM sys.columns
                    WHERE object_id = OBJECT_ID('dbo.auction_results') AND name = ?
                    """, String.class, column));
        }
        assertEquals("nvarchar:40", jdbc.queryForObject("""
                SELECT CONCAT(TYPE_NAME(user_type_id), ':', max_length) FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_results') AND name = 'result_status'
                """, String.class));
        assertEquals("nvarchar:1000", jdbc.queryForObject("""
                SELECT CONCAT(TYPE_NAME(user_type_id), ':', max_length) FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_results') AND name = 'reason'
                """, String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT is_nullable FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_results') AND name = 'winner_id'
                """, Integer.class));
    }

    private void assertOrderSchema(JdbcTemplate jdbc) {
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys.tables WHERE object_id = OBJECT_ID('dbo.auction_orders')", Integer.class));
        assertEquals(List.of("auction_order_id"), jdbc.queryForList("""
                SELECT c.name FROM sys.indexes i
                JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                WHERE i.object_id = OBJECT_ID('dbo.auction_orders') AND i.is_primary_key = 1
                  AND ic.key_ordinal > 0 ORDER BY ic.key_ordinal
                """, String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT is_identity FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_orders') AND name = 'auction_order_id'
                """, Integer.class));
        for (String column : List.of("auction_id", "order_number")) {
            assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys.indexes i
                    WHERE i.object_id = OBJECT_ID('dbo.auction_orders') AND i.is_unique = 1
                      AND i.is_disabled = 0 AND i.has_filter = 0
                      AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id = i.object_id
                           AND ic.index_id = i.index_id AND ic.key_ordinal > 0) = 1
                      AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c
                                  ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                                  WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
                                    AND ic.key_ordinal = 1 AND c.name = ?)
                    """, Integer.class, column));
        }
        for (String[] fk : List.of(
                new String[]{"FK_auction_orders_auctions", "auction_id", "auctions", "auction_id"},
                new String[]{"FK_auction_orders_buyer", "buyer_id", "members", "member_id"},
                new String[]{"FK_auction_orders_seller", "seller_id", "members", "member_id"})) {
            assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys.foreign_keys fk
                    JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
                    JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
                    JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
                    WHERE fk.parent_object_id = OBJECT_ID('dbo.auction_orders') AND fk.name = ?
                      AND pc.name = ? AND OBJECT_NAME(fk.referenced_object_id) = ? AND rc.name = ?
                      AND fk.is_disabled = 0 AND fk.is_not_trusted = 0
                    """, Integer.class, (Object[]) fk));
        }
        for (String owner : List.of("buyer", "seller")) {
            assertEquals(List.of(owner + "_id", "created_at"), jdbc.queryForList("""
                    SELECT c.name FROM sys.indexes i
                    JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                    WHERE i.object_id = OBJECT_ID('dbo.auction_orders') AND i.name = ? AND i.is_disabled = 0
                      AND ic.key_ordinal > 0 ORDER BY ic.key_ordinal
                    """, String.class, "IX_auction_orders_" + owner + "_created_at"));
        }
        var states = java.util.Map.of(
                "payment_status", List.of("PENDING", "PAID", "FAILED"),
                "shipping_status", List.of("NOT_SHIPPED", "SHIPPED", "DELIVERED"),
                "order_status", List.of("PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"));
        states.forEach((column, values) -> {
            String definition = jdbc.queryForObject("""
                    SELECT definition FROM sys.check_constraints
                    WHERE parent_object_id = OBJECT_ID('dbo.auction_orders') AND name = ?
                      AND is_disabled = 0 AND is_not_trusted = 0
                    """, String.class, "CK_auction_orders_" + column);
            assertNotNull(definition);
            assertTrue(definition.contains(column));
            var actual = new java.util.HashSet<String>();
            var matcher = java.util.regex.Pattern.compile("'([^']*)'").matcher(definition);
            while (matcher.find()) actual.add(matcher.group(1));
            assertEquals(new java.util.HashSet<>(values), actual);
        });
        var strings = java.util.Map.of(
                "order_number", 40, "payment_method", 50,
                "payment_status", 20, "shipping_status", 20, "order_status", 30,
                "receiver_name", 50, "receiver_phone", 20, "shipping_address", 300);
        strings.forEach((column, length) -> assertEquals("nvarchar:" + length * 2, jdbc.queryForObject("""
                SELECT CONCAT(TYPE_NAME(user_type_id), ':', max_length) FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_orders') AND name = ?
                """, String.class, column)));
        for (String column : List.of("auction_order_id", "auction_id", "buyer_id", "seller_id")) {
            assertEquals("bigint", jdbc.queryForObject("""
                    SELECT TYPE_NAME(user_type_id) FROM sys.columns
                    WHERE object_id = OBJECT_ID('dbo.auction_orders') AND name = ?
                    """, String.class, column));
        }
        assertEquals("decimal:12:2", jdbc.queryForObject("""
                SELECT CONCAT(TYPE_NAME(user_type_id), ':', precision, ':', scale) FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_orders') AND name = 'final_price'
                """, String.class));
        for (String column : List.of("created_at", "updated_at", "paid_at", "shipped_at", "completed_at")) {
            assertEquals("datetime2:0", jdbc.queryForObject("""
                    SELECT CONCAT(TYPE_NAME(user_type_id), ':', scale) FROM sys.columns
                    WHERE object_id = OBJECT_ID('dbo.auction_orders') AND name = ?
                    """, String.class, column));
        }
        assertEquals(List.of("completed_at", "paid_at", "shipped_at"), jdbc.queryForList("""
                SELECT name FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.auction_orders') AND is_nullable = 1 ORDER BY name
                """, String.class));
        assertEquals(18, jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.auction_orders')
                """, Integer.class));
    }

    private void persistScalarOrder(SessionFactory factory, long auctionId, long memberId) {
        var order = new com.project.sneakermarket.auction.entity.AuctionOrder();
        order.setOrderNumber("AO-schema");
        order.setAuctionId(auctionId);
        order.setBuyerId(memberId);
        order.setSellerId(memberId);
        order.setFinalPrice(new BigDecimal("5500.00"));
        order.setPaymentMethod("轉帳");
        order.setPaymentStatus(com.project.sneakermarket.auction.entity.AuctionPaymentStatus.PENDING);
        order.setShippingStatus(com.project.sneakermarket.auction.entity.AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(com.project.sneakermarket.auction.entity.AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName("收件快照");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("台北市測試地址");
        order.setCreatedAt(LocalDateTime.of(2026, 9, 10, 12, 0));
        order.setUpdatedAt(order.getCreatedAt());
        try (var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            session.persist(order);
            transaction.commit();
        }
        assertNotNull(order.getAuctionOrderId());
    }}
