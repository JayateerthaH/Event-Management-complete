package com.event.socialmodule.push;

/** What the service worker shows. {@code tag} replaces an earlier notification with the same tag. */
public record PushMessage(String title, String body, String url, String tag) {
}
