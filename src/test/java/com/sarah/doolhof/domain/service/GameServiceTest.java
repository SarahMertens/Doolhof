package com.sarah.doolhof.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.GameStatus;
import com.sarah.doolhof.common.TileMemory;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.model.Progress;
import com.sarah.doolhof.dto.request.PlayerInputDTO;
import com.sarah.doolhof.dto.response.CellDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.HunterDTO;
import com.sarah.doolhof.exception.LevelLockedException;
import com.sarah.doolhof.exception.LevelNotFoundException;
import com.sarah.doolhof.exception.NoGameRunningException;
import com.sarah.doolhof.persistence.ProgressRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GameServiceTest {

    private static final PlayerInputDTO RIGHT = new PlayerInputDTO(1, 0, false, false);
    private static final PlayerInputDTO FLARE = new PlayerInputDTO(0, 0, false, true);

    /** One sixtieth of a second: a frame on most screens. */
    private static final double FRAME = 1.0 / 60;

    private ProgressRepository progressRepository;
    private LevelService levelService;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        // Everything unlocked, so each test can start the level it needs.
        startWith(new Progress(7, Map.of()));
    }

    private void startWith(Progress progress) {
        progressRepository = mock(ProgressRepository.class);
        when(progressRepository.load()).thenReturn(progress);
        levelService = new LevelService(progressRepository);
        gameService = new GameService(levelService);
    }

    @Test
    void nothingCanBeAskedBeforeALevelIsStarted() {
        assertThrows(NoGameRunningException.class, () -> gameService.getGame());
        assertThrows(NoGameRunningException.class, () -> gameService.advance(PlayerInputDTO.NONE, FRAME));
    }

    @Test
    void startLevelGivesTheLevelAtItsBeginning() {
        GameDTO game = gameService.startLevel(2);

        assertEquals(2, game.level().number());
        assertEquals("De jager", game.level().name());
        assertEquals(GameStatus.PLAYING, game.status());
        assertEquals(1.5, game.player().x());
        assertEquals(1.5, game.player().y());
        assertEquals(1, game.player().stamina());
        assertEquals(3, game.keys().size());
        assertEquals(3, game.totalKeys());
        assertEquals(0, game.keysCollected());
        assertEquals(1, game.hunters().size());
        assertEquals(GameRules.START_LIVES, game.lives());
        assertEquals(GameRules.START_LIVES, game.maxLives());
        assertEquals(2, game.flares());
        assertEquals(0, game.elapsed());
        assertFalse(game.doorOpen());
        assertFalse(game.shiftPending());
        assertTrue(game.events().isEmpty());
    }

    @Test
    void aLockedOrUnknownLevelCannotBeStarted() {
        startWith(new Progress());

        assertThrows(LevelLockedException.class, () -> gameService.startLevel(2));
        assertThrows(LevelNotFoundException.class, () -> gameService.startLevel(42));
        assertThrows(NoGameRunningException.class, () -> gameService.getGame(), "a refused start starts nothing");
    }

    @Test
    void theMazeInTheDtoMatchesTheLevel() {
        GameDTO game = gameService.startLevel(0);

        assertEquals(29, game.maze().cols());
        assertEquals(15, game.maze().rows());
        assertTrue(game.maze().isWall(0, 0));
        assertTrue(game.maze().isOpen(1, 1));
        assertTrue(game.maze().isWall(-1, 5), "outside the maze counts as wall");
        assertEquals(new CellDTO(27, 13), game.door());
        assertTrue(game.doorOpen(), "the original has no keys");
    }

    @Test
    void inTheDarkOnlyTheSurroundingsAreLitAndRemembered() {
        GameDTO game = gameService.startLevel(1);

        assertTrue(game.level().dark());
        assertTrue(game.maze().isLit(1, 1));
        assertEquals(TileMemory.FLOOR, game.maze().memoryOf(1, 1));
        assertEquals(TileMemory.WALL, game.maze().memoryOf(1, 0));

        int far = game.maze().cols() - 2;
        assertFalse(game.maze().isLit(far, game.maze().rows() - 2));
        assertEquals(TileMemory.UNKNOWN, game.maze().memoryOf(far, game.maze().rows() - 2));
        assertTrue(game.light().cornerCount() > 100);
    }

    @Test
    void advanceMovesThePlayerAtWalkingSpeed() {
        GameDTO start = gameService.startLevel(0);

        GameDTO game = start;
        for (int frame = 0; frame < 30; frame++) {
            game = gameService.advance(RIGHT, FRAME);
        }

        assertEquals(start.player().x() + GameRules.WALK_SPEED * 0.5, game.player().x(), 0.01);
        assertEquals(0.5, game.elapsed(), 1e-9);
    }

    @Test
    void theGameRunsTheSameAtAnyFrameRate() {
        gameService.startLevel(0);
        for (int frame = 0; frame < 30; frame++) {
            gameService.advance(RIGHT, 1.0 / 30);
        }
        double slowComputer = gameService.getGame().player().x();

        gameService.startLevel(0);
        for (int frame = 0; frame < 240; frame++) {
            gameService.advance(RIGHT, 1.0 / 240);
        }
        double fastComputer = gameService.getGame().player().x();

        assertEquals(slowComputer, fastComputer, 1e-9);
    }

    @Test
    void timeThatDoesNotFillAStepIsKeptForLater() {
        gameService.startLevel(0);

        GameDTO game = gameService.advance(RIGHT, GameRules.STEP * 0.6);
        assertEquals(0, game.elapsed(), "not enough time for a step yet");

        game = gameService.advance(RIGHT, GameRules.STEP * 0.6);
        assertEquals(GameRules.STEP, game.elapsed(), 1e-12);
    }

    @Test
    void aLongFreezeIsNotCaughtUp() {
        gameService.startLevel(0);

        GameDTO game = gameService.advance(RIGHT, 30);

        assertEquals(GameRules.MAX_CATCH_UP, game.elapsed(), GameRules.STEP);
    }

    @Test
    void aFlareIsFiredOnceAndReportedAsAnEvent() {
        gameService.startLevel(1);

        GameDTO game = gameService.advance(FLARE, FRAME);

        assertEquals(List.of(GameEvent.FLARE_FIRED), game.events());
        assertTrue(game.flareActive());
        assertEquals(0, game.flares(), "level 1 gives a single flare");
        assertTrue(gameService.advance(PlayerInputDTO.NONE, FRAME).events().isEmpty(), "events are handed out once");
    }

    @Test
    void aFlareAskedForInAVeryShortFrameIsNotLost() {
        gameService.startLevel(1);

        GameDTO tooShort = gameService.advance(FLARE, GameRules.STEP / 4);
        assertFalse(tooShort.flareActive());

        GameDTO game = gameService.advance(PlayerInputDTO.NONE, FRAME);
        assertTrue(game.flareActive());
    }

    @Test
    void getGameDoesNotLetTimePass() {
        gameService.startLevel(0);
        gameService.advance(RIGHT, FRAME);

        GameDTO first = gameService.getGame();
        GameDTO second = gameService.getGame();

        assertEquals(first.elapsed(), second.elapsed());
        assertEquals(first.player(), second.player());
    }

    @Test
    void aSnapshotDoesNotChangeWhenTheGameMovesOn() {
        GameDTO before = gameService.startLevel(0);
        double x = before.player().x();

        gameService.advance(RIGHT, FRAME);

        assertEquals(x, before.player().x());
        assertNotEquals(x, gameService.getGame().player().x());
    }

    @Test
    void huntersAreDescribedRelativeToThePlayer() {
        GameDTO game = gameService.startLevel(2);
        HunterDTO hunter = game.hunters().get(0);

        assertEquals(0, hunter.id());
        assertEquals(Math.hypot(hunter.x() - game.player().x(), hunter.y() - game.player().y()),
            hunter.distanceToPlayer(), 1e-9);
        assertEquals(hunter.distanceToPlayer(), game.nearestHunterDistance(), 1e-9);
        assertFalse(hunter.inLight(), "hunters start far away, in the dark");
        assertFalse(game.chased());
    }

    @Test
    void theDemoPlaysItselfAndIsNeverSaved() {
        GameDTO start = gameService.startDemo();
        assertTrue(start.hunters().isEmpty(), "the demo never ends in a chase");

        GameDTO game = start;
        for (int frame = 0; frame < 120; frame++) {
            game = gameService.advance(PlayerInputDTO.NONE, FRAME);
        }
        assertTrue(Math.hypot(game.player().x() - 1.5, game.player().y() - 1.5) > 2, "the demo walks on its own");

        game = playWithAutopilotUntilOver();
        assertEquals(GameStatus.WON, game.status());
        assertFalse(game.newRecord());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void winningALevelSavesTheTimeAndUnlocksTheNext() {
        startWith(new Progress());
        gameService.startLevel(0);

        List<GameEvent> events = new ArrayList<>();
        GameDTO game = walkTheOriginalMaze(events);

        assertEquals(GameStatus.WON, game.status());
        assertTrue(game.newRecord());
        assertTrue(events.contains(GameEvent.LEVEL_WON));

        ArgumentCaptor<Progress> saved = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(saved.capture());
        assertEquals(game.elapsed(), saved.getValue().bestTime(0).getAsDouble());
        assertEquals(game.elapsed(), levelService.getByNumber(0).bestTime().getAsDouble());

        double elapsed = game.elapsed();
        game = gameService.advance(RIGHT, FRAME);
        assertEquals(elapsed, game.elapsed(), "the clock stops when the level is won");
        assertTrue(game.newRecord(), "still shown on the result screen");
        verify(progressRepository).save(any());
    }

    @Test
    void restartingALevelStartsFromScratch() {
        gameService.startLevel(1);
        gameService.advance(FLARE, FRAME);

        GameDTO game = gameService.startLevel(1);

        assertEquals(0, game.elapsed());
        assertEquals(1, game.flares());
        assertFalse(game.flareActive());
    }

    /** Plays level 0 the way the screen would: knowing only what is in the DTO. */
    private GameDTO walkTheOriginalMaze(List<GameEvent> events) {
        GameDTO game = gameService.getGame();
        for (int frame = 0; frame < 60 * 600 && !game.status().isOver(); frame++) {
            game = gameService.advance(DtoPilot.towardsDoor(game), FRAME);
            events.addAll(game.events());
        }
        return game;
    }

    private GameDTO playWithAutopilotUntilOver() {
        GameDTO game = gameService.getGame();
        for (int frame = 0; frame < 60 * 600; frame++) {
            game = gameService.advance(PlayerInputDTO.NONE, FRAME);
            if (game.status().isOver()) {
                return game;
            }
        }
        return fail("the demo did not finish in ten minutes");
    }
}
