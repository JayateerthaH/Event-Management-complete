package com.event.sharedmodule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate creates a CHECK constraint listing an enum column's values when it creates the table, and
 * ddl-auto=update never widens it, so adding an enum value would make inserts fail on an existing database.
 * The enums are validated in Java, so these constraints are dropped at startup. Idempotent; schema only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnumCheckMigration implements ApplicationRunner {

    /** schema, table, column: enum columns whose values may grow. */
    private static final List<String[]> COLUMNS = List.of(
            new String[]{"social", "buddy_notification", "type"},
            new String[]{"social", "buddy_request", "status"},
            new String[]{"social", "user_report", "status"}
    );

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] c : COLUMNS) {
            String constraint = c[1] + "_" + c[2] + "_check";   // Hibernate's naming
            try {
                Integer exists = jdbc.queryForObject("""
                        select count(*) from pg_constraint k join pg_class t on t.oid = k.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where n.nspname = ? and t.relname = ? and k.conname = ? and k.contype = 'c'""",
                        Integer.class, c[0], c[1], constraint);
                if (exists == null || exists == 0) continue;
                jdbc.execute("alter table " + c[0] + "." + c[1] + " drop constraint " + constraint);
                log.info("Dropped enum check {}.{}.{} so new values can be stored", c[0], c[1], constraint);
            } catch (RuntimeException e) {
                log.warn("Could not drop enum check {}.{}: {}", c[1], constraint, e.getMessage());
            }
        }
    }
}
