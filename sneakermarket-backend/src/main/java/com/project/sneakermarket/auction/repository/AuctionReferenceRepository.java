package com.project.sneakermarket.auction.repository;
import com.project.sneakermarket.auction.dto.AuctionLookupOption;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
/**
 * 競標用唯讀整合入口：引用 owner 已存在的 Entity，不複製主資料或提供寫入方法。
 * 待 owner 提供正式 Service 契約時，可集中替換此處。
 */
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
