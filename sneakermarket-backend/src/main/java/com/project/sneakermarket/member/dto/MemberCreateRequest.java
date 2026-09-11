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

    @NotBlank(message = "帳號不可空白")
    @Size(max = 50, message = "帳號不可超過 50 個字元")
    private String account;

    @NotBlank(message = "密碼不可空白")
    @Size(min = 8, message = "密碼至少需要 8 碼")
    private String password;

    @NotBlank(message = "姓名不可空白")
    @Size(max = 50, message = "姓名不可超過 50 個字元")
    private String name;

    @NotBlank(message = "Email 不可空白")
    @Email(message = "Email 格式不正確")
    @Size(max = 100, message = "Email 不可超過 100 個字元")
    private String email;

    @NotBlank(message = "手機不可空白")
    @Pattern(regexp = "^09\\d{8}$", message = "手機格式錯誤，需為 09 開頭後接 8 個數字")
    private String phone;

    @Size(max = 255, message = "地址不可超過 255 個字元")
    private String address;
}