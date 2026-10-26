package dev.labtrace.telemetry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

/** Passively records player position and rotation; it does not simulate inputs or alter movement. */
public final class CsvRecorder {
    private static final String HEADER = "timestamp,tick,x,y,z,yaw,pitch,on_ground" + System.lineSeparator();
    private final Path file;

    public CsvRecorder(Path logDirectory, String sessionLabel) {
        String safeLabel = sessionLabel.replaceAll("[^A-Za-z0-9._-]", "_");
        this.file = logDirectory.resolve(safeLabel + "-movement.csv");
    }

    public synchronized void record(Minecraft client, long tick) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file) || Files.size(file) == 0L) {
                Files.writeString(file, HEADER, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            }
            String line = String.format(Locale.ROOT, "%s,%d,%.5f,%.5f,%.5f,%.3f,%.3f,%s%n",
                    csv(Instant.now().toString()), tick,
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.onGround());
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("[LabTrace] Could not write CSV telemetry: " + exception.getMessage());
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
