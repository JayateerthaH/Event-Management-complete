package com.event.aimodule.llm;

import java.util.Optional;

/**
 * Provider-agnostic LLM access. Implementations return a value typed by a JSON schema derived from
 * {@code responseType}, or {@link Optional#empty()} on any failure (timeout, refusal, provider error)
 * so callers can always fall back to non-AI behavior.
 */
public interface LlmClient {

    boolean isEnabled();

    /** Model identifier, for display/diagnostics. */
    String model();

    <T> Optional<T> generate(String systemPrompt, String userContent, Class<T> responseType,
                             LlmEffort effort, long maxTokens);
}
