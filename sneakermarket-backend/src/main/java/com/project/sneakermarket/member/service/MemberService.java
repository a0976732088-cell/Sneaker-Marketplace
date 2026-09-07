package com.project.sneakermarket.member.service;

import com.project.sneakermarket.member.dto.MemberCreateRequest;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.dto.MemberUpdateRequest;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.member.dto.LoginRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    // CREATE
    @Transactional
    public MemberResponse createMember(MemberCreateRequest request) {

        if (memberRepository.existsByAccount(request.getAccount())) {
            throw new RuntimeException("帳號已存在");
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email 已存在");
        }

        if (request.getPassword() == null ||
                request.getPassword().length() < 8) {

            throw new RuntimeException("密碼至少需要 8 碼");
        }

        Member member = Member.builder()
                .account(request.getAccount())
                .passwordHash(
                        passwordEncoder.encode(request.getPassword())
                )
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role("ROLE_MEMBER")
                .build();

        Member savedMember =
                memberRepository.save(member);

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

        Member member =
                memberRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException("找不到會員")
                        );

        return MemberResponse.from(member);
    }


    // UPDATE
    @Transactional
    public MemberResponse updateMember(
            Long id,
            MemberUpdateRequest request) {

        Member member =
                memberRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException("找不到會員")
                        );

        if (request.getEmail() != null &&
                !request.getEmail().equals(member.getEmail())) {

            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email 已被其他會員使用");
            }

            member.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            member.setName(request.getName());
        }

        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }

        return MemberResponse.from(member);
    }


    // DELETE
    @Transactional
    public void deleteMember(Long id) {

        Member member =
                memberRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException("找不到會員")
                        );

        memberRepository.delete(member);
    }
    
 // LOGIN
    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request) {

        Member member = memberRepository
                .findByAccount(request.getAccount())
                .orElseThrow(
                        () -> new RuntimeException("帳號或密碼錯誤")
                );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        member.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new RuntimeException("帳號或密碼錯誤");
        }

        return MemberResponse.from(member);
    }
    
    
    
}