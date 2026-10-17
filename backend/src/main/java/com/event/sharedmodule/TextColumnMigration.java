package com.event.sharedmodule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Widens free-text columns that older databases created as varchar(255) (or 300) to TEXT, so long event
 * descriptions, FAQ answers, bios etc. don't fail with "value too long". The entities now declare TEXT,
 * which covers new databases; ddl-auto=update never changes an existing column's type, hence this runner.
 * Idempotent: a column that is already TEXT, or doesn't exist, is skipped.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TextColumnMigration implements ApplicationRunner {

    private static final List<String[]> COLUMNS = List.of(
            new String[]{"events", "events", "description"},
            new String[]{"events", "about_event", "about"},
            new String[]{"events", "announcement", "announcement"},
            new String[]{"events", "event_category", "small_description"},
            new String[]{"events", "key_highlights", "small_description"},
            new String[]{"events", "faqs", "questions"},
            new String[]{"events", "faqs", "answers"},
            new String[]{"events", "feed_back", "feed_back"},
            new String[]{"events", "event_speakers", "short_experience"},
            new String[]{"events", "event_link", "url"},
            new String[]{"profile", "social_profile_update", "url_link"},
            new String[]{"profile", "profile_student", "bio"},
            new String[]{"profile", "student_profile", "bio"},
            new String[]{"profile", "badges", "badge_description"}
    );

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] c : COLUMNS) {
            Integer limited = jdbc.queryForObject(
                    "select count(*) from information_schema.columns where table_schema = ? and table_name = ?"
                            + " and column_name = ? and data_type = 'character varying'",
                    Integer.class, c[0], c[1], c[2]);
            if (limited == null || limited == 0) continue;
            try {
                // Names come from the constant list above, never from input
                jdbc.execute("alter table " + c[0] + "." + c[1] + " alter column " + c[2] + " type text");
                log.info("Widened {}.{}.{} to TEXT", c[0], c[1], c[2]);
            } catch (RuntimeException e) {
                log.warn("Could not widen {}.{}.{} to TEXT: {}", c[0], c[1], c[2], e.getMessage());
            }
        }
    }
}
