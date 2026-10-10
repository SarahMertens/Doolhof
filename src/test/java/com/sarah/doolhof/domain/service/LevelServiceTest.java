package com.sarah.doolhof.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sarah.doolhof.domain.model.Levels;
import com.sarah.doolhof.domain.model.Progress;
import com.sarah.doolhof.dto.response.LevelDTO;
import com.sarah.doolhof.exception.LevelLockedException;
import com.sarah.doolhof.exception.LevelNotFoundException;
import com.sarah.doolhof.exception.ProgressStorageException;
import com.sarah.doolhof.persistence.ProgressRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LevelServiceTest {

    private ProgressRepository progressRepository;
    private LevelService levelService;

    @BeforeEach
    void setUp() {
        progressRepository = mock(ProgressRepository.class);
        when(progressRepository.load()).thenReturn(new Progress());
        levelService = new LevelService(progressRepository);
    }

    /** The progress that was handed to the repository the last time it was asked to save. */
    private Progress lastSaved(int expectedSaves) {
        ArgumentCaptor<Progress> saved = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository, times(expectedSaves)).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void progressIsLoadedOnceAndReadingNeverSaves() {
        levelService.getAll();
        levelService.getByNumber(1);
        levelService.getFurthestUnlockedNumber();

        verify(progressRepository, times(1)).load();
        verify(progressRepository, never()).save(any());
    }

    @Test
    void getAllReturnsEveryLevelInOrder() {
        List<LevelDTO> levels = levelService.getAll();

        assertEquals(Levels.count(), levels.size());
        for (int i = 0; i < levels.size(); i++) {
            assertEquals(i, levels.get(i).number());
            assertEquals(Levels.get(i).name(), levels.get(i).name());
        }
        assertTrue(levels.get(levels.size() - 1).last());
        assertFalse(levels.get(0).last());
    }

    @Test
    void aNewPlayerSeesOnlyTheFirstTwoLevelsUnlocked() {
        List<LevelDTO> levels = levelService.getAll();

        assertTrue(levels.get(0).unlocked());
        assertTrue(levels.get(1).unlocked());
        assertTrue(levels.stream().skip(2).noneMatch(LevelDTO::unlocked));
        assertTrue(levels.stream().allMatch(level -> level.bestTime().isEmpty()));
        assertEquals(1, levelService.getFurthestUnlockedNumber());
    }

    @Test
    void theDtoDescribesTheLevel() {
        LevelDTO original = levelService.getByNumber(0);
        LevelDTO hardest = levelService.getByNumber(7);

        assertFalse(original.dark());
        assertFalse(original.shiftingWalls());
        assertEquals(0, original.hunters());

        assertTrue(hardest.dark());
        assertTrue(hardest.shiftingWalls());
        assertEquals(4, hardest.hunters());
        assertEquals(6, hardest.keys());
        assertEquals(15, hardest.shiftInterval());
    }

    @Test
    void savedProgressIsShown() {
        when(progressRepository.load()).thenReturn(new Progress(3, Map.of(2, 75.5)));
        levelService = new LevelService(progressRepository);

        assertTrue(levelService.getByNumber(3).unlocked());
        assertFalse(levelService.getByNumber(4).unlocked());
        assertEquals(75.5, levelService.getByNumber(2).bestTime().getAsDouble());
        assertEquals(3, levelService.getFurthestUnlockedNumber());
    }

    @Test
    void anUnknownLevelIsNotFound() {
        assertThrows(LevelNotFoundException.class, () -> levelService.getByNumber(-1));
        assertThrows(LevelNotFoundException.class, () -> levelService.getByNumber(Levels.count()));
        assertThrows(LevelNotFoundException.class, () -> levelService.getUnlocked(99));
    }

    @Test
    void aLockedLevelCannotBeFetchedForPlaying() {
        assertEquals(1, levelService.getUnlocked(1).number());
        assertThrows(LevelLockedException.class, () -> levelService.getUnlocked(2));
    }

    @Test
    void recordWinUnlocksTheNextLevelAndSaves() {
        assertTrue(levelService.recordWin(1, 50));

        assertTrue(levelService.getByNumber(2).unlocked());
        assertEquals(50, levelService.getByNumber(1).bestTime().getAsDouble());

        Progress saved = lastSaved(1);
        assertTrue(saved.isUnlocked(2));
        assertEquals(50, saved.bestTime(1).getAsDouble());
    }

    @Test
    void aSlowerTimeIsNotARecord() {
        levelService.recordWin(1, 50);

        assertFalse(levelService.recordWin(1, 60));
        assertEquals(50, lastSaved(2).bestTime(1).getAsDouble(), "every win is saved, the best time stays");
    }

    @Test
    void afterTheLastLevelTheMenuStillPointsAtAnExistingLevel() {
        for (int number = 1; number < Levels.count(); number++) {
            levelService.recordWin(number, 10);
        }

        assertEquals(Levels.count() - 1, levelService.getFurthestUnlockedNumber());
    }

    @Test
    void theGameCarriesOnWhenProgressCannotBeSaved() {
        doThrow(new ProgressStorageException("disk full", null)).when(progressRepository).save(any());

        assertTrue(levelService.recordWin(1, 12));

        assertTrue(levelService.getByNumber(2).unlocked(), "still remembered for this session");
    }
}
