package com.event.aimodule.llm;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LlmConfig {

    /** Claude when ANTHROPIC_API_KEY is set; otherwise AI features degrade to their non-AI fallbacks. */
    @Bean
    public LlmClient llmClient(@Value("${app.ai.anthropic.api-key:}") String apiKey,
                               @Value("${app.ai.anthropic.base-url:}") String baseUrl,
                               @Value("${app.ai.model:claude-opus-5-5}") String model,
                               @Value("${app.ai.timeout-seconds:30}") long timeoutSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("AI: no ANTHROPIC_API_KEY configured; LLM features use their fallbacks");
            return new DisabledLlmClient();
        }
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(1);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);   // proxies / test stubs
        }
        log.info("AI: Claude enabled (model {})", model);
        return new ClaudeLlmClient(builder.build(), model);
    }
}
