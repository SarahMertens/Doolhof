package com.sarah.doolhof.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ProgressTest {

    @Test
    void aNewPlayerCanPlayTheFirstTwoLevels() {
        Progress progress = new Progress();

        assertTrue(progress.isUnlocked(0));
        assertTrue(progress.isUnlocked(1));
        assertFalse(progress.isUnlocked(2));
        assertTrue(progress.bestTime(1).isEmpty());
    }

    @Test
    void winningUnlocksTheNextLevelAndKeepsTheTime() {
        Progress progress = new Progress();

        assertTrue(progress.recordWin(1, 42.5), "the first time is always a record");

        assertTrue(progress.isUnlocked(2));
        assertFalse(progress.isUnlocked(3));
        assertEquals(42.5, progress.bestTime(1).getAsDouble());
    }

    @Test
    void onlyAFasterTimeReplacesTheBest() {
        Progress progress = new Progress();
        progress.recordWin(1, 40);

        assertFalse(progress.recordWin(1, 55));
        assertEquals(40, progress.bestTime(1).getAsDouble());

        assertTrue(progress.recordWin(1, 31.2));
        assertEquals(31.2, progress.bestTime(1).getAsDouble());
    }

    @Test
    void replayingAnEarlyLevelDoesNotLockLaterOnes() {
        Progress progress = new Progress();
        progress.recordWin(1, 40);
        progress.recordWin(2, 40);
        progress.recordWin(3, 40);

        progress.recordWin(1, 30);

        assertEquals(4, progress.highestUnlocked());
    }

    @Test
    void savedValuesThatMakeNoSenseAreIgnored() {
        Progress progress = new Progress(-5, Map.of(1, -3.0, 2, Double.NaN, 3, 12.5, -1, 9.0));

        assertEquals(1, progress.highestUnlocked(), "the first levels are always open");
        assertEquals(Map.of(3, 12.5), progress.bestTimes());
    }

    @Test
    void theListOfBestTimesCannotBeChangedFromOutside() {
        Progress progress = new Progress();
        progress.recordWin(1, 40);

        assertThrows(UnsupportedOperationException.class, () -> progress.bestTimes().put(1, 1.0));
    }
}
