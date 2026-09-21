package com.project.sneakermarket.auction.logistics.ecpay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCvsStoreSelection;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionCvsStoreSelectionRepository;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCvsStoreSelectionService {
    public static final String CVS_SHIPPING_METHOD = "超商寄送";

    private final AuctionCvsStoreSelectionRepository selections;
    private final AuctionOrderRepository orders;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final AuctionTime time;
    private final AuctionEcpayStoreListClient storeLists;

    public boolean requiresStoreSelection(AuctionOrder order) {
        return CVS_SHIPPING_METHOD.equals(productFor(order).getShippingMethod());
    }

    /**
     * 付款與 LINE Pay confirm 都會再執行此 guard，避免 checkout PATCH 將官方門市地址換掉。
     */
    public void requireSelectedStoreIfNeeded(AuctionOrder order) {
        if (!requiresStoreSelection(order)) return;
        AuctionCvsStoreSelection selection = selections.findByAuctionOrderId(order.getAuctionOrderId())
                .orElseThrow(() -> new AuctionConflictException("請先選擇超商取貨門市"));
        if (!isValidSelection(selection)) {
            throw new AuctionConflictException("請先選擇超商取貨門市");
        }
        if (!selection.getCvsAddress().equals(order.getShippingAddress())) {
            throw new AuctionConflictException("超商取貨地址與已選門市不符，請重新載入訂單");
        }
    }

    @Transactional(readOnly = true)
    public List<AuctionCvsStoreResponse> getStores(
            String orderNumber,
            Long memberId,
            AuctionCvsStoreType storeType) {
        requireMember(memberId);
        if (storeType == null) throw new AuctionConflictException("不支援的超商店別");

        AuctionOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));
        requireBuyer(order, memberId);
        requirePendingBeforeDeadline(order, time.now());
        if (!requiresStoreSelection(order)) {
            throw new AuctionConflictException("此競標訂單不是超商寄送");
        }
        return storeLists.getStores(storeType);
    }

    /**
     * 只接受官方清單中的 storeType + storeId；名稱、地址、電話均取自 provider canonical 資料。
     * 鎖定順序固定為 order 再 selection，與付款流程一致。
     */
    @Transactional
    public AuctionCvsSelectionResponse selectStore(
            String orderNumber,
            Long memberId,
            AuctionCvsSelectionRequest request) {
        requireMember(memberId);
        if (request == null || request.storeType() == null) {
            throw new AuctionConflictException("不支援的超商店別");
        }
        String requestedStoreId = requireExactStoreId(request.storeId());

        AuctionOrder order = orders.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));
        requireBuyer(order, memberId);
        LocalDateTime now = time.now();
        requirePendingBeforeDeadline(order, now);
        if (!requiresStoreSelection(order)) {
            throw new AuctionConflictException("此競標訂單不是超商寄送");
        }

        AuctionCvsStoreResponse officialStore = storeLists.getStores(request.storeType()).stream()
                .filter(store -> requestedStoreId.equals(store.storeId()))
                .findFirst()
                .orElseThrow(() -> new AuctionConflictException(
                        "所選超商門市不存在或已停止服務，請重新查詢"));

        AuctionCvsStoreSelection selection = selections
                .findByAuctionOrderIdForUpdate(order.getAuctionOrderId())
                .orElseGet(() -> newSelection(order.getAuctionOrderId()));
        selection.setStoreType(officialStore.storeType());
        // 保留既有 schema / response 契約；GetStoreList 的 CvsType 仍映射至原 C2C subtype。
        selection.setLogisticsSubType(officialStore.storeType().logisticsSubType());
        selection.setCvsStoreId(officialStore.storeId());
        selection.setCvsStoreName(officialStore.storeName());
        selection.setCvsAddress(officialStore.storeAddress());
        selection.setCvsTelephone(officialStore.storePhone());
        selection.setCvsOutside(null);
        selection.setSelectedAt(now);
        selection.setUpdatedAt(now);
        // 舊電子地圖 callback 欄位保留在 schema，但站內選店不再建立 pending callback。
        selection.setRequestedStoreType(null);
        selection.setRequestedLogisticsSubType(null);
        selections.save(selection);

        order.setShippingAddress(officialStore.storeAddress());
        order.setUpdatedAt(now);
        orders.save(order);
        return toResponse(selection);
    }

    @Transactional(readOnly = true)
    public AuctionCvsSelectionResponse getSelection(String orderNumber, Long memberId) {
        requireMember(memberId);
        AuctionOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));
        requireBuyer(order, memberId);
        return selections.findByAuctionOrderId(order.getAuctionOrderId())
                .filter(this::isValidSelection)
                .map(this::toResponse)
                .orElseGet(AuctionCvsSelectionResponse::notSelected);
    }

    private AuctionCvsStoreSelection newSelection(Long auctionOrderId) {
        AuctionCvsStoreSelection selection = new AuctionCvsStoreSelection();
        selection.setAuctionOrderId(auctionOrderId);
        selection.setMapToken(nextLegacyMapToken());
        return selection;
    }

    private AuctionCvsSelectionResponse toResponse(AuctionCvsStoreSelection selection) {
        return new AuctionCvsSelectionResponse(
                true,
                selection.getStoreType(),
                selection.getLogisticsSubType(),
                selection.getCvsStoreId(),
                selection.getCvsStoreName(),
                selection.getCvsAddress(),
                selection.getCvsTelephone(),
                selection.getCvsOutside(),
                selection.getSelectedAt());
    }

    private AuctionProduct productFor(AuctionOrder order) {
        Auction auction = auctions.findById(order.getAuctionId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此訂單的競標資料"));
        return products.findById(auction.getAuctionProductId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此訂單的商品資料"));
    }

    private boolean isValidSelection(AuctionCvsStoreSelection selection) {
        return selection.getSelectedAt() != null
                && selection.getStoreType() != null
                && selection.getStoreType().logisticsSubType().equals(selection.getLogisticsSubType())
                && hasText(selection.getCvsStoreId())
                && hasText(selection.getCvsStoreName())
                && hasText(selection.getCvsAddress());
    }

    private void requirePendingBeforeDeadline(AuctionOrder order, LocalDateTime now) {
        if (order.getPaymentStatus() != AuctionPaymentStatus.PENDING
                || order.getOrderStatus() != AuctionOrderStatus.PENDING_PAYMENT) {
            throw new AuctionConflictException("此競標訂單目前無法選擇門市");
        }
        if (order.getPaymentDueAt() == null || !now.isBefore(order.getPaymentDueAt())) {
            throw new AuctionConflictException("競標訂單付款期限已過");
        }
    }

    private void requireBuyer(AuctionOrder order, Long memberId) {
        if (!memberId.equals(order.getBuyerId())) {
            throw new ForbiddenException("你沒有權限操作此競標訂單的超商門市");
        }
    }

    private void requireMember(Long memberId) {
        if (memberId == null || memberId <= 0) throw new AuctionUnauthorizedException();
    }

    private String requireExactStoreId(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 10
                || !value.equals(value.trim())) {
            throw new AuctionConflictException("超商門市代碼格式不正確");
        }
        return value;
    }

    private String nextLegacyMapToken() {
        String token;
        do {
            token = "SL" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 18)
                    .toUpperCase();
        } while (selections.existsByMapToken(token));
        return token;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
