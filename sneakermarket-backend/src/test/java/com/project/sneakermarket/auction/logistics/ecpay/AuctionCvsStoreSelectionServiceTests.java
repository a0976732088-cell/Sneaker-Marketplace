package com.project.sneakermarket.auction.logistics.ecpay;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCvsStoreSelection;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionIntegrationException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionCvsStoreSelectionRepository;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.common.exception.ForbiddenException;

class AuctionCvsStoreSelectionServiceTests {
    private static final String ORDER_NUMBER = "AO-CVS-001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 21, 18, 0);

    private AuctionCvsStoreSelectionRepository selections;
    private AuctionOrderRepository orders;
    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private AuctionTime time;
    private AuctionEcpayStoreListClient storeLists;
    private AuctionCvsStoreSelectionService service;
    private AuctionOrder order;
    private AuctionProduct product;

    @BeforeEach
    void setup() {
        selections = mock(AuctionCvsStoreSelectionRepository.class);
        orders = mock(AuctionOrderRepository.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        time = mock(AuctionTime.class);
        storeLists = mock(AuctionEcpayStoreListClient.class);
        service = new AuctionCvsStoreSelectionService(
                selections, orders, auctions, products, time, storeLists);

        order = pendingOrder();
        Auction auction = new Auction();
        auction.setAuctionId(7L);
        auction.setAuctionProductId(17L);
        product = new AuctionProduct();
        product.setAuctionProductId(17L);
        product.setShippingMethod(AuctionCvsStoreSelectionService.CVS_SHIPPING_METHOD);

        when(orders.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(orders.findByOrderNumberForUpdate(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(auctions.findById(7L)).thenReturn(Optional.of(auction));
        when(products.findById(17L)).thenReturn(Optional.of(product));
        when(time.now()).thenReturn(NOW);
    }

    @Test
    void buyerCanListOfficialStoresForPendingCvsOrder() {
        List<AuctionCvsStoreResponse> official = List.of(officialStore(
                AuctionCvsStoreType.UNIMART, "001234", "官方門市", "官方地址"));
        when(storeLists.getStores(AuctionCvsStoreType.UNIMART)).thenReturn(official);

        assertSame(official, service.getStores(
                ORDER_NUMBER, 10L, AuctionCvsStoreType.UNIMART));

        verify(storeLists).getStores(AuctionCvsStoreType.UNIMART);
    }

    @Test
    void anonymousAndNonBuyerCannotListStores() {
        assertThrows(AuctionUnauthorizedException.class,
                () -> service.getStores(ORDER_NUMBER, null, AuctionCvsStoreType.UNIMART));
        assertThrows(ForbiddenException.class,
                () -> service.getStores(ORDER_NUMBER, 99L, AuctionCvsStoreType.UNIMART));

        verifyNoInteractions(storeLists);
    }

    @Test
    void nonCvsOrderCannotListStores() {
        product.setShippingMethod("宅配");

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.getStores(ORDER_NUMBER, 10L, AuctionCvsStoreType.UNIMART));

        assertEquals("此競標訂單不是超商寄送", error.getMessage());
        verifyNoInteractions(storeLists);
    }

    @Test
    void expiredOrAlreadyPaidOrderCannotListStores() {
        order.setPaymentDueAt(NOW);
        assertThrows(AuctionConflictException.class,
                () -> service.getStores(ORDER_NUMBER, 10L, AuctionCvsStoreType.FAMI));

        order.setPaymentDueAt(NOW.plusHours(1));
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.PAID);
        assertThrows(AuctionConflictException.class,
                () -> service.getStores(ORDER_NUMBER, 10L, AuctionCvsStoreType.FAMI));

        verifyNoInteractions(storeLists);
    }

    @Test
    void selectionUsesCanonicalProviderSnapshotAndDoesNotChangeStatuses() {
        AuctionCvsStoreResponse official = officialStore(
                AuctionCvsStoreType.UNIMART,
                "001234",
                "官方測試門市",
                "臺北市中正區官方路 1 號");
        when(storeLists.getStores(AuctionCvsStoreType.UNIMART)).thenReturn(List.of(official));
        when(selections.findByAuctionOrderIdForUpdate(8L)).thenReturn(Optional.empty());
        when(selections.existsByMapToken(anyString())).thenReturn(false);

        AuctionCvsSelectionResponse response = service.selectStore(
                ORDER_NUMBER,
                10L,
                new AuctionCvsSelectionRequest(AuctionCvsStoreType.UNIMART, "001234"));

        ArgumentCaptor<AuctionCvsStoreSelection> saved =
                ArgumentCaptor.forClass(AuctionCvsStoreSelection.class);
        verify(selections).save(saved.capture());
        AuctionCvsStoreSelection selection = saved.getValue();
        assertEquals(8L, selection.getAuctionOrderId());
        assertTrue(selection.getMapToken().matches("SL[A-F0-9]{18}"));
        assertEquals(AuctionCvsStoreType.UNIMART, selection.getStoreType());
        assertEquals("UNIMARTC2C", selection.getLogisticsSubType());
        assertEquals("001234", selection.getCvsStoreId());
        assertEquals("官方測試門市", selection.getCvsStoreName());
        assertEquals("臺北市中正區官方路 1 號", selection.getCvsAddress());
        assertEquals("02-12345678", selection.getCvsTelephone());
        assertNull(selection.getCvsOutside());
        assertEquals(NOW, selection.getSelectedAt());
        assertEquals(NOW, selection.getUpdatedAt());

        assertTrue(response.selected());
        assertEquals("官方測試門市", response.cvsStoreName());
        assertEquals("臺北市中正區官方路 1 號", order.getShippingAddress());
        assertEquals(NOW, order.getUpdatedAt());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        verify(orders).save(order);
    }

    @Test
    void forgedOrStaleStoreIdCannotPersistFrontendData() {
        when(storeLists.getStores(AuctionCvsStoreType.FAMI)).thenReturn(List.of(
                officialStore(AuctionCvsStoreType.FAMI, "009999", "官方門市", "官方地址")));
        String originalAddress = order.getShippingAddress();

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.FAMI, "FAKE01")));

