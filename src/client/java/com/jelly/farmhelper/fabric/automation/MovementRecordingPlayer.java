package com.jelly.farmhelper.fabric.automation;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MovementRecordingPlayer {
    private record Step(
            boolean forward,
            boolean left,
            boolean backward,
            boolean right,
            boolean sneak,
            boolean sprint,
            boolean fly,
            boolean jump,
            boolean attack,
            float yaw,
            float pitch,
            int delayTicks
    ) {
    }

    private final Map<String, List<Step>> recordings = new HashMap<>();
    private final Random random = new Random();

    private boolean loaded;
    private boolean playing;
    private String activeRecording = "";
    private List<Step> activeSteps = List.of();
    private int stepIndex;
    private int stepTicks;
    private float yawOffset;

    public boolean isPlaying() {
        return playing;
    }

    public String activeRecordingName() {
        return activeRecording;
    }

    public boolean playRandomRecording(String pattern, Minecraft client) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        ensureLoaded();
        if (recordings.isEmpty()) {
            return false;
        }

        String normalized = pattern.toLowerCase(Locale.ROOT);
        List<Map.Entry<String, List<Step>>> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Step>> entry : recordings.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).contains(normalized) && !entry.getValue().isEmpty()) {
                candidates.add(entry);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }

        Map.Entry<String, List<Step>> selected = candidates.get(random.nextInt(candidates.size()));
        activeRecording = selected.getKey();
        activeSteps = selected.getValue();
        stepIndex = 0;
        stepTicks = 0;
        yawOffset = client.player == null || activeSteps.isEmpty() ? 0f : client.player.getYRot() - activeSteps.getFirst().yaw();
        playing = true;
        return true;
    }

    public void stop(Minecraft client) {
        releaseMovement(client);
        playing = false;
        activeRecording = "";
        activeSteps = List.of();
        stepIndex = 0;
        stepTicks = 0;
        yawOffset = 0f;
    }

    public void tick(Minecraft client) {
        if (!playing) {
            return;
        }
        if (client.player == null || client.options == null || activeSteps.isEmpty()) {
            stop(client);
            return;
        }
        if (stepIndex < 0 || stepIndex >= activeSteps.size()) {
            stop(client);
            return;
        }

        Step step = activeSteps.get(stepIndex);
        setKey(client.options.keyUp, step.forward());
        setKey(client.options.keyDown, step.backward());
        setKey(client.options.keyLeft, step.left());
        setKey(client.options.keyRight, step.right());
        setKey(client.options.keyShift, step.sneak());
        setKey(client.options.keySprint, step.sprint());
        setKey(client.options.keyJump, step.jump());
        setKey(client.options.keyAttack, step.attack());

        if (client.player.getAbilities().mayfly) {
            boolean wasFlying = client.player.getAbilities().flying;
            boolean wantFlying = step.fly();
            if (wasFlying != wantFlying) {
                client.player.getAbilities().flying = wantFlying;
                client.player.onUpdateAbilities();
            }
        }
        float targetYaw = Mth.wrapDegrees(step.yaw() + yawOffset);
        float targetPitch = Mth.clamp(step.pitch(), -90f, 90f);
        client.player.setYRot(approachAngle(client.player.getYRot(), targetYaw, 14f));
        client.player.setXRot(approach(client.player.getXRot(), targetPitch, 10f));

        stepTicks++;
        if (stepTicks >= Math.max(1, step.delayTicks())) {
            stepTicks = 0;
            stepIndex++;
            if (stepIndex >= activeSteps.size()) {
                stop(client);
            }
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        FabricLoader.getInstance().getModContainer(FarmHelperFabric.MOD_ID)
                .flatMap(container -> container.findPath("assets/farmhelperfabric/legacy/farmhelper/movrec"))
                .ifPresent(this::loadFromDirectory);
    }

    private void loadFromDirectory(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".movement"))
                    .forEach(this::loadSingleRecording);
        } catch (IOException e) {
            FarmHelperFabric.LOGGER.warn("Failed to load movement recordings from {}", root, e);
        }
    }

    private void loadSingleRecording(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            List<Step> steps = new ArrayList<>();
            for (String line : lines) {
                Step step = parseStep(line);
                if (step != null) {
                    steps.add(step);
                }
            }
            if (!steps.isEmpty()) {
                String key = Objects.toString(path.getFileName(), path.toString());
                recordings.put(key, List.copyOf(steps));
            }
        } catch (IOException e) {
            FarmHelperFabric.LOGGER.debug("Could not read movement recording {}", path, e);
        }
    }

    private Step parseStep(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] split = raw.split(";");
        if (split.length < 12) {
            return null;
        }
        try {
            return new Step(
                    Boolean.parseBoolean(split[0].trim()),
                    Boolean.parseBoolean(split[1].trim()),
                    Boolean.parseBoolean(split[2].trim()),
                    Boolean.parseBoolean(split[3].trim()),
                    Boolean.parseBoolean(split[4].trim()),
                    Boolean.parseBoolean(split[5].trim()),
                    Boolean.parseBoolean(split[6].trim()),
                    Boolean.parseBoolean(split[7].trim()),
                    Boolean.parseBoolean(split[8].trim()),
                    Float.parseFloat(split[9].trim()),
                    Float.parseFloat(split[10].trim()),
                    Integer.parseInt(split[11].trim())
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void releaseMovement(Minecraft client) {
        if (client == null || client.options == null) {
            return;
        }
        setKey(client.options.keyUp, false);
        setKey(client.options.keyDown, false);
        setKey(client.options.keyLeft, false);
        setKey(client.options.keyRight, false);
        setKey(client.options.keyShift, false);
        setKey(client.options.keySprint, false);
        setKey(client.options.keyJump, false);
        setKey(client.options.keyAttack, false);
    }

    private void setKey(KeyMapping key, boolean pressed) {
        if (key != null) {
            key.setDown(pressed);
        }
    }

    private float approach(float current, float target, float maxStep) {
        float delta = target - current;
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, delta);
    }

    private float approachAngle(float current, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, delta);
    }
}
