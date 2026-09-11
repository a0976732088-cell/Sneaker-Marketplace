package com.project.sneakermarket.auction.service;
import com.project.sneakermarket.auction.dto.AuctionOptionsResponse;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionReferenceRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    public void validateProductReferences(Long brandId, Long categoryId) {
        if (!references.brandExists(brandId)) throw new BusinessException("品牌不存在，請重新選擇");
        if (!references.categoryExists(categoryId)) throw new BusinessException("分類不存在，請重新選擇");
    }
    public AuctionOptionsResponse getOptions() {
        return new AuctionOptionsResponse(references.findBrands(), references.findCategories());
    }
}