        assertEquals("所選超商門市不存在或已停止服務，請重新查詢", error.getMessage());
        assertEquals(originalAddress, order.getShippingAddress());
        verify(selections, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    void reselectionOverwritesExistingSnapshotOnlyAfterOfficialLookupSucceeds() {
        AuctionCvsStoreSelection existing = selectedStore(
                AuctionCvsStoreType.FAMI, "001111", "原門市", "原門市地址");
        when(selections.findByAuctionOrderIdForUpdate(8L)).thenReturn(Optional.of(existing));
        when(storeLists.getStores(AuctionCvsStoreType.HILIFE)).thenReturn(List.of(
                officialStore(AuctionCvsStoreType.HILIFE, "002222", "新門市", "新門市地址")));

        service.selectStore(
                ORDER_NUMBER,
                10L,
                new AuctionCvsSelectionRequest(AuctionCvsStoreType.HILIFE, "002222"));

        assertEquals("SL123456789012345678", existing.getMapToken());
        assertEquals(AuctionCvsStoreType.HILIFE, existing.getStoreType());
        assertEquals("HILIFEC2C", existing.getLogisticsSubType());
        assertEquals("002222", existing.getCvsStoreId());
        assertEquals("新門市", existing.getCvsStoreName());
        assertEquals("新門市地址", existing.getCvsAddress());
        assertEquals("新門市地址", order.getShippingAddress());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        verify(selections).save(existing);
    }

    @Test
    void reselectionProviderFailureKeepsExistingSelectionAndOrderAddressUntouched() {
        AuctionCvsStoreSelection existing = selectedStore(
                AuctionCvsStoreType.FAMI, "001111", "原門市", "原門市地址");
        existing.setCvsOutside("0");
        SelectionSnapshot before = snapshot(existing);
        String originalAddress = order.getShippingAddress();
        LocalDateTime originalUpdatedAt = order.getUpdatedAt();
        when(selections.findByAuctionOrderIdForUpdate(8L)).thenReturn(Optional.of(existing));
        when(storeLists.getStores(AuctionCvsStoreType.HILIFE))
                .thenThrow(AuctionIntegrationException.provider("ECPay 查詢失敗"));

        assertThrows(AuctionIntegrationException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.HILIFE, "002222")));

        assertEquals(before, snapshot(existing));
        assertEquals(originalAddress, order.getShippingAddress());
        assertEquals(originalUpdatedAt, order.getUpdatedAt());
        verify(selections, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    void reselectionMissingStoreIdKeepsExistingSelectionAndOrderAddressUntouched() {
        AuctionCvsStoreSelection existing = selectedStore(
                AuctionCvsStoreType.UNIMART, "001111", "原門市", "原門市地址");
        existing.setCvsOutside("0");
        SelectionSnapshot before = snapshot(existing);
        String originalAddress = order.getShippingAddress();
        LocalDateTime originalUpdatedAt = order.getUpdatedAt();
        when(selections.findByAuctionOrderIdForUpdate(8L)).thenReturn(Optional.of(existing));
        when(storeLists.getStores(AuctionCvsStoreType.HILIFE)).thenReturn(List.of(
                officialStore(AuctionCvsStoreType.HILIFE, "009999", "其他門市", "其他地址")));

        assertThrows(AuctionConflictException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.HILIFE, "002222")));

