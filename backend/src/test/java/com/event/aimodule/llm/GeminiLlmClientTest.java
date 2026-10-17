package com.event.aimodule.llm;

import com.event.aimodule.assistant.EventAnswer;
import com.event.aimodule.feedback.FeedbackInsights;
import com.event.aimodule.search.SearchIntent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Drives GeminiLlmClient against a local stub of the Interactions API. */
class GeminiLlmClientTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastKey = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int status = 200;
    private volatile String responseJson;
    private volatile long delayMs;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            lastPath.set(exchange.getRequestURI().getPath());
            lastKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { }
            byte[] out = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GeminiLlmClient client(String model, Duration timeout) {
        return new GeminiLlmClient("test-key", model, "http://127.0.0.1:" + server.getAddress().getPort() + "/", timeout);
    }

    private GeminiLlmClient client() {
        return client("gemini-3.8-flash", Duration.ofSeconds(5));
    }

    /** An Interactions API response: optional thought step, then a model_output step with the given text parts. */
    private static String interaction(String status, String... texts) {
        StringBuilder parts = new StringBuilder();
        for (String t : texts) {
            if (!parts.isEmpty()) parts.append(',');
            parts.append("{\"type\":\"text\",\"text\":").append(JSON.writeValueAsString(t)).append('}');
        }
        return """
                {"id":"v1_x","model":"gemini-3.8-flash","object":"interaction","status":"%s",
                 "steps":[{"type":"thought","signature":"abc"},
                          {"type":"model_output","content":[%s]}],
                 "usage":{"total_input_tokens":10,"total_output_tokens":20}}""".formatted(status, parts);
    }

    @Test
    void sendsSchemaSystemPromptEffortAndKeyThenParsesTypedResult() throws Exception {
        responseJson = interaction("completed",
                "{\"keywords\":\"robotics\",\"category\":\"Workshop\",\"venue\":\"\",\"department\":\"\",\"dateRange\":\"THIS_WEEK\",\"costType\":\"FREE\"}");

        Optional<SearchIntent> result = client().generate("system prompt", "free robotics workshops this week",
                SearchIntent.class, LlmEffort.LOW, 2000);

        assertThat(result).hasValueSatisfying(i -> {
            assertThat(i.keywords()).isEqualTo("robotics");
            assertThat(i.dateRange()).isEqualTo(SearchIntent.DateRange.THIS_WEEK);
            assertThat(i.costType()).isEqualTo(SearchIntent.CostType.FREE);
        });
        assertThat(lastPath.get()).isEqualTo("/v1beta/interactions");
        assertThat(lastKey.get()).isEqualTo("test-key");
        JsonNode body = JSON.readTree(lastBody.get());
        assertThat(body.path("model").asString()).isEqualTo("gemini-3.8-flash");
        assertThat(body.path("system_instruction").asString()).isEqualTo("system prompt");
        assertThat(body.path("input").asString()).isEqualTo("free robotics workshops this week");
        assertThat(body.path("store").asBoolean()).isFalse();
        assertThat(body.path("generation_config").path("thinking_level").asString()).isEqualTo("low");
        assertThat(body.path("generation_config").path("max_output_tokens").asLong())
                .isEqualTo(2000 + GeminiLlmClient.THINKING_HEADROOM);
        JsonNode format = body.path("response_format");
        assertThat(format.path("type").asString()).isEqualTo("text");
        assertThat(format.path("mime_type").asString()).isEqualTo("application/json");
        assertThat(format.path("schema").path("properties").has("dateRange")).isTrue();
        assertThat(lastBody.get()).doesNotContain("test-key");   // key only in the header
    }

    @Test
    void mediumEffortAndEnumsAndListsRoundTrip() throws Exception {
        responseJson = interaction("completed",
                "{\"sentiment\":\"MIXED\",\"summary\":\"Good talks, late start.\",",
                "\"highlights\":[\"Speakers\"],\"improvements\":[\"Start on time\"]}");   // split across two parts

        Optional<FeedbackInsights> result = client().generate("s", "u", FeedbackInsights.class, LlmEffort.MEDIUM, 4000);

        assertThat(result).hasValueSatisfying(f -> {
            assertThat(f.sentiment()).isEqualTo(FeedbackInsights.Sentiment.MIXED);
            assertThat(f.highlights()).containsExactly("Speakers");
            assertThat(f.improvements()).containsExactly("Start on time");
        });
        assertThat(JSON.readTree(lastBody.get()).path("generation_config").path("thinking_level").asString()).isEqualTo("medium");
    }

    @Test
    void gemini2ModelsGetNoThinkingLevel() throws Exception {
        responseJson = interaction("completed", "{\"answer\":\"Yes.\",\"answeredFromDetails\":true}");

        Optional<EventAnswer> result = client("gemini-2.5-flash", Duration.ofSeconds(5))
                .generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500);

        assertThat(result).contains(new EventAnswer("Yes.", true));
        assertThat(JSON.readTree(lastBody.get()).path("generation_config").has("thinking_level")).isFalse();
    }

    @Test
    void toleratesCodeFencedJson() {
        responseJson = interaction("completed", "```json\n{\"answer\":\"Free.\",\"answeredFromDetails\":true}\n```");
        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500))
                .contains(new EventAnswer("Free.", true));
    }

    @Test
    void incompleteOrFailedInteractionsYieldEmpty() {
        responseJson = interaction("incomplete", "{\"answer\":\"Ye");   // hit max_output_tokens mid-answer
        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();

        responseJson = interaction("failed");
        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();
    }

    @Test
    void malformedOrMismatchedOutputYieldsEmpty() {
        responseJson = interaction("completed", "Sorry, I can't help with that.");
        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();

        responseJson = interaction("completed");   // no text at all
        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();

        responseJson = interaction("completed", "{\"sentiment\":\"ECSTATIC\",\"summary\":\"x\",\"highlights\":[],\"improvements\":[]}");
        assertThat(client().generate("s", "u", FeedbackInsights.class, LlmEffort.MEDIUM, 4000)).isEmpty();
    }

    @Test
    void clientErrorsYieldEmptyWithoutRetry() {
        status = 400;
        responseJson = "{\"error\":{\"code\":400,\"message\":\"API key not valid.\",\"status\":\"INVALID_ARGUMENT\"}}";

        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void rateLimitsAreRetriedOnceThenGiveUp() {
        status = 429;
        responseJson = "{\"error\":{\"code\":429,\"message\":\"Resource exhausted\"}}";

        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void timeoutYieldsEmpty() {
        delayMs = 1500;
        responseJson = interaction("completed", "{\"answer\":\"late\",\"answeredFromDetails\":true}");

        assertThat(client("gemini-3.8-flash", Duration.ofMillis(300))
                .generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();
    }

    @Test
    void unreachableServerYieldsEmpty() {
        server.stop(0);
        assertThat(client().generate("s", "u", EventAnswer.class, LlmEffort.LOW, 1500)).isEmpty();
    }
}
