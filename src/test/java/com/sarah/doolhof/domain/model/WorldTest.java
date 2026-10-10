package com.sarah.doolhof.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.GameStatus;
import com.sarah.doolhof.common.HunterState;
import com.sarah.doolhof.common.TileMemory;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WorldTest {

    private static final double STEP = GameRules.STEP;

    private static final Input RIGHT = new Input(1, 0, false, false);
    private static final Input LEFT = new Input(-1, 0, false, false);
    private static final Input DOWN = new Input(0, 1, false, false);
    private static final Input SPRINT_RIGHT = new Input(1, 0, true, false);
    private static final Input FLARE = new Input(0, 0, false, true);

    /** A straight corridor, 12 tiles long, from (1,1) to (12,1). */
    private static Maze corridor() {
        return Maze.fromAscii(
            "##############",
            "#            #",
            "##############");
    }

    private static Level config(int flares, double lightRadius) {
        return new Level(9, "Test", "", 0, 0, 1, 0, 0, flares, lightRadius, 0, 0, false);
    }

    private static World world(Maze maze, Cell door, List<Cell> keys, List<Cell> hunters) {
        return new World(config(1, 4), maze, new Cell(1, 1), door, keys, hunters, new Random(1));
    }

    private static void run(World world, Input input, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            world.step(input, STEP);
        }
    }

    @Test
    void thePlayerWalksAtWalkingSpeedAndStopsAtAWall() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of());

        run(world, RIGHT, 1);
        assertEquals(1.5 + GameRules.WALK_SPEED, world.player().x(), 0.01);

        run(world, LEFT, 3);
        assertEquals(1 + GameRules.BODY_HALF_SIZE, world.player().x(), 1e-6, "flush against the west wall");
        assertEquals(1.5, world.player().y(), 1e-9);
    }

    @Test
    void walkingDiagonallyIsNotFaster() {
        Maze room = Maze.fromAscii(
            "########",
            "#      #",
            "#      #",
            "#      #",
            "#      #",
            "########");
        World world = world(room, new Cell(6, 4), List.of(new Cell(6, 1)), List.of());

        run(world, new Input(1, 1, false, false), 0.5);

        double travelled = world.player().distanceTo(1.5, 1.5);
        assertEquals(GameRules.WALK_SPEED * 0.5, travelled, 0.01);
    }

    @Test
    void thePlayerIsNudgedRoundACornerIntoASideCorridor() {
        Maze maze = Maze.fromAscii(
            "#######",
            "#     #",
            "### ###",
            "### ###",
            "#######");
        World world = world(maze, new Cell(3, 3), List.of(new Cell(5, 1)), List.of());

        // Stop a little short of the opening, then only press "down".
        run(world, RIGHT, 0.40);
        assertTrue(world.player().x() < 3.5 && world.player().x() > 3.0);

        run(world, DOWN, 1.5);

        assertEquals(new Cell(3, 3), world.player().cell());
        assertEquals(4 - GameRules.BODY_HALF_SIZE, world.player().y(), 1e-6, "all the way down the side corridor");
    }

    @Test
    void sprintingIsFasterButEmptiesTheStaminaBar() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of());

        run(world, SPRINT_RIGHT, 1);
        assertEquals(1.5 + GameRules.SPRINT_SPEED, world.player().x(), 0.01);
        assertTrue(world.player().isSprinting());
        assertTrue(world.player().stamina() < 0.7);
    }

    @Test
    void anExhaustedPlayerMustRecoverBeforeSprintingAgain() {
        Maze longCorridor = Maze.fromAscii("#".repeat(40), "#" + " ".repeat(38) + "#", "#".repeat(40));
        World world = world(longCorridor, new Cell(38, 1), List.of(new Cell(38, 1)), List.of());

        run(world, SPRINT_RIGHT, 2.45);
        assertTrue(world.player().isExhausted());
        assertFalse(world.player().isSprinting(), "holding sprint with an empty bar is just walking");

        run(world, Input.NONE, 1);
        assertTrue(world.player().isExhausted(), "one second of rest is not enough");

        run(world, Input.NONE, 1);
        assertFalse(world.player().isExhausted());
    }

    @Test
    void theDoorOpensWhenTheLastKeyIsCollected() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(4, 1), new Cell(7, 1)), List.of());
        assertFalse(world.isDoorOpen());

        run(world, RIGHT, 0.8);
        assertEquals(1, world.keysCollected());
        assertFalse(world.isDoorOpen());
        assertEquals(List.of(GameEvent.KEY_COLLECTED), world.drainEvents());

        run(world, RIGHT, 0.8);
        assertEquals(2, world.keysCollected());
        assertTrue(world.isDoorOpen());
        assertEquals(List.of(GameEvent.KEY_COLLECTED, GameEvent.DOOR_OPENED), world.drainEvents());
        assertTrue(world.drainEvents().isEmpty(), "events are reported once");
    }

    @Test
    void aLockedDoorDoesNotLetThePlayerWin() {
        World world = world(corridor(), new Cell(3, 1), List.of(new Cell(12, 1)), List.of());

        run(world, RIGHT, 1);

        assertEquals(GameStatus.PLAYING, world.status());
    }

    @Test
    void reachingTheOpenDoorWinsAndStopsTheClock() {
        World world = world(corridor(), new Cell(5, 1), List.of(), List.of());
        assertTrue(world.isDoorOpen(), "no keys means the door is open from the start");

        run(world, RIGHT, 2);

        assertEquals(GameStatus.WON, world.status());
        assertTrue(world.drainEvents().contains(GameEvent.LEVEL_WON));

        double time = world.elapsed();
        assertTrue(time > 0.5 && time < 1.5, "took " + time);
        run(world, RIGHT, 1);
        assertEquals(time, world.elapsed(), "the clock stands still after winning");
    }

    @Test
    void aHunterThatSeesThePlayerChasesAndCatchesIt() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of(new Cell(7, 1)));

        world.step(Input.NONE, STEP);
        assertEquals(HunterState.CHASE, world.hunters().get(0).state());
        assertTrue(world.drainEvents().contains(GameEvent.HUNTER_ALERTED));

        run(world, Input.NONE, 2);

        assertEquals(GameRules.START_LIVES - 1, world.lives());
        assertTrue(world.drainEvents().contains(GameEvent.PLAYER_CAUGHT));
    }

    @Test
    void afterBeingCaughtEveryoneGoesBackAndThePlayerGetsABreather() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of(new Cell(7, 1)));
        run(world, RIGHT, 0.5);
        run(world, Input.NONE, 1.2);
        assertEquals(GameStatus.CAUGHT, world.status());

        while (world.status() == GameStatus.CAUGHT) {
            world.step(RIGHT, STEP);
        }

        assertEquals(GameStatus.PLAYING, world.status());
        assertTrue(world.isProtected());
        assertTrue(world.player().x() < 2.5, "back near the start");
        assertEquals(HunterState.PATROL, world.hunters().get(0).state(), "protected players are not seen");
    }

    @Test
    void losingTheLastLifeLosesTheLevel() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of(new Cell(7, 1)));

        run(world, Input.NONE, 40);

        assertEquals(0, world.lives());
        assertEquals(GameStatus.LOST, world.status());
        assertTrue(world.drainEvents().contains(GameEvent.LEVEL_LOST));
    }

    @Test
    void aHunterBehindAWallDoesNotNoticeAQuietPlayer() {
        Maze maze = Maze.fromAscii(
            "#########",
            "#   #   #",
            "# # # # #",
            "#       #",
            "#########");
        World world = world(maze, new Cell(7, 3), List.of(new Cell(7, 1)), List.of());
        Hunter hunter = new Hunter(new Cell(5, 1));

        assertFalse(hunter.canSee(maze, world.player().x(), world.player().y()));
        assertTrue(hunter.canSee(maze, 7.5, 1.5));
    }

    @Test
    void aHunterHearsASprintingPlayerNearby() {
        Maze maze = Maze.fromAscii(
            "###########",
            "#     #   #",
            "##### # # #",
            "#         #",
            "###########");
        World world = new World(config(0, 4), maze, new Cell(1, 1), new Cell(9, 1),
            List.of(new Cell(9, 3)), List.of(new Cell(4, 3)), new Random(4));

        // Walking: out of sight behind the wall, so nothing happens.
        world.step(new Input(1, 0, false, false), STEP);
        assertEquals(HunterState.PATROL, world.hunters().get(0).state());

        world.step(SPRINT_RIGHT, STEP);
        assertEquals(HunterState.SEARCH, world.hunters().get(0).state());
    }

    @Test
    void aFlareStunsNearbyHuntersAndLightsUpTheWholeLevel() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of(new Cell(7, 1)));
        assertEquals(TileMemory.UNKNOWN, world.memoryOf(12, 1), "the far end is in the dark");

        world.step(FLARE, STEP);

        assertEquals(0, world.flares());
        assertTrue(world.isFlareActive());
        assertEquals(HunterState.STUNNED, world.hunters().get(0).state());
        assertEquals(TileMemory.FLOOR, world.memoryOf(12, 1));
        assertEquals(TileMemory.WALL, world.memoryOf(13, 1));

        run(world, Input.NONE, 3);
        assertEquals(GameRules.START_LIVES, world.lives(), "a stunned hunter cannot catch anyone");
    }

    @Test
    void withoutFlaresNothingHappens() {
        World world = new World(config(0, 4), corridor(), new Cell(1, 1), new Cell(12, 1),
            List.of(new Cell(12, 1)), List.of(), new Random(1));

        world.step(FLARE, STEP);

        assertFalse(world.isFlareActive());
        assertTrue(world.drainEvents().isEmpty());
    }

    @Test
    void thePlayerOnlyRemembersWhatHasBeenInTheLight() {
        World world = world(corridor(), new Cell(12, 1), List.of(new Cell(12, 1)), List.of());

        assertEquals(TileMemory.FLOOR, world.memoryOf(3, 1));
        assertEquals(TileMemory.WALL, world.memoryOf(3, 0));
        assertEquals(TileMemory.UNKNOWN, world.memoryOf(9, 1));

        run(world, RIGHT, 1.5);
        assertEquals(TileMemory.FLOOR, world.memoryOf(9, 1));
        assertEquals(TileMemory.FLOOR, world.memoryOf(2, 1), "still remembered after walking on");
    }

    @Test
    void aLitLevelIsKnownFromTheStart() {
        World world = WorldFactory.create(Levels.get(0));

        for (Cell cell : world.maze().openCells()) {
            assertEquals(TileMemory.FLOOR, world.memoryOf(cell.col(), cell.row()));
        }
    }
}
