package com.project.sneakermarket.auction.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.project.sneakermarket.auction.dto.AuctionAdminManagementState;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/** 管理列表專用條件；不套用公開商城的 removed/deleted 排除規則。 */
public final class AuctionAdminSpecification {
    private AuctionAdminSpecification() {
    }

    public static Specification<Auction> withCriteria(AuctionAdminSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && criteria.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
            }

            AuctionAdminManagementState state = criteria == null ? null : criteria.getManagementState();
            if (state == AuctionAdminManagementState.VISIBLE) {
                predicates.add(builder.isNull(root.get("removedAt")));
                predicates.add(builder.isNull(root.get("deletedAt")));
                predicates.add(builder.notEqual(root.get("status"), AuctionStatus.REMOVED));
            } else if (state == AuctionAdminManagementState.REMOVED) {
                predicates.add(builder.isNull(root.get("deletedAt")));
                predicates.add(builder.or(
                        builder.isNotNull(root.get("removedAt")),
                        builder.equal(root.get("status"), AuctionStatus.REMOVED)));
            } else if (state == AuctionAdminManagementState.DELETED) {
                predicates.add(builder.isNotNull(root.get("deletedAt")));
            }

            if (criteria != null && StringUtils.hasText(criteria.getKeyword())) {
                String keyword = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                Subquery<Long> products = query.subquery(Long.class);
                Root<AuctionProduct> product = products.from(AuctionProduct.class);
                products.select(product.get("auctionProductId")).where(builder.or(
                        builder.like(builder.lower(product.get("productName")), keyword),
                        builder.like(builder.lower(product.get("model")), keyword)));
                predicates.add(root.get("auctionProductId").in(products));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
