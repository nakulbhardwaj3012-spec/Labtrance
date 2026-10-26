package dev.labtrace.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.function.Predicate;

/** A validated, JSON-serializable module setting. */
public final class Setting<T> {
    private final String key;
    private final T defaultValue;
    private final Class<T> type;
    private final Predicate<T> validator;
    private T value;

    public Setting(String key, T defaultValue, Class<T> type, Predicate<T> validator) {
        this.key = requireKey(key);
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.type = Objects.requireNonNull(type, "type");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.value = defaultValue;
    }

    public Setting(String key, T defaultValue, Class<T> type) {
        this(key, defaultValue, type, ignored -> true);
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Setting key must not be blank");
        }
        return key;
    }

    public String key() {
        return key;
    }

    public T value() {
        return value;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public void set(T candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (!validator.test(candidate)) {
            throw new IllegalArgumentException("Rejected value for setting '" + key + "'");
        }
        value = candidate;
    }

    public JsonElement toJson(Gson gson) {
        return gson.toJsonTree(value, type);
    }

    public void load(JsonElement json, Gson gson) {
        if (json == null || json.isJsonNull()) {
            value = defaultValue;
            return;
        }
        T candidate = gson.fromJson(json, type);
        if (candidate != null && validator.test(candidate)) {
            value = candidate;
        } else {
            value = defaultValue;
        }
    }
}