        assertEquals(before, snapshot(existing));
        assertEquals(originalAddress, order.getShippingAddress());
        assertEquals(originalUpdatedAt, order.getUpdatedAt());
        verify(selections, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    void expiredOrAlreadyPaidOrderCannotSelectOrOverwriteStore() {
        order.setPaymentDueAt(NOW);
        assertThrows(AuctionConflictException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.UNIMART, "001234")));

        order.setPaymentDueAt(NOW.plusHours(1));
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.PAID);
        assertThrows(AuctionConflictException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.UNIMART, "001234")));

        verifyNoInteractions(storeLists);
        verify(selections, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    void storeIdMustBeExactAndCannotBeTrimmedIntoAnotherProviderId() {
        assertThrows(AuctionConflictException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.UNIMART, " 001234")));
        assertThrows(AuctionConflictException.class,
                () -> service.selectStore(
                        ORDER_NUMBER,
                        10L,
                        new AuctionCvsSelectionRequest(AuctionCvsStoreType.UNIMART, "")));

        verifyNoInteractions(storeLists);
    }

    @Test
    void getSelectionReturnsFalseWhenNoCanonicalSelectionExists() {
        when(selections.findByAuctionOrderId(8L)).thenReturn(Optional.empty());

        AuctionCvsSelectionResponse response = service.getSelection(ORDER_NUMBER, 10L);

        assertFalse(response.selected());
        assertNull(response.storeType());
        assertNull(response.cvsStoreId());
    }

    @Test
    void paymentGuardRequiresCanonicalSelectionAndMatchingOrderAddress() {
        when(selections.findByAuctionOrderId(8L)).thenReturn(Optional.empty());
        assertThrows(AuctionConflictException.class,
                () -> service.requireSelectedStoreIfNeeded(order));

        AuctionCvsStoreSelection selected = selectedStore(
                AuctionCvsStoreType.UNIMART,
                "001234",
                "官方門市",
                "官方地址");
        when(selections.findByAuctionOrderId(8L)).thenReturn(Optional.of(selected));
        assertThrows(AuctionConflictException.class,
                () -> service.requireSelectedStoreIfNeeded(order));

        order.setShippingAddress("官方地址");
        assertDoesNotThrow(() -> service.requireSelectedStoreIfNeeded(order));

        product.setShippingMethod("宅配");
        when(selections.findByAuctionOrderId(8L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.requireSelectedStoreIfNeeded(order));
    }

    private AuctionOrder pendingOrder() {
        AuctionOrder value = new AuctionOrder();
        value.setAuctionOrderId(8L);
        value.setOrderNumber(ORDER_NUMBER);
        value.setAuctionId(7L);
        value.setBuyerId(10L);
        value.setSellerId(11L);
        value.setPaymentStatus(AuctionPaymentStatus.PENDING);
        value.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        value.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        value.setPaymentDueAt(NOW.plusHours(1));
        value.setShippingAddress("原收件地址");
        value.setUpdatedAt(NOW.minusHours(1));
        return value;
    }

    private AuctionCvsStoreResponse officialStore(
            AuctionCvsStoreType type,
            String id,
            String name,
            String address) {
        return new AuctionCvsStoreResponse(type, id, name, address, "02-12345678");
    }

    private AuctionCvsStoreSelection selectedStore(
            AuctionCvsStoreType type,
            String id,
            String name,
            String address) {
        AuctionCvsStoreSelection value = new AuctionCvsStoreSelection();
        value.setId(2L);
        value.setAuctionOrderId(8L);
        value.setMapToken("SL123456789012345678");
        value.setStoreType(type);
        value.setLogisticsSubType(type.logisticsSubType());
        value.setCvsStoreId(id);
        value.setCvsStoreName(name);
        value.setCvsAddress(address);
        value.setCvsTelephone("02-12345678");
        value.setSelectedAt(NOW.minusMinutes(5));
        value.setUpdatedAt(NOW.minusMinutes(5));
        return value;
    }

    private SelectionSnapshot snapshot(AuctionCvsStoreSelection value) {
        return new SelectionSnapshot(
                value.getId(),
                value.getAuctionOrderId(),
                value.getMapToken(),
                value.getStoreType(),
                value.getLogisticsSubType(),
                value.getRequestedStoreType(),
                value.getRequestedLogisticsSubType(),
                value.getCvsStoreId(),
                value.getCvsStoreName(),
                value.getCvsAddress(),
                value.getCvsTelephone(),
                value.getCvsOutside(),
                value.getSelectedAt(),
                value.getUpdatedAt());
    }

    private record SelectionSnapshot(
            Long id,
            Long auctionOrderId,
            String mapToken,
            AuctionCvsStoreType storeType,
            String logisticsSubType,
            AuctionCvsStoreType requestedStoreType,
            String requestedLogisticsSubType,
            String cvsStoreId,
            String cvsStoreName,
            String cvsAddress,
            String cvsTelephone,
            String cvsOutside,
            LocalDateTime selectedAt,
            LocalDateTime updatedAt) {
    }
}
