package com.project.sneakermarket.auction.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

class AuctionPaymentIntegrationEntityMappingTests {

    @Test
    void linePayAttemptsUseOwnTableAndBothProviderIdentifiersAreUnique() {
        Table table = AuctionLinePayTransaction.class.getAnnotation(Table.class);

        assertEquals("auction_line_pay_transactions", table.name());
        assertEquals("dbo", table.schema());
        assertEquals(Set.of(
                        "UQ_auction_line_pay_transactions_merchant_order_id",
                        "UQ_auction_line_pay_transactions_line_pay_transaction_id"),
                Arrays.stream(table.uniqueConstraints())
                        .map(constraint -> constraint.name())
                        .collect(Collectors.toSet()));
    }

    @Test
    void linePayTransactionIdRemainsAStringAndNullable() throws Exception {
        Field field = AuctionLinePayTransaction.class.getDeclaredField("linePayTransactionId");
        Column column = field.getAnnotation(Column.class);

        assertEquals(String.class, field.getType());
        assertTrue(column.nullable());
        assertEquals(100, column.length());
    }

    @Test
    void cvsSelectionUsesOwnTableWithOneCurrentRowAndUniqueMapToken() {
        Table table = AuctionCvsStoreSelection.class.getAnnotation(Table.class);

        assertEquals("auction_cvs_store_selections", table.name());
        assertEquals("dbo", table.schema());
        assertEquals(Set.of(
                        "UQ_auction_cvs_store_selections_auction_order_id",
                        "UQ_auction_cvs_store_selections_map_token"),
                Arrays.stream(table.uniqueConstraints())
                        .map(constraint -> constraint.name())
                        .collect(Collectors.toSet()));
    }

    @Test
    void neitherIntegrationCascadesAuctionOrderWrites() throws Exception {
        Field linePayOrder = AuctionLinePayTransaction.class.getDeclaredField("auctionOrder");
        ManyToOne manyToOne = linePayOrder.getAnnotation(ManyToOne.class);
        JoinColumn linePayJoin = linePayOrder.getAnnotation(JoinColumn.class);
        assertEquals(0, manyToOne.cascade().length);
        assertFalse(linePayJoin.insertable());
        assertFalse(linePayJoin.updatable());

        Field cvsOrder = AuctionCvsStoreSelection.class.getDeclaredField("auctionOrder");
        OneToOne oneToOne = cvsOrder.getAnnotation(OneToOne.class);
        JoinColumn cvsJoin = cvsOrder.getAnnotation(JoinColumn.class);
        assertEquals(0, oneToOne.cascade().length);
        assertFalse(cvsJoin.insertable());
        assertFalse(cvsJoin.updatable());
    }
}
