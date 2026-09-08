package com.project.sneakermarket.auction.service;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
/** 沿用會員登入的 Session memberId (Long)，不接受固定會員備援或前端身分。 */
@Component
@RequiredArgsConstructor
public class AuctionSessionMemberResolver {
    private final AuctionReferenceService references;

    public Long resolveMemberId(HttpSession session) {
        Long id = resolveMemberIdOrNull(session);
        if (id == null) throw new AuctionUnauthorizedException();
        return id;
    }

    /** 訪客、失效 Session 或已刪除會員仍可瀏覽，但不具有賣家操作權。 */
    public Long resolveMemberIdOrNull(HttpSession session) {
        if (session == null) return null;
        Object value;
        try {
            value = session.getAttribute("memberId");
        } catch (IllegalStateException ex) {
            return null;
        }
        if (!(value instanceof Long id) || id <= 0) return null;
        return references.isActiveMember(id) ? id : null;
    }
}
