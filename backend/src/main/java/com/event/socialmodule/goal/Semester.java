package com.event.socialmodule.goal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Academic half-years: January–June and July–December, in Indian time. */
public record Semester(String key, String label, LocalDate start, LocalDate endExclusive) {

    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public static Semester of(LocalDate day) {
        int y = day.getYear();
        return day.getMonthValue() <= 6
                ? new Semester(y + "-1", "Jan–Jun " + y, LocalDate.of(y, 1, 1), LocalDate.of(y, 7, 1))
                : new Semester(y + "-2", "Jul–Dec " + y, LocalDate.of(y, 7, 1), LocalDate.of(y + 1, 1, 1));
    }

    public static Semester current() {
        return of(LocalDate.now(ZONE));
    }

    public Semester previous() {
        return of(start.minusDays(1));
    }

    public boolean contains(Date date) {
        if (date == null) return false;
        // getTime() works for java.sql.Date too, whose toInstant() throws
        LocalDate d = Instant.ofEpochMilli(date.getTime()).atZone(ZONE).toLocalDate();
        return !d.isBefore(start) && d.isBefore(endExclusive);
    }

    /** Last day of the semester (for display). */
    public LocalDate end() {
        return endExclusive.minusDays(1);
    }
}
