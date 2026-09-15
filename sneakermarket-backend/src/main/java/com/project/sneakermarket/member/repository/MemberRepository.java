package com.project.sneakermarket.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.sneakermarket.member.entity.Member;

public interface MemberRepository
        extends JpaRepository<Member, Long> {

    // LOGIN
    // 只允許 deleted_at IS NULL 的會員登入
    Optional<Member> findByAccountAndDeletedAtIsNull(
            String account
    );


    // 查詢有效 Email
    Optional<Member> findByEmailAndDeletedAtIsNull(
            String email
    );


    // 註冊：檢查「有效會員」帳號是否重複
    // 已被 Soft Delete 的帳號不算重複
    boolean existsByAccountAndDeletedAtIsNull(
            String account
    );


    // 註冊 / 修改資料：
    // 檢查有效會員 Email 是否重複
    boolean existsByEmailAndDeletedAtIsNull(
            String email
    );


    // ADMIN：
    // 只取得未刪除會員
    List<Member> findAllByDeletedAtIsNullOrderByMemberIdAsc();


    // 查詢單一有效會員
    Optional<Member> findByMemberIdAndDeletedAtIsNull(
            Long memberId
    );
}