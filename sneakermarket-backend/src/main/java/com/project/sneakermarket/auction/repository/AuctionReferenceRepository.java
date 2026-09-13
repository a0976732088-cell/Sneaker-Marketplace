package com.project.sneakermarket.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionLookupOption;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/** 競標用唯讀整合入口：引用 owner 已存在的 Entity，不複製會員或主資料。 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionReferenceRepository {
    private final EntityManager entityManager;

    public boolean isActiveMember(Long id) {
        return id != null && id > 0 && entityManager.createQuery(
                "select count(m) from Member m where m.memberId = :id and m.deletedAt is null", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }

    /** 同時取得未刪除會員的角色，讓呼叫端可區分失效身分與權限不足。 */
    public Optional<String> findActiveMemberRole(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return entityManager.createQuery(
                "select m.role from Member m where m.memberId = :id and m.deletedAt is null", String.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    public boolean brandExists(Long id) {
        return id != null && id > 0 && entityManager.createQuery(
                "select count(b) from Brand b where b.brandId = :id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }

    public boolean categoryExists(Long id) {
        return id != null && id > 0 && entityManager.createQuery(
                "select count(c) from Category c where c.categoryId = :id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }

    public List<AuctionLookupOption> findBrands() {
        return entityManager.createQuery(
                "select new com.project.sneakermarket.auction.dto.AuctionLookupOption(b.brandId, b.brandName) "
                + "from Brand b order by b.displayOrder, b.brandId", AuctionLookupOption.class).getResultList();
    }

    public List<AuctionLookupOption> findCategories() {
        return entityManager.createQuery(
                "select new com.project.sneakermarket.auction.dto.AuctionLookupOption(c.categoryId, c.categoryName) "
                + "from Category c order by c.displayOrder, c.categoryId", AuctionLookupOption.class).getResultList();
    }
}
