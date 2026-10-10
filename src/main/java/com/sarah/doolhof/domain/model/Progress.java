package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.config.GameRules;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

public final class Progress {

    private final Map<Integer, Double> bestTimes = new TreeMap<>();
    private int highestUnlocked;

    public Progress() {
        this(GameRules.ALWAYS_UNLOCKED_LEVEL, Map.of());
    }

    public Progress(int highestUnlocked, Map<Integer, Double> bestTimes) {
        this.highestUnlocked = Math.max(GameRules.ALWAYS_UNLOCKED_LEVEL, highestUnlocked);
        bestTimes.forEach((level, seconds) -> {
            if (level >= 0 && seconds > 0 && Double.isFinite(seconds)) {
                this.bestTimes.put(level, seconds);
            }
        });
    }

    public int highestUnlocked() {
        return highestUnlocked;
    }

    public boolean isUnlocked(int level) {
        return level <= highestUnlocked;
    }

    public OptionalDouble bestTime(int level) {
        Double seconds = bestTimes.get(level);
        return seconds == null ? OptionalDouble.empty() : OptionalDouble.of(seconds);
    }

    public Map<Integer, Double> bestTimes() {
        return Map.copyOf(bestTimes);
    }

    public boolean recordWin(int level, double seconds) {
        boolean isRecord = !bestTimes.containsKey(level) || seconds < bestTimes.get(level);
        if (isRecord) {
            bestTimes.put(level, seconds);
        }
        highestUnlocked = Math.max(highestUnlocked, level + 1);
        return isRecord;
    }
}
