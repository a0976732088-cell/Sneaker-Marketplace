package com.project.sneakermarket.member.dto;

import java.time.LocalDateTime;

import com.project.sneakermarket.member.entity.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MemberResponse {
	
	private Long memberId;
	
	private String account;
	
	private String name;
	
	private String email;
	
	private String phone;
	
	 private String address;
	
	private String role;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
	
	
	public static MemberResponse from (Member member) {
		
		return MemberResponse.builder()
				.memberId(member.getMemberId())
				.account(member.getAccount())
				.name(member.getName())
				.email(member.getEmail())
				.phone(member.getPhone())
				.address(member.getAddress())
				.role(member.getRole())
				.createdAt(member.getCreatedAt())
				.updatedAt(member.getUpdatedAt())
				.build();
	}
}
