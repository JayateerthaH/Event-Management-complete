package com.event.aimodule;

import com.event.aimodule.feedback.FeedbackInsights;
import com.event.aimodule.feedback.FeedbackSummaryResponse;
import com.event.aimodule.feedback.FeedbackSummaryService;
import com.event.aimodule.llm.DisabledLlmClient;
import com.event.aimodule.llm.LlmClient;
import com.event.aimodule.llm.LlmEffort;
import com.event.aimodule.privacy.PiiRedactor;
import com.event.aimodule.support.AiRateLimiter;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.FeedbackEntry;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiSupportTest {

    @Test
    void redactsEmailsAndPhonesButKeepsOrdinaryNumbers() {
        String out = PiiRedactor.redact("Mail a.b@uni.edu or call 080-1234-5678 / +91 98765 43210. Room 204, 2026 batch, 5 stars");
        assertThat(out).doesNotContain("a.b@uni.edu", "1234-5678", "98765")
                .contains("[email removed]", "[phone removed]", "Room 204", "2026 batch", "5 stars");
    }

    @Test
    void rateLimiterBlocksAfterBudget() {
        AiRateLimiter limiter = new AiRateLimiter();
        for (int i = 0; i < 3; i++) limiter.check("search", "7", 3, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limiter.check("search", "7", 3, Duration.ofMinutes(1)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(429));
        limiter.check("search", "8", 3, Duration.ofMinutes(1));      // other users unaffected
        limiter.check("describe", "7", 3, Duration.ofMinutes(1));    // other features unaffected
    }

    @Test
    void feedbackSummaryWithoutLlmStillReportsStats() {
        EventApi eventApi = mock(EventApi.class);
        when(eventApi.feedbackForEvent(5L)).thenReturn(List.of(new FeedbackEntry(5, "Great"), new FeedbackEntry(4, "Good")));

        FeedbackSummaryResponse r = new FeedbackSummaryService(eventApi, new DisabledLlmClient()).summarize(5L);

        assertThat(r.responses()).isEqualTo(2);
        assertThat(r.averageStars()).isEqualTo(4.5);
        assertThat(r.aiAvailable()).isFalse();
        assertThat(r.insights()).isNull();
    }

    @Test
    void feedbackSentToLlmIsRedacted() {
        EventApi eventApi = mock(EventApi.class);
        when(eventApi.feedbackForEvent(5L)).thenReturn(List.of(new FeedbackEntry(2, "Contact me at me@x.com, 9876543210")));
        String[] seen = new String[1];
        LlmClient llm = new LlmClient() {
            public boolean isEnabled() { return true; }
            public String model() { return "stub"; }
            @SuppressWarnings("unchecked")
            public <T> Optional<T> generate(String s, String u, Class<T> t, LlmEffort e, long m) {
                seen[0] = u;
                return Optional.of((T) new FeedbackInsights(FeedbackInsights.Sentiment.NEGATIVE, "s", List.of(), List.of("x")));
            }
        };

        FeedbackSummaryResponse r = new FeedbackSummaryService(eventApi, llm).summarize(5L);

        assertThat(seen[0]).doesNotContain("me@x.com", "9876543210").contains("2/5");
        assertThat(r.insights().sentiment()).isEqualTo(FeedbackInsights.Sentiment.NEGATIVE);
    }
}
