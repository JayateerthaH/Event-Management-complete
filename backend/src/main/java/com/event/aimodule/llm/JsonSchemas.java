package com.event.aimodule.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;

/**
 * JSON Schema for the AI response records (strings, booleans, numbers, enums, lists, nested records), for
 * providers that take a plain JSON Schema. Every field is required and no extra fields are allowed, so the
 * model's output always maps onto the record. Descriptions come from {@link JsonPropertyDescription}.
 */
public final class JsonSchemas {

    private JsonSchemas() {
    }

    public static Map<String, Object> of(Class<?> type) {
        if (!type.isRecord()) throw new IllegalArgumentException(type.getName() + " is not a record");
        return schema(type);
    }

    private static Map<String, Object> schema(Type type) {
        Map<String, Object> s = new LinkedHashMap<>();
        if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> raw
                && Collection.class.isAssignableFrom(raw)) {
            s.put("type", "array");
            s.put("items", schema(p.getActualTypeArguments()[0]));
            return s;
        }
        if (!(type instanceof Class<?> c)) throw new IllegalArgumentException("Unsupported type " + type);

        if (c == String.class) {
            s.put("type", "string");
        } else if (c == boolean.class || c == Boolean.class) {
            s.put("type", "boolean");
        } else if (c == int.class || c == Integer.class || c == long.class || c == Long.class) {
            s.put("type", "integer");
        } else if (c == double.class || c == Double.class || c == float.class || c == Float.class) {
            s.put("type", "number");
        } else if (c.isEnum()) {
            s.put("type", "string");
            s.put("enum", Arrays.stream(c.getEnumConstants()).map(e -> ((Enum<?>) e).name()).toList());
        } else if (c.isRecord()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (RecordComponent rc : c.getRecordComponents()) {
                Map<String, Object> prop = schema(rc.getGenericType());
                JsonPropertyDescription d = rc.getAnnotation(JsonPropertyDescription.class);
                if (d == null && rc.getAccessor() != null) d = rc.getAccessor().getAnnotation(JsonPropertyDescription.class);
                if (d != null) prop.put("description", d.value());
                properties.put(rc.getName(), prop);
            }
            s.put("type", "object");
            s.put("properties", properties);
            s.put("required", new ArrayList<>(properties.keySet()));
            s.put("additionalProperties", false);
        } else {
            throw new IllegalArgumentException("Unsupported type " + c.getName());
        }
        return s;
    }
}
