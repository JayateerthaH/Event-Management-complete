package com.event.aimodule.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claude via the official Anthropic Java SDK, using structured outputs (JSON schema derived from a record)
 * so responses are always valid, typed objects.
 */
@Slf4j
public class ClaudeLlmClient implements LlmClient {

    // Server-side refusal fallback: if a safety classifier declines, the API retries on a suitable model
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final AnthropicClient client;
    private final String model;
    private final Map<Class<?>, JsonOutputFormat> schemas = new ConcurrentHashMap<>();

    public ClaudeLlmClient(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
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
            StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(maxTokens)
                    .system(systemPrompt)
                    .outputConfig(responseType)
                    // One OutputConfig carrying both the schema and the effort (setting them separately drops one)
                    .outputConfig(OutputConfig.builder()
                            .effort(effort == LlmEffort.LOW ? OutputConfig.Effort.LOW : OutputConfig.Effort.MEDIUM)
                            .format(schemaFor(responseType))
                            .build())
                    .addUserMessage(userContent)
                    .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    .build();

            StructuredMessage<T> message = client.messages().create(params);

            Optional<StopReason> stop = message.stopReason();
            if (stop.isPresent() && (stop.get().equals(StopReason.REFUSAL) || stop.get().equals(StopReason.MAX_TOKENS))) {
                log.warn("LLM returned no usable output (stop_reason={})", stop.get());
                return Optional.empty();
            }
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst();
        } catch (RateLimitException e) {
            log.warn("LLM rate limited: {}", e.getMessage());
        } catch (AnthropicServiceException e) {
            log.warn("LLM request failed ({}): {}", e.statusCode(), e.getMessage());
        } catch (AnthropicIoException e) {
            log.warn("LLM unreachable or timed out: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.warn("LLM call failed: {}", e.toString());
        }
        return Optional.empty();
    }

    // The SDK derives the JSON schema from the record; computed once per type
    private JsonOutputFormat schemaFor(Class<?> type) {
        return schemas.computeIfAbsent(type, t -> MessageCreateParams.builder()
                .model(model).maxTokens(1L).addUserMessage("schema")
                .outputConfig(t).build()
                .rawParams().outputConfig().orElseThrow().format().orElseThrow());
    }
}
