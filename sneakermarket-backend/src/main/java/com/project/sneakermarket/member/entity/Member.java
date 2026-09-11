package com.project.sneakermarket.member.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="member_id")
	private Long memberId;
	
	@Column(name="account",nullable = false, unique = true, length = 50)
	private  String account;
	
	@Column(name="password_hash",nullable = false,length =255)
	private String passwordHash;
	
	@Column(name="name",nullable=false, length=50)
	private String name;
	
	@Column(name="email",nullable = false, unique = true, length = 100)
	private String email;
	
	@Column(name="phone",nullable = false,  length = 20)
	private String phone;
	
	@Column(name="address",nullable = true,  length = 255)
	private String address;
	
	@Column(name="role",nullable = false,  length = 20)
	private String role;
	
	@Column(name="created_at")
	private LocalDateTime createdAt ;
	
	@Column(name="updated_at")
	private LocalDateTime updatedAt ;
	
	@Column(name="deleted_at")
	private LocalDateTime deletedAt ;
	
	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		
		createdAt =now;
		updatedAt =now;
		
		if (role==null) {
			role ="Role_Member";
		}
	}	
		@PreUpdate
		public void preUpdate() {
	        updatedAt = LocalDateTime.now();
	    }
	
}
