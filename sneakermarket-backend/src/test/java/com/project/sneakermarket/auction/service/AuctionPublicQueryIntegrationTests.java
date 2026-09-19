package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionSummaryResponse;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionSpecification;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 JPA/SQL 驗證公開 Auction 查詢邊界；每個案例只建立自己的 fixture 並回滾。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional
class AuctionPublicQueryIntegrationTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 12, 0);

    @Autowired private AuctionQueryService publicQueries;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private MemberRepository members;

    private Long sellerId;
    private String fixtureToken;

    @BeforeEach
    void setup() {
        fixtureToken = "public-draft-" + UUID.randomUUID();

        Member seller = new Member();
        seller.setAccount(fixtureToken);
        seller.setPasswordHash("integration-fixture-not-a-login");
        seller.setName("Public query fixture seller");
        seller.setEmail(fixtureToken + "@example.invalid");
        seller.setPhone("0900000015");
        seller.setAddress("台北市測試路 15 號");
        seller.setRole("ROLE_MEMBER");
        sellerId = members.saveAndFlush(seller).getMemberId();
    }

    @Test
    void draftIsExcludedByRepositorySpecificationAndEveryPublicQueryServiceEntryPoint() {
        Long draftId = fixture(AuctionStatus.DRAFT, null, null);

        assertTrue(auctions.findPublicById(draftId).isEmpty());

        AuctionSearchCriteria criteria = criteriaForFixture();
        assertTrue(auctions.findAll(AuctionSpecification.withCriteria(criteria)).isEmpty());
        assertFalse(publicIds(publicQueries.getAllAuctions()).contains(draftId));
        assertTrue(publicQueries.searchAuctions(criteria, PageRequest.of(0, 20)).items().isEmpty());

        criteria.setStatus(AuctionStatus.DRAFT);
        assertTrue(publicQueries.searchAuctions(criteria, PageRequest.of(0, 20)).items().isEmpty());

        assertThrows(ResourceNotFoundException.class,
                () -> publicQueries.getAuctionDetail(draftId, null));
        assertThrows(ResourceNotFoundException.class,
                () -> publicQueries.getAuctionStatus(draftId));
        assertThrows(ResourceNotFoundException.class,
                () -> publicQueries.getBidHistory(draftId));
    }

    @Test
    void publicStatusesRemainVisibleWhileRemovedDeletedAndLegacyRemovedStayHidden() {
        Map<Long, AuctionStatus> visible = new LinkedHashMap<>();
        for (AuctionStatus status : EnumSet.of(
                AuctionStatus.SCHEDULED,
                AuctionStatus.ACTIVE,
                AuctionStatus.SOLD,
                AuctionStatus.UNSOLD,
                AuctionStatus.CANCELLED)) {
            visible.put(fixture(status, null, null), status);
        }

        Long removedAtId = fixture(AuctionStatus.ACTIVE, NOW.minusMinutes(5), null);
        Long deletedAtId = fixture(AuctionStatus.ACTIVE, null, NOW.minusMinutes(5));
        Long removedStatusId = fixture(AuctionStatus.REMOVED, null, null);

        AuctionSearchCriteria criteria = criteriaForFixture();
        Set<Long> specificationIds = auctions.findAll(AuctionSpecification.withCriteria(criteria))
                .stream().map(Auction::getAuctionId).collect(Collectors.toSet());
        Set<Long> serviceSearchIds = publicIds(
                publicQueries.searchAuctions(criteria, PageRequest.of(0, 20)).items());
        Set<Long> allPublicIds = publicIds(publicQueries.getAllAuctions());

        assertEquals(visible.keySet(), specificationIds);
        assertEquals(visible.keySet(), serviceSearchIds);
        assertTrue(allPublicIds.containsAll(visible.keySet()));

        for (Long visibleId : visible.keySet()) {
            assertTrue(auctions.findPublicById(visibleId).isPresent());
        }
        for (Long hiddenId : Set.of(removedAtId, deletedAtId, removedStatusId)) {
            assertTrue(auctions.findPublicById(hiddenId).isEmpty());
            assertFalse(specificationIds.contains(hiddenId));
            assertFalse(serviceSearchIds.contains(hiddenId));
            assertFalse(allPublicIds.contains(hiddenId));
        }
    }

    @Test
    void pluralCriteriaUseOrWithinEachDimensionAndAndAcrossDimensions() {
        Long firstMatch = fixture(
                AuctionStatus.ACTIVE, null, null,
                11L, 21L, "US 8", AuctionCondition.NEW);
        Long secondMatch = fixture(
                AuctionStatus.SCHEDULED, null, null,
                12L, 22L, "US 9", AuctionCondition.USED);

        // 每一筆排除資料只違反其中一個維度，藉此保護跨維度必須使用 AND。
        fixture(AuctionStatus.ACTIVE, null, null,
                99L, 21L, "US 8", AuctionCondition.NEW);
        fixture(AuctionStatus.ACTIVE, null, null,
                11L, 99L, "US 8", AuctionCondition.NEW);
        fixture(AuctionStatus.ACTIVE, null, null,
                11L, 21L, "US 11", AuctionCondition.NEW);
        fixture(AuctionStatus.ACTIVE, null, null,
                11L, 21L, "US 8", AuctionCondition.LIKE_NEW);
        fixture(AuctionStatus.CANCELLED, null, null,
                11L, 21L, "US 8", AuctionCondition.NEW);

        AuctionSearchCriteria criteria = criteriaForFixture();
        criteria.setBrandIds(List.of(11L, 12L, 11L));
        criteria.setCategoryIds(List.of(21L, 22L));
        criteria.setShoeSizes(List.of(" us 8 ", "US 9", "us 9"));
        criteria.setConditions(List.of(AuctionCondition.NEW, AuctionCondition.USED));
        criteria.setStatuses(List.of(AuctionStatus.ACTIVE, AuctionStatus.SCHEDULED));

        Set<Long> actualIds = publicIds(
                publicQueries.searchAuctions(criteria, PageRequest.of(0, 20)).items());

        assertEquals(Set.of(firstMatch, secondMatch), actualIds);
    }

    @Test
    void legacyScalarCriteriaRemainEffectiveAndMergeWithPluralCriteria() {
        Long legacyMatch = fixture(
                AuctionStatus.SOLD, null, null,
                31L, 41L, "US 10", AuctionCondition.LIKE_NEW);
        Long pluralMatch = fixture(
                AuctionStatus.ACTIVE, null, null,
                32L, 42L, "US 9", AuctionCondition.NEW);
        fixture(AuctionStatus.ACTIVE, null, null,
                99L, 42L, "US 9", AuctionCondition.NEW);

        AuctionSearchCriteria criteria = criteriaForFixture();
        criteria.setBrandId(31L);
        criteria.setBrandIds(List.of(32L));
        criteria.setCategoryId(41L);
        criteria.setCategoryIds(List.of(42L));
        criteria.setShoeSize(" us 10 ");
        criteria.setShoeSizes(List.of("US 9"));
        criteria.setCondition(AuctionCondition.LIKE_NEW);
        criteria.setConditions(List.of(AuctionCondition.NEW));
        criteria.setStatus(AuctionStatus.SOLD);
        criteria.setStatuses(List.of(AuctionStatus.ACTIVE));

        Set<Long> actualIds = publicIds(
                publicQueries.searchAuctions(criteria, PageRequest.of(0, 20)).items());

        assertEquals(Set.of(legacyMatch, pluralMatch), actualIds);
    }

    private AuctionSearchCriteria criteriaForFixture() {
        AuctionSearchCriteria criteria = new AuctionSearchCriteria();
        criteria.setKeyword(fixtureToken);
        return criteria;
    }

    private Long fixture(
            AuctionStatus status,
            LocalDateTime removedAt,
            LocalDateTime deletedAt) {

        return fixture(
                status,
                removedAt,
                deletedAt,
                1L,
                1L,
                "US 9",
                AuctionCondition.NEW
        );
    }

    private Long fixture(
            AuctionStatus status,
            LocalDateTime removedAt,
            LocalDateTime deletedAt,
            Long brandId,
            Long categoryId,
            String shoeSize,
            AuctionCondition condition) {

        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(brandId);
        product.setCategoryId(categoryId);
        product.setProductName(fixtureToken + " " + status + " " + UUID.randomUUID());
        product.setModel("PUBLIC-DRAFT-BOUNDARY");
        product.setSize(shoeSize);
        product.setCondition(condition);
        product.setDescription("Public Auction DRAFT boundary integration fixture");
        product.setLocation("台北市");
        product.setShippingMethod("宅配");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(NOW.minusHours(2));
        product.setUpdatedAt(NOW.minusHours(2));
        product = products.saveAndFlush(product);

        Auction auction = new Auction();
        auction.setAuctionProductId(product.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW.plusHours(1));
        auction.setStatus(status);
        auction.setHighestBidderId(null);
        auction.setBidCount(0);
        auction.setCreatedAt(NOW.minusHours(2));
        auction.setUpdatedAt(NOW.minusHours(2));
        auction.setRemovedAt(removedAt);
        auction.setDeletedAt(deletedAt);
        if (removedAt != null) {
            auction.setRemovedByMemberId(sellerId);
            auction.setRemoveReason("公開隱藏回歸測試");
            auction.setRemovedFromStatus(status);
        }
        if (deletedAt != null) {
            auction.setDeletedByMemberId(sellerId);
            auction.setDeleteReason("公開隱藏回歸測試");
        }
        return auctions.saveAndFlush(auction).getAuctionId();
    }

    private Set<Long> publicIds(java.util.Collection<AuctionSummaryResponse> rows) {
        return rows.stream()
                .map(AuctionSummaryResponse::auctionId)
                .collect(Collectors.toSet());
    }
}
