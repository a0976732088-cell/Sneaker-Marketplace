package com.project.sneakermarket.auction.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.project.sneakermarket.auction.dto.AuctionSearchCriteria;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * 競標列表動態查詢條件。
 *
 * 依 AuctionSearchCriteria 組合需要的搜尋與篩選條件。
 */
public final class AuctionSpecification {

    private AuctionSpecification() {
    }

    /**
     * 建立競標列表查詢條件。
     *
     * @param criteria 搜尋與篩選條件
     * @return 競標 Specification
     */
    public static Specification<Auction> withCriteria(
            AuctionSearchCriteria criteria) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();
            List<com.project.sneakermarket.auction.entity.AuctionStatus> statuses =
                    mergeCriteriaValues(criteria.getStatus(), criteria.getStatuses());
            List<Long> brandIds =
                    mergeCriteriaValues(criteria.getBrandId(), criteria.getBrandIds());
            List<Long> categoryIds =
                    mergeCriteriaValues(criteria.getCategoryId(), criteria.getCategoryIds());
            List<String> shoeSizes =
                    mergeTextCriteriaValues(criteria.getShoeSize(), criteria.getShoeSizes());
            List<com.project.sneakermarket.auction.entity.AuctionCondition> conditions =
                    mergeCriteriaValues(criteria.getCondition(), criteria.getConditions());

            // 公開商城一律排除未啟用、已下架、已 Soft Delete 與舊版僅有 REMOVED status 的資料。
            predicates.add(criteriaBuilder.isNull(root.get("removedAt")));
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            predicates.add(criteriaBuilder.notEqual(
                    root.get("status"),
                    com.project.sneakermarket.auction.entity.AuctionStatus.REMOVED));
            predicates.add(criteriaBuilder.notEqual(
                    root.get("status"),
                    com.project.sneakermarket.auction.entity.AuctionStatus.DRAFT));

            // 競標狀態
            if (!statuses.isEmpty()) {
                predicates.add(
                        root.get("status").in(statuses)
                );
            }

            // 最低目前價格
            if (criteria.getMinPrice() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("currentPrice"),
                                criteria.getMinPrice()
                        )
                );
            }

            // 最高目前價格
            if (criteria.getMaxPrice() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("currentPrice"),
                                criteria.getMaxPrice()
                        )
                );
            }

            // 判斷是否有競標商品相關的搜尋條件
            boolean hasProductCriteria =
                    StringUtils.hasText(criteria.getKeyword())
                            || !brandIds.isEmpty()
                            || !categoryIds.isEmpty()
                            || !shoeSizes.isEmpty()
                            || !conditions.isEmpty();

            if (hasProductCriteria) {

                /*
                 * Auction 與 AuctionProduct 目前沒有使用 @ManyToOne 關聯，
                 * 因此透過 auctionProductId 子查詢取得符合條件的商品。
                 */
                Subquery<Long> productSubquery =
                        query.subquery(Long.class);

                Root<AuctionProduct> productRoot =
                        productSubquery.from(AuctionProduct.class);

                List<Predicate> productPredicates =
                        new ArrayList<>();

                // 商品名稱或型號關鍵字
                if (StringUtils.hasText(criteria.getKeyword())) {

                    String keyword =
                            "%" + criteria.getKeyword()
                                    .trim()
                                    .toLowerCase() + "%";

                    productPredicates.add(
                            criteriaBuilder.or(
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    productRoot.get("productName")
                                            ),
                                            keyword
                                    ),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    productRoot.get("model")
                                            ),
                                            keyword
                                    )
                            )
                    );
                }

                // 品牌
                if (!brandIds.isEmpty()) {
                    productPredicates.add(
                            productRoot.get("brandId").in(brandIds)
                    );
                }

                // 分類
                if (!categoryIds.isEmpty()) {
                    productPredicates.add(
                            productRoot.get("categoryId").in(categoryIds)
                    );
                }

                // 球鞋尺寸
                if (!shoeSizes.isEmpty()) {
                    productPredicates.add(
                            criteriaBuilder.lower(
                                    productRoot.get("size")
                            ).in(shoeSizes)
                    );
                }

                // 商品狀況
                if (!conditions.isEmpty()) {
                    productPredicates.add(
                            productRoot.get("condition").in(conditions)
                    );
                }

                productSubquery
                        .select(productRoot.get("auctionProductId"))
                        .where(productPredicates.toArray(Predicate[]::new));

                predicates.add(
                        root.get("auctionProductId")
                                .in(productSubquery)
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    /**
     * 合併舊版單值條件與新版多值條件，忽略 null 並維持首次出現順序。
     */
    private static <T> List<T> mergeCriteriaValues(
            T legacyValue,
            List<T> values) {

        LinkedHashSet<T> mergedValues = new LinkedHashSet<>();

        if (legacyValue != null) {
            mergedValues.add(legacyValue);
        }

        if (values != null) {
            values.stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(mergedValues::add);
        }

        return List.copyOf(mergedValues);
    }

    /**
     * 文字條件在進入 SQL 前統一 trim/lower，避免空白與大小寫造成重複條件。
     */
    private static List<String> mergeTextCriteriaValues(
            String legacyValue,
            List<String> values) {

        LinkedHashSet<String> mergedValues = new LinkedHashSet<>();
        addNormalizedText(mergedValues, legacyValue);

        if (values != null) {
            values.forEach(value -> addNormalizedText(mergedValues, value));
        }

        return List.copyOf(mergedValues);
    }

    private static void addNormalizedText(
            LinkedHashSet<String> values,
            String value) {

        if (StringUtils.hasText(value)) {
            values.add(value.trim().toLowerCase(Locale.ROOT));
        }
    }
}
