package com.jelly.farmhelper.fabric.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure hotbar weapon classification for sea-creature combat.
 */
public final class FishingWeaponMatcher {
    public enum WeaponClass {
        HYPERION,
        FIRE_VEIL,
        FROZEN_SCYTHE,
        SWORD,
        ROD,
        UNKNOWN
    }

    private FishingWeaponMatcher() {
    }

    public static WeaponClass classify(String displayName, String loreOrMeta) {
        String n = clean(displayName);
        String m = clean(loreOrMeta);
        String blob = n + " " + m;
        if (blob.contains("hyperion") || blob.contains("scylla") || blob.contains("astraea") || blob.contains("valkyrie")) {
            return WeaponClass.HYPERION;
        }
        if (blob.contains("fire veil")) {
            return WeaponClass.FIRE_VEIL;
        }
        if (blob.contains("frozen scythe") || blob.contains("ice spray")) {
            return WeaponClass.FROZEN_SCYTHE;
        }
        if (blob.contains("fishing rod") || n.endsWith(" rod") || n.contains("rod of the sea") || n.contains("challenging rod")) {
            return WeaponClass.ROD;
        }
        if (blob.contains("sword") || blob.contains("blade") || blob.contains("cleaver") || blob.contains("rapier")
                || blob.contains("livid dagger") || blob.contains("aspect of the")) {
            return WeaponClass.SWORD;
        }
        return WeaponClass.UNKNOWN;
    }

    public static int preferenceRank(WeaponClass weaponClass) {
        return switch (weaponClass) {
            case HYPERION -> 0;
            case FIRE_VEIL -> 1;
            case FROZEN_SCYTHE -> 2;
            case SWORD -> 3;
            case UNKNOWN -> 8;
            case ROD -> 9;
        };
    }

    /**
     * Preference rank from CSV order (0 = first token). Unknown classes fall back to {@link #preferenceRank}.
     */
    public static int preferenceRank(WeaponClass weaponClass, String preferenceCsv) {
        List<String> prefs = preferenceQueries(preferenceCsv);
        if (prefs.isEmpty()) {
            return preferenceRank(weaponClass);
        }
        String needle = switch (weaponClass) {
            case HYPERION -> "hyperion";
            case FIRE_VEIL -> "fire veil";
            case FROZEN_SCYTHE -> "frozen scythe";
            case SWORD -> "sword";
            case ROD -> "rod";
            case UNKNOWN -> "";
        };
        if (!needle.isEmpty()) {
            for (int i = 0; i < prefs.size(); i++) {
                if (prefs.get(i).contains(needle) || needle.contains(prefs.get(i))) {
                    return i;
                }
            }
        }
        return 100 + preferenceRank(weaponClass);
    }

    public static boolean usesRightClick(WeaponClass weaponClass) {
        return weaponClass == WeaponClass.HYPERION
                || weaponClass == WeaponClass.FIRE_VEIL
                || weaponClass == WeaponClass.FROZEN_SCYTHE;
    }

    public static boolean isRod(String displayName, String loreOrMeta) {
        return classify(displayName, loreOrMeta) == WeaponClass.ROD
                || clean(displayName).contains("rod");
    }

    public static List<String> preferenceQueries(String preferenceCsv) {
        List<String> out = new ArrayList<>();
        if (preferenceCsv == null || preferenceCsv.isBlank()) {
            out.add("hyperion");
            out.add("fire veil");
            out.add("frozen scythe");
            out.add("sword");
            return out;
        }
        for (String part : preferenceCsv.split(",")) {
            String token = clean(part);
            if (!token.isBlank()) {
                out.add(token);
            }
        }
        if (out.isEmpty()) {
            out.add("hyperion");
        }
        return out;
    }

    public static String primarySelectQuery(String preferenceCsv) {
        return preferenceQueries(preferenceCsv).get(0);
    }

    /**
     * Pick the best hotbar index for combat. Returns -1 when none qualify.
     */
    public static int pickBestHotbarIndex(List<String> displayNames, String preferenceCsv) {
        if (displayNames == null || displayNames.isEmpty()) {
            return -1;
        }
        int bestIndex = -1;
        int bestRank = Integer.MAX_VALUE;
        for (int i = 0; i < displayNames.size(); i++) {
            WeaponClass wc = classify(displayNames.get(i), "");
            if (wc == WeaponClass.ROD || wc == WeaponClass.UNKNOWN) {
                continue;
            }
            int rank = preferenceRank(wc, preferenceCsv);
            if (rank < bestRank) {
                bestRank = rank;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('§', ' ').trim();
    }
}
