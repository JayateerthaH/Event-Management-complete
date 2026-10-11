package com.event.aimodule;

import com.event.aimodule.llm.LlmClient;
import com.event.aimodule.llm.LlmEffort;
import com.event.aimodule.search.AiSearchResponse;
import com.event.aimodule.search.AiSearchService;
import com.event.aimodule.search.SearchIntent;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.api.EventSearchCriteria;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiSearchServiceTest {

    private final EventApi eventApi = mock(EventApi.class);
    private final EventCard robotics = new EventCard(1L, "Robotics Workshop", "Build robots", "Workshop", "ECE",
            new Date(), "Lab", 30L, "0", null, 3);

    /** Records the prompt it received and returns a canned intent. */
    static class StubLlm implements LlmClient {
        final Object reply; String lastUserContent; int calls;
        StubLlm(Object reply) { this.reply = reply; }
        public boolean isEnabled() { return true; }
        public String model() { return "stub"; }
        @SuppressWarnings("unchecked")
        public <T> Optional<T> generate(String s, String u, Class<T> t, LlmEffort e, long m) {
            calls++; lastUserContent = u; return Optional.ofNullable((T) reply);
        }
    }

    @Test
    void usesLlmFiltersAndDropsUnknownCategory() {
        when(eventApi.eventCategories()).thenReturn(List.of("Workshop", "Seminar"));
        when(eventApi.searchEvents(any(), anyInt())).thenReturn(List.of(robotics));
        StubLlm llm = new StubLlm(new SearchIntent("robots", "Space Opera", "", "ECE",
                SearchIntent.DateRange.THIS_WEEK, SearchIntent.CostType.FREE));

        AiSearchResponse response = new AiSearchService(eventApi, llm).search("free robot stuff for ECE this week");

        assertThat(response.aiUsed()).isTrue();
        assertThat(response.filters()).isEqualTo(new EventSearchCriteria("robots", null, null, "ECE", "THIS_WEEK", "FREE"));
        assertThat(response.results()).containsExactly(robotics);
    }

    @Test
    void neverSendsEmailsOrPhoneNumbersToTheLlm() {
        when(eventApi.eventCategories()).thenReturn(List.of());
        StubLlm llm = new StubLlm(new SearchIntent("", "", "", "", SearchIntent.DateRange.ANY, SearchIntent.CostType.ANY));

        new AiSearchService(eventApi, llm).search("events by jane.doe@college.edu call +91 98765 43210");

        assertThat(llm.lastUserContent).doesNotContain("jane.doe@college.edu").doesNotContain("98765")
                .contains("[email removed]").contains("[phone removed]");
    }

    @Test
    void cachesInterpretationOfRepeatedQueries() {
        when(eventApi.eventCategories()).thenReturn(List.of("Workshop"));
        StubLlm llm = new StubLlm(new SearchIntent("robots", "", "", "", SearchIntent.DateRange.ANY, SearchIntent.CostType.ANY));
        AiSearchService service = new AiSearchService(eventApi, llm);

        service.search("Robot events");
        service.search("robot events ");

        assertThat(llm.calls).isEqualTo(1);
    }

    @Test
    void retriesWithoutKeywordsWhenTheyAreTooNarrow() {
        when(eventApi.eventCategories()).thenReturn(List.of("Workshop"));
        when(eventApi.searchEvents(argThat(c -> c != null && c.keywords() != null), anyInt())).thenReturn(List.of());
        when(eventApi.searchEvents(argThat(c -> c != null && c.keywords() == null), anyInt())).thenReturn(List.of(robotics));
        StubLlm llm = new StubLlm(new SearchIntent("quadcopter", "Workshop", "", "", SearchIntent.DateRange.ANY, SearchIntent.CostType.ANY));

        AiSearchResponse response = new AiSearchService(eventApi, llm).search("quadcopter workshops");

        assertThat(response.results()).containsExactly(robotics);
        assertThat(response.filters().keywords()).isNull();
        assertThat(response.note()).contains("related events");
    }

    @Test
    void fallsBackToWordSearchWhenLlmUnavailable() {
        LlmClient disabled = new com.event.aimodule.llm.DisabledLlmClient();
        when(eventApi.searchEvents(argThat(c -> c != null && "robotics workshop for beginners".equals(c.keywords())), anyInt())).thenReturn(List.of());
        when(eventApi.searchEvents(argThat(c -> c != null && "robotics".equals(c.keywords())), anyInt())).thenReturn(List.of(robotics));
        when(eventApi.searchEvents(argThat(c -> c != null && !"robotics".equals(c.keywords())
                && !"robotics workshop for beginners".equals(c.keywords())), anyInt())).thenReturn(List.of());

        AiSearchResponse response = new AiSearchService(eventApi, disabled).search("robotics workshop for beginners");

        assertThat(response.aiUsed()).isFalse();
        assertThat(response.note()).contains("not configured");
        assertThat(response.results()).containsExactly(robotics);
        ArgumentCaptor<EventSearchCriteria> searched = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(eventApi, atLeastOnce()).searchEvents(searched.capture(), anyInt());
        assertThat(searched.getAllValues()).extracting(EventSearchCriteria::keywords).doesNotContain("for");
    }

    @Test
    void rejectsEmptyOrHugeQueries() {
        AiSearchService service = new AiSearchService(eventApi, new com.event.aimodule.llm.DisabledLlmClient());
        assertThatThrownBy(() -> service.search("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("x".repeat(301))).isInstanceOf(IllegalArgumentException.class);
    }
}
