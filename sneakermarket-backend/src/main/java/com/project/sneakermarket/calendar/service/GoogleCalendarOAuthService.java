package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.dto.GoogleCalendarConnectionResponse;
import com.project.sneakermarket.calendar.dto.GoogleOAuthTokenResponse;
import com.project.sneakermarket.calendar.entity.GoogleCalendarConnection;
import com.project.sneakermarket.calendar.repository.GoogleCalendarConnectionRepository;


import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "google.calendar.enabled",
        havingValue = "true"
)
public class GoogleCalendarOAuthService {

    private static final String AUTHORIZE_URL =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String TOKEN_URL =
            "https://oauth2.googleapis.com/token";

    private static final String CALENDAR_SCOPE =
            "https://www.googleapis.com/auth/calendar.events";

    private final GoogleCalendarConnectionRepository
            connectionRepository;

    private final GoogleCalendarTokenCipher tokenCipher;

    private final RestClient restClient =
            RestClient.create();

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    public String buildAuthorizationUrl(
            String state
    ) {
        return UriComponentsBuilder
                .fromUriString(AUTHORIZE_URL)
                .queryParam(
                        "client_id",
                        clientId
                )
                .queryParam(
                        "redirect_uri",
                        redirectUri
                )
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam(
                        "scope",
                        CALENDAR_SCOPE
                )
                .queryParam(
                        "access_type",
                        "offline"
                )
                .queryParam(
                        "include_granted_scopes",
                        "true"
                )
                .queryParam(
                        "prompt",
                        "consent"
                )
                .queryParam(
                        "state",
                        state
                )
                .build()
                .encode()
                .toUriString();
    }

    @Transactional
    public void exchangeAuthorizationCode(
            Long memberId,
            String authorizationCode
    ) {
        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("code", authorizationCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add(
                "grant_type",
                "authorization_code"
        );

        GoogleOAuthTokenResponse tokenResponse =
                requestToken(form);

        GoogleCalendarConnection connection =
                connectionRepository
                        .findByMemberId(memberId)
                        .orElseGet(() -> {
                            GoogleCalendarConnection created =
                                    new GoogleCalendarConnection();

                            created.setMemberId(memberId);

                            return created;
                        });

        updateConnectionTokens(
                connection,
                tokenResponse,
                true
        );

        connectionRepository.save(connection);
    }

    /**
     * 取得仍然有效的 Access Token。
     * 如果即將過期，便自動使用 Refresh Token 更新。
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public String getValidAccessToken(
            Long memberId
    ) {
        GoogleCalendarConnection connection =
                connectionRepository
                        .findByMemberId(memberId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "會員尚未連結 Google Calendar"
                                )
                        );

        LocalDateTime expiresAt =
                connection.getAccessTokenExpiresAt();

        boolean stillValid =
                expiresAt != null
                        && expiresAt.isAfter(
                        LocalDateTime.now()
                                .plusSeconds(30)
                );

        if (stillValid) {
            return tokenCipher.decrypt(
                    connection.getAccessTokenEncrypted()
            );
        }

        return refreshAccessToken(connection);
    }

    private String refreshAccessToken(
            GoogleCalendarConnection connection
    ) {
        String encryptedRefreshToken =
                connection.getRefreshTokenEncrypted();

        if (encryptedRefreshToken == null
                || encryptedRefreshToken.isBlank()) {
            throw new IllegalStateException(
                    "找不到 Google Refresh Token，"
                            + "請重新連結 Google Calendar"
            );
        }

        String refreshToken =
                tokenCipher.decrypt(
                        encryptedRefreshToken
                );

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add(
                "grant_type",
                "refresh_token"
        );

        GoogleOAuthTokenResponse tokenResponse =
                requestToken(form);

        updateConnectionTokens(
                connection,
                tokenResponse,
                false
        );

        connectionRepository.save(connection);

        return tokenResponse.accessToken();
    }

    private GoogleOAuthTokenResponse requestToken(
            MultiValueMap<String, String> form
    ) {
        GoogleOAuthTokenResponse response =
                restClient.post()
                        .uri(TOKEN_URL)
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED
                        )
                        .body(form)
                        .retrieve()
                        .body(
                                GoogleOAuthTokenResponse.class
                        );

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new IllegalStateException(
                    "Google 未回傳 Access Token"
            );
        }

        return response;
    }

    private void updateConnectionTokens(
            GoogleCalendarConnection connection,
            GoogleOAuthTokenResponse tokenResponse,
            boolean requireRefreshToken
    ) {
        connection.setAccessTokenEncrypted(
                tokenCipher.encrypt(
                        tokenResponse.accessToken()
                )
        );

        if (tokenResponse.refreshToken() != null
                && !tokenResponse.refreshToken().isBlank()) {

            connection.setRefreshTokenEncrypted(
                    tokenCipher.encrypt(
                            tokenResponse.refreshToken()
                    )
            );
        }

        if (requireRefreshToken
                && connection.getRefreshTokenEncrypted() == null) {
            throw new IllegalStateException(
                    "Google 未回傳 Refresh Token，"
                            + "請撤銷舊授權後重新連線"
            );
        }

        long expiresIn =
                tokenResponse.expiresIn() != null
                        ? tokenResponse.expiresIn()
                        : 3600L;

        connection.setAccessTokenExpiresAt(
                LocalDateTime.now()
                        .plusSeconds(
                                Math.max(
                                        expiresIn - 60,
                                        60
                                )
                        )
        );

        if (tokenResponse.scope() != null
                && !tokenResponse.scope().isBlank()) {

            connection.setGrantedScope(
                    tokenResponse.scope()
            );
        }
    }

    @Transactional(readOnly = true)
    public GoogleCalendarConnectionResponse
    getConnectionStatus(Long memberId) {

        return connectionRepository
                .findByMemberId(memberId)
                .map(connection ->
                        new GoogleCalendarConnectionResponse(
                                true,
                                connection.getUpdatedAt()
                        )
                )
                .orElseGet(() ->
                        new GoogleCalendarConnectionResponse(
                                false,
                                null
                        )
                );
    }

    @Transactional
    public void disconnect(Long memberId) {
        connectionRepository.deleteByMemberId(
                memberId
        );
    }
}