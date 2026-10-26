package dev.labtrace.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.labtrace.event.EventBus;
import dev.labtrace.event.LabEvent;
import dev.labtrace.logging.SessionLogger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Registers modules, dispatches events, and records lifecycle changes. */
public final class ModuleManager {
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final EventBus eventBus;
    private final SessionLogger logger;
    private final LongSupplier tickSupplier;
    private final Gson gson;
    private Runnable saveCallback = () -> { };

    public ModuleManager(EventBus eventBus, SessionLogger logger, LongSupplier tickSupplier, Gson gson) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");
        this.gson = Objects.requireNonNull(gson, "gson");
        this.eventBus.subscribe(LabEvent.class, this::dispatch);
    }

    public void setSaveCallback(Runnable callback) {
        this.saveCallback = Objects.requireNonNull(callback, "callback");
    }

    public void register(Module module) {
        Objects.requireNonNull(module, "module");
        String key = normalize(module.id());
        if (modules.putIfAbsent(key, module) != null) {
            throw new IllegalArgumentException("A module with id '" + module.id() + "' is already registered");
        }
    }

    public Collection<Module> all() {
        return ListSupport.copy(modules.values());
    }

    public Optional<Module> find(String nameOrId) {
        if (nameOrId == null) {
            return Optional.empty();
        }
        String key = normalize(nameOrId);
        Module byId = modules.get(key);
        if (byId != null) {
            return Optional.of(byId);
        }
        return modules.values().stream()
                .filter(module -> normalize(module.name()).equals(key))
                .findFirst();
    }

    public boolean toggleByName(String nameOrId) {
        Optional<Module> result = find(nameOrId);
        if (result.isEmpty()) {
            return false;
        }
        Module module = result.get();
        setEnabled(module, !module.enabled(), "command_or_keybind");
        return true;
    }

    public void handleKeyPressed(int keyCode) {
        if (keyCode < 0) {
            return;
        }
        for (Module module : modules.values()) {
            if (module.keybind() == keyCode) {
                setEnabled(module, !module.enabled(), "keybind");
            }
        }
    }

    public void setEnabled(Module module, boolean enabled, String cause) {
        boolean before = module.enabled();
        module.applyEnabled(enabled);
        if (before == module.enabled()) {
            return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("cause", cause);
        data.add("module_state", module.describe(gson));
        logger.log("module_state_change", module.id(), tickSupplier.getAsLong(), data);
        saveCallback.run();
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        for (Module module : modules.values()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.enabled());
            moduleJson.addProperty("keybind", module.keybind());
            moduleJson.add("settings", module.settingsToJson(gson));
            result.add(module.id(), moduleJson);
        }
        return result;
    }

    public void loadFromJson(JsonObject json) {
        if (json == null) {
            return;
        }
        for (Module module : modules.values()) {
            if (!json.has(module.id()) || !json.get(module.id()).isJsonObject()) {
                continue;
            }
            JsonObject state = json.getAsJsonObject(module.id());
            if (state.has("keybind")) {
                try {
                    module.setKeybind(state.get("keybind").getAsInt());
                } catch (RuntimeException ignored) {
                    module.setKeybind(-1);
                }
            }
            module.loadSettings(state.has("settings") && state.get("settings").isJsonObject()
                    ? state.getAsJsonObject("settings") : null, gson);
            module.setEnabledSilently(state.has("enabled") && state.get("enabled").getAsBoolean());
        }
    }

    private void dispatch(LabEvent event) {
        for (Module module : modules.values()) {
            if (module.enabled()) {
                module.onEvent(event);
            }
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class ListSupport {
        private static Collection<Module> copy(Collection<Module> values) {
            return java.util.List.copyOf(values);
        }
    }
}
