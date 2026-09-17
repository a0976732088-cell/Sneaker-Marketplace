package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.member.service.MemberService;
import com.project.sneakermarket.notification.service.NotificationEmailJobService;
import com.project.sneakermarket.notification.service.NotificationSessionMemberResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
public class NotificationEmailJobController {

    private final NotificationSessionMemberResolver sessionMemberResolver;
    private final MemberService memberService;
    private final NotificationEmailJobService notificationEmailJobService;
    private final NotificationEmailJobRepository jobRepository;

    @PostMapping("/api/admin/notification-email-jobs/{jobId}/retry")
    public ResponseEntity<Void> retry(
            @PathVariable Long jobId,
            HttpServletRequest request
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(request);
        if (!memberService.isAdmin(memberId)) {
            throw new ForbiddenException("需要管理員權限");
        }

        notificationEmailJobService.retryFailed(jobId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/notification-email-jobs")
    public ResponseEntity<Page<NotificationEmailJob>> list(
            @RequestParam(defaultValue = "FAILED") NotificationEmailStatus status,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(request);
        if (!memberService.isAdmin(memberId)) {
            throw new ForbiddenException("需要管理員權限");
        }

        Page<NotificationEmailJob> jobs =
                jobRepository.findByStatusOrderByCreatedAtDesc(
                        status,
                        PageRequest.of(Math.max(page, 0), 20)
                );
        return ResponseEntity.ok(jobs);
    }
}