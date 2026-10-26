package dev.labtrace.module;

import com.google.gson.JsonObject;
import dev.labtrace.core.Module;
import dev.labtrace.core.Setting;
import dev.labtrace.event.LabEvent;
import dev.labtrace.event.PacketReceiveEvent;
import dev.labtrace.event.PacketSendEvent;
import dev.labtrace.event.TickEvent;
import dev.labtrace.logging.SessionLogger;
import dev.labtrace.telemetry.CsvRecorder;
import net.minecraft.client.Minecraft;

/** Example observational module. It never sends packets or changes the player's state. */
public final class TelemetryModule extends Module {
    private final Minecraft client;
    private final SessionLogger logger;
    private final CsvRecorder csvRecorder;
    private final Setting<Boolean> logPacketTypes = addSetting(
            new Setting<>("log_packet_types", true, Boolean.class));
    private final Setting<Boolean> exportCsv = addSetting(
            new Setting<>("export_movement_csv", false, Boolean.class));

    public TelemetryModule(Minecraft client, SessionLogger logger, CsvRecorder csvRecorder) {
        super("telemetry", "Passive Telemetry", "DIAGNOSTICS");
        this.client = client;
        this.logger = logger;
        this.csvRecorder = csvRecorder;
    }

    public Setting<Boolean> logPacketTypes() {
        return logPacketTypes;
    }

    public Setting<Boolean> exportCsv() {
        return exportCsv;
    }

    @Override
    public void onEvent(LabEvent event) {
        if (event instanceof TickEvent tickEvent) {
            if (exportCsv.value()) {
                csvRecorder.record(client, tickEvent.tick());
            }
        } else if (event instanceof PacketSendEvent packetEvent && logPacketTypes.value()) {
            logPacket("packet_send_observed", packetEvent.tick(), packetEvent.packetClass(), packetEvent.threadName());
        } else if (event instanceof PacketReceiveEvent packetEvent && logPacketTypes.value()) {
            logPacket("packet_receive_observed", packetEvent.tick(), packetEvent.packetClass(), packetEvent.threadName());
        }
    }

    private void logPacket(String eventName, long tick, String packetClass, String threadName) {
        JsonObject data = new JsonObject();
        data.addProperty("packet_class", packetClass);
        data.addProperty("thread", threadName);
        logger.log(eventName, id(), tick, data);
    }
}
