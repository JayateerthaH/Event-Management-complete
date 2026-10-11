package com.event.aimodule.llm;

/** How much reasoning a request needs; maps to the provider's effort setting. */
public enum LlmEffort {
    LOW,     // short extraction tasks where latency matters (search parsing)
    MEDIUM   // drafting and summarizing
}
