package com.project.sneakermarket.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthMemberResponse {

    private Long memberId;

    private String account;

    private String name;

    private String role;
}