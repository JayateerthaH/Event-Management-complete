package com.event.eventmodule;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.share.SharePreviewController;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SharePreviewTest {

    private final EventsRepository repo = mock(EventsRepository.class);
    private final SharePreviewController controller = new SharePreviewController(repo, "https://campus.example/");

    private Events event(boolean approved) {
        Events e = new Events();
        e.setId(7L);
        e.setName("Robotics <Workshop> & Demo");
        e.setDescription("Build a line follower.\n Bring a laptop.");
        e.setEventDate(Date.from(Instant.parse("2026-12-15T05:30:00Z")));
        e.setVenue("Hall B");
        e.setCostOfRegistration("0");
        e.setImageUrl("https://res.cloudinary.com/demo/image/upload/v1/campusevents/events/7/cover.png");
        e.setIsApproved(approved);
        return e;
    }

    @Test
    void approvedEventGetsEscapedOpenGraphTagsAndCroppedCover() {
        when(repo.findById(7L)).thenReturn(Optional.of(event(true)));
        String html = controller.eventPreview(7L).getBody();
        assertThat(html)
                .contains("<meta property=\"og:title\" content=\"Robotics &lt;Workshop&gt; &amp; Demo\">")
                .contains("Tue 15 Dec 2026, 11:00 AM · Hall B · Free. Build a line follower. Bring a laptop.")
                .contains("<meta property=\"og:url\" content=\"https://campus.example/events/7\">")
                .contains("/upload/f_jpg,q_auto,c_fill,g_auto,w_1200,h_630/v1/campusevents/events/7/cover.png")
                .contains("http-equiv=\"refresh\"");
    }

    @Test
    void unapprovedOrMissingEventsRevealNothing() {
        when(repo.findById(7L)).thenReturn(Optional.of(event(false)));
        String html = controller.eventPreview(7L).getBody();
        assertThat(html).doesNotContain("Robotics").contains("content=\"https://campus.example/og-default.jpg\"");
    }
}
