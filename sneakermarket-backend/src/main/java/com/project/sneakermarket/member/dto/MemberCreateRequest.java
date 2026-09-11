package com.project.sneakermarket.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreateRequest {
	
	private String account;
	
	private String password;
	
	private String name;
	
	private String email;
	
	private String phone;
	
	private String address;

}
