package com.event.eventmodule.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${app.images.folder}")
    private String uploadFolder;

    /**
     * Saves a multipart file to the uploads folder.
     * Uses java.nio.file.Path exclusively — works on both Windows and Linux.
     *
     * @return the generated unique filename stored in the DB
     */
    public String saveFile(MultipartFile file) throws IOException {
        Path folder = Paths.get(uploadFolder).toAbsolutePath().normalize();
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            log.info("[FileService] Created uploads directory: {}", folder);
        }

        // Preserve original file extension for content-type detection
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.lastIndexOf('.') > 0) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String fileName = UUID.randomUUID() + ext;
        Path dest = folder.resolve(fileName);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("[FileService] Saved file: {} ({})", fileName, file.getSize());
        return fileName;
    }

    /**
     * Deletes a file from the uploads folder. Does nothing if the file does not exist.
     */
    public void deleteFile(String fileName) {
        try {
            Path file = Paths.get(uploadFolder).toAbsolutePath().normalize().resolve(fileName);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) log.info("[FileService] Deleted file: {}", fileName);
        } catch (IOException e) {
            log.warn("[FileService] Could not delete file {}: {}", fileName, e.getMessage());
        }
    }
}
