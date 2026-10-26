package dev.labtrace.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.labtrace.core.ModuleManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Config is stored under config/labtrace/config.json. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private String sessionLabel = "lab-session";
    private JsonObject loadedRoot = new JsonObject();

    public ConfigStore(Path configDirectory) {
        this.path = configDirectory.resolve("labtrace").resolve("config.json");
    }

    public void load() {
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                loadedRoot = new JsonObject();
                return;
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            loadedRoot = GSON.fromJson(json, JsonObject.class);
            if (loadedRoot == null) {
                loadedRoot = new JsonObject();
            }
            if (loadedRoot.has("sessionLabel") && loadedRoot.get("sessionLabel").isJsonPrimitive()) {
                sessionLabel = sanitizeLabel(loadedRoot.get("sessionLabel").getAsString());
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[LabTrace] Could not load config; defaults will be used: " + exception.getMessage());
            loadedRoot = new JsonObject();
            sessionLabel = "lab-session";
        }
    }

    public String sessionLabel() {
        return sessionLabel;
    }

    public void setSessionLabel(String candidate) {
        this.sessionLabel = sanitizeLabel(candidate);
    }

    public void applyTo(ModuleManager manager) {
        JsonObject moduleJson = loadedRoot.has("modules") && loadedRoot.get("modules").isJsonObject()
                ? loadedRoot.getAsJsonObject("modules") : null;
        manager.loadFromJson(moduleJson);
    }

    public synchronized void save(ModuleManager manager) {
        JsonObject root = new JsonObject();
        root.addProperty("sessionLabel", sessionLabel);
        root.add("modules", manager.toJson());
        Path temp = path.resolveSibling("config.json.tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temp, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnavailable) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[LabTrace] Could not save config: " + exception.getMessage());
        }
    }

    private static String sanitizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "lab-session";
        }
        String cleaned = value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "lab-session" : cleaned.substring(0, Math.min(cleaned.length(), 64));
    }
}
