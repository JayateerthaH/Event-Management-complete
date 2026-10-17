package com.event.eventmodule.image;

import com.event.eventmodule.config.EventAccessService;
import com.event.eventmodule.config.RequiresEventAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/event/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final EventImageService eventImageService;
    private final EventAccessService eventAccessService;

    /**
     * Upload a single image for an event.
     * POST /event/images/upload/{eventId}
     * Accepts multipart/form-data with field name "file".
     */
    @RequiresEventAccess
    @PostMapping(value = "/upload/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> uploadImage(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("[ImageController] Uploading image for event {} - file: {} ({} bytes)",
                eventId, file.getOriginalFilename(), file.getSize());
        // Validation failures (IllegalArgumentException) become a 400 ApiError via GlobalExceptionHandler
        EventImageService.ImageView saved = eventImageService.uploadImage(eventId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Get all gallery images for an event.
     * GET /event/images/{eventId}
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<List<EventImageService.ImageView>> getImages(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventImageService.getImagesForEvent(eventId));
    }

    /**
     * Make a gallery image the event's cover (shown on event cards and the event page).
     * PUT /event/images/{imageId}/cover
     */
    @PutMapping("/{imageId}/cover")
    public ResponseEntity<EventImageService.ImageView> setCover(@PathVariable Long imageId,
                                                               @RequestAttribute(value = "userId", required = false) String userId,
                                                               @RequestAttribute(value = "userRoles", required = false) String roles,
                                                               @RequestAttribute(value = "username", required = false) String email) {
        Long eventId = eventImageService.getEventIdForImage(imageId);
        if (!eventAccessService.isAllowed(eventId, userId, roles, email)) {
            throw new AccessDeniedException("Only creator or collaborator is allowed");
        }
        return ResponseEntity.ok(eventImageService.setCover(imageId));
    }

    /**
     * Delete a gallery image by its ID.
     * DELETE /event/images/{imageId}
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId,
                                            @RequestAttribute(value = "userId", required = false) String userId,
                                            @RequestAttribute(value = "userRoles", required = false) String roles,
                                            @RequestAttribute(value = "username", required = false) String email) {
        // @RequiresEventAccess can't be used here: the path id is an image id, not an event id
        Long eventId = eventImageService.getEventIdForImage(imageId);
        if (!eventAccessService.isAllowed(eventId, userId, roles, email)) {
            throw new AccessDeniedException("Only creator or collaborator is allowed");
        }
        eventImageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
