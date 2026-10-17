package com.event.sharedmodule;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default entity listener (registered for every entity in META-INF/orm.xml): before a row is saved, checks each
 * String field against its column length, so the user is told which field is too long and by how much instead
 * of the database rejecting the whole row with a generic error. Fields declared with a columnDefinition (TEXT)
 * or @Lob have no limit and are skipped; @Column without a length means JPA's default of 255.
 */
public class FieldLengthCheck {

    private record Limit(Field field, int max) {}

    private static final Map<Class<?>, List<Limit>> LIMITS = new ConcurrentHashMap<>();

    @PrePersist
    @PreUpdate
    public void check(Object entity) {
        for (Limit limit : LIMITS.computeIfAbsent(entity.getClass(), FieldLengthCheck::limitsOf)) {
            Object value;
            try {
                value = limit.field().get(entity);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (value instanceof String s && s.length() > limit.max()) {
                throw new FieldTooLongException(label(limit.field().getName()), s.length(), limit.max());
            }
        }
    }

    private static List<Limit> limitsOf(Class<?> type) {
        List<Limit> limits = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class || Modifier.isStatic(f.getModifiers())
                        || Modifier.isTransient(f.getModifiers()) || f.isAnnotationPresent(Transient.class)
                        || f.isAnnotationPresent(Lob.class)) continue;
                Column column = f.getAnnotation(Column.class);
                if (column != null && !column.columnDefinition().isBlank()) continue;
                f.setAccessible(true);
                limits.add(new Limit(f, column != null ? column.length() : 255));
            }
        }
        return limits;
    }

    /** costOfRegistration -> "Cost of registration" */
    static String label(String fieldName) {
        String words = fieldName.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
