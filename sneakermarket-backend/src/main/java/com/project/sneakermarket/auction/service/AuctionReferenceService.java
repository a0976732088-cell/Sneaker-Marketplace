package com.project.sneakermarket.auction.service;

import org.springframework.stereotype.Service;

import com.project.sneakermarket.auction.dto.AuctionOptionsResponse;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionReferenceRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionReferenceService {
    private final AuctionReferenceRepository references;

    public boolean isActiveMember(Long id) {
        return references.isActiveMember(id);
    }

    public void requireActiveMember(Long id) {
        if (!isActiveMember(id)) throw new AuctionUnauthorizedException();
    }

    /** Admin API 的後端權限邊界：會員不存在或已刪除為 401，角色不符為 403。 */
    public void requireAdminMember(Long id) {
        String role = references.findActiveMemberRole(id)
                .orElseThrow(AuctionUnauthorizedException::new);
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ForbiddenException("需要管理員權限");
        }
    }

    public void validateProductReferences(Long brandId, Long categoryId) {
        if (!references.brandExists(brandId)) throw new BusinessException("品牌不存在，請重新選擇");
        if (!references.categoryExists(categoryId)) throw new BusinessException("分類不存在，請重新選擇");
    }

    public AuctionOptionsResponse getOptions() {
        return new AuctionOptionsResponse(references.findBrands(), references.findCategories());
    }
}
