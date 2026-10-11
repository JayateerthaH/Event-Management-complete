package com.event.aimodule.feedback;

/**
 * Counts and averages are computed locally and always present; {@code insights} needs the LLM.
 */
public record FeedbackSummaryResponse(long responses, double averageStars, boolean aiAvailable,
                                      FeedbackInsights insights, String note) {
}
