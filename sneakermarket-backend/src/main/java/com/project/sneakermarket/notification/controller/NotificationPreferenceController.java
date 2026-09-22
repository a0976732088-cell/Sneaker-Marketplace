package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.common.response.ApiResponse;
import com.project.sneakermarket.notification.dto.NotificationPreferenceResponse;
import com.project.sneakermarket.notification.dto.NotificationPreferenceUpdateRequest;
import com.project.sneakermarket.notification.service.NotificationPreferenceService;
import com.project.sneakermarket.notification.service.NotificationSessionMemberResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final NotificationSessionMemberResolver memberResolver;

    @GetMapping("/me")
    public ApiResponse<NotificationPreferenceResponse>
    getMyPreference(HttpServletRequest httpRequest) {

        Long memberId =
                memberResolver.requireMemberId(httpRequest);

        NotificationPreferenceResponse response =
                preferenceService.getMyPreference(memberId);

        return ApiResponse.ok(
                "取得通知偏好成功",
                response
        );
    }

    @PutMapping("/me")
    public ApiResponse<NotificationPreferenceResponse>
    updateMyPreference(
            @Valid
            @RequestBody
            NotificationPreferenceUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long memberId =
                memberResolver.requireMemberId(httpRequest);

        NotificationPreferenceResponse response =
                preferenceService.updateMyPreference(
                        memberId,
                        request
                );

        return ApiResponse.ok(
                "通知偏好已更新",
                response
        );
    }
}