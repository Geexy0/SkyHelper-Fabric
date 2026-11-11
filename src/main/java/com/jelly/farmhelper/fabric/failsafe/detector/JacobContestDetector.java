package com.jelly.farmhelper.fabric.failsafe.detector;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.failsafe.FailsafeType;
import com.jelly.farmhelper.fabric.failsafe.JacobChatSignals;
import com.jelly.farmhelper.fabric.runtime.RuntimeSnapshot;

import java.util.Optional;

public class JacobContestDetector implements FailsafeDetector {
    private long lastProcessedChatSeq = -1L;

    @Override
    public FailsafeType type() {
        return FailsafeType.JACOB;
    }

    @Override
    public Optional<String> detect(RuntimeSnapshot snapshot, FarmHelperConfig config, DetectorState state) {
        if (!config.enableJacobFailsafe) {
            return Optional.empty();
        }
        if (state.latestChatSeq <= 0 || state.latestChatSeq == lastProcessedChatSeq) {
            return Optional.empty();
        }
        lastProcessedChatSeq = state.latestChatSeq;

        String raw = state.latestChatMessage == null ? "" : state.latestChatMessage;
        if (raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = JacobChatSignals.normalize(raw);
        if (!JacobChatSignals.isContestStart(normalized)) {
            return Optional.empty();
        }

        return Optional.of("Jacob contest event detected: " + raw);
    }
}
