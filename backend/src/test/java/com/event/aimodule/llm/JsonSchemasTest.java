package com.event.aimodule.llm;

import com.event.aimodule.assistant.DescriptionDraft;
import com.event.aimodule.feedback.FeedbackInsights;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonSchemasTest {

    @Test
    @SuppressWarnings("unchecked")
    void describesRecordsEnumsListsAndDescriptions() {
        Map<String, Object> s = JsonSchemas.of(FeedbackInsights.class);

        assertThat(s).containsEntry("type", "object").containsEntry("additionalProperties", false);
        assertThat((List<String>) s.get("required")).containsExactly("sentiment", "summary", "highlights", "improvements");
        Map<String, Map<String, Object>> props = (Map<String, Map<String, Object>>) s.get("properties");
        assertThat(props.get("sentiment")).containsEntry("type", "string")
                .containsEntry("enum", List.of("POSITIVE", "MIXED", "NEGATIVE"));
        assertThat(props.get("summary")).containsEntry("type", "string")
                .containsEntry("description", "2-3 sentence overview for the organizer.");
        assertThat(props.get("highlights")).containsEntry("type", "array")
                .containsEntry("items", Map.of("type", "string"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void nestsRecordsInsideLists() {
        Map<String, Object> props = (Map<String, Object>) JsonSchemas.of(DescriptionDraft.class).get("properties");
        Map<String, Object> faqItem = (Map<String, Object>) ((Map<String, Object>) props.get("faqs")).get("items");

        assertThat(faqItem).containsEntry("type", "object");
        assertThat((List<String>) faqItem.get("required")).containsExactly("question", "answer");
    }

    @Test
    void rejectsNonRecords() {
        assertThatThrownBy(() -> JsonSchemas.of(String.class)).isInstanceOf(IllegalArgumentException.class);
    }
}
