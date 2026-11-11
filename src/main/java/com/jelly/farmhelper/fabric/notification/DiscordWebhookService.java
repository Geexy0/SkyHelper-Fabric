package com.jelly.farmhelper.fabric.notification;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.failsafe.FailsafeType;
import com.jelly.farmhelper.fabric.feature.FeatureRuntimeState;
import com.jelly.farmhelper.fabric.macro.MacroState;
import com.jelly.farmhelper.fabric.runtime.RuntimeSnapshot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Local debug / status logger. Discord webhook / analytics phone-home removed.
 * Class name kept so existing call sites continue to compile without a mass rename.
 */
public class DiscordWebhookService {
    private static final long DEBUG_FLUSH_INTERVAL_MS = 3L * 60L * 1000L;
    private static final long DEBUG_MAX_FILE_BYTES = 9L * 1024L * 1024L;
    private static final long HEARTBEAT_INTERVAL_MS = 30_000L;
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Object debugLock = new Object();
    private long lastDebugFlushMs;
    private long lastHeartbeatMs;
    private final Path debugRootDir = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("farmhelper");
    private final Path debugArchiveDir = debugRootDir.resolve("archive");
    private final Path debugFile = debugRootDir.resolve("farmhelper-debug-current.log");

    public void onMacroToggled(boolean enabled) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.sendMacroEnableDisableLogs) {
            return;
        }
        sendLog("Macro " + (enabled ? "enabled" : "disabled"));
    }

    public void onFailsafeTriggered(FailsafeType type, String reason) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.sendFailsafeLogs) {
            return;
        }
        sendLog("Failsafe triggered: " + type.name() + " | " + reason);
    }

    public void onFailsafeCleared(FailsafeType type) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.sendFailsafeLogs) {
            return;
        }
        sendLog("Failsafe cleared: " + type.name());
    }

    public void sendStatusUpdate(MacroState state, long runtimeTicks, int stationaryTicks, String activeFailsafe) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.sendStatusUpdates) {
            return;
        }
        sendLog("Status | state=" + state + " runtime_ticks=" + runtimeTicks
                + " stationary_ticks=" + stationaryTicks + " failsafe=" + activeFailsafe);
    }

    public void onVisitorEvent(String message, boolean pingEveryone) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.sendVisitorsMacroLogs) {
            return;
        }
        sendLog("Visitors: " + message);
    }

    public void sendFeatureLog(String message) {
        sendLog("Feature: " + message);
        debugTrace("feature", message);
    }

    public void debugTrace(String source, String message) {
        appendDebugLine("INFO", source, message);
    }

    public void debugCritical(String source, String message) {
        appendDebugLine("CRITICAL", source, message);
    }

    public void tickDebug(RuntimeSnapshot snapshot, FeatureRuntimeState featureState, FarmHelperConfig config, int actionQueueSize) {
        if (snapshot == null || config == null || !config.writeDebugLogsToFile) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastHeartbeatMs >= HEARTBEAT_INTERVAL_MS) {
            lastHeartbeatMs = now;
            appendDebugLine("HEARTBEAT", "runtime", buildHeartbeat(snapshot, featureState, config, actionQueueSize));
        }
        if (now - lastDebugFlushMs >= DEBUG_FLUSH_INTERVAL_MS || isDebugFileNearLimit()) {
            flushDebugLog("periodic");
        }
    }

    public void sendCurrentDebugLog(String reason) {
        flushDebugLog(reason == null ? "manual" : reason);
    }

    private void sendLog(String message) {
        appendDebugLine("INFO", "log", message);
    }

    private void appendDebugLine(String level, String source, String message) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.writeDebugLogsToFile) {
            return;
        }
        String line = "[" + TS_FORMAT.format(Instant.now()) + "] "
                + "[" + sanitize(level) + "]"
                + "[" + sanitize(source) + "] "
                + sanitize(message);
        synchronized (debugLock) {
            writeDebugFileLine(line);
        }
    }

    private void flushDebugLog(String reason) {
        synchronized (debugLock) {
            lastDebugFlushMs = System.currentTimeMillis();
        }
        appendDebugLine("INFO", "flush", "debug flush (" + sanitize(reason) + ")");
    }

    private String buildHeartbeat(RuntimeSnapshot snapshot, FeatureRuntimeState featureState, FarmHelperConfig config, int actionQueueSize) {
        String failsafe = featureState == null || featureState.activeFailsafe == null || featureState.activeFailsafe.isEmpty()
                ? "none"
                : featureState.activeFailsafe.get().name();
        return String.format(Locale.US,
                "macro=%s state=%s rtTicks=%d pos=(%.2f,%.2f,%.2f) yaw=%.1f pitch=%.1f queue=%d screen=\"%s\" failsafe=%s nearSpawn=%s nearRewarp=%s "
                        + "vacuum[range=%.1f dps=%.0f cd=%.1fs] cfg[macroType=%s pattern=%s scheduler=%s visitors=%s pests=%s pestExchange=%s bazaar=%s autoSell=%s failsafe=%s rewarpPts=%d]",
                snapshot.macroToggled,
                snapshot.macroState,
                snapshot.macroRuntimeTicks,
                snapshot.posX, snapshot.posY, snapshot.posZ,
                snapshot.yaw, snapshot.pitch,
                actionQueueSize,
                sanitize(snapshot.screenTitle),
                failsafe,
                snapshot.nearSpawnPoint,
                snapshot.nearRewarpPoint,
                snapshot.vacuumRange,
                snapshot.vacuumDps,
                snapshot.vacuumTrackerCooldownSeconds,
                config.macroType,
                config.macroPattern,
                config.enableScheduler,
                config.visitorsMacro,
                config.enablePestsDestroyer,
                config.autoPestExchange,
                config.autoBazaar,
                config.enableAutoSell,
                config.enableFailsafes,
                config.rewarpPoints == null ? 0 : config.rewarpPoints.size()
        );
    }

    private boolean isDebugFileNearLimit() {
        try {
            if (!Files.exists(debugFile)) {
                return false;
            }
            return Files.size(debugFile) >= DEBUG_MAX_FILE_BYTES;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void writeDebugFileLine(String line) {
        try {
            Files.createDirectories(debugRootDir);
            Files.createDirectories(debugArchiveDir);
            if (Files.exists(debugFile) && Files.size(debugFile) >= DEBUG_MAX_FILE_BYTES) {
                rotateDebugFile();
            }
            Files.writeString(
                    debugFile,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            FarmHelperFabric.LOGGER.warn("Failed writing debug log file", e);
        }
    }

    private void rotateDebugFile() {
        try {
            if (!Files.exists(debugFile)) {
                return;
            }
            String suffix = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
            Path rotated = debugArchiveDir.resolve("farmhelper-debug-" + suffix + ".log");
            Files.move(debugFile, rotated, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            FarmHelperFabric.LOGGER.warn("Failed rotating debug log file", e);
        }
    }

    public Path getDebugLogDirectory() {
        return debugRootDir;
    }

    public Path getDebugLogFile() {
        return debugFile;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", " ").replace("\r", " ").trim();
    }
}
