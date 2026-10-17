package com.event.eventmodule.image;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Where event images are kept. Cloudinary when CLOUDINARY_URL is set (survives restarts and redeploys),
 * otherwise the server's disk (fine locally; wiped on Render's free plan). See ImageStorageConfig.
 */
public interface ImageStorage {

    /**
     * @param fileName  stored name (disk) or a unique name (Cloudinary)
     * @param url       absolute URL (Cloudinary) or a path the frontend prefixes with the API URL (disk)
     * @param publicId  Cloudinary's id for deleting the image; null on disk
     */
    record StoredImage(String fileName, String url, String publicId) {}

    StoredImage store(MultipartFile file, Long eventId) throws IOException;

    /** Whether the stored image still exists (files on Render's free-plan disk vanish on restart). */
    default boolean exists(String fileName) {
        return true;
    }

    /** Best effort: a failure is logged, never thrown, so the database row can still be removed. */
    void delete(String fileName, String publicId);

    String name();
}
