package com.jelly.farmhelper.fabric.automation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Hypixel garden pest identification.
 * Ambient bats / vanilla mobs must never light ESP without an explicit pest species name or texture hash.
 */
public class PestEntityHeuristics {
    private static final Pattern FORMATTING = Pattern.compile("§.|(&[0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    /** Full garden pest species (matched with word boundaries). */
    private static final List<String> PEST_SPECIES = List.of(
            "beetle", "cricket", "earthworm", "earth worm", "fly", "locust", "mite",
            "mosquito", "moth", "rat", "slug", "praying mantis", "mantis",
            "firefly", "dragonfly"
    );

    private static final List<String> NON_PEST_NAME_KEYWORDS = List.of(
            "pesthunter", "philip", "visitor", "npc", "jacob",
            "comparator", "repeater", "redstone", "hopper", "dropper", "dispenser"
    );

    /** Underlying MC types Hypixel reuses for pests — never enough alone. */
    private static final List<String> REUSED_MOB_TYPES = List.of(
            "bat", "silverfish", "endermite", "spider", "cave_spider", "slime", "magma_cube"
    );

    private static final List<String> HARD_REJECT_TYPE_KEYWORDS = List.of(
            "item_frame", "glow_item_frame", "item_display", "block_display", "text_display",
            "interaction", "marker", "experience_orb", "arrow", "firework", "painting",
            "minecart", "boat", "llama", "villager", "wandering_trader"
    );

    private static final List<String> VANILLA_DISPLAY_NAMES = List.of(
            "bat", "silverfish", "endermite", "spider", "cave spider", "slime", "magma cube", "armor stand"
    );

    // Legacy texture-signature fragments from the 1.8.9 implementation.
    private static final List<String> TEXTURE_SIGNATURES = List.of(
            "70a1e836bf1968b2", // beetle
            "a24c69f96ce55622", // cricket
            "6403ba4027a333d8", // earthworm
            "9d90e777826a5246", // fly
            "4b24a482a32db1ea", // locust
            "be6baf6431a9daa2", // mite
            "52a9fe05bc663efc", // mosquito
            "65485c4b34e5b547", // moth
            "a8abb471db0ab787", // rat
            "7a79d0fd677b5453", // slug
            "1e04bb6367caa4e8", // praying mantis
            "4ce79e90adf34718", // firefly
            "254aff4c0b2dce3a"  // dragonfly
    );

    public double score(Entity entity) {
        if (entity == null) {
            return 0.0;
        }
        return scoreSignals(
                normalizedName(entity),
                normalizedType(entity),
                commandTags(entity),
                equipmentMetadata(entity),
                entity.hasCustomName(),
                entity.isInvisible()
        );
    }

    public double scoreSignals(
            String name,
            String typeName,
            String tags,
            String equipment,
            boolean hasCustomName,
            boolean invisible
    ) {
        String n = clean(name);
        String t = clean(typeName);
        String tg = clean(tags);
        String eq = clean(equipment);

        if (containsAny(n, NON_PEST_NAME_KEYWORDS) || containsAny(eq, NON_PEST_NAME_KEYWORDS)) {
            return 0.0;
        }
        if (containsAny(t, HARD_REJECT_TYPE_KEYWORDS)) {
            return 0.0;
        }
        if (isBareReusedMob(n, t) && !hasTextureSignature(eq)) {
            return 0.0;
        }

        boolean speciesName = hasPestSpecies(n);
        boolean speciesTags = hasPestSpecies(tg);
        boolean speciesEquip = hasPestSpecies(eq);
        boolean textureSignature = hasTextureSignature(eq);
        boolean reusedType = containsAny(t, REUSED_MOB_TYPES) || t.contains("armor_stand");
        boolean strongEvidence = speciesName || textureSignature || speciesEquip;

        if (!strongEvidence) {
            return 0.0;
        }

        double score = 0.0;
        if (speciesName) {
            score += 8.0;
        }
        if (reusedType && strongEvidence) {
            score += 7.5;
        }
        if (speciesTags) {
            score += 3.5;
        }
        if (speciesEquip) {
            score += 4.0;
        }
        if (textureSignature) {
            score += 12.0;
        }
        if (hasCustomName && strongEvidence) {
            score += 1.0;
        }
        if (invisible && strongEvidence) {
            score += 0.6;
        }
        return score;
    }

    public boolean isLikelyPest(Entity entity) {
        if (entity == null) {
            return false;
        }
        return isLikelyPestSignals(
                normalizedName(entity),
                normalizedType(entity),
                commandTags(entity),
                equipmentMetadata(entity),
                entity.hasCustomName(),
                entity.isInvisible()
        );
    }

    public boolean isLikelyPestSignals(
            String name,
            String typeName,
            String tags,
            String equipment,
            boolean hasCustomName,
            boolean invisible
    ) {
        return scoreSignals(name, typeName, tags, equipment, hasCustomName, invisible) >= 8.0;
    }

    /**
     * ESP-only gate: stricter than hunting heuristics. Bare bats / vanilla names never pass.
     */
    public boolean isEspPest(Entity entity) {
        if (entity == null) {
            return false;
        }
        return isEspPestSignals(
                normalizedName(entity),
                normalizedType(entity),
                equipmentMetadata(entity)
        );
    }

    public boolean isEspPestSignals(String name, String typeName, String equipment) {
        String n = clean(name);
        String t = clean(typeName);
        String eq = clean(equipment);
        if (containsAny(n, NON_PEST_NAME_KEYWORDS) || containsAny(eq, NON_PEST_NAME_KEYWORDS)) {
            return false;
        }
        if (containsAny(t, HARD_REJECT_TYPE_KEYWORDS)) {
            return false;
        }
        // Bare reused mobs (hub bats, silverfish, etc.) never pass — even if equipment NBT
        // accidentally contains a texture-hash substring.
        if (isBareReusedMob(n, t)) {
            return false;
        }
        if (hasTextureSignature(eq)) {
            return true;
        }
        // Must be an explicit pest species name (not generic "pest", not vanilla "Bat").
        return hasPestSpecies(n);
    }

    public boolean isConfirmedPest(Entity entity) {
        if (entity == null) {
            return false;
        }
        String name = normalizedName(entity);
        String typeName = normalizedType(entity);
        String tags = commandTags(entity);
        String equipment = equipmentMetadata(entity);
        String n = clean(name);
        String t = clean(typeName);
        String tg = clean(tags);
        String eq = clean(equipment);
        if (containsAny(n, NON_PEST_NAME_KEYWORDS) || containsAny(eq, NON_PEST_NAME_KEYWORDS)) {
            return false;
        }
        if (containsAny(t, HARD_REJECT_TYPE_KEYWORDS)) {
            return false;
        }
        if (hasTextureSignature(eq)) {
            return true;
        }
        if (isBareReusedMob(n, t)) {
            return false;
        }
        boolean speciesName = hasPestSpecies(n);
        boolean reusedType = containsAny(t, REUSED_MOB_TYPES) || t.contains("armor_stand");
        if (speciesName && reusedType) {
            return true;
        }
        return speciesName && hasPestSpecies(tg);
    }

    private static boolean isBareReusedMob(String cleanedName, String cleanedType) {
        boolean reused = containsAny(cleanedType, REUSED_MOB_TYPES);
        if (!reused) {
            return false;
        }
        if (hasPestSpecies(cleanedName)) {
            return false;
        }
        // Vanilla / empty / generic labels on reused mobs are ambient false positives.
        if (cleanedName.isBlank() || VANILLA_DISPLAY_NAMES.contains(cleanedName)) {
            return true;
        }
        if (cleanedName.equals("pest") || cleanedName.startsWith("pest ")) {
            return true;
        }
        // Name equals the type token (e.g. "bat", "entity.minecraft.bat").
        for (String token : REUSED_MOB_TYPES) {
            if (cleanedName.equals(token) || cleanedName.endsWith("." + token) || cleanedName.endsWith(" " + token)) {
                return true;
            }
        }
        return !hasPestSpecies(cleanedName);
    }

    private static boolean hasPestSpecies(String cleaned) {
        if (cleaned == null || cleaned.isBlank()) {
            return false;
        }
        for (String species : PEST_SPECIES) {
            if (containsWord(cleaned, species)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTextureSignature(String cleanedEquipment) {
        return containsAny(cleanedEquipment, TEXTURE_SIGNATURES);
    }

    private static boolean containsWord(String haystack, String phrase) {
        if (haystack == null || phrase == null || phrase.isBlank()) {
            return false;
        }
        String pattern = "(^|[^a-z0-9])" + Pattern.quote(phrase.toLowerCase(Locale.ROOT)) + "([^a-z0-9]|$)";
        return Pattern.compile(pattern).matcher(haystack).find();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String stripped = FORMATTING.matcher(value).replaceAll("");
        return stripped.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        if (haystack == null || haystack.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String commandTags(Entity entity) {
        try {
            Set<String> tags = java.util.Collections.emptySet();
            return String.join(" ", tags).toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String normalizedName(Entity entity) {
        return entity.getName().getString();
    }

    private String normalizedType(Entity entity) {
        return entity.getType().toString().toLowerCase(Locale.ROOT);
    }

    private String equipmentMetadata(Entity entity) {
        StringBuilder builder = new StringBuilder();
        for (ItemStack stack : reflectedEquipment(entity)) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            builder.append(' ').append(stack.getHoverName().getString().toLowerCase(Locale.ROOT));
            builder.append(' ').append(stack.toString().toLowerCase(Locale.ROOT));
            try {
                Method method = stack.getClass().getMethod("getComponents");
                Object value = method.invoke(stack);
                if (value != null) {
                    builder.append(' ').append(value.toString().toLowerCase(Locale.ROOT));
                }
            } catch (Throwable ignored) {
                // Best effort.
            }
            try {
                Method method = stack.getClass().getMethod("getNbt");
                Object value = method.invoke(stack);
                if (value != null) {
                    builder.append(' ').append(value.toString().toLowerCase(Locale.ROOT));
                }
            } catch (Throwable ignored) {
                // Best effort.
            }
        }
        return builder.toString();
    }

    private List<ItemStack> reflectedEquipment(Entity entity) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Method method : entity.getClass().getMethods()) {
            if (!method.getName().equals("getArmorItems")
                    && !method.getName().equals("getHandItems")
                    && !method.getName().equals("getItemsEquipped")) {
                continue;
            }
            try {
                Object value = method.invoke(entity);
                if (value instanceof Iterable<?> iterable) {
                    for (Object item : iterable) {
                        if (item instanceof ItemStack stack) {
                            stacks.add(stack);
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Best effort.
            }
        }
        return stacks;
    }
}
