package com.project.sneakermarket.calendar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@ConditionalOnProperty(
        name = "google.calendar.enabled",
        havingValue = "true"
)
public class GoogleCalendarTokenCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom =
            new SecureRandom();

    private final SecretKey secretKey;

    public GoogleCalendarTokenCipher(
            @Value(
                    "${google.calendar.token-encryption-key:}"
            )
            String encodedKey
    ) {
        if (encodedKey == null
                || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "啟用 Google Calendar 前必須設定 "
                            + "GOOGLE_CALENDAR_TOKEN_KEY"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes =
                    Base64.getDecoder()
                            .decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Google Calendar Token Key "
                            + "必須使用 Base64 格式",
                    exception
            );
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Google Calendar Token Key "
                            + "解碼後必須是 32 bytes"
            );
        }

        secretKey =
                new SecretKeySpec(
                        keyBytes,
                        "AES"
                );
    }

    public String encrypt(String plainText) {
        if (plainText == null
                || plainText.isBlank()) {
            throw new IllegalArgumentException(
                    "Token 不可為空"
            );
        }

        try {
            byte[] iv =
                    new byte[IV_LENGTH];

            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] encrypted =
                    cipher.doFinal(
                            plainText.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            ByteBuffer output =
                    ByteBuffer.allocate(
                            iv.length
                                    + encrypted.length
                    );

            output.put(iv);
            output.put(encrypted);

            return Base64.getEncoder()
                    .encodeToString(
                            output.array()
                    );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Token 加密失敗",
                    exception
            );
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] combined =
                    Base64.getDecoder()
                            .decode(encryptedText);

            ByteBuffer input =
                    ByteBuffer.wrap(combined);

            byte[] iv =
                    new byte[IV_LENGTH];

            input.get(iv);

            byte[] encrypted =
                    new byte[input.remaining()];

            input.get(encrypted);

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv
                    )
            );

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Token 解密失敗",
                    exception
            );
        }
    }
}