package com.project.sneakermarket.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.sneakermarket.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member,Long> {
	
	Optional<Member> findByAccount(String account);
	
	Optional<Member> findByEmail(String email);
	
	boolean existsByAccount(String account);
	
	boolean existsByEmail(String email);
	
	

}
