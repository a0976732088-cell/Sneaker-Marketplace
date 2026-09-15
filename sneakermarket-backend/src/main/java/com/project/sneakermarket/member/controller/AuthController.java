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


    // =====================================================
    // LOGIN
    //
    // POST /api/auth/login
    //
    // MemberService.login()
    // 已經只查 deleted_at IS NULL
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(

            @RequestBody
            LoginRequest request,

            HttpServletRequest httpRequest) {


        MemberResponse member =
                memberService.login(
                    request
                );


        HttpSession session =
                httpRequest.getSession(true);


        session.setAttribute(
                "memberId",
                member.getMemberId()
        );


        return ResponseEntity.ok(
                member
        );
    }


    // =====================================================
    // CURRENT LOGIN MEMBER
    //
    // GET /api/auth/me
    // =====================================================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentMember(
            HttpServletRequest request) {


        HttpSession session =
                request.getSession(false);


        if (session == null) {

            return ResponseEntity
                    .status(
                        HttpStatus.UNAUTHORIZED
                    )
                    .body(
                        "尚未登入"
                    );
        }


        Long memberId =
                (Long)
                session.getAttribute(
                    "memberId"
                );


        if (memberId == null) {

            return ResponseEntity
                    .status(
                        HttpStatus.UNAUTHORIZED
                    )
                    .body(
                        "尚未登入"
                    );
        }


        try {

            /*
             * getMember()
             * 只搜尋 deleted_at IS NULL
             */
            MemberResponse member =
                    memberService
                        .getMember(
                            memberId
                        );


            return ResponseEntity.ok(
                    member
            );

        } catch (Exception e) {

            /*
             * 例如：
             *
             * Admin 在後台把某會員 Soft Delete，
             * 但該會員瀏覽器仍留著舊 Session。
             *
             * 此時把 Session 一起清掉。
             */
            session.invalidate();


            return ResponseEntity
                    .status(
                        HttpStatus.UNAUTHORIZED
                    )
                    .body(
                        "會員不存在或帳號已刪除"
                    );
        }
    }


    // =====================================================
    // LOGOUT
    //
    // POST /api/auth/logout
    // =====================================================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request) {


        HttpSession session =
                request.getSession(false);


        if (session != null) {

            session.invalidate();
        }


        return ResponseEntity
                .noContent()
                .build();
    }
}