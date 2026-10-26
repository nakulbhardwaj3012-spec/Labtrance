package dev.labtrace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.labtrace.config.ConfigStore;
import dev.labtrace.core.ModuleManager;
import dev.labtrace.event.EventBus;
import dev.labtrace.event.TickEvent;
import dev.labtrace.logging.SessionLogger;
import dev.labtrace.module.TelemetryModule;
import dev.labtrace.telemetry.CsvRecorder;
import dev.labtrace.ui.LabTraceScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.chat.Component;

public final class LabTraceClient implements ClientModInitializer {
    private static final EventBus EVENTS = new EventBus();
    private static final AtomicLong TICK = new AtomicLong();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicReference<LabTraceClient> INSTANCE = new AtomicReference<>();

    private ConfigStore config;
    private SessionLogger logger;
    private ModuleManager moduleManager;
    private TelemetryModule telemetryModule;
    private KeyMapping guiKey;

    @Override
    public void onInitializeClient() {
        INSTANCE.set(this);
        Minecraft client = Minecraft.getInstance();
        Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();

        config = new ConfigStore(configDir);
        config.load();
        logger = new SessionLogger(gameDir.resolve("logs").resolve("labtrace"), config.sessionLabel());
        moduleManager = new ModuleManager(EVENTS, logger, TICK::get, GSON);
        CsvRecorder csvRecorder = new CsvRecorder(gameDir.resolve("logs").resolve("labtrace"), config.sessionLabel());
        telemetryModule = new TelemetryModule(client, logger, csvRecorder);
        moduleManager.register(telemetryModule);
        config.applyTo(moduleManager);
        moduleManager.setSaveCallback(this::saveConfig);
        config.save(moduleManager);

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("labtrace", "main"));
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.labtrace.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(currentClient -> {
            long tick = TICK.incrementAndGet();
            EVENTS.post(new TickEvent(tick));
            while (guiKey.consumeClick()) {
                currentClient.gui.setScreen(new LabTraceScreen());
            }
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String trimmed = message.trim();
            if (!trimmed.toLowerCase(java.util.Locale.ROOT).startsWith(".toggle ")) {
                return true;
            }
            String moduleName = trimmed.substring(".toggle ".length()).trim();
            boolean found = moduleManager.toggleByName(moduleName);
            if (client.player != null) {
                String result = found ? "Toggled module: " + moduleName : "Unknown module: " + moduleName;
                client.player.displayClientMessage(Component.literal("[LabTrace] " + result), false);
            }
            return false;
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveConfig();
            logger.close();
        }, "LabTrace-Shutdown"));

        logger.log("startup", "labtrace", TICK.get(), new com.google.gson.JsonObject());
        System.out.println("[LabTrace] Stage 1 loaded. Press Right Shift for diagnostics; use .toggle telemetry in chat.");
    }

    public static void post(Object event) {
        EVENTS.post(event);
    }

    public static long tick() {
        return TICK.get();
    }

    public static void handleModuleKey(int keyCode) {
        LabTraceClient instance = INSTANCE.get();
        if (instance != null && instance.moduleManager != null) {
            instance.moduleManager.handleKeyPressed(keyCode);
        }
    }

    public static ModuleManager moduleManager() {
        LabTraceClient instance = INSTANCE.get();
        if (instance == null || instance.moduleManager == null) {
            throw new IllegalStateException("LabTrace has not finished initializing");
        }
        return instance.moduleManager;
    }

    public static TelemetryModule telemetryModule() {
        LabTraceClient instance = INSTANCE.get();
        if (instance == null || instance.telemetryModule == null) {
            throw new IllegalStateException("LabTrace has not finished initializing");
        }
        return instance.telemetryModule;
    }

    public static void saveConfig() {
        LabTraceClient instance = INSTANCE.get();
        if (instance != null && instance.config != null && instance.moduleManager != null) {
            instance.config.save(instance.moduleManager);
        }
    }
}
