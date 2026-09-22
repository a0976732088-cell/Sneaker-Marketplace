package com.project.sneakermarket.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreateRequest {

    // =====================================================
    // 帳號
    //
    // 1. 必填
    // 2. 最多 50 字元
    // 3. 不可包含中文
    // =====================================================

    @NotBlank(
        message = "帳號不可空白"
    )
    @Size(
        max = 50,
        message = "帳號不可超過 50 個字元"
    )
    @Pattern(
        regexp = "^[^\\u4E00-\\u9FFF]+$",
        message = "帳號不可輸入中文"
    )
    private String account;


    // =====================================================
    // 密碼
    //
    // 1. 必填
    // 2. 至少 8 碼
    // 3. 不可包含中文
    // =====================================================

    @NotBlank(
        message = "密碼不可空白"
    )
    @Size(
        min = 8,
        message = "密碼至少需要 8 碼"
    )
    @Pattern(
        regexp = "^[^\\u4E00-\\u9FFF]+$",
        message = "密碼不可輸入中文"
    )
    private String password;


    // =====================================================
    // 姓名
    // =====================================================

    @NotBlank(
        message = "姓名不可空白"
    )
    @Size(
        max = 50,
        message = "姓名不可超過 50 個字元"
    )
    private String name;


    // =====================================================
    // Email
    // =====================================================

    @NotBlank(
        message = "Email 不可空白"
    )
    @Email(
        message = "Email 格式不正確"
    )
    @Size(
        max = 100,
        message = "Email 不可超過 100 個字元"
    )
    private String email;

    @NotBlank(
        message = "手機不可空白"
    )
    @Pattern(
        regexp = "^09\\d{8}$",
        message = "手機格式錯誤，需為 09 開頭後接 8 個數字"
    )
    private String phone;

    @NotBlank(
        message = "請輸入地址"
    )
    @Size(
        max = 255,
        message = "地址不可超過 255 個字元"
    )
    private String address;
}