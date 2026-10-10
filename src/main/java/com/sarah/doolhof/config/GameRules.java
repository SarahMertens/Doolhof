package com.sarah.doolhof.config;

public final class GameRules {

    private GameRules() {
    }

    // ── The simulation ──────────────────────────────────────────────────────

    /** The game advances in steps of this size, whatever the frame rate. */
    public static final double STEP = 1.0 / 120;

    /** After a hiccup (a dragged window, a slow frame) the game never catches up more than this. */
    public static final double MAX_CATCH_UP = 0.1;

    // ── The player ──────────────────────────────────────────────────────────

    public static final int START_LIVES = 3;

    /** Half the width of a body. Corridors are 1 wide, so this leaves room on both sides. */
    public static final double BODY_HALF_SIZE = 0.3;

    public static final double WALK_SPEED = 4.2;
    public static final double SPRINT_SPEED = 6.6;

    /** Share of the stamina bar used per second of sprinting. */
    public static final double STAMINA_DRAIN = 0.42;
    public static final double STAMINA_REGAIN = 0.2;

    /** After running the bar empty, sprinting is blocked until this much is back. */
    public static final double STAMINA_RECOVERED_AT = 0.3;

    public static final double PICKUP_DISTANCE = 0.55;

    /** Number of rays cast to work out what the player sees. */
    public static final int SIGHT_RAYS = 540;

    // ── Hunters ─────────────────────────────────────────────────────────────

    public static final double HUNTER_PATROL_SPEED = 2.4;
    public static final double HUNTER_SEARCH_SPEED = 3.6;

    /** Faster than the player walks, slower than the player sprints. */
    public static final double HUNTER_CHASE_SPEED = 5.0;

    public static final double HUNTER_SIGHT_RANGE = 7.5;
    public static final double HUNTER_HEARING_RANGE = 5.5;

    /** How long a hunter keeps looking around at the last known position. */
    public static final double HUNTER_SEARCH_TIME = 2.5;

    /** A chasing hunter re-plans its route this often. */
    public static final double HUNTER_REPLAN_INTERVAL = 0.25;

    public static final double CATCH_DISTANCE = 0.6;

    /** How long the game freezes after the player is caught. */
    public static final double CAUGHT_PAUSE = 1.6;

    /** After a respawn, hunters cannot see or catch the player for this long. */
    public static final double RESPAWN_GRACE_TIME = 2.5;

    // ── Flares ──────────────────────────────────────────────────────────────

    /** How long a flare lights up the whole level. */
    public static final double FLARE_DURATION = 6;
    public static final double FLARE_STUN_TIME = 3.5;
    public static final double FLARE_STUN_RANGE = 8;

    /** A second flare cannot be fired this soon after the previous one. */
    public static final double FLARE_COOLDOWN = 1;

    // ── Shifting walls ──────────────────────────────────────────────────────

    /** How long the walls rumble before they move. */
    public static final double SHIFT_WARNING_TIME = 3.5;

    /** Tiles this close to a body are never closed by a shift. */
    public static final int SHIFT_SAFE_RADIUS = 2;

    // ── Building a level ────────────────────────────────────────────────────

    /** Share of dead ends that are opened into loops, so hunters can be dodged. */
    public static final double MAZE_BRAID = 0.35;

    /** Hunters start at least this share of the longest route away from the player. */
    public static final double HUNTER_MIN_START_DISTANCE = 0.45;

    // ── Progress ────────────────────────────────────────────────────────────

    /** Levels up to and including this one are open from the first start. */
    public static final int ALWAYS_UNLOCKED_LEVEL = 1;

    /** Level that plays itself behind the menu. */
    public static final int DEMO_LEVEL = 4;
}
