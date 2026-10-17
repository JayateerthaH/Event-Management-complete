package com.event.eventmodule.image;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventImage;
import com.event.eventmodule.repository.EventImageRepository;
import com.event.eventmodule.repository.EventsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class EventImageServiceTest {

    private final List<EventImage> images = new ArrayList<>();
    private final Events event = new Events();
    private final ImageStorage storage = mock(ImageStorage.class);
    private EventImageService service;

    @BeforeEach
    void setUp() throws Exception {
        event.setId(7L);
        EventImageRepository imageRepo = mock(EventImageRepository.class);
        EventsRepository eventsRepo = mock(EventsRepository.class);
        AtomicLong ids = new AtomicLong();
        when(imageRepo.save(any())).thenAnswer(inv -> {
            EventImage img = inv.getArgument(0);
            img.setId(ids.incrementAndGet());
            images.add(img);
            return img;
        });
        when(imageRepo.findById(anyLong())).thenAnswer(inv -> images.stream().filter(i -> i.getId().equals(inv.getArgument(0))).findFirst());
        doAnswer(inv -> images.removeIf(i -> i.getId().equals(inv.getArgument(0)))).when(imageRepo).deleteById(anyLong());
        when(imageRepo.findByEventIdOrderByUploadedAtDesc(7L)).thenAnswer(inv ->
                images.stream().sorted(Comparator.comparing(EventImage::getId).reversed()).toList());
        when(eventsRepo.findById(7L)).thenReturn(Optional.of(event));
        when(storage.store(any(), anyLong())).thenAnswer(inv ->
                new ImageStorage.StoredImage("f" + ids.get(), "https://res.cloudinary.com/x/image/upload/img" + (ids.get() + 1) + ".jpg", "pid" + ids.get()));
        service = new EventImageService(storage, imageRepo, eventsRepo);
    }

    private EventImageService.ImageView upload() throws Exception {
        return service.uploadImage(7L, new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1}));
    }

    @Test
    void firstUploadBecomesTheCoverAndTheOrganizerCanPickAnother() throws Exception {
        var first = upload();
        var second = upload();
        assertThat(first.cover()).isTrue();
        assertThat(second.cover()).isFalse();
        assertThat(event.getImageUrl()).isEqualTo(first.url());

        service.setCover(second.id());
        assertThat(event.getCoverImageId()).isEqualTo(second.id());
        assertThat(event.getImageUrl()).isEqualTo(second.url());
        assertThat(service.getImagesForEvent(7L)).filteredOn(EventImageService.ImageView::cover)
                .extracting(EventImageService.ImageView::id).containsExactly(second.id());
    }

    @Test
    void deletingTheCoverFallsBackToTheNewestImageThenToNone() throws Exception {
        var first = upload();
        var second = upload();
        service.deleteImage(first.id());               // the cover
        assertThat(event.getCoverImageId()).isEqualTo(second.id());
        assertThat(event.getImageUrl()).isEqualTo(second.url());
        verify(storage).delete(any(), eq("pid0"));   // the first image's Cloudinary copy is removed too

        service.deleteImage(second.id());
        assertThat(event.getCoverImageId()).isNull();
        assertThat(event.getImageUrl()).isNull();
    }

    @Test
    void rejectsNonImagesAndFilesOver10Mb() {
        assertThatThrownBy(() -> service.uploadImage(7L, new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1})))
                .hasMessage("Only image files are allowed");
        assertThatThrownBy(() -> service.uploadImage(7L, new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[11 * 1024 * 1024])))
                .hasMessage("The image is too large: 11 MB (maximum 10 MB)");
    }

    @Test
    void imagesWhoseFileIsGoneAreDroppedAndTheCoverMovesToOneThatExists() throws Exception {
        var first = upload();
        var second = upload();
        service.setCover(second.id());
        // Disk storage on a host that wiped the file of the cover (second) on restart
        when(storage.exists(any())).thenAnswer(inv -> !inv.getArgument(0).equals(images.get(1).getFileName()));
        images.forEach(i -> i.setPublicId(null));

        assertThat(service.getImagesForEvent(7L)).extracting(EventImageService.ImageView::id).containsExactly(first.id());
        assertThat(event.getCoverImageId()).isEqualTo(first.id());
        assertThat(images).extracting(EventImage::getId).containsExactly(first.id());
    }
}
