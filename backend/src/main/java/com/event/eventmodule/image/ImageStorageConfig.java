package com.event.eventmodule.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cloudinary when CLOUDINARY_URL is set and valid, otherwise the server's disk. */
@Configuration
@Slf4j
public class ImageStorageConfig {

    @Bean
    public ImageStorage imageStorage(@Value("${CLOUDINARY_URL:}") String cloudinaryUrl,
                                     @Value("${CLOUDINARY_API_BASE:https://api.cloudinary.com}") String apiBase,
                                     @Value("${CLOUDINARY_FOLDER:campusevents}") String folder,
                                     FileService fileService) {
        CloudinaryImageStorage.Credentials credentials = CloudinaryImageStorage.Credentials.parse(cloudinaryUrl);
        if (credentials != null) {
            ImageStorage storage = new CloudinaryImageStorage(credentials, apiBase, folder, fileService);
            log.info("Event images: {}", storage.name());
            return storage;
        }
        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            log.warn("CLOUDINARY_URL is set but not in the form cloudinary://API_KEY:API_SECRET@CLOUD_NAME; using local disk");
        }
        log.info("Event images: local disk (set CLOUDINARY_URL to keep them across restarts)");
        return new LocalImageStorage(fileService);
    }
}
