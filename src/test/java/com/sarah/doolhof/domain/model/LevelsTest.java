package com.sarah.doolhof.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.GameStatus;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;
import com.sarah.doolhof.domain.maze.Pathfinder;
import com.sarah.doolhof.domain.service.Autopilot;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LevelsTest {

    private static final double STEP = GameRules.STEP;

    static IntStream levelNumbers() {
        return IntStream.range(0, Levels.count());
    }

    @Test
    void levelsAreNumberedInOrderAndGetBigger() {
        for (int i = 0; i < Levels.count(); i++) {
            assertEquals(i, Levels.get(i).number());
        }
        for (int i = 2; i < Levels.count(); i++) {
            assertTrue(Levels.get(i).cols() > Levels.get(i - 1).cols());
        }
    }

    @ParameterizedTest
    @MethodSource("levelNumbers")
    void everythingIsPlacedOnReachableFloorAndNothingOverlaps(int number) {
        Level config = Levels.get(number);
        World world = WorldFactory.create(config);
        Maze maze = world.maze();
        int[] distance = Pathfinder.distancesFrom(maze, world.start());

        assertEquals(config.keys(), world.totalKeys());
        assertEquals(config.hunters(), world.hunters().size());

        Set<Cell> used = new HashSet<>();
        assertTrue(used.add(world.start()));
        assertTrue(used.add(world.door()), "door on the start tile");
        for (Cell key : world.keys()) {
            assertTrue(used.add(key), "two things on " + key);
            assertTrue(distance[maze.index(key)] > 5, "key right next to the start");
        }
        for (Hunter hunter : world.hunters()) {
            assertTrue(distance[maze.index(hunter.home())] > 12, "hunter starts too close: " + hunter.home());
        }
        for (Cell cell : used) {
            assertTrue(distance[maze.index(cell)] != Pathfinder.UNREACHABLE, cell + " cannot be reached");
        }
    }

    @ParameterizedTest
    @MethodSource("levelNumbers")
    void theSameLevelIsBuiltTheSameWayEveryTime(int number) {
        World first = WorldFactory.create(Levels.get(number));
        World second = WorldFactory.create(Levels.get(number));

        assertEquals(first.maze().toAscii(), second.maze().toAscii());
        assertEquals(first.keys(), second.keys());
        assertEquals(first.door(), second.door());
    }

    /**
     * Lets the autopilot play each level without hunters. If it reaches the
     * door, the level can be finished: keys are reachable, the door opens and
     * shifting walls never trap the player.
     */
    @ParameterizedTest
    @MethodSource("levelNumbers")
    void everyLevelCanBeFinished(int number) {
        World world = WorldFactory.create(Levels.get(number).withoutHunters());

        for (int step = 0; step < 600 / STEP; step++) {
            world.step(Autopilot.nextInput(world), STEP);
            if (world.status() == GameStatus.WON) {
                assertEquals(world.totalKeys(), world.keysCollected());
                return;
            }
        }
        fail("Not finished after ten minutes; player at " + world.player().cell()
            + ", keys left " + world.keys() + "\n" + world.maze().toAscii());
    }

    /**
     * Plays each level with hunters for three simulated minutes and checks
     * after every step that the world still makes sense.
     */
    @ParameterizedTest
    @MethodSource("levelNumbers")
    void theWorldStaysConsistentDuringALongGame(int number) {
        World world = WorldFactory.create(Levels.get(number));
        Maze maze = world.maze();

        for (int step = 0; step < 180 / STEP && world.status() != GameStatus.WON
            && world.status() != GameStatus.LOST; step++) {

            // Now and then: sprint, and fire a flare.
            Input auto = Autopilot.nextInput(world);
            Input input = new Input(auto.moveX(), auto.moveY(), step % 700 < 200, step % 3000 == 2999);
            world.step(input, STEP);

            assertFalse(Body.hitsWall(maze, world.player().x(), world.player().y()),
                "player inside a wall at step " + step);
            for (Hunter hunter : world.hunters()) {
                assertTrue(maze.isOpen(hunter.cell()), "hunter inside a wall at step " + step);
            }
            assertTrue(world.lives() >= 0 && world.lives() <= GameRules.START_LIVES);

            if (world.drainEvents().contains(GameEvent.WALLS_SHIFTED)) {
                assertTrue(Pathfinder.isFullyConnected(maze), "cut off after a shift\n" + maze.toAscii());
                assertTrue(maze.isOpen(world.door()));
                world.keys().forEach(key -> assertTrue(maze.isOpen(key)));
            }
        }
    }
}
