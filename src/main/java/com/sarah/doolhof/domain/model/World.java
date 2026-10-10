package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.GameStatus;
import com.sarah.doolhof.common.HunterState;
import com.sarah.doolhof.common.TileMemory;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;
import com.sarah.doolhof.domain.maze.MazeShifter;
import com.sarah.doolhof.domain.maze.ShiftPlan;
import com.sarah.doolhof.domain.maze.Visibility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class World {

    private final Level level;
    private final Maze maze;
    private final Cell start;
    private final Cell door;
    private final Player player;
    private final List<Hunter> hunters;
    private final List<Cell> keys;
    private final int totalKeys;
    private final Random random;
    private final TileMemory[] memory;
    private final List<GameEvent> events = new ArrayList<>();

    private GameStatus status = GameStatus.PLAYING;
    private Visibility.View view;
    private ShiftPlan pendingShift;
    private ShiftPlan lastShift = new ShiftPlan(List.of(), List.of());
    private double shiftCountdown;
    private double elapsed;
    private double caughtTimer;
    private double graceTimer;
    private double flareTimer;
    private int lives = GameRules.START_LIVES;
    private int flares;
    private boolean doorOpen;

    World(Level level, Maze maze, Cell start, Cell door, List<Cell> keys, List<Cell> hunterHomes, Random random) {
        this.level = level;
        this.maze = maze;
        this.start = start;
        this.door = door;
        this.keys = new ArrayList<>(keys);
        this.totalKeys = keys.size();
        this.random = random;
        this.player = new Player(start);
        this.hunters = hunterHomes.stream().map(Hunter::new).toList();
        this.memory = new TileMemory[maze.tileCount()];
        Arrays.fill(memory, TileMemory.UNKNOWN);
        this.flares = level.flares();
        this.shiftCountdown = level.shiftInterval();
        this.doorOpen = keys.isEmpty();

        look();
    }

    // --- The state of the level ------------------------------------------------

    public Level level() {
        return level;
    }

    public Maze maze() {
        return maze;
    }

    public Player player() {
        return player;
    }

    public List<Hunter> hunters() {
        return hunters;
    }

    public List<Cell> keys() {
        return List.copyOf(keys);
    }

    public int keysCollected() {
        return totalKeys - keys.size();
    }

    public int totalKeys() {
        return totalKeys;
    }

    public Cell start() {
        return start;
    }

    public Cell door() {
        return door;
    }

    public boolean isDoorOpen() {
        return doorOpen;
    }

    public GameStatus status() {
        return status;
    }

    public int lives() {
        return lives;
    }

    public int flares() {
        return flares;
    }

    public double elapsed() {
        return elapsed;
    }

    public Visibility.View view() {
        return view;
    }

    public TileMemory memoryOf(int col, int row) {
        return maze.contains(col, row) ? memory[maze.index(col, row)] : TileMemory.UNKNOWN;
    }

    public boolean isInPlayersLight(Body body) {
        return view.seen()[maze.index(body.cell())]
            && (!level.isDark() || body.distanceTo(player) <= level.lightRadius());
    }

    public boolean isFlareActive() {
        return flareTimer > 0;
    }

    public double flareStrength() {
        return Math.max(0, flareTimer / GameRules.FLARE_DURATION);
    }

    public boolean isProtected() {
        return graceTimer > 0;
    }

    public ShiftPlan pendingShift() {
        return pendingShift;
    }

    public ShiftPlan lastShift() {
        return lastShift;
    }

    public double shiftCountdown() {
        return shiftCountdown;
    }

    public double nearestHunterDistance() {
        return hunters.stream()
            .filter(hunter -> hunter.state() != HunterState.STUNNED)
            .mapToDouble(hunter -> hunter.distanceTo(player))
            .min()
            .orElse(Double.POSITIVE_INFINITY);
    }

    public boolean isPlayerChased() {
        return hunters.stream().anyMatch(hunter -> hunter.state() == HunterState.CHASE);
    }

    public List<GameEvent> drainEvents() {
        List<GameEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    // --- The rules ----------------------------------------------------------

    public void step(Input input, double dt) {
        if (status == GameStatus.CAUGHT) {
            caughtTimer -= dt;
            if (caughtTimer <= 0) {
                respawn();
            }
            return;
        }
        if (status != GameStatus.PLAYING) {
            return;
        }

        elapsed += dt;
        graceTimer = Math.max(0, graceTimer - dt);
        flareTimer = Math.max(0, flareTimer - dt);

        if (input.flare()) {
            fireFlare();
        }

        player.update(maze, input, dt);
        collectKeys();
        updateShiftingWalls(dt);
        updateHunters(dt);
        look();

        if (doorOpen && player.distanceTo(door.centerX(), door.centerY()) < GameRules.PICKUP_DISTANCE) {
            status = GameStatus.WON;
            events.add(GameEvent.LEVEL_WON);
        }
    }

    private void fireFlare() {
        if (flares == 0 || flareTimer > GameRules.FLARE_DURATION - GameRules.FLARE_COOLDOWN) {
            return;
        }

        flares--;
        flareTimer = GameRules.FLARE_DURATION;
        events.add(GameEvent.FLARE_FIRED);

        for (Hunter hunter : hunters) {
            if (hunter.distanceTo(player) <= GameRules.FLARE_STUN_RANGE) {
                hunter.stun(GameRules.FLARE_STUN_TIME);
            }
        }
    }

    private void collectKeys() {
        boolean collected = keys.removeIf(
            key -> player.distanceTo(key.centerX(), key.centerY()) < GameRules.PICKUP_DISTANCE);

        if (collected) {
            events.add(GameEvent.KEY_COLLECTED);
            if (keys.isEmpty() && !doorOpen) {
                doorOpen = true;
                events.add(GameEvent.DOOR_OPENED);
            }
        }
    }

    private void updateHunters(double dt) {
        for (Hunter hunter : hunters) {
            if (hunter.update(maze, player, isProtected(), random, dt)) {
                events.add(GameEvent.HUNTER_ALERTED);
            }

            boolean caught = !isProtected()
                && hunter.state() != HunterState.STUNNED
                && hunter.distanceTo(player) < GameRules.CATCH_DISTANCE;

            if (caught) {
                loseLife();
                return;
            }
        }
    }

    private void loseLife() {
        lives--;
        events.add(GameEvent.PLAYER_CAUGHT);

        if (lives == 0) {
            status = GameStatus.LOST;
            events.add(GameEvent.LEVEL_LOST);
        } else {
            status = GameStatus.CAUGHT;
            caughtTimer = GameRules.CAUGHT_PAUSE;
        }
    }

    private void respawn() {
        player.reset(start);
        hunters.forEach(Hunter::reset);
        graceTimer = GameRules.RESPAWN_GRACE_TIME;
        status = GameStatus.PLAYING;
        look();
    }

    private void updateShiftingWalls(double dt) {
        if (!level.hasShiftingWalls()) {
            return;
        }

        shiftCountdown -= dt;

        if (pendingShift == null && shiftCountdown <= GameRules.SHIFT_WARNING_TIME) {
            pendingShift = MazeShifter.plan(maze, random, level.shiftSize(), tilesThatMustStay());
            events.add(GameEvent.SHIFT_WARNING);
        }

        if (shiftCountdown <= 0) {
            lastShift = MazeShifter.apply(maze, pendingShift, tilesThatMustStay());
            pendingShift = null;
            shiftCountdown = level.shiftInterval();
            hunters.forEach(Hunter::forgetRoute);
            events.add(GameEvent.WALLS_SHIFTED);
        }
    }

    private Set<Cell> tilesThatMustStay() {
        Set<Cell> tiles = new HashSet<>();
        tiles.add(start);
        tiles.add(door);
        tiles.addAll(keys);

        addSurroundings(tiles, player.cell());
        for (Hunter hunter : hunters) {
            addSurroundings(tiles, hunter.cell());
            tiles.add(hunter.home());
        }
        return tiles;
    }

    private static void addSurroundings(Set<Cell> tiles, Cell centre) {
        for (int dRow = -GameRules.SHIFT_SAFE_RADIUS; dRow <= GameRules.SHIFT_SAFE_RADIUS; dRow++) {
            for (int dCol = -GameRules.SHIFT_SAFE_RADIUS; dCol <= GameRules.SHIFT_SAFE_RADIUS; dCol++) {
                tiles.add(new Cell(centre.col() + dCol, centre.row() + dRow));
            }
        }
    }

    private void look() {
        boolean everythingLit = !level.isDark() || isFlareActive();
        double radius = level.isDark() ? level.lightRadius() : Math.max(maze.cols(), maze.rows());

        view = Visibility.compute(maze, player.x(), player.y(), radius, GameRules.SIGHT_RAYS);

        for (int row = 0; row < maze.rows(); row++) {
            for (int col = 0; col < maze.cols(); col++) {
                int index = maze.index(col, row);
                if (everythingLit || view.seen()[index]) {
                    memory[index] = maze.isWall(col, row) ? TileMemory.WALL : TileMemory.FLOOR;
                }
            }
        }
    }
}
