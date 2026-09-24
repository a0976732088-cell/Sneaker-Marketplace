package com.project.sneakermarket.commerce.linepay;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商城 LINE Pay 的連線設定。
 * 三個值都從 IntelliJ Run Configuration 的環境變數讀進來，金鑰不寫進程式碼、不進 Git。
 */
@Getter
@Component
public class CommerceLinePayProperties {

    @Value("${commerce.linepay.channel-id}")
    private String channelId;

    @Value("${commerce.linepay.channel-secret}")
    private String channelSecret;

    // 沒設定時預設走 Sandbox 測試環境
    @Value("${commerce.linepay.api-base-url}")
    private String apiBaseUrl;

    // LINE Pay 付款完成或取消後，要把使用者導回的前端網址
    @Value("${app.frontend-base-url}")
    private String frontendUrl;

    /** 金鑰兩個都有填才算可以用 */
    public boolean isReady() {
        return StringUtils.hasText(channelId) && StringUtils.hasText(channelSecret);
    }
}