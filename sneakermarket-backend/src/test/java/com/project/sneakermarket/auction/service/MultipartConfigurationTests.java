package com.project.sneakermarket.auction.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class MultipartConfigurationTests {

    @Test
    void applicationPropertiesKeepSingleFileAndRequestLimitsIndependent() throws IOException {
        Properties properties = new Properties();

        try (InputStreamReader reader = new InputStreamReader(
                requireNonNull(getClass().getResourceAsStream("/application.properties")),
                UTF_8)) {
            properties.load(reader);
        }

        assertEquals("5MB", properties.getProperty("spring.servlet.multipart.max-file-size"));
        assertEquals("100MB", properties.getProperty("spring.servlet.multipart.max-request-size"));
    }
}
