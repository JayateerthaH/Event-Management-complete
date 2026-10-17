package com.event.eventmodule.image;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Images on the server's disk, served from /images/** (WebConfig). */
@RequiredArgsConstructor
public class LocalImageStorage implements ImageStorage {

    private final FileService fileService;

    @Override
    public StoredImage store(MultipartFile file, Long eventId) throws IOException {
        String fileName = fileService.saveFile(file);
        return new StoredImage(fileName, "/images/" + fileName, null);
    }

    @Override
    public boolean exists(String fileName) {
        return fileService.exists(fileName);
    }

    @Override
    public void delete(String fileName, String publicId) {
        fileService.deleteFile(fileName);
    }

    @Override
    public String name() {
        return "local disk";
    }
}
