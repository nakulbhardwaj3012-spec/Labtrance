package dev.labtrace.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.labtrace.event.LabEvent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Base lifecycle for observational lab modules. Modules must not alter gameplay state. */
public abstract class Module {
    private final String id;
    private final String name;
    private final String category;
    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();
    private boolean enabled;
    private int keybind = -1;

    protected Module(String id, String name, String category) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.category = requireText(category, "category");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    protected final <T> Setting<T> addSetting(Setting<T> setting) {
        Objects.requireNonNull(setting, "setting");
        if (settings.putIfAbsent(setting.key(), setting) != null) {
            throw new IllegalArgumentException("Duplicate setting key: " + setting.key());
        }
        return setting;
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return name;
    }

    public final String category() {
        return category;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final int keybind() {
        return keybind;
    }

    public final void setKeybind(int keybind) {
        if (keybind < -1) {
            throw new IllegalArgumentException("Keybind must be -1 (unbound) or a non-negative key code");
        }
        this.keybind = keybind;
    }

    public final void setEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    final void applyEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public final Collection<Setting<?>> settings() {
        return settings.values();
    }

    public final JsonObject settingsToJson(Gson gson) {
        JsonObject result = new JsonObject();
        settings.forEach((key, setting) -> result.add(key, setting.toJson(gson)));
        return result;
    }

    public final void loadSettings(JsonObject json, Gson gson) {
        if (json == null) {
            return;
        }
        settings.forEach((key, setting) -> setting.load(json.get(key), gson));
    }

    public final JsonObject describe(Gson gson) {
        JsonObject result = new JsonObject();
        result.addProperty("id", id);
        result.addProperty("name", name);
        result.addProperty("category", category);
        result.addProperty("enabled", enabled);
        result.addProperty("keybind", keybind);
        JsonObject values = settingsToJson(gson);
        result.add("settings", values);
        return result;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public abstract void onEvent(LabEvent event);
}
