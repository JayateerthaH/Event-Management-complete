package com.event.eventmodule.api;

/** One piece of event feedback without any information about who wrote it. */
public record FeedbackEntry(long stars, String text) {
}
