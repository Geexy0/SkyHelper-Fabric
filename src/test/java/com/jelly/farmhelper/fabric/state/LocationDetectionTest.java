package com.jelly.farmhelper.fabric.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure location classification mirrors GameStateHandler.updateLocation scoreboard rules.
 * Keeps hub from being labeled garden when tablists mention the Garden warp.
 */
class LocationDetectionTest {
    enum Loc { GARDEN, BARN, HUB, OTHER }

    static Loc classify(String scoreboard, String tablist) {
        String aggregate = scoreboard == null ? "" : scoreboard.toLowerCase();
        String tabAggregate = tablist == null ? "" : tablist.toLowerCase();
        boolean scoreboardHub = aggregate.contains("hub")
                || aggregate.contains("village")
                || aggregate.contains("⏣ hub");
        boolean tabHub = tabAggregate.contains("area: hub")
                || tabAggregate.contains("area: village");
        boolean scoreboardGarden = aggregate.contains("garden") || aggregate.contains("⏣ garden");
        boolean scoreboardBarn = aggregate.contains("barn") || aggregate.contains("⏣ barn");
        if (scoreboardHub || tabHub) {
            return Loc.HUB;
        }
        if (scoreboardGarden) {
            return Loc.GARDEN;
        }
        if (scoreboardBarn) {
            return Loc.BARN;
        }
        return Loc.OTHER;
    }

    @Test
    void hubWithGardenWarpInTabIsStillHub() {
        assertEquals(Loc.HUB, classify("⏣ Hub", "Warps: Garden Barn Dungeon"));
    }

    @Test
    void gardenScoreboardWinsWithoutHub() {
        assertEquals(Loc.GARDEN, classify("⏣ Garden", "Plot - 12"));
    }

    @Test
    void tabOnlyGardenDoesNotCount() {
        assertEquals(Loc.OTHER, classify("SkyBlock", "Warps: Garden Hub"));
    }
}
