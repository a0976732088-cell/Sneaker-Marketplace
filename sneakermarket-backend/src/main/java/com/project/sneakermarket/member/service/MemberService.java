package com.project.sneakermarket.member.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import com.project.sneakermarket.member.dto.ChangePasswordRequest;
import com.project.sneakermarket.member.dto.LoginRequest;
import com.project.sneakermarket.member.dto.MemberCreateRequest;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.dto.MemberUpdateRequest;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE / 註冊會員
    @Transactional
    public MemberResponse createMember(MemberCreateRequest request) {

        String account = request.getAccount().trim();
        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();
        String name = request.getName().trim();
        String address = normalizeNullableText(request.getAddress());

        if (memberRepository.existsByAccount(account)) {
            throw new BusinessException("帳號已存在");
        }

        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException("Email 已存在");
        }

        validatePhone(phone);

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BusinessException("密碼至少需要 8 碼");
        }

        Member member = Member.builder()
                .account(account)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .role("ROLE_MEMBER")
                .build();

        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }

    // READ ALL
    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    // READ ONE
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long id) {
        return MemberResponse.from(findMemberOrThrow(id));
    }

    // UPDATE
    // Admin 修改會員 / 會員修改自己資料共用
    @Transactional
    public MemberResponse updateMember(Long id, MemberUpdateRequest request) {

        Member member = findMemberOrThrow(id);

        if (request.getName() != null) {
            String name = request.getName().trim();

            if (name.isBlank()) {
                throw new BusinessException("姓名不可空白");
            }

            member.setName(name);
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();

            if (email.isBlank()) {
                throw new BusinessException("Email 不可空白");
            }

            if (!email.equalsIgnoreCase(member.getEmail())
                    && memberRepository.existsByEmail(email)) {
                throw new BusinessException("Email 已被其他會員使用");
            }

            member.setEmail(email);
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            validatePhone(phone);
            member.setPhone(phone);
        }

        if (request.getAddress() != null) {
            member.setAddress(normalizeNullableText(request.getAddress()));
        }

        return MemberResponse.from(member);
    }
    
    // 修改自己的密碼
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {

        Member member = findMemberOrThrow(memberId);

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                member.getPasswordHash())) {
            throw new BusinessException("目前密碼不正確");
        }

        if (request.getNewPassword() == null
                || request.getNewPassword().length() < 8) {
            throw new BusinessException("新密碼至少需要 8 碼");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("兩次輸入的新密碼不一致");
        }

        member.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword())
        );
    }

    // DELETE
    @Transactional
    public void deleteMember(Long id) {
        Member member = findMemberOrThrow(id);
        memberRepository.delete(member);
    }

    // LOGIN
    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request) {

        if (request.getAccount() == null || request.getPassword() == null) {
            throw new BusinessException("帳號或密碼錯誤");
        }

        Member member = memberRepository
                .findByAccount(request.getAccount().trim())
                .orElseThrow(
                        () -> new BusinessException("帳號或密碼錯誤")
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                member.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new BusinessException("帳號或密碼錯誤");
        }

        return MemberResponse.from(member);
    }

    // 共用：確認會員是否為 ADMIN
    @Transactional(readOnly = true)
    public boolean isAdmin(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        return "ROLE_ADMIN".equals(member.getRole());
    }

    // 共用方法
    private Member findMemberOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("找不到會員")
                );
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^09\\d{8}$")) {
            throw new BusinessException(
                    "手機格式錯誤，需為 09 開頭後接 8 個數字"
            );
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}