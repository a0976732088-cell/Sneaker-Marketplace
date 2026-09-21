package com.project.sneakermarket.calendar.controller;

import com.project.sneakermarket.calendar.dto.GoogleCalendarConnectionResponse;
import com.project.sneakermarket.calendar.service.CalendarSessionMemberResolver;
import com.project.sneakermarket.calendar.service.GoogleCalendarOAuthService;
import com.project.sneakermarket.calendar.service.GoogleCalendarSyncService;
import com.project.sneakermarket.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/calendar/google")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "google.calendar.enabled",
        havingValue = "true"
)
public class GoogleCalendarOAuthController {

    private static final String STATE_SESSION_KEY =
            "googleCalendarOAuthState";

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final GoogleCalendarOAuthService oauthService;
    private final GoogleCalendarSyncService googleCalendarSyncService;
    private final CalendarSessionMemberResolver memberResolver;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @GetMapping("/connect")
    public ResponseEntity<Void> connect(
            HttpServletRequest request
    ) {
        memberResolver.requireMemberId(request);

        byte[] stateBytes =
                new byte[32];

        SECURE_RANDOM.nextBytes(stateBytes);

        String state =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(stateBytes);

        HttpSession session =
                request.getSession(true);

        session.setAttribute(
                STATE_SESSION_KEY,
                state
        );

        String authorizationUrl =
                oauthService.buildAuthorizationUrl(
                        state
                );

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(
                        URI.create(authorizationUrl)
                )
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false)
            String code,

            @RequestParam(required = false)
            String state,

            @RequestParam(required = false)
            String error,

            HttpServletRequest request
    ) {
        Long memberId =
                memberResolver.requireMemberId(request);

        HttpSession session =
                request.getSession(false);

        String expectedState =
                session != null
                        ? (String) session.getAttribute(
                        STATE_SESSION_KEY
                )
                        : null;

        if (session != null) {
            session.removeAttribute(
                    STATE_SESSION_KEY
            );
        }

        if (error != null
                || code == null
                || !stateMatches(
                expectedState,
                state
        )) {
            return redirectToFrontend(
                    "failed"
            );
        }

        oauthService.exchangeAuthorizationCode(
                memberId,
                code
        );

        /*
         * 補同步會員在連結 Google 前
         * 就已經關注的未來活動。
         */
        googleCalendarSyncService
                .syncExistingFollows(
                        memberId
                );

        return redirectToFrontend(
                "connected"
        );
    }

    @GetMapping("/status")
    public ApiResponse<GoogleCalendarConnectionResponse>
    status(HttpServletRequest request) {

        Long memberId =
                memberResolver.requireMemberId(request);

        return ApiResponse.ok(
                "取得 Google Calendar 連線狀態成功",
                oauthService.getConnectionStatus(
                        memberId
                )
        );
    }

    @DeleteMapping("/connection")
    public ApiResponse<Void> disconnect(
            HttpServletRequest request
    ) {
        Long memberId =
                memberResolver.requireMemberId(request);

        /*
         * 必須先使用仍有效的 Token 刪除 Google 行程，
         * 最後才能刪除 OAuth 連線資料。
         */
        googleCalendarSyncService
                .removeAllForMember(
                        memberId
                );

        oauthService.disconnect(
                memberId
        );

        return ApiResponse.ok(
                "已解除 Google Calendar 連線",
                null
        );
    }

    private boolean stateMatches(
            String expected,
            String actual
    ) {
        if (expected == null
                || actual == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(
                        StandardCharsets.UTF_8
                ),
                actual.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private ResponseEntity<Void> redirectToFrontend(
            String result
    ) {
        String target =
                frontendBaseUrl.replaceAll(
                        "/+$",
                        ""
                )
                        + "/calendar?googleCalendar="
                        + result;

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(target))
                .build();
    }
}