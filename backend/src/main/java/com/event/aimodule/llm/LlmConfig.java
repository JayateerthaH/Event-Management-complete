package com.event.aimodule.llm;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@Configuration
public class LlmConfig {

    /**
     * Picks the LLM provider from AI_PROVIDER: "claude", "gemini", or "auto" (default: Claude if
     * ANTHROPIC_API_KEY is set, else Gemini if GEMINI_API_KEY is set). Without a usable key, AI features
     * degrade to their non-AI fallbacks.
     */
    @Bean
    public LlmClient llmClient(@Value("${app.ai.provider:auto}") String provider,
                               @Value("${app.ai.anthropic.api-key:}") String anthropicKey,
                               @Value("${app.ai.anthropic.base-url:}") String anthropicBaseUrl,
                               @Value("${app.ai.model:claude-opus-5-5}") String claudeModel,
                               @Value("${app.ai.gemini.api-key:}") String geminiKey,
                               @Value("${app.ai.gemini.base-url:}") String geminiBaseUrl,
                               @Value("${app.ai.gemini.model:gemini-3.8-flash}") String geminiModel,
                               @Value("${app.ai.timeout-seconds:40}") long timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        return switch (choose(provider, anthropicKey, geminiKey)) {
            case "claude" -> {
                AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                        .apiKey(anthropicKey)
                        .timeout(timeout)
                        .maxRetries(1);
                if (!isBlank(anthropicBaseUrl)) {
                    builder.baseUrl(anthropicBaseUrl);   // proxies / test stubs
                }
                log.info("AI: Claude enabled (model {})", claudeModel);
                yield new ClaudeLlmClient(builder.build(), claudeModel);
            }
            case "gemini" -> {
                log.info("AI: Gemini enabled (model {})", geminiModel);
                yield new GeminiLlmClient(geminiKey, geminiModel, geminiBaseUrl, timeout);
            }
            default -> new DisabledLlmClient();
        };
    }

    /** "claude", "gemini" or "none"; logs why when nothing usable is configured. */
    static String choose(String provider, String anthropicKey, String geminiKey) {
        String p = isBlank(provider) ? "auto" : provider.trim().toLowerCase(Locale.ROOT);
        switch (p) {
            case "claude", "anthropic" -> {
                if (!isBlank(anthropicKey)) return "claude";
                log.warn("AI: AI_PROVIDER=claude but ANTHROPIC_API_KEY is not set; LLM features use their fallbacks");
            }
            case "gemini", "google" -> {
                if (!isBlank(geminiKey)) return "gemini";
                log.warn("AI: AI_PROVIDER=gemini but GEMINI_API_KEY is not set; LLM features use their fallbacks");
            }
            case "auto" -> {
                if (!isBlank(anthropicKey)) return "claude";
                if (!isBlank(geminiKey)) return "gemini";
                log.info("AI: no ANTHROPIC_API_KEY or GEMINI_API_KEY configured; LLM features use their fallbacks");
            }
            case "none", "off", "disabled" -> log.info("AI: LLM features disabled by AI_PROVIDER={}", provider);
            default -> log.warn("AI: unknown AI_PROVIDER '{}' (use auto, claude or gemini); LLM features use their fallbacks", provider);
        }
        return "none";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
