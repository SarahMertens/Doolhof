package com.sarah.doolhof.domain.service;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.model.Input;
import com.sarah.doolhof.domain.model.Level;
import com.sarah.doolhof.domain.model.Levels;
import com.sarah.doolhof.domain.model.World;
import com.sarah.doolhof.domain.model.WorldFactory;
import com.sarah.doolhof.dto.request.PlayerInputDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.exception.NoGameRunningException;
import java.util.ArrayList;
import java.util.List;

public final class GameService {

    private final LevelService levelService;

    private World world;
    private boolean demo;
    private boolean newRecord;
    private boolean flareRequested;
    private double unspentTime;

    public GameService(LevelService levelService) {
        this.levelService = levelService;
    }

    public GameDTO startLevel(int number) {
        Level level = levelService.getUnlocked(number);
        begin(level, false);
        return getGame();
    }

    public GameDTO startDemo() {
        begin(Levels.get(GameRules.DEMO_LEVEL).withoutHunters(), true);
        return getGame();
    }

    public GameDTO advance(PlayerInputDTO input, double seconds) {
        requireGame();

        Input move = GameMapper.toInput(input);
        // Remembered until a step is taken, so a key press is never lost in a very short frame.
        flareRequested |= move.flare();
        unspentTime += Math.clamp(seconds, 0, GameRules.MAX_CATCH_UP);

        List<GameEvent> events = new ArrayList<>();

        while (unspentTime >= GameRules.STEP) {
            unspentTime -= GameRules.STEP;

            world.step(demo ? Autopilot.nextInput(world) : move.withFlare(flareRequested), GameRules.STEP);
            flareRequested = false;

            List<GameEvent> happened = world.drainEvents();
            if (happened.contains(GameEvent.LEVEL_WON) && !demo) {
                newRecord = levelService.recordWin(world.level().number(), world.elapsed());
            }
            events.addAll(happened);
        }

        return snapshot(events);
    }

    public GameDTO getGame() {
        requireGame();
        return snapshot(List.of());
    }

    private void begin(Level level, boolean asDemo) {
        world = WorldFactory.create(level);
        demo = asDemo;
        newRecord = false;
        flareRequested = false;
        unspentTime = 0;
    }

    private GameDTO snapshot(List<GameEvent> events) {
        return GameMapper.toDto(world, levelService.toDto(world.level()), events, newRecord);
    }

    private void requireGame() {
        if (world == null) {
            throw new NoGameRunningException();
        }
    }
}
