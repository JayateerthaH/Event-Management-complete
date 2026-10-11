package com.event.aimodule.llm;

import java.util.Optional;

/** Used when no API key is configured: every AI call falls back to the non-AI path. */
public class DisabledLlmClient implements LlmClient {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String model() {
        return "none";
    }

    @Override
    public <T> Optional<T> generate(String systemPrompt, String userContent, Class<T> responseType,
                                    LlmEffort effort, long maxTokens) {
        return Optional.empty();
    }
}
