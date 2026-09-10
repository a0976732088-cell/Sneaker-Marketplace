package com.project.sneakermarket.advertisement.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdvertisementImageResourceConfig implements WebMvcConfigurer {

    private final Path uploadRoot;

    public AdvertisementImageResourceConfig(
            @Value("${advertisement.image.upload-dir:uploads/advertisements}")
            String uploadDir) {

        this.uploadRoot = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        String resourceLocation = uploadRoot.toUri().toString();

        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry
                .addResourceHandler("/uploads/advertisements/**")
                .addResourceLocations(resourceLocation);
    }
}