package com.project.sneakermarket.member.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import com.project.sneakermarket.common.exception.ForbiddenException;

import com.project.sneakermarket.member.dto.ChangePasswordRequest;
import com.project.sneakermarket.member.dto.MemberCreateRequest;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.dto.MemberUpdateRequest;

import com.project.sneakermarket.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;


    
    // PUBLIC：會員註冊
    //
    // POST /api/members
    
    @PostMapping
    public ResponseEntity<MemberResponse> createMember(
            @Valid
            @RequestBody
            MemberCreateRequest request) {

        MemberResponse result =
                memberService.createMember(
                    request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }


    
    // MEMBER：修改自己的資料
    //
    // PUT /api/members/me
    
    @PutMapping("/me")
    public MemberResponse updateMyProfile(
            @Valid
            @RequestBody
            MemberUpdateRequest request,

            HttpServletRequest httpRequest) {

        Long currentMemberId =
                requireLoggedInMemberId(
                    httpRequest
                );


        return memberService.updateMember(
                currentMemberId,
                request
        );
    }


    
    // MEMBER：修改自己的密碼
    //
    // PUT /api/members/me/password
    
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(

            @Valid
            @RequestBody
            ChangePasswordRequest request,

            HttpServletRequest httpRequest) {


        Long currentMemberId =
                requireLoggedInMemberId(
                    httpRequest
                );


        memberService.changePassword(
                currentMemberId,
                request
        );


        return ResponseEntity
                .noContent()
                .build();
    }


    
    // MEMBER：刪除自己的帳號
    //
    // DELETE /api/members/me
    //
    // 注意：
    // 現在實際是 Soft Delete。
    
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            HttpServletRequest httpRequest) {


        Long currentMemberId =
                requireLoggedInMemberId(
                    httpRequest
                );


        /*
         * Admin 不允許從會員中心
         * 刪除自己的管理帳號。
         */
        if (
            memberService.isAdmin(
                currentMemberId
            )
        ) {

            throw new ForbiddenException(
                "管理員不可刪除自己的管理員帳號"
            );
        }


        /*
         * Soft Delete
         */
        memberService.deleteMember(
                currentMemberId
        );


        /*
         * 刪除自己之後，
         * Session 一起清掉。
         */
        HttpSession session =
                httpRequest.getSession(false);


        if (session != null) {

            session.invalidate();
        }


        return ResponseEntity
                .noContent()
                .build();
    }


    
    // ADMIN：查詢全部會員
    //
    // GET /api/members
    //
    // Service 只回傳 deleted_at IS NULL。
    
    @GetMapping
    public List<MemberResponse> getAllMembers(
            HttpServletRequest httpRequest) {


        requireAdmin(
            httpRequest
        );


        return memberService
                .getAllMembers();
    }


    
    // ADMIN：查詢單一會員
    //
    // GET /api/members/{id}
    
    @GetMapping("/{id}")
    public MemberResponse getMember(

            @PathVariable Long id,

            HttpServletRequest httpRequest) {


        requireAdmin(
            httpRequest
        );


        return memberService
                .getMember(id);
    }


    
    // ADMIN：修改會員
    //
    // PUT /api/members/{id}
    
    @PutMapping("/{id}")
    public MemberResponse updateMember(

            @PathVariable Long id,

            @Valid
            @RequestBody
            MemberUpdateRequest request,

            HttpServletRequest httpRequest) {


        requireAdmin(
            httpRequest
        );


        return memberService
                .updateMember(
                    id,
                    request
                );
    }


    
    // ADMIN：刪除指定會員
    //
    // DELETE /api/members/{id}
    //
    // 現在同樣為 Soft Delete。
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(

            @PathVariable Long id,

            HttpServletRequest httpRequest) {


        Long currentAdminId =
                requireAdmin(
                    httpRequest
                );


        if (
            currentAdminId.equals(id)
        ) {

            throw new ForbiddenException(
                "管理員不可刪除自己的管理員帳號"
            );
        }


        memberService.deleteMember(id);


        return ResponseEntity
                .noContent()
                .build();
    }


    
    // 取得 Session 中目前會員 ID
    
    private Long requireLoggedInMemberId(
            HttpServletRequest request) {


        HttpSession session =
                request.getSession(false);


        if (session == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "尚未登入"
            );
        }


        Long memberId =
                (Long)
                session.getAttribute(
                    "memberId"
                );


        if (memberId == null) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "尚未登入"
            );
        }


        return memberId;
    }


    
    // 確認目前會員為 ADMIN
    
    private Long requireAdmin(
            HttpServletRequest request) {


        Long memberId =
                requireLoggedInMemberId(
                    request
                );


        if (
            !memberService.isAdmin(
                memberId
            )
        ) {

            throw new ForbiddenException(
                "需要管理員權限"
            );
        }


        return memberId;
    }
}