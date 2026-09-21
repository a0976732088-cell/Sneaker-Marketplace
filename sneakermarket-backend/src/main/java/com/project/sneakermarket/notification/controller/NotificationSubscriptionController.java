package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.common.response.ApiResponse;
import com.project.sneakermarket.notification.dto.NotificationSubscriptionRequest;
import com.project.sneakermarket.notification.dto.NotificationSubscriptionResponse;
import com.project.sneakermarket.notification.service.NotificationSessionMemberResolver;
import com.project.sneakermarket.notification.service.NotificationSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService subscriptionService;
    private final NotificationSessionMemberResolver memberResolver;

    @GetMapping
    public ApiResponse<List<NotificationSubscriptionResponse>>
    getMySubscriptions(HttpServletRequest httpRequest) {

        Long memberId =
                memberResolver.requireMemberId(httpRequest);

        return ApiResponse.ok(
                "取得關注項目成功",
                subscriptionService.getMySubscriptions(
                        memberId
                )
        );
    }

    @PostMapping
    public ApiResponse<NotificationSubscriptionResponse>
    subscribe(
            @Valid
            @RequestBody
            NotificationSubscriptionRequest request,
            HttpServletRequest httpRequest
    ) {
        Long memberId =
                memberResolver.requireMemberId(httpRequest);

        return ApiResponse.ok(
                "關注成功",
                subscriptionService.subscribe(
                        memberId,
                        request
                )
        );
    }

    @DeleteMapping("/{subscriptionId}")
    public ApiResponse<Void> unsubscribe(
            @PathVariable Long subscriptionId,
            HttpServletRequest httpRequest
    ) {
        Long memberId =
                memberResolver.requireMemberId(httpRequest);

        subscriptionService.unsubscribe(
                memberId,
                subscriptionId
        );

        return ApiResponse.ok(
                "已取消關注",
                null
        );
    }
}