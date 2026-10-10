package com.sarah.doolhof.presentation.render;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.GameStatus;
import com.sarah.doolhof.common.HunterState;
import com.sarah.doolhof.common.TileMemory;
import com.sarah.doolhof.dto.response.CellDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.HunterDTO;
import com.sarah.doolhof.dto.response.LightDTO;
import com.sarah.doolhof.dto.response.MazeDTO;
import com.sarah.doolhof.dto.response.PlayerDTO;
import com.sarah.doolhof.dto.response.WallShiftDTO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public final class WorldRenderer {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static final double DEFAULT_TILE = 48;
    private static final double FLARE_RING_TIME = 0.9;

    private final BufferedImage scene = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage lit = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage vignette = createVignette();
    private final Image doorImage;
    private final Camera camera = new Camera();
    private final Particles particles = new Particles();

    private double tile = DEFAULT_TILE;
    private double time;
    private double flareRing = FLARE_RING_TIME;
    private double caughtFlash;
    private double trailTimer;

    public WorldRenderer(Image doorImage) {
        this.doorImage = doorImage;
    }

    public void reset(GameDTO game) {
        MazeDTO maze = game.maze();

        // A lit level is shown as a whole; a dark level is seen from close by.
        tile = game.level().dark()
            ? DEFAULT_TILE
            : Math.min(DEFAULT_TILE, Math.min((double) WIDTH / maze.cols(), (double) HEIGHT / maze.rows()));

        particles.clear();
        flareRing = FLARE_RING_TIME;
        caughtFlash = 0;
        camera.snapTo(maze.cols(), maze.rows(), game.player().x(), game.player().y(), viewCols(), viewRows());
    }

    public void update(GameDTO game, double dt) {
        time += dt;
        flareRing += dt;
        caughtFlash = Math.max(0, caughtFlash - dt * 1.4);

        PlayerDTO player = game.player();

        for (GameEvent event : game.events()) {
            switch (event) {
                case KEY_COLLECTED -> particles.burst(player.x(), player.y(), 28, 4.5, 0.7, 0.14, Palette.KEY);
                case DOOR_OPENED -> {
                    particles.burst(game.door().centerX(), game.door().centerY(), 40, 3.5, 1.1, 0.14, Palette.DOOR_OPEN);
                    camera.addShake(0.25);
                }
                case FLARE_FIRED -> {
                    flareRing = 0;
                    camera.addShake(0.35);
                    particles.burst(player.x(), player.y(), 50, 9, 0.6, 0.12, Palette.FLARE);
                }
                case PLAYER_CAUGHT -> {
                    caughtFlash = 1;
                    camera.addShake(1);
                    particles.burst(player.x(), player.y(), 60, 6, 0.9, 0.16, Palette.PLAYER);
                }
                case SHIFT_WARNING -> camera.addShake(0.2);
                case WALLS_SHIFTED -> {
                    camera.addShake(0.8);
                    dustAt(game.maze(), game.lastShift());
                }
                case LEVEL_WON -> particles.burst(player.x(), player.y(), 80, 7, 1.2, 0.16, Palette.DOOR_OPEN);
                default -> { }
            }
        }

        // The ground keeps trembling while a shift is coming.
        if (game.shiftPending() && game.status() == GameStatus.PLAYING) {
            camera.addShake(dt * 0.9);
        }

        if (player.sprinting()) {
            trailTimer -= dt;
            if (trailTimer <= 0) {
                particles.burst(player.x(), player.y(), 2, 0.6, 0.45, 0.2, Palette.PLAYER);
                trailTimer = 0.03;
            }
        }

        particles.update(dt);
        camera.follow(game.maze().cols(), game.maze().rows(), player.x(), player.y(), viewCols(), viewRows(), dt);
    }

    private void dustAt(MazeDTO maze, WallShiftDTO shift) {
        for (CellDTO cell : shift.opening()) {
            if (maze.isKnown(cell)) {
                particles.burst(cell.centerX(), cell.centerY(), 14, 2.2, 0.9, 0.16, Palette.WALL_EDGE);
            }
        }
        for (CellDTO cell : shift.closing()) {
            if (maze.isKnown(cell)) {
                particles.burst(cell.centerX(), cell.centerY(), 14, 2.2, 0.9, 0.16, Palette.WARNING);
            }
        }
    }

    public void draw(Graphics2D g, GameDTO game) {
        double left = camera.left(viewCols());
        double top = camera.top(viewRows());
        boolean dark = game.level().dark();

        g.setColor(Palette.VOID);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        if (dark) {
            drawMemory(g, game, left, top);
        }

        // Outside the light nothing of the scene is shown, so in the dark
        // only the part around the player has to be drawn at all.
        boolean wholeScreen = !dark || game.flareActive();
        Rectangle area = wholeScreen ? new Rectangle(0, 0, WIDTH, HEIGHT) : lightBounds(game, left, top);

        drawScene(game, left, top, area);

        if (dark) {
            if (game.flareActive()) {
                // The flare lights everything, fading as it burns out.
                float strength = (float) Math.min(1, Math.sqrt(game.flareStrength()) * 1.1);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
                g.drawImage(scene, 0, 0, null);
                g.setComposite(AlphaComposite.SrcOver);
            }

            Rectangle light = lightBounds(game, left, top);
            cutOutLight(game, left, top, light);
            g.drawImage(lit, light.x, light.y, light.x + light.width, light.y + light.height,
                light.x, light.y, light.x + light.width, light.y + light.height, null);
            drawEyesInTheDark(g, game, left, top);
        } else {
            g.drawImage(scene, 0, 0, null);
        }

        drawShiftWarning(g, game, left, top);
        drawFlareRing(g, game, left, top);
        g.drawImage(vignette, 0, 0, null);

        if (caughtFlash > 0) {
            g.setColor(new Color(255, 30, 60, (int) (caughtFlash * 130)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    // --- Layer 1: memory -----------------------------------------------------

    private void drawMemory(Graphics2D g, GameDTO game, double left, double top) {
        for (int row = firstRow(top); row <= lastRow(top); row++) {
            for (int col = firstCol(left); col <= lastCol(left); col++) {
                TileMemory memory = game.maze().memoryOf(col, row);
                if (memory == TileMemory.UNKNOWN) {
                    continue;
                }
                g.setColor(memory == TileMemory.WALL ? Palette.MEMORY_WALL : Palette.MEMORY_FLOOR);
                g.fill(tileRect(col, row, left, top));
            }
        }
    }

    // --- Layer 2: the scene --------------------------------------------------

    private void drawScene(GameDTO game, double left, double top, Rectangle area) {
        Graphics2D g = scene.createGraphics();
        g.setClip(area);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawTiles(g, game.maze(), left, top);
        drawDoor(g, game, left, top);
        for (CellDTO key : game.keys()) {
            drawKey(g, key, left, top);
        }
        for (HunterDTO hunter : game.hunters()) {
            drawHunter(g, hunter, game.player(), left, top);
        }
        particles.draw(g, left, top, tile);
        drawPlayer(g, game, left, top);

        g.dispose();
    }

    private void drawTiles(Graphics2D g, MazeDTO maze, double left, double top) {
        for (int row = firstRow(top); row <= lastRow(top); row++) {
            for (int col = firstCol(left); col <= lastCol(left); col++) {
                if (!maze.contains(col, row)) {
                    continue;
                }

                Rectangle2D rect = tileRect(col, row, left, top);

                if (maze.isOpen(col, row)) {
                    g.setColor((col + row) % 2 == 0 ? Palette.FLOOR : Palette.FLOOR_ALT);
                    g.fill(rect);
                    continue;
                }

                g.setColor(Palette.WALL_TOP);
                g.fill(rect);

                // A darker strip at the bottom gives the wall some height
                // wherever there is floor in front of it.
                if (maze.isOpen(col, row + 1)) {
                    g.setColor(Palette.WALL_FRONT);
                    g.fill(new Rectangle2D.Double(rect.getX(), rect.getMaxY() - tile * 0.3, rect.getWidth(), tile * 0.3));
                }
                if (maze.isOpen(col, row - 1)) {
                    g.setColor(Palette.WALL_EDGE);
                    g.fill(new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), Math.max(2, tile * 0.06)));
                }
            }
        }
    }

    private void drawDoor(Graphics2D g, GameDTO game, double left, double top) {
        CellDTO door = game.door();
        double cx = screenX(door.centerX(), left);
        double cy = screenY(door.centerY(), top);
        double pulse = 0.5 + 0.5 * Math.sin(time * 4);

        Color glow = game.doorOpen() ? Palette.DOOR_OPEN : Palette.DOOR_LOCKED;
        double radius = tile * (game.doorOpen() ? 1.0 + 0.15 * pulse : 0.7);
        fillGlow(g, cx, cy, radius, glow, game.doorOpen() ? 0.55 : 0.3);

        int size = (int) Math.round(tile * 0.92);
        g.drawImage(doorImage, (int) Math.round(cx - size / 2.0), (int) Math.round(cy - size / 2.0), size, size, null);

        if (!game.doorOpen()) {
            // A red bar across the door: locked until every key is found.
            g.setColor(Palette.withAlpha(Palette.DOOR_LOCKED, 0.85));
            g.fill(new RoundRectangle2D.Double(cx - tile * 0.34, cy - tile * 0.06, tile * 0.68, tile * 0.12, 4, 4));
        }
    }

    private void drawKey(Graphics2D g, CellDTO key, double left, double top) {
        double bob = Math.sin(time * 3 + key.col() * 1.7 + key.row()) * tile * 0.06;
        double cx = screenX(key.centerX(), left);
        double cy = screenY(key.centerY(), top) + bob;
        double r = tile * 0.24;

        fillGlow(g, cx, cy, tile * 0.75, Palette.KEY, 0.5);

        Path2D diamond = new Path2D.Double();
        diamond.moveTo(cx, cy - r * 1.25);
        diamond.lineTo(cx + r, cy);
        diamond.lineTo(cx, cy + r * 1.25);
        diamond.lineTo(cx - r, cy);
        diamond.closePath();

        g.setColor(Palette.KEY);
        g.fill(diamond);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - r * 0.45, cy - r * 0.7, r * 0.4, r * 0.4));
    }

    private void drawHunter(Graphics2D g, HunterDTO hunter, PlayerDTO player, double left, double top) {
        double cx = screenX(hunter.x(), left);
        double cy = screenY(hunter.y(), top);
        double r = tile * 0.36;

        Color eye = switch (hunter.state()) {
            case CHASE -> Palette.HUNTER_EYE;
            case SEARCH -> Palette.HUNTER_SEARCH;
            case STUNNED -> Palette.HUNTER_STUNNED;
            case PATROL -> Palette.mix(Palette.HUNTER_EYE, Palette.HUNTER, 0.35);
        };

        if (hunter.state() == HunterState.CHASE) {
            fillGlow(g, cx, cy, tile * 1.1, Palette.HUNTER_EYE, 0.45);
        }

        // A ragged dark shape: a circle with a wobbling outline.
        Path2D body = new Path2D.Double();
        int points = 14;
        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            double wobble = 1 + 0.12 * Math.sin(time * 7 + i * 2.4 + hunter.id() * 1.9);
            double px = cx + Math.cos(angle) * r * wobble;
            double py = cy + Math.sin(angle) * r * wobble;
            if (i == 0) {
                body.moveTo(px, py);
            } else {
                body.lineTo(px, py);
            }
        }
        g.setColor(Palette.HUNTER);
        g.fill(body);
        g.setColor(Palette.withAlpha(eye, 0.5));
        g.setStroke(new BasicStroke((float) Math.max(1.5, tile * 0.04)));
        g.draw(body);

        if (hunter.state() == HunterState.STUNNED) {
            // Stars circling a dazed hunter.
            for (int i = 0; i < 3; i++) {
                double angle = time * 5 + i * 2 * Math.PI / 3;
                g.setColor(Palette.FLARE);
                g.fill(new Ellipse2D.Double(cx + Math.cos(angle) * r * 1.1 - 2.5, cy - r * 0.9 + Math.sin(angle) * r * 0.3 - 2.5, 5, 5));
            }
            return;
        }

        // Eyes that look at the player.
        double look = Math.atan2(player.y() - hunter.y(), player.x() - hunter.x());
        double ex = Math.cos(look) * r * 0.25;
        double ey = Math.sin(look) * r * 0.25;
        double eyeSize = r * (hunter.state() == HunterState.CHASE ? 0.42 : 0.32);
        g.setColor(eye);
        g.fill(new Ellipse2D.Double(cx - r * 0.32 + ex - eyeSize / 2, cy - r * 0.1 + ey - eyeSize / 2, eyeSize, eyeSize));
        g.fill(new Ellipse2D.Double(cx + r * 0.32 + ex - eyeSize / 2, cy - r * 0.1 + ey - eyeSize / 2, eyeSize, eyeSize));
    }

    private void drawPlayer(Graphics2D g, GameDTO game, double left, double top) {
        PlayerDTO player = game.player();
        double cx = screenX(player.x(), left);
        double cy = screenY(player.y(), top);
        double half = tile * player.halfSize();

        if (game.status() == GameStatus.CAUGHT) {
            half *= 0.5 + 0.5 * caughtFlash;
        }
        // Blink while protected after a respawn.
        if (player.isProtected() && (int) (time * 10) % 2 == 0) {
            return;
        }

        fillGlow(g, cx, cy, tile * 1.2, Palette.PLAYER, 0.45);

        double arc = half * 0.7;
        g.setColor(Palette.PLAYER);
        g.fill(new RoundRectangle2D.Double(cx - half, cy - half, half * 2, half * 2, arc, arc));
        g.setColor(Palette.PLAYER_CORE);
        g.fill(new RoundRectangle2D.Double(cx - half * 0.5, cy - half * 0.5, half, half, arc * 0.5, arc * 0.5));

        if (player.isProtected()) {
            g.setColor(Palette.withAlpha(Palette.PLAYER_CORE, 0.8));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(cx - half * 1.8, cy - half * 1.8, half * 3.6, half * 3.6));
        }
    }

    // --- Layer 3: light ------------------------------------------------------

    private Rectangle lightBounds(GameDTO game, double left, double top) {
        double radius = game.level().lightRadius() * tile * 1.05 + 4;
        double cx = screenX(game.player().x(), left);
        double cy = screenY(game.player().y(), top);

        return new Rectangle((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2))
            .intersection(new Rectangle(0, 0, WIDTH, HEIGHT));
    }

    private void cutOutLight(GameDTO game, double left, double top, Rectangle bounds) {
        LightDTO light = game.light();
        PlayerDTO player = game.player();
        MazeDTO maze = game.maze();

        Path2D area = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int i = 0; i < light.cornerCount(); i++) {
            double x = screenX(light.x(i), left);
            double y = screenY(light.y(i), top);
            if (i == 0) {
                area.moveTo(x, y);
            } else {
                area.lineTo(x, y);
            }
        }
        area.closePath();

        // A wall that is hit by a ray is lit as a whole tile. Lighting only the
        // sliver the ray reaches would make walls look like thin outlines.
        for (int row = firstRow(top); row <= lastRow(top); row++) {
            for (int col = firstCol(left); col <= lastCol(left); col++) {
                if (maze.contains(col, row) && maze.isWall(col, row) && maze.isLit(col, row)) {
                    area.append(tileRect(col, row, left, top), false);
                }
            }
        }

        // A candle-like flicker keeps the light alive.
        double flicker = 1 + 0.018 * Math.sin(time * 9) + 0.012 * Math.sin(time * 23);
        float radius = (float) (game.level().lightRadius() * tile * flicker);
        Point2D centre = new Point2D.Double(screenX(player.x(), left), screenY(player.y(), top));

        Graphics2D g = lit.createGraphics();
        g.setClip(bounds);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // First the mask: opaque near the player, transparent at the edge...
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new RadialGradientPaint(centre, radius,
            new float[] {0f, 0.55f, 0.85f, 1f},
            new Color[] {
                new Color(255, 255, 255, 255),
                new Color(255, 255, 255, 240),
                new Color(255, 255, 255, 120),
                new Color(255, 255, 255, 0)}));
        g.fill(area);

        // ...then the scene, kept only where the mask is: "source in".
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(scene, 0, 0, null);

        // A warm tint on top, so the light has a colour of its own.
        g.setComposite(AlphaComposite.SrcAtop);
        g.setPaint(new RadialGradientPaint(centre, radius,
            new float[] {0f, 1f},
            new Color[] {new Color(255, 214, 170, 46), new Color(255, 120, 220, 0)}));
        g.fill(area);
        g.dispose();
    }

    private void drawEyesInTheDark(Graphics2D g, GameDTO game, double left, double top) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (HunterDTO hunter : game.hunters()) {
            double distance = hunter.distanceToPlayer();
            if (hunter.inLight() || game.flareActive() || distance > 11 || hunter.state() == HunterState.STUNNED) {
                continue;
            }

            double alpha = Math.clamp(1.15 - distance / 11, 0, 1) * (0.75 + 0.25 * Math.sin(time * 6));
            double cx = screenX(hunter.x(), left);
            double cy = screenY(hunter.y(), top);
            double gap = tile * 0.12;
            double size = tile * 0.1;

            g.setColor(Palette.withAlpha(Palette.HUNTER_EYE, alpha));
            g.fill(new Ellipse2D.Double(cx - gap - size / 2, cy - size / 2, size, size));
            g.fill(new Ellipse2D.Double(cx + gap - size / 2, cy - size / 2, size, size));
        }
    }

    // --- Overlays ------------------------------------------------------------

    private void drawShiftWarning(Graphics2D g, GameDTO game, double left, double top) {
        if (!game.shiftPending()) {
            return;
        }
        WallShiftDTO plan = game.pendingShift();
        MazeDTO maze = game.maze();

        double pulse = 0.35 + 0.35 * Math.sin(time * 14);
        g.setStroke(new BasicStroke(3f));

        for (CellDTO cell : plan.closing()) {
            if (!maze.isKnown(cell)) {
                continue;
            }
            Rectangle2D rect = tileRect(cell.col(), cell.row(), left, top);
            g.setColor(Palette.withAlpha(Palette.WARNING, pulse * 0.6));
            g.fill(rect);
            g.setColor(Palette.withAlpha(Palette.WARNING, 0.9));
            g.draw(rect);
        }
        for (CellDTO cell : plan.opening()) {
            if (!maze.isKnown(cell)) {
                continue;
            }
            g.setColor(Palette.withAlpha(Palette.WARNING, pulse));
            g.draw(tileRect(cell.col(), cell.row(), left, top));
        }
    }

    private void drawFlareRing(Graphics2D g, GameDTO game, double left, double top) {
        if (flareRing >= FLARE_RING_TIME) {
            return;
        }

        double progress = flareRing / FLARE_RING_TIME;
        double radius = progress * tile * 16;
        double cx = screenX(game.player().x(), left);
        double cy = screenY(game.player().y(), top);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke((float) (10 * (1 - progress) + 2)));
        g.setColor(Palette.withAlpha(Palette.FLARE, 1 - progress));
        g.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));

        g.setColor(Palette.withAlpha(Palette.FLARE, (1 - progress) * 0.35));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    // --- Helpers -------------------------------------------------------------

    private static void fillGlow(Graphics2D g, double cx, double cy, double radius, Color color, double alpha) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) radius,
            new float[] {0f, 1f},
            new Color[] {Palette.withAlpha(color, alpha), Palette.withAlpha(color, 0)}));
        g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    private static BufferedImage createVignette() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(new RadialGradientPaint(new Point2D.Double(WIDTH / 2.0, HEIGHT / 2.0), WIDTH * 0.62f,
            new float[] {0.55f, 1f},
            new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.dispose();
        return image;
    }

    private double viewCols() {
        return WIDTH / tile;
    }

    private double viewRows() {
        return HEIGHT / tile;
    }

    private double screenX(double worldX, double left) {
        return (worldX - left) * tile;
    }

    private double screenY(double worldY, double top) {
        return (worldY - top) * tile;
    }

    private Rectangle2D tileRect(int col, int row, double left, double top) {
        // Rounded to whole pixels and one pixel oversized, so no gaps show between tiles.
        double x = Math.floor(screenX(col, left));
        double y = Math.floor(screenY(row, top));
        return new Rectangle2D.Double(x, y, Math.ceil(tile) + 1, Math.ceil(tile) + 1);
    }

    private int firstCol(double left) {
        return (int) Math.floor(left) - 1;
    }

    private int lastCol(double left) {
        return (int) Math.ceil(left + viewCols()) + 1;
    }

    private int firstRow(double top) {
        return (int) Math.floor(top) - 1;
    }

    private int lastRow(double top) {
        return (int) Math.ceil(top + viewRows()) + 1;
    }
}
