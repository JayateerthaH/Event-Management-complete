package com.event.eventmodule.image;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventImage;
import com.event.eventmodule.repository.EventImageRepository;
import com.event.eventmodule.repository.EventsRepository;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An event's gallery and its cover image (the "thumbnail" shown on event cards and the event page).
 * The first image uploaded becomes the cover; the organizer can pick another; deleting the cover moves
 * it to the newest remaining image.
 */
@Service
@RequiredArgsConstructor
public class EventImageService {

    static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ImageStorage imageStorage;
    private final EventImageRepository eventImageRepository;
    private final EventsRepository eventsRepository;

    /** An image as the frontend sees it: where to show it from, and whether it's the event's cover. */
    public record ImageView(Long id, Long eventId, String fileName, String originalName, LocalDateTime uploadedAt,
                            String url, boolean cover) {}

    @Transactional
    public ImageView uploadImage(Long eventId, MultipartFile file) throws IOException {
        Events event = validateUpload(eventId, file);
        ImageStorage.StoredImage stored = imageStorage.store(file, eventId);

        EventImage image = new EventImage();
        image.setEventId(eventId);
        image.setFileName(stored.fileName());
        image.setOriginalName(file.getOriginalFilename());
        image.setUploadedAt(LocalDateTime.now());
        image.setUrl(stored.url().startsWith("/images/") ? null : stored.url());
        image.setPublicId(stored.publicId());
        image = eventImageRepository.save(image);

        if (event.getCoverImageId() == null) {   // the first image becomes the cover
            makeCover(event, image);
        }
        return view(image, event);
    }

    /**
     * The gallery, newest first. Images whose file is gone (disk storage on a host that wipes it on restart)
     * are removed, and the cover moves to one that still exists, so nobody sees broken images.
     */
    @Transactional
    public List<ImageView> getImagesForEvent(Long eventId) {
        Events event = eventsRepository.findById(eventId).orElse(null);
        List<EventImage> all = eventImageRepository.findByEventIdOrderByUploadedAtDesc(eventId);
        List<EventImage> present = all.stream()
                .filter(img -> img.getPublicId() != null || imageStorage.exists(img.getFileName()))
                .toList();
        if (present.size() < all.size()) {
            all.stream().filter(img -> !present.contains(img)).forEach(img -> eventImageRepository.deleteById(img.getId()));
            if (event != null && event.getCoverImageId() != null
                    && present.stream().noneMatch(img -> img.getId().equals(event.getCoverImageId()))) {
                if (present.isEmpty()) {
                    event.setCoverImageId(null);
                    event.setImageUrl(null);
                    eventsRepository.save(event);
                } else {
                    makeCover(event, present.get(0));
                }
            }
        }
        return present.stream().map(img -> view(img, event)).toList();
    }

    public Long getEventIdForImage(Long imageId) {
        return image(imageId).getEventId();
    }

    @Transactional
    public ImageView setCover(Long imageId) {
        EventImage image = image(imageId);
        Events event = event(image.getEventId());
        makeCover(event, image);
        return view(image, event);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        eventImageRepository.findById(imageId).ifPresent(img -> {
            imageStorage.delete(img.getFileName(), img.getPublicId());
            eventImageRepository.deleteById(imageId);
            eventsRepository.findById(img.getEventId()).ifPresent(event -> {
                if (imageId.equals(event.getCoverImageId())) {
                    List<EventImage> rest = eventImageRepository.findByEventIdOrderByUploadedAtDesc(event.getId()).stream()
                            .filter(other -> other.getPublicId() != null || imageStorage.exists(other.getFileName()))
                            .toList();
                    if (rest.isEmpty()) {
                        event.setCoverImageId(null);
                        event.setImageUrl(null);
                        eventsRepository.save(event);
                    } else {
                        makeCover(event, rest.get(0));
                    }
                }
            });
        });
    }

    private void makeCover(Events event, EventImage image) {
        event.setCoverImageId(image.getId());
        event.setImageUrl(image.displayUrl());
        eventsRepository.save(event);
    }

    private ImageView view(EventImage img, Events event) {
        boolean cover = event != null && img.getId().equals(event.getCoverImageId());
        return new ImageView(img.getId(), img.getEventId(), img.getFileName(), img.getOriginalName(), img.getUploadedAt(),
                img.displayUrl(), cover);
    }

    private EventImage image(Long imageId) {
        return eventImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));
    }

    private Events event(Long eventId) {
        return eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    private Events validateUpload(Long eventId, MultipartFile file) {
        Events event = eventId == null ? null : eventsRepository.findById(eventId).orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Invalid eventId: event does not exist");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("The image is too large: " + (file.getSize() / (1024 * 1024)) + " MB (maximum 10 MB)");
        }
        return event;
    }
}
