package com.jelly.farmhelper.fabric.failsafe;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure Jacob contest chat/scoreboard signal parsing (no Minecraft types).
 */
public final class JacobChatSignals {
    private static final Pattern MC_FORMATTING = Pattern.compile("§.");

    private JacobChatSignals() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return MC_FORMATTING.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isNoiseLine(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return true;
        }
        return normalized.contains("[npc] jacob")
                || normalized.contains("let me count the final results")
                || normalized.contains("didn't earn a medal")
                || normalized.contains("participation reward")
                || normalized.contains("claimed your rewards");
    }

    public static boolean isContestStart(String normalized) {
        if (isNoiseLine(normalized) || !normalized.contains("jacob")) {
            return false;
        }
        return normalized.contains("contest has started")
                || normalized.contains("jacob's contest has started")
                || normalized.contains("jacobs contest has started")
                || normalized.contains("new jacob contest")
                || normalized.contains("started a farming contest");
    }

    public static boolean isContestEnd(String normalized) {
        if (isNoiseLine(normalized)) {
            return false;
        }
        if (!(normalized.contains("jacob") || normalized.contains("contest"))) {
            return false;
        }
        return normalized.contains("contest has ended")
                || normalized.contains("contest is over")
                || normalized.contains("contest ended")
                || normalized.contains("farming contest ended")
                || normalized.contains("the contest is over");
    }

    /**
     * Scoreboard/tab aggregate: require Jacob + contest together (avoid bare "contest").
     */
    public static boolean isActiveFromHudAggregate(String aggregateNormalized) {
        if (aggregateNormalized == null || aggregateNormalized.isBlank()) {
            return false;
        }
        String n = aggregateNormalized.toLowerCase(Locale.ROOT);
        if (!(n.contains("jacob") || n.contains("jacob's"))) {
            return false;
        }
        return n.contains("contest") || n.contains("farming contest") || n.contains("jacob contest");
    }
}
