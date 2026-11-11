package com.jelly.farmhelper.fabric.automation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Hypixel garden pest identification. Entity-type hints alone are never enough —
 * ambient bats / armor stands / displays must not light ESP without pest name or texture evidence.
 */
public class PestEntityHeuristics {
    private static final List<String> PEST_KEYWORDS = List.of(
            "beetle", "cricket", "earthworm", "fly", "locust", "mite",
            "mosquito", "moth", "rat", "slug", "praying mantis",
            "firefly", "dragonfly", "pest"
    );
    private static final List<String> NON_PEST_NAME_KEYWORDS = List.of(
            "pesthunter", "philip", "visitor", "npc", "jacob",
            "comparator", "repeater", "redstone", "hopper", "dropper", "dispenser"
    );
    private static final List<String> PEST_ENTITY_TYPE_KEYWORDS = List.of(
            "silverfish", "bat", "endermite", "spider", "cave_spider", "slime", "magma_cube", "armor_stand"
    );
    private static final List<String> HARD_REJECT_TYPE_KEYWORDS = List.of(
            "item_frame", "glow_item_frame", "item_display", "block_display", "text_display",
            "interaction", "marker", "experience_orb", "arrow", "firework", "painting",
            "minecart", "boat", "llama", "villager", "wandering_trader"
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

    /**
     * Pure scoring for unit tests (no Minecraft entity required).
     */
    public double scoreSignals(
            String name,
            String typeName,
            String tags,
            String equipment,
            boolean hasCustomName,
            boolean invisible
    ) {
        String n = nullToEmpty(name);
        String t = nullToEmpty(typeName);
        String tg = nullToEmpty(tags);
        String eq = nullToEmpty(equipment);

        if (containsAny(n, NON_PEST_NAME_KEYWORDS) || containsAny(eq, NON_PEST_NAME_KEYWORDS)) {
            return 0.0;
        }
        if (containsAny(t, HARD_REJECT_TYPE_KEYWORDS)) {
            return 0.0;
        }

        boolean nameHasKeyword = containsAny(n, PEST_KEYWORDS);
        boolean typeMatches = containsAny(t, PEST_ENTITY_TYPE_KEYWORDS);
        boolean tagsMatch = containsAny(tg, PEST_KEYWORDS);
        boolean equipmentHasKeyword = containsAny(eq, PEST_KEYWORDS);
        boolean textureSignature = containsAny(eq, TEXTURE_SIGNATURES);
        boolean strongEvidence = nameHasKeyword || textureSignature || equipmentHasKeyword;

        double score = 0.0;
        if (nameHasKeyword) {
            score += 8.0;
        }
        // Type is only a supporting signal once pest evidence exists.
        if (typeMatches && strongEvidence) {
            score += 7.5;
        } else if (typeMatches) {
            score += 1.0;
        }
        if (tagsMatch) {
            score += 3.5;
        }
        if (equipmentHasKeyword) {
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

        if (t.contains("armor_stand") && !textureSignature && !equipmentHasKeyword && !nameHasKeyword) {
            score = 0.0;
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
        String n = nullToEmpty(name);
        String t = nullToEmpty(typeName);
        String eq = nullToEmpty(equipment);
        if (containsAny(n, NON_PEST_NAME_KEYWORDS) || containsAny(eq, NON_PEST_NAME_KEYWORDS)) {
            return false;
        }
        if (containsAny(t, HARD_REJECT_TYPE_KEYWORDS)) {
            return false;
        }
        boolean nameHasKeyword = containsAny(n, PEST_KEYWORDS);
        boolean textureSignature = containsAny(eq, TEXTURE_SIGNATURES);
        boolean equipmentHasKeyword = containsAny(eq, PEST_KEYWORDS);
        // Ambient bats / bare silverfish / etc. never pass without pest evidence.
        if (!nameHasKeyword && !textureSignature && !equipmentHasKeyword) {
            return false;
        }
        return scoreSignals(name, typeName, tags, equipment, hasCustomName, invisible) >= 8.0;
    }

    public boolean isConfirmedPest(Entity entity) {
        if (entity == null) {
            return false;
        }
        String name = normalizedName(entity);
        String typeName = normalizedType(entity);
        String tags = commandTags(entity);
        String equipment = equipmentMetadata(entity);
        if (containsAny(name, NON_PEST_NAME_KEYWORDS) || containsAny(equipment, NON_PEST_NAME_KEYWORDS)) {
            return false;
        }
        if (containsAny(typeName, HARD_REJECT_TYPE_KEYWORDS)) {
            return false;
        }
        boolean nameHasKeyword = containsAny(name, PEST_KEYWORDS);
        boolean typeMatches = containsAny(typeName, PEST_ENTITY_TYPE_KEYWORDS);
        boolean tagsMatch = containsAny(tags, PEST_KEYWORDS);
        boolean equipmentHasKeyword = containsAny(equipment, PEST_KEYWORDS);
        boolean textureSignature = containsAny(equipment, TEXTURE_SIGNATURES);
        if (textureSignature) {
            return true;
        }
        if (nameHasKeyword && typeMatches) {
            return true;
        }
        return (nameHasKeyword || equipmentHasKeyword) && tagsMatch;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String haystack, List<String> needles) {
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
        return entity.getName().getString().toLowerCase(Locale.ROOT);
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
