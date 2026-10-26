package dev.labtrace.ui;

import dev.labtrace.LabTraceClient;
import dev.labtrace.module.TelemetryModule;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minimal settings view for the passive telemetry module. */
public final class LabTraceScreen extends Screen {
    public LabTraceScreen() {
        super(Component.literal("LabTrace — Test Diagnostics"));
    }

    @Override
    protected void init() {
        TelemetryModule telemetry = LabTraceClient.telemetryModule();
        Button toggle = Button.builder(toggleLabel(telemetry), button -> {
            LabTraceClient.moduleManager().toggleByName("telemetry");
            button.setMessage(toggleLabel(telemetry));
        }).bounds(width / 2 - 100, height / 2 - 44, 200, 20).build();
        addRenderableWidget(toggle);

        Button csv = Button.builder(csvLabel(telemetry), button -> {
            telemetry.exportCsv().set(!telemetry.exportCsv().value());
            button.setMessage(csvLabel(telemetry));
            LabTraceClient.saveConfig();
        }).bounds(width / 2 - 100, height / 2 - 18, 200, 20).build();
        addRenderableWidget(csv);

        Button packets = Button.builder(packetLabel(telemetry), button -> {
            telemetry.logPacketTypes().set(!telemetry.logPacketTypes().value());
            button.setMessage(packetLabel(telemetry));
            LabTraceClient.saveConfig();
        }).bounds(width / 2 - 100, height / 2 + 8, 200, 20).build();
        addRenderableWidget(packets);

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(width / 2 - 100, height / 2 + 40, 200, 20).build());
    }

    private static Component toggleLabel(TelemetryModule module) {
        return Component.literal("Recorder: " + (module.enabled() ? "ON" : "OFF"));
    }

    private static Component csvLabel(TelemetryModule module) {
        return Component.literal("Movement CSV: " + (module.exportCsv().value() ? "ON" : "OFF"));
    }

    private static Component packetLabel(TelemetryModule module) {
        return Component.literal("Packet type log: " + (module.logPacketTypes().value() ? "ON" : "OFF"));
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }
}
