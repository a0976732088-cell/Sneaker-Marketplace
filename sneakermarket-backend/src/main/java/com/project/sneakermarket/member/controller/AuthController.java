package com.project.sneakermarket.member.controller;

import com.project.sneakermarket.member.dto.LoginRequest;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;


    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        MemberResponse member =
                memberService.login(request);

        HttpSession session =
                httpRequest.getSession(true);

        session.setAttribute(
                "memberId",
                member.getMemberId()
        );

        return ResponseEntity.ok(member);
    }


    // CURRENT LOGIN MEMBER
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentMember(
            HttpServletRequest request) {

        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("尚未登入");
        }

        Long memberId =
                (Long) session.getAttribute("memberId");

        if (memberId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("尚未登入");
        }

        return ResponseEntity.ok(
                memberService.getMember(memberId)
        );
    }


    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request) {

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }
}