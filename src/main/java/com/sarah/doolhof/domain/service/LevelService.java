package com.sarah.doolhof.domain.service;

import com.sarah.doolhof.domain.model.Level;
import com.sarah.doolhof.domain.model.Levels;
import com.sarah.doolhof.domain.model.Progress;
import com.sarah.doolhof.dto.response.LevelDTO;
import com.sarah.doolhof.exception.LevelLockedException;
import com.sarah.doolhof.exception.ProgressStorageException;
import com.sarah.doolhof.persistence.ProgressRepository;
import java.util.List;
import java.util.logging.Logger;

public final class LevelService {

    private static final Logger LOGGER = Logger.getLogger(LevelService.class.getName());

    private final ProgressRepository progressRepository;
    private final Progress progress;

    public LevelService(ProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
        this.progress = progressRepository.load();
    }

    public List<LevelDTO> getAll() {
        return Levels.all().stream()
            .map(this::toDto)
            .toList();
    }

    public LevelDTO getByNumber(int number) {
        return toDto(Levels.get(number));
    }

    public int getFurthestUnlockedNumber() {
        return Math.min(progress.highestUnlocked(), Levels.count() - 1);
    }

    Level getUnlocked(int number) {
        Level level = Levels.get(number);
        if (!progress.isUnlocked(number)) {
            throw new LevelLockedException(number);
        }
        return level;
    }

    boolean recordWin(int number, double seconds) {
        boolean isRecord = progress.recordWin(number, seconds);

        try {
            progressRepository.save(progress);
        } catch (ProgressStorageException e) {
            // Saving is a convenience: the progress still counts for as long as the game is open.
            LOGGER.warning("Progress is kept for this session only: " + e.getMessage());
        }
        return isRecord;
    }

    LevelDTO toDto(Level level) {
        return new LevelDTO(
            level.number(),
            level.name(),
            level.hint(),
            level.keys(),
            level.hunters(),
            level.flares(),
            level.isDark(),
            level.lightRadius(),
            level.hasShiftingWalls(),
            level.shiftInterval(),
            progress.isUnlocked(level.number()),
            progress.bestTime(level.number()),
            Levels.isLast(level.number()));
    }
}
