package com.event.aimodule;

import com.event.aimodule.assistant.AnnouncementDraft;
import com.event.aimodule.assistant.AnnouncementDraftRequest;
import com.event.aimodule.assistant.AnnouncementDraftService;
import com.event.aimodule.assistant.DescriptionAssistantService;
import com.event.aimodule.assistant.DescriptionDraft;
import com.event.aimodule.assistant.DescriptionRequest;
import com.event.aimodule.assistant.EventAnswer;
import com.event.aimodule.assistant.EventQuestionService;
import com.event.aimodule.assistant.QuestionRequest;
import com.event.aimodule.feedback.FeedbackSummaryResponse;
import com.event.aimodule.feedback.FeedbackSummaryService;
import com.event.aimodule.llm.LlmClient;
import com.event.aimodule.recommend.RecommendationService;
import com.event.aimodule.recommend.RecommendedEvent;
import com.event.aimodule.search.AiSearchResponse;
import com.event.aimodule.search.AiSearchService;
import com.event.aimodule.support.AiRateLimiter;
import com.event.eventmodule.api.EventApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI endpoints. All require login except GET /ai/events/{id}/similar (no LLM, public event data only);
 * LLM-backed ones are rate limited per user.
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final AiApi aiApi;
    private final AiSearchService searchService;
    private final DescriptionAssistantService descriptionService;
    private final EventQuestionService questionService;
    private final AnnouncementDraftService announcementService;
    private final RecommendationService recommendationService;
    private final FeedbackSummaryService feedbackService;
    private final AiRateLimiter rateLimiter;
    private final EventApi eventApi;
    private final LlmClient llm;

    /** Lets the UI show LLM features only when they can work. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("llmEnabled", llm.isEnabled(), "model", llm.model());
    }

    @GetMapping("/recommendations")
    public List<RecommendedEvent> recommendations(@RequestAttribute("userId") String userId,
                                                  @RequestParam(defaultValue = "6") int limit) {
        rateLimiter.check("recommendations", userId, 60, MINUTE);
        return aiApi.recommendationsFor(Long.valueOf(userId), limit);
    }

    @GetMapping("/search")
    public AiSearchResponse search(@RequestAttribute("userId") String userId, @RequestParam("q") String query) {
        rateLimiter.check("search", userId, 20, MINUTE);
        return searchService.search(query);
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PostMapping("/events/generate-description")
    public DescriptionDraft generateDescription(@RequestAttribute("userId") String userId,
                                                @RequestBody DescriptionRequest request) {
        rateLimiter.check("describe", userId, 10, MINUTE);
        return descriptionService.draft(request);
    }

    @GetMapping("/events/{eventId}/feedback-summary")
    public FeedbackSummaryResponse feedbackSummary(@PathVariable Long eventId,
                                                   @RequestAttribute("userId") String userId,
                                                   @RequestAttribute(value = "userRoles", required = false) String roles,
                                                   @RequestAttribute(value = "username", required = false) String email) {
        if (!eventApi.canManageEvent(eventId, userId, roles, email)) {
            throw new AccessDeniedException("Only the event's organizer, collaborators or admins can see this");
        }
        rateLimiter.check("feedback-summary", userId, 10, MINUTE);
        return feedbackService.summarize(eventId);
    }

    /** Public: shown on the event page to visitors too. */
    @GetMapping("/events/{eventId}/similar")
    public List<RecommendedEvent> similar(@PathVariable Long eventId, @RequestParam(defaultValue = "4") int limit) {
        return recommendationService.similarTo(eventId, limit);
    }

    @PostMapping("/events/{eventId}/ask")
    public EventAnswer ask(@PathVariable Long eventId, @RequestAttribute("userId") String userId,
                           @RequestBody QuestionRequest request) {
        rateLimiter.check("ask", userId, 20, MINUTE);
        return questionService.ask(eventId, request);
    }

    @PostMapping("/events/{eventId}/draft-announcement")
    public AnnouncementDraft draftAnnouncement(@PathVariable Long eventId,
                                               @RequestAttribute("userId") String userId,
                                               @RequestAttribute(value = "userRoles", required = false) String roles,
                                               @RequestAttribute(value = "username", required = false) String email,
                                               @RequestBody AnnouncementDraftRequest request) {
        if (!eventApi.canManageEvent(eventId, userId, roles, email)) {
            throw new AccessDeniedException("Only the event's organizer, collaborators or admins can send announcements");
        }
        rateLimiter.check("announce", userId, 10, MINUTE);
        return announcementService.draft(eventId, request);
    }
}
