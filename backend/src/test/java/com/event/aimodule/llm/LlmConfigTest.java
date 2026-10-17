package com.event.aimodule.llm;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LlmConfigTest {

    @Test
    void autoPrefersClaudeThenGeminiThenNone() {
        assertThat(LlmConfig.choose("auto", "sk-ant", "gm")).isEqualTo("claude");
        assertThat(LlmConfig.choose("auto", "", "gm")).isEqualTo("gemini");
        assertThat(LlmConfig.choose(null, null, null)).isEqualTo("none");
    }

    @Test
    void explicitProviderNeedsItsOwnKey() {
        assertThat(LlmConfig.choose("gemini", "sk-ant", "gm")).isEqualTo("gemini");
        assertThat(LlmConfig.choose(" Gemini ", "sk-ant", "")).isEqualTo("none");
        assertThat(LlmConfig.choose("claude", "", "gm")).isEqualTo("none");
        assertThat(LlmConfig.choose("none", "sk-ant", "gm")).isEqualTo("none");
        assertThat(LlmConfig.choose("openai", "sk-ant", "gm")).isEqualTo("none");
    }

    @Test
    void buildsTheChosenClient() {
        LlmConfig config = new LlmConfig();
        LlmClient gemini = config.llmClient("gemini", "", "", "claude-opus-5-5", "gm-key", "", "gemini-3.5-flash-lite", 30);
        assertThat(gemini).isInstanceOf(GeminiLlmClient.class);
        assertThat(gemini.model()).isEqualTo("gemini-3.5-flash-lite");
        assertThat(config.llmClient("auto", "", "", "m", "", "", "g", 30)).isInstanceOf(DisabledLlmClient.class);
    }
}
