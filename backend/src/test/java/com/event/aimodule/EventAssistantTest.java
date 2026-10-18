package com.event.aimodule;

import com.event.aimodule.assistant.AnnouncementDraft;
import com.event.aimodule.assistant.DescriptionAssistantService;
import com.event.aimodule.feedback.FeedbackSummaryService;
import com.event.aimodule.recommend.RecommendationService;
import com.event.aimodule.search.AiSearchService;
import com.event.aimodule.support.AiRateLimiter;
import org.springframework.security.access.AccessDeniedException;
import com.event.aimodule.assistant.AnnouncementDraftRequest;
import com.event.aimodule.assistant.AnnouncementDraftService;
import com.event.aimodule.assistant.EventAnswer;
import com.event.aimodule.assistant.EventQuestionService;
import com.event.aimodule.assistant.QuestionRequest;
import com.event.aimodule.llm.DisabledLlmClient;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.api.EventProfile;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EventAssistantTest {

    private final EventApi eventApi = mock(EventApi.class);

    private static EventProfile profile(boolean approved) {
        EventCard card = new EventCard(7L, "TechFest", "A day of tech talks", "Technical", "CSE",
                new Date(), "Main Auditorium", 200L, "150", null, 40);
        return new EventProfile(card, approved, "Contact techfest@college.edu or +91 98765 43210", "CSE Department",
                "Beginners welcome", "English", 6L, List.of("CSE"),
                List.of(new EventProfile.ScheduleItem("10:00 AM", "Keynote", "Opening talk")),
                List.of(new EventProfile.Faq("Is lunch provided?", "Yes, lunch is included.")),
                List.of(new EventProfile.Speaker("Dr. Rao", "Professor", null)),
                List.of("Laptop"), List.of("Certificate: for all attendees"));
    }

    // ── Ask about this event ──

    @Test
    void answersFromTheEventPageWithoutContactDetails() {
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(profile(true)));
        var llm = new AiSearchServiceTest.StubLlm(new EventAnswer("Yes, lunch is included.", true));

        EventAnswer answer = new EventQuestionService(eventApi, llm).ask(7L, new QuestionRequest("Is lunch provided?"));

        assertThat(answer.answeredFromDetails()).isTrue();
        assertThat(llm.lastUserContent)
                .contains("Event: TechFest", "Venue: Main Auditorium", "Registration cost: ₹150",
                        "- 10:00 AM - Keynote - Opening talk", "- Q: Is lunch provided? A: Yes, lunch is included.",
                        "- Laptop", "Student's question: Is lunch provided?")
                .doesNotContain("techfest@college.edu").doesNotContain("98765");
    }

    @Test
    void cachesRepeatedQuestionsAboutTheSameEvent() {
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(profile(true)));
        var llm = new AiSearchServiceTest.StubLlm(new EventAnswer("Yes.", true));
        EventQuestionService service = new EventQuestionService(eventApi, llm);

        service.ask(7L, new QuestionRequest("Is lunch provided?"));
        service.ask(7L, new QuestionRequest("  is LUNCH provided? "));

        assertThat(llm.calls).isEqualTo(1);
    }

    @Test
    void unapprovedEventsAreNotAnswered() {
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(profile(false)));
        var llm = new AiSearchServiceTest.StubLlm(new EventAnswer("x", true));

        assertThatThrownBy(() -> new EventQuestionService(eventApi, llm).ask(7L, new QuestionRequest("When?")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(llm.calls).isZero();
    }

    @Test
    void rejectsBlankOrHugeQuestionsAndReportsWhenAiIsOff() {
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(profile(true)));
        EventQuestionService off = new EventQuestionService(eventApi, new DisabledLlmClient());

        assertThatThrownBy(() -> off.ask(7L, new QuestionRequest("  "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> off.ask(7L, new QuestionRequest("x".repeat(501)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> off.ask(7L, new QuestionRequest("When?")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("not configured");
    }

    // ── Announcement drafts ──

    @Test
    void draftsAnnouncementInTheRequestedToneWithoutPii() {
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(profile(false)));   // organizers can draft before approval
        var llm = new AiSearchServiceTest.StubLlm(new AnnouncementDraft("Venue change", "We have moved..."));

        AnnouncementDraft draft = new AnnouncementDraftService(eventApi, llm)
                .draft(7L, new AnnouncementDraftRequest("moved to hall B, questions to ravi@college.edu", "urgent"));

        assertThat(draft.title()).isEqualTo("Venue change");
        assertThat(llm.lastUserContent).contains("Event: TechFest", "Tone: urgent", "moved to hall B")
                .doesNotContain("ravi@college.edu");
    }

    @Test
    void unknownToneFallsBackToFriendlyAndBlankNotesAreRejected() {
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(profile(true)));
        var llm = new AiSearchServiceTest.StubLlm(new AnnouncementDraft("t", "m"));
        AnnouncementDraftService service = new AnnouncementDraftService(eventApi, llm);

        service.draft(7L, new AnnouncementDraftRequest("Bring your ID cards", "sarcastic"));

        assertThat(llm.lastUserContent).contains("Tone: friendly");
        assertThatThrownBy(() -> service.draft(7L, new AnnouncementDraftRequest(" ", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyEventManagersCanDraftAnnouncements() {
        when(eventApi.canManageEvent(7L, "5", "ROLE_STUDENT", "s@college.edu")).thenReturn(false);
        AnnouncementDraftService drafts = mock(AnnouncementDraftService.class);
        AiController controller = new AiController(mock(AiApi.class), mock(AiSearchService.class),
                mock(DescriptionAssistantService.class), mock(EventQuestionService.class), drafts,
                mock(RecommendationService.class), mock(FeedbackSummaryService.class), new AiRateLimiter(), eventApi,
                new DisabledLlmClient(), mock(com.event.aimodule.plan.PlanWeekService.class));

        assertThatThrownBy(() -> controller.draftAnnouncement(7L, "5", "ROLE_STUDENT", "s@college.edu",
                new AnnouncementDraftRequest("hi", null))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(drafts);
    }

    @Test
    void dateOnlyEventsAreNotGivenAnInventedTime() {
        // The create form sends "2026-12-20", stored as UTC midnight; in IST that would read as 05:30
        EventCard card = new EventCard(7L, "TechFest", null, null, null,
                Date.from(java.time.Instant.parse("2026-12-20T00:00:00Z")), null, null, "0", null, 0);
        when(eventApi.eventProfile(7L)).thenReturn(Optional.of(new EventProfile(card, true, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));
        var llm = new AiSearchServiceTest.StubLlm(new EventAnswer("x", true));

        new EventQuestionService(eventApi, llm).ask(7L, new QuestionRequest("When is it?"));

        assertThat(llm.lastUserContent).contains("Date: Sunday, 20 December 2026\n").contains("Registration cost: Free");
    }
}
