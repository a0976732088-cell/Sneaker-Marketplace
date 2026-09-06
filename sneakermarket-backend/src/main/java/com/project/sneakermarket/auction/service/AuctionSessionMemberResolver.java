package com.project.sneakermarket.auction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.common.exception.BusinessException;

import jakarta.servlet.http.HttpSession;

/**
 * 競標模組登入會員 Session 解析器。
 *
 * 【臨時開發版本】
 *
 * 目前會員模組仍在同步開發，尚未確認正式的：
 * 1. Session Attribute Key
 * 2. Session Value 型別
 *
 * 因此競標模組先透過此類別集中處理會員身分。
 *
 * 未來會員模組合板後，只需要修改本類別，
 * 不要讓 AuctionController / AuctionCommandService
 * 直接依賴會員模組目前尚未確定的 Session 結構。
 */
@Component
public class AuctionSessionMemberResolver {

    /**
     * 暫時使用的 Session Attribute Key。
     *
     * 預設為 memberId。
     *
     * 【未來合板可修改位置 1】
     * 如果會員組正式使用：
     *
     * session.setAttribute("loginMemberId", ...)
     *
     * 可直接修改 application-local.properties，
     * 不需要改 Controller。
     */
    private final String memberIdSessionKey;

    /**
     * 開發階段備用會員 ID。
     *
     * 【僅供會員登入功能尚未合板前使用】
     *
     * 如果目前 HttpSession 裡沒有會員資料，
     * 開發環境可暫時使用固定會員 ID，
     * 讓建立競標功能可以繼續開發與測試。
     *
     * 正式整合會員登入後應停用。
     */
    private final Long fallbackMemberId;

    /**
     * 是否允許使用開發用 fallback memberId。
     */
    private final boolean fallbackEnabled;

    public AuctionSessionMemberResolver(
            @Value("${auction.dev.session-member-id-key:memberId}")
            String memberIdSessionKey,

            @Value("${auction.dev.fallback-member-id:1}")
            Long fallbackMemberId,

            @Value("${auction.dev.session-fallback-enabled:false}")
            boolean fallbackEnabled) {

        this.memberIdSessionKey = memberIdSessionKey;
        this.fallbackMemberId = fallbackMemberId;
        this.fallbackEnabled = fallbackEnabled;
    }

    /**
     * 取得目前登入會員 ID。
     *
     * @param session HttpSession
     * @return 會員 ID
     */
    public Long resolveMemberId(HttpSession session) {

        if (session != null) {

            Object sessionValue =
                    session.getAttribute(memberIdSessionKey);

            if (sessionValue != null) {
                return extractMemberId(sessionValue);
            }
        }

        /*
         * 會員登入功能尚未合板前，
         * local 開發環境可暫時使用固定會員 ID。
         */
        if (fallbackEnabled) {
            return fallbackMemberId;
        }

        throw new BusinessException("尚未登入會員");
    }


    /**
     * 嘗試取得目前登入會員 ID。
     *
     * 與 resolveMemberId() 不同：
     * 此方法適合競標詳細頁等「訪客也可以瀏覽」的公開 API。
     *
     * 如果有登入會員資料，回傳 memberId。
     * local 開發環境若啟用 fallback，則回傳 fallback memberId。
     * 若沒有登入且沒有啟用 fallback，回傳 null，
     * 不會因未登入而拋出例外。
     *
     * @param session HttpSession
     * @return 目前會員 ID；未登入時可能為 null
     */
    public Long resolveMemberIdOrNull(HttpSession session) {

        if (session != null) {

            Object sessionValue =
                    session.getAttribute(memberIdSessionKey);

            if (sessionValue != null) {
                return extractMemberId(sessionValue);
            }
        }

        /*
         * local 開發期間仍沿用 fallback，
         * 讓目前功能可以用 memberId = 1 驗收。
         */
        if (fallbackEnabled) {
            return fallbackMemberId;
        }

        // 公開查詢不因訪客未登入而失敗
        return null;
    }

    /**
     * 將 Session 中的會員資料轉成 Long memberId。
     *
     * 【未來合板最主要修改位置 2】
     *
     * 目前暫時支援：
     * Long / Integer / 其他 Number / 數字字串。
     *
     * 如果會員組未來 Session 放的是 DTO，例如：
     *
     * MemberLoginResponse loginMember
     *
     * 則只需要在這個方法補：
     *
     * if (sessionValue instanceof MemberLoginResponse member) {
     *     return member.memberId();
     * }
     *
     * 其他競標程式不需要修改。
     */
    private Long extractMemberId(Object sessionValue) {

        if (sessionValue instanceof Number number) {
            return number.longValue();
        }

        if (sessionValue instanceof String value) {

            try {
                return Long.valueOf(value);

            } catch (NumberFormatException e) {
                throw new BusinessException(
                        "Session 中的會員 ID 格式不正確"
                );
            }
        }

        /*
         * TODO：
         * 會員模組合板後，如果 Session 保存的是
         * Member / DTO 物件，在這裡加入正式轉換。
         */

        throw new BusinessException(
                "目前無法解析 Session 中的會員資料型別："
                        + sessionValue.getClass().getSimpleName()
        );
    }
}