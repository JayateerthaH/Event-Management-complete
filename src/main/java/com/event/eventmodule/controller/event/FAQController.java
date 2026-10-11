package com.event.eventmodule.controller.event;

import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.FAQs;
import com.event.eventmodule.service.events.FAQsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequiredArgsConstructor
//@RequestMapping("/organizer")
public class FAQController {

    private final FAQsService faqService;
    private final CheckAvailability checkAvailability;

    @RequiresEventAccess
    @PostMapping("organizer/events/{eventId}/faqs")
    public ResponseEntity<?> addFAQ(@PathVariable Long eventId,
                                    @RequestBody FAQs faq
    ) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(faqService.addFAQ(eventId, faq));
    }


    @GetMapping("/events/{eventId}/faqs")
    public ResponseEntity<List<FAQs>> getFAQs(@PathVariable Long eventId) {

        return ResponseEntity.ok(
                faqService.getFAQsByEvent(eventId)
        );
    }

}
