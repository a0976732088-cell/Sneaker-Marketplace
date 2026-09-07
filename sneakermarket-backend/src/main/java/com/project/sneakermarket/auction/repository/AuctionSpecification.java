package com.project.sneakermarket.auction.repository;

import java.util.ArrayList;
import java.util.List;

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

            // 競標狀態
            if (criteria.getStatus() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                criteria.getStatus()
                        )
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
                            || criteria.getBrandId() != null
                            || criteria.getCategoryId() != null
                            || StringUtils.hasText(criteria.getShoeSize())
                            || criteria.getCondition() != null;

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
                if (criteria.getBrandId() != null) {
                    productPredicates.add(
                            criteriaBuilder.equal(
                                    productRoot.get("brandId"),
                                    criteria.getBrandId()
                            )
                    );
                }

                // 分類
                if (criteria.getCategoryId() != null) {
                    productPredicates.add(
                            criteriaBuilder.equal(
                                    productRoot.get("categoryId"),
                                    criteria.getCategoryId()
                            )
                    );
                }

                // 球鞋尺寸
                if (StringUtils.hasText(criteria.getShoeSize())) {
                    productPredicates.add(
                            criteriaBuilder.equal(
                                    criteriaBuilder.lower(
                                            productRoot.get("size")
                                    ),
                                    criteria.getShoeSize()
                                            .trim()
                                            .toLowerCase()
                            )
                    );
                }

                // 商品狀況
                if (criteria.getCondition() != null) {
                    productPredicates.add(
                            criteriaBuilder.equal(
                                    productRoot.get("condition"),
                                    criteria.getCondition()
                            )
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
}