package com.event.aimodule.privacy;

import java.util.regex.Pattern;

/** Removes email addresses and phone numbers from any text before it is sent to an LLM. */
public final class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    // 8+ digits, optionally with +country code and common separators: "+91 98765 43210", "(080) 1234-5678"
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s().-]{6,}\\d");

    private PiiRedactor() {
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        String out = EMAIL.matcher(text).replaceAll("[email removed]");
        return PHONE.matcher(out).replaceAll(m ->
                m.group().replaceAll("\\D", "").length() >= 8 ? "[phone removed]" : m.group());
    }
}
