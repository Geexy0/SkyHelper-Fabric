package com.jelly.farmhelper.fabric.fishing;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure name heuristics for sea creatures and trophy / golden fish.
 */
public final class FishingEntityHeuristics {
    private static final Pattern FORMATTING = Pattern.compile("§.|(&[0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    private static final List<String> SEA_CREATURES = List.of(
            "squid", "sea walker", "night squid", "sea guardian", "sea witch", "sea archer",
            "rider of the deep", "catfish", "carrot king", "sea leech", "guardian defender",
            "deep sea protector", "water hydra", "sea emperor", "agarimoo", "oasis rabbit",
            "oasis sheep", "water worm", "poisoned water worm", "zombie miner", "scarecrow",
            "nightmare", "werewolf", "phantom fisher", "grim reaper", "plhlegblast",
            "magma slug", "moogma", "lava leech", "pyroclastic worm", "lava flame",
            "fire eel", "taurus", "thunder", "lord jawbus", "great white shark",
            "tiger shark", "blue shark", "nurse shark", "shark"
    );

    private FishingEntityHeuristics() {
    }

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return FORMATTING.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isSeaCreature(String name) {
        String n = clean(name);
        if (n.isBlank() || n.contains("player") || n.contains("armor stand")) {
            return false;
        }
        for (String creature : SEA_CREATURES) {
            if (containsWord(n, creature)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGoldenFish(String name) {
        String n = clean(name);
        return n.contains("golden fish") || n.equals("goldenfish") || n.contains("gold fish");
    }

    public static boolean isTrophyLabel(String name, String extraCsv) {
        if (isGoldenFish(name)) {
            return true;
        }
        String n = clean(name);
        if (n.contains("trophy fish") || n.contains("sulphur") && n.contains("fish")) {
            return true;
        }
        if (extraCsv == null || extraCsv.isBlank()) {
            return false;
        }
        for (String part : extraCsv.split(",")) {
            String token = clean(part);
            if (!token.isBlank() && n.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static String seaCreatureNamesCsv() {
        return String.join(",", SEA_CREATURES);
    }

    private static boolean containsWord(String haystack, String phrase) {
        String pattern = "(^|[^a-z0-9])" + Pattern.quote(phrase) + "([^a-z0-9]|$)";
        return Pattern.compile(pattern).matcher(haystack).find();
    }
}
