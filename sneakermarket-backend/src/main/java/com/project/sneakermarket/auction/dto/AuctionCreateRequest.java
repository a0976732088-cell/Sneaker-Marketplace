package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionCondition;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 建立競標 Request DTO。
 *
 * 接收使用者建立競標時輸入的商品、交易與競標資訊。
 *
 * sellerId 不由前端傳入；
 * 正式環境未來應由登入會員身分取得，
 * 目前開發階段會由後端暫時提供固定測試會員 ID。
 *
 * 圖片也不放在此 DTO，
 * 後續會以 MultipartFile 另外接收。
 */
public record AuctionCreateRequest(

        // 商品品牌 ID
        @NotNull(message = "品牌不可為空")
        @Positive(message = "品牌 ID 必須大於 0")
        Long brandId,

        // 商品分類 ID
        @NotNull(message = "分類不可為空")
        @Positive(message = "分類 ID 必須大於 0")
        Long categoryId,

        // 商品名稱
        @NotBlank(message = "商品名稱不可為空")
        @Size(max = 150, message = "商品名稱不可超過 150 個字元")
        String productName,

        // 商品型號／系列，可不填
        @Size(max = 100, message = "商品型號不可超過 100 個字元")
        String model,

        // 球鞋尺寸，例如 US 9
        @NotBlank(message = "球鞋尺寸不可為空")
        @Size(max = 20, message = "球鞋尺寸不可超過 20 個字元")
        String size,

        // 商品狀況：NEW、LIKE_NEW、USED
        @NotNull(message = "商品狀況不可為空")
        AuctionCondition condition,

        // 商品介紹
        @NotBlank(message = "商品介紹不可為空")
        String description,

        // 瑕疵說明，可不填
        @Size(max = 1000, message = "瑕疵說明不可超過 1000 個字元")
        String defectNote,

        // 真偽／來源說明，可不填
        @Size(max = 1000, message = "真偽說明不可超過 1000 個字元")
        String authenticityNote,

        // 商品所在地，可不填
        @Size(max = 100, message = "商品所在地不可超過 100 個字元")
        String location,

        // 運送方式
        @NotBlank(message = "運送方式不可為空")
        @Size(max = 50, message = "運送方式不可超過 50 個字元")
        String shippingMethod,

        // 運費，可為 0
        @NotNull(message = "運費不可為空")
        @DecimalMin(value = "0.00", message = "運費不可小於 0")
        @Digits(integer = 10, fraction = 2, message = "運費格式不正確")
        BigDecimal shippingFee,

        // 付款方式
        @NotBlank(message = "付款方式不可為空")
        @Size(max = 50, message = "付款方式不可超過 50 個字元")
        String paymentMethod,

        // 是否允許面交
        @NotNull(message = "請指定是否允許面交")
        Boolean allowMeetup,

        // 是否允許退貨
        @NotNull(message = "請指定是否允許退貨")
        Boolean allowReturn,

        // 保固／售後說明，可不填
        @Size(max = 500, message = "保固或售後說明不可超過 500 個字元")
        String warrantyNote,

        // 起標價格
        @NotNull(message = "起標價格不可為空")
        @Positive(message = "起標價格必須大於 0")
        @Digits(integer = 10, fraction = 2, message = "起標價格格式不正確")
        BigDecimal startPrice,

        // 每次最低加價金額
        @NotNull(message = "最低加價金額不可為空")
        @Positive(message = "最低加價金額必須大於 0")
        @Digits(integer = 10, fraction = 2, message = "最低加價金額格式不正確")
        BigDecimal minIncrement,

        // 競標開始時間
        @NotNull(message = "競標開始時間不可為空")
        LocalDateTime startAt,

        // 競標結束時間
        @NotNull(message = "競標結束時間不可為空")
        LocalDateTime endAt

) {
}