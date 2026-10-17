package com.event.aimodule.llm;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Google Gemini via the Interactions API ({@code POST /v1beta/interactions}) with a JSON Schema response format,
 * so replies map onto the same records as the Claude client. Any failure (HTTP error, timeout, token limit,
 * blocked or malformed output) returns {@link Optional#empty()} so callers fall back to non-AI behavior.
 */
@Slf4j
public class GeminiLlmClient implements LlmClient {

    public static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    // Thinking tokens count toward max_output_tokens; without headroom a reply can end empty with MAX_TOKENS
    static final long THINKING_HEADROOM = 4096;
    private static final Set<Integer> RETRYABLE = Set.of(429, 500, 502, 503, 504);

    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final Duration timeout;
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    public GeminiLlmClient(String apiKey, String model, String baseUrl, Duration timeout) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = timeout;
        String base = (baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl).replaceAll("/+$", "");
        this.endpoint = URI.create(base + "/v1beta/interactions");
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public <T> Optional<T> generate(String systemPrompt, String userContent, Class<T> responseType,
                                    LlmEffort effort, long maxTokens) {
        try {
            String body = json.writeValueAsString(requestBody(systemPrompt, userContent, responseType, effort, maxTokens));
            HttpResponse<String> response = send(body);
            if (response.statusCode() / 100 != 2) {
                log.warn("Gemini request failed ({}): {}", response.statusCode(), errorMessage(response.body()));
                return Optional.empty();
            }
            return parse(response.body(), responseType);
        } catch (IOException e) {
            log.warn("Gemini unreachable or timed out: {}", e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.warn("Gemini call failed: {}", e.toString());
        }
        return Optional.empty();
    }

    Map<String, Object> requestBody(String systemPrompt, String userContent, Class<?> responseType,
                                    LlmEffort effort, long maxTokens) {
        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("max_output_tokens", maxTokens + THINKING_HEADROOM);
        // thinking_level exists on Gemini 3+; Gemini 2.x models only take a token budget, so leave it to their default
        if (!model.startsWith("gemini-2") && !model.startsWith("gemini-1")) {
            generation.put("thinking_level", effort == LlmEffort.LOW ? "low" : "medium");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("system_instruction", systemPrompt);
        body.put("input", userContent);
        body.put("generation_config", generation);
        body.put("response_format", Map.of(
                "type", "text",
                "mime_type", "application/json",
                "schema", JsonSchemas.of(responseType)));
        body.put("store", false);   // not saved for later retrieval; each call is self-contained
        return body;
    }

    private HttpResponse<String> send(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (RETRYABLE.contains(response.statusCode())) {   // one retry for rate limits / transient errors
            Thread.sleep(800);
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return response;
    }

    /** Joins the text of the final model_output steps and maps it onto the record. */
    @SuppressWarnings("unchecked")
    <T> Optional<T> parse(String responseBody, Class<T> responseType) {
        Map<String, Object> interaction = json.readValue(responseBody, Map.class);
        Object status = interaction.get("status");
        if (!"completed".equals(status)) {
            log.warn("Gemini returned no usable output (status={})", status);
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        for (Object step : asList(interaction.get("steps"))) {
            if (!(step instanceof Map<?, ?> s) || !"model_output".equals(s.get("type"))) continue;   // skip thoughts
            for (Object part : asList(s.get("content"))) {
                if (part instanceof Map<?, ?> p && "text".equals(p.get("type")) && p.get("text") instanceof String t) {
                    text.append(t);
                }
            }
        }
        String out = stripCodeFence(text.toString().trim());
        if (out.isEmpty()) {
            log.warn("Gemini returned no text output");
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(json.readValue(out, responseType));
        } catch (RuntimeException e) {
            log.warn("Gemini output did not match {}: {}", responseType.getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    private static List<?> asList(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    // Schema-constrained output is plain JSON, but tolerate ```json fences just in case
    private static String stripCodeFence(String s) {
        if (!s.startsWith("```")) return s;
        String inner = s.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return inner.endsWith("```") ? inner.substring(0, inner.length() - 3).trim() : inner;
    }

    @SuppressWarnings("unchecked")
    private String errorMessage(String body) {
        try {
            Object error = json.readValue(body, Map.class).get("error");
            if (error instanceof Map<?, ?> e && e.get("message") != null) return String.valueOf(e.get("message"));
        } catch (RuntimeException ignored) {
            // not JSON
        }
        return body == null ? "" : body.substring(0, Math.min(200, body.length()));
    }
}
