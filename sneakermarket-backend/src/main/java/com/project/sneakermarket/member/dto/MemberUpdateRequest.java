package com.project.sneakermarket.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateRequest {
	
	private String name;
	
	private String email;
	
	private String phone;

}
