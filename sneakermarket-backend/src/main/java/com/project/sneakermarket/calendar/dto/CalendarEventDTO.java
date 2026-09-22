package com.project.sneakermarket.calendar.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class CalendarEventDTO {
    private Long brandId;
    private Long createdBy;
    private String eventType;

    @NotBlank(message = "活動標題不可為空")
    @Size(max = 150, message = "活動標題最多 150 個字")
    @Pattern(
            regexp = "^[^\\uFFFD]*$",
            message = "活動標題包含無法辨識的字元"
    )
    private String title;

    @Pattern(
            regexp = "^[^\\uFFFD]*$",
            message = "活動說明包含無法辨識的字元"
    )
    private String description;
    @Size(
            max = 150,
            message = "場地名稱最多 150 個字"
    )
    private String venueName;

    @Size(
            max = 300,
            message = "活動地址最多 300 個字"
    )
    private String address;

    @DecimalMin(
            value = "-90.000000",
            message = "緯度不可小於 -90"
    )
    @DecimalMax(
            value = "90.000000",
            message = "緯度不可大於 90"
    )
    private BigDecimal latitude;

    @DecimalMin(
            value = "-180.000000",
            message = "經度不可小於 -180"
    )
    @DecimalMax(
            value = "180.000000",
            message = "經度不可大於 180"
    )
    private BigDecimal longitude;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String targetUrl;
    private String photoUrl;
    private String sourceType;
    private Long sourceId;
    private String status;
    private byte[] photo;

    @AssertTrue(
            message = "緯度與經度必須同時填寫"
    )
    public boolean isCoordinatePairValid() {
        return (
                latitude == null
        ) == (
                longitude == null
        );
    }
}
