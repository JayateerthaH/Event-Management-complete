package com.event.aimodule;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.event.aimodule.llm.ClaudeLlmClient;
import com.event.aimodule.llm.LlmEffort;
import com.event.aimodule.search.SearchIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Drives the real Anthropic SDK client against a local stub of the Messages API. */
class ClaudeLlmClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastBeta = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseJson;
    private volatile long delayMs = 0;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastBeta.set(exchange.getRequestHeaders().getFirst("anthropic-beta"));
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

    private ClaudeLlmClient client(Duration timeout) {
        return new ClaudeLlmClient(AnthropicOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .timeout(timeout)
                .maxRetries(0)
                .build(), "claude-opus-5-5");
    }

    private static String message(String stopReason, String text) {
        return """
                {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"text","text":%s}],
                 "stop_reason":"%s","stop_sequence":null,
                 "usage":{"input_tokens":12,"output_tokens":20}}""".formatted(
                new ObjectMapper().valueToTree(text).toString(), stopReason);
    }

    @Test
    void sendsSchemaEffortAndFallbackAndParsesTypedResult() throws Exception {
        responseJson = message("end_turn",
                "{\"keywords\":\"robotics\",\"category\":\"Workshop\",\"venue\":\"\",\"department\":\"\",\"dateRange\":\"THIS_WEEK\",\"costType\":\"FREE\"}");

        Optional<SearchIntent> result = client(Duration.ofSeconds(5))
                .generate("system prompt", "free robotics workshops this week", SearchIntent.class, LlmEffort.LOW, 2000);

        assertThat(result).hasValueSatisfying(i -> {
            assertThat(i.keywords()).isEqualTo("robotics");
            assertThat(i.dateRange()).isEqualTo(SearchIntent.DateRange.THIS_WEEK);
            assertThat(i.costType()).isEqualTo(SearchIntent.CostType.FREE);
        });

        JsonNode body = new ObjectMapper().readTree(lastBody.get());
        assertThat(body.path("model").asText()).isEqualTo("claude-opus-5-5");
        assertThat(body.path("output_config").path("effort").asText()).isEqualTo("low");
        assertThat(body.path("output_config").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(body.path("output_config").path("format").path("schema").path("properties").has("dateRange")).isTrue();
        assertThat(body.path("fallbacks").asText()).isEqualTo("default");
        assertThat(lastBeta.get()).contains("server-side-fallback-2026-07-01");
    }

    @Test
    void refusalYieldsEmptySoCallersFallBack() {
        responseJson = message("refusal", "");
        assertThat(client(Duration.ofSeconds(5)).generate("s", "u", SearchIntent.class, LlmEffort.LOW, 2000)).isEmpty();
    }

    @Test
    void providerErrorYieldsEmpty() {
        status = 529;
        responseJson = "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}";
        assertThat(client(Duration.ofSeconds(5)).generate("s", "u", SearchIntent.class, LlmEffort.LOW, 2000)).isEmpty();
    }

    @Test
    void timeoutYieldsEmpty() {
        delayMs = 3000;
        responseJson = message("end_turn", "{}");
        assertThat(client(Duration.ofSeconds(1)).generate("s", "u", SearchIntent.class, LlmEffort.LOW, 2000)).isEmpty();
    }
}
