package com.project.sneakermarket.member.controller;


import com.project.sneakermarket.member.dto.MemberCreateRequest;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.dto.MemberUpdateRequest;
import com.project.sneakermarket.member.service.MemberService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;


    // CREATE
    @PostMapping
    public ResponseEntity<MemberResponse> createMember(
            @RequestBody MemberCreateRequest request) {

        MemberResponse result =
                memberService.createMember(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }


    // READ ALL
    @GetMapping
    public List<MemberResponse> getAllMembers() {

        return memberService.getAllMembers();
    }


    // READ ONE
    @GetMapping("/{id}")
    public MemberResponse getMember(
            @PathVariable Long id) {

        return memberService.getMember(id);
    }


    // UPDATE
    @PutMapping("/{id}")
    public MemberResponse updateMember(
            @PathVariable Long id,
            @RequestBody MemberUpdateRequest request) {

        return memberService.updateMember(id, request);
    }


    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long id) {

        memberService.deleteMember(id);

        return ResponseEntity.noContent().build();
    }
}