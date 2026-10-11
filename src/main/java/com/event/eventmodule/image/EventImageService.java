package com.event.eventmodule.image;

import com.event.eventmodule.entity.event.EventImage;
import com.event.eventmodule.repository.EventImageRepository;
import com.event.eventmodule.repository.EventsRepository;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventImageService {

    private final FileService fileService;
    private final EventImageRepository eventImageRepository;
    private final EventsRepository eventsRepository;

    public EventImage uploadImage(Long eventId, MultipartFile file) throws IOException {
        validateUpload(eventId, file);

        String fileName = fileService.saveFile(file);
        EventImage image = new EventImage();
        image.setEventId(eventId);
        image.setFileName(fileName);
        image.setOriginalName(file.getOriginalFilename());
        image.setUploadedAt(LocalDateTime.now());
        return eventImageRepository.save(image);
    }

    public List<EventImage> getImagesForEvent(Long eventId) {
        return eventImageRepository.findByEventIdOrderByUploadedAtDesc(eventId);
    }

    public Long getEventIdForImage(Long imageId) {
        return eventImageRepository.findById(imageId)
                .map(EventImage::getEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));
    }

    public void deleteImage(Long imageId) {
        eventImageRepository.findById(imageId).ifPresent(img -> {
            fileService.deleteFile(img.getFileName());
            eventImageRepository.deleteById(imageId);
        });
    }

    private void validateUpload(Long eventId, MultipartFile file) {
        if (eventId == null || !eventsRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Invalid eventId: event does not exist");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }
}
