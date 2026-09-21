package com.project.sneakermarket.auction.logistics.ecpay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AuctionEcpayCheckMacValueServiceTests {
    private static final String HASH_KEY = "XBERn1YOvpM9nfZc";
    private static final String HASH_IV = "h1ONHk4P4yqbl5LK";

    private final AuctionEcpayCheckMacValueService service =
            new AuctionEcpayCheckMacValueService();

    @Test
    void matchesEcpayOfficialLogisticsFixedVector() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("MerchantID", "2000933");
        parameters.put("MerchantTradeNo", "A20130312153023");
        parameters.put("MerchantTradeDate", "2013/03/12 15:30:23");
        parameters.put("LogisticsType", "CVS");
        parameters.put("LogisticsSubType", "FAMIC2C");
        parameters.put("GoodsAmount", "1000");
        parameters.put("IsCollection", "N");
        parameters.put("ServerReplyURL", "https://www.ecpay.com.tw/ServerReplyURL");
        parameters.put("SenderName", "寄件者姓名");
        parameters.put("ReceiverName", "收件者姓名");
        parameters.put("ReceiverStoreID", "001779");

        assertEquals("692FD6E2CDB539CCDB7206C76DC239AD",
                service.calculate(parameters, HASH_KEY, HASH_IV));
    }

    @Test
    void matchesGetStoreListStageVectorAndExcludesExistingCheckMacValue() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("PlatformID", "");
        parameters.put("MerchantID", "2000933");
        parameters.put("CvsType", "UNIMART");
        parameters.put("CheckMacValue", "MUST_NOT_BE_HASHED");

        assertEquals("0BDC6B22EB3A9A13FFB9AE4A73D0A9F7",
                service.calculate(parameters, HASH_KEY, HASH_IV));
    }

    @Test
    void credentialsCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(Map.of("MerchantID", "2000933"), " ", HASH_IV));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(Map.of("MerchantID", "2000933"), HASH_KEY, null));
    }
}
