package com.project.sneakermarket.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "請輸入目前密碼")
    private String currentPassword;

    @NotBlank(message = "請輸入新密碼")
    @Size(min = 8, message = "新密碼至少需要 8 碼")
    private String newPassword;

    @NotBlank(message = "請再次輸入新密碼")
    private String confirmPassword;
}