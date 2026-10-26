package dev.labtrace.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Writes JSON Lines records with an ISO-8601 timestamp and session label. */
public final class SessionLogger implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);
    private final Path file;
    private final ExecutorService writer;
    private volatile String sessionLabel;

    public SessionLogger(Path logDirectory, String sessionLabel) {
        this.sessionLabel = Objects.requireNonNull(sessionLabel, "sessionLabel");
        String safeLabel = sessionLabel.replaceAll("[^A-Za-z0-9._-]", "_");
        this.file = logDirectory.resolve(safeLabel + "-" + FILE_TIME.format(Instant.now()) + ".jsonl");
        this.writer = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "LabTrace-LogWriter");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create LabTrace log directory: " + logDirectory, exception);
        }
        log("session_start", "labtrace", 0L, new JsonObject());
    }

    public Path file() {
        return file;
    }

    public void setSessionLabel(String sessionLabel) {
        this.sessionLabel = Objects.requireNonNull(sessionLabel, "sessionLabel");
    }

    public void log(String eventType, String module, long tick, JsonObject data) {
        JsonObject record = new JsonObject();
        record.addProperty("timestamp", Instant.now().toString());
        record.addProperty("session", sessionLabel);
        record.addProperty("event", eventType);
        record.addProperty("module", module);
        record.addProperty("tick", tick);
        record.add("data", data == null ? new JsonObject() : data.deepCopy());
        String line = GSON.toJson(record) + System.lineSeparator();
        writer.execute(() -> appendLine(line));
    }

    private void appendLine(String line) {
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("[LabTrace] Could not append log record: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        }
    }
}
