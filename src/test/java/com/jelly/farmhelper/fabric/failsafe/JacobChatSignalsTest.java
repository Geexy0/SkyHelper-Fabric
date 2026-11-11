package com.jelly.farmhelper.fabric.failsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacobChatSignalsTest {
    @Test
    void detectsContestStartVariants() {
        assertTrue(JacobChatSignals.isContestStart(
                JacobChatSignals.normalize("§eJacob's Contest has started!")
        ));
        assertTrue(JacobChatSignals.isContestStart(
                JacobChatSignals.normalize("A new Jacob contest is live")
        ));
    }

    @Test
    void detectsContestEndVariants() {
        assertTrue(JacobChatSignals.isContestEnd(
                JacobChatSignals.normalize("Jacob's Contest has ended!")
        ));
        assertTrue(JacobChatSignals.isContestEnd(
                JacobChatSignals.normalize("The farming contest ended")
        ));
    }

    @Test
    void ignoresNpcNoiseAndBareContest() {
        assertFalse(JacobChatSignals.isContestStart(
                JacobChatSignals.normalize("[NPC] Jacob: Let me count the final results!")
        ));
        assertFalse(JacobChatSignals.isActiveFromHudAggregate("garden contest plot 12"));
        assertTrue(JacobChatSignals.isActiveFromHudAggregate("jacob's contest 12m left"));
    }
}
