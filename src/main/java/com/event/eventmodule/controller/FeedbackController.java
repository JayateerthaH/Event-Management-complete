package com.event.eventmodule.controller;

import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.feedback.EventLink;
import com.event.eventmodule.entity.feedback.FeedBack;
import com.event.eventmodule.entity.feedback.FeedBackDto;
import com.event.eventmodule.service.FeedBackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class FeedbackController {

    private final FeedBackService feedBackService;
    private final CheckAvailability checkAvailability;

    @GetMapping("organizer/feedback/{eventId}")
    @RequiresEventAccess
    public ResponseEntity<?> getAllFeedback(
            @PathVariable Long eventId
    ) throws AccessDeniedException {
        return ResponseEntity.ok(feedBackService.getAllTheFeedback(eventId));
    }

    @PostMapping("/organizer/feedback/request")
    @RequiresEventAccess
    public ResponseEntity<?> requestFeedback(@RequestBody EventLink eventLink) {
        feedBackService.createFeedBackRequest(eventLink);
        return ResponseEntity.ok("Request Processed Successfully");
    }


    @GetMapping("/feedback/check")
    public List<EventLink> feedbackGet(@RequestAttribute("userId") String userId){
        //based on security context take userId
        return feedBackService.getAllEvents(Long.valueOf(userId));
    }

    @PostMapping("/feedback/submit/{eventId}")
    public ResponseEntity<FeedBackDto> submit(@PathVariable Long eventId, @RequestBody FeedBack feedback, @RequestAttribute("userId") String userId){
        //based on security context take userId
        feedback.setId(null);   // a client-supplied id would overwrite someone else's feedback
        feedback.setUserId(Long.valueOf(userId));
        feedback.setEventId(eventId);
        return ResponseEntity.ok(feedBackService.submitFeedBack(feedback));

    }
}
