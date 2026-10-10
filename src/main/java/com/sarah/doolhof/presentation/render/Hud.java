package com.sarah.doolhof.presentation.render;

import com.sarah.doolhof.common.TileMemory;
import com.sarah.doolhof.dto.response.CellDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.LevelDTO;
import com.sarah.doolhof.dto.response.MazeDTO;
import com.sarah.doolhof.dto.response.PlayerDTO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import java.util.OptionalDouble;

public final class Hud {

    static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    static final Font BIG = new Font(Font.SANS_SERIF, Font.BOLD, 30);

    private static final int W = WorldRenderer.WIDTH;
    private static final int H = WorldRenderer.HEIGHT;
    private static final int MARGIN = 22;

    private static final double HINT_TIME = 7;

    public void draw(Graphics2D g, GameDTO game, boolean showMap, double time) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawDanger(g, game, time);
        drawStatus(g, game);
        drawClock(g, game);
        drawShiftBar(g, game, time);
        drawStamina(g, game);
        drawHint(g, game);

        if (showMap && game.level().dark()) {
            drawMap(g, game, time);
        }
    }

    private void drawDanger(Graphics2D g, GameDTO game, double time) {
        double distance = game.nearestHunterDistance();
        if (!game.chased() || distance > 9) {
            return;
        }

        double closeness = 1 - distance / 9;
        double beat = 0.5 + 0.5 * Math.sin(time * (5 + closeness * 12));
        int alpha = (int) (closeness * (70 + 110 * beat));

        g.setStroke(new BasicStroke(26f));
        g.setColor(new Color(255, 30, 50, Math.clamp(alpha, 0, 255)));
        g.drawRect(0, 0, W, H);
    }

    private void drawStatus(Graphics2D g, GameDTO game) {
        LevelDTO level = game.level();
        int rows = 1 + (game.totalKeys() > 0 ? 1 : 0) + (game.hunters().isEmpty() ? 0 : 1)
            + (level.flares() > 0 ? 1 : 0);
        panel(g, MARGIN, MARGIN, 290, 22 + rows * 30);

        int y = MARGIN + 30;
        g.setFont(LABEL);
        g.setColor(Palette.TEXT);
        g.drawString(level.number() + "  " + level.name(), MARGIN + 16, y);

        if (game.totalKeys() > 0) {
            y += 30;
            label(g, "Sleutels", y);
            for (int i = 0; i < game.totalKeys(); i++) {
                diamond(g, MARGIN + 118 + i * 24, y - 6, 8, i < game.keysCollected());
            }
        }

        if (!game.hunters().isEmpty()) {
            y += 30;
            label(g, "Levens", y);
            for (int i = 0; i < game.maxLives(); i++) {
                RoundRectangle2D life = new RoundRectangle2D.Double(MARGIN + 110 + i * 24, y - 14, 16, 16, 5, 5);
                if (i < game.lives()) {
                    g.setColor(Palette.PLAYER);
                    g.fill(life);
                } else {
                    g.setColor(Palette.TEXT_DIM);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(life);
                }
            }
        }

        if (level.flares() > 0) {
            y += 30;
            label(g, "Flitsen", y);
            for (int i = 0; i < level.flares(); i++) {
                Ellipse2D flare = new Ellipse2D.Double(MARGIN + 110 + i * 24, y - 14, 16, 16);
                if (i < game.flares()) {
                    g.setColor(Palette.FLARE);
                    g.fill(flare);
                } else {
                    g.setColor(Palette.TEXT_DIM);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(flare);
                }
            }
        }
    }

    private void drawClock(Graphics2D g, GameDTO game) {
        OptionalDouble bestTime = game.level().bestTime();
        panel(g, W - MARGIN - 190, MARGIN, 190, bestTime.isPresent() ? 78 : 56);

        g.setFont(BIG);
        g.setColor(Palette.TEXT);
        drawRight(g, formatTime(game.elapsed()), W - MARGIN - 16, MARGIN + 40);

        if (bestTime.isPresent()) {
            g.setFont(SMALL);
            g.setColor(Palette.TEXT_DIM);
            drawRight(g, "Beste tijd " + formatTime(bestTime.getAsDouble()), W - MARGIN - 16, MARGIN + 64);
        }
    }

    private void drawShiftBar(Graphics2D g, GameDTO game, double time) {
        if (!game.level().shiftingWalls()) {
            return;
        }

        int width = 340;
        int x = (W - width) / 2;
        boolean warning = game.shiftPending();
        panel(g, x, MARGIN, width, 56);

        g.setFont(LABEL);
        if (warning) {
            g.setColor(Palette.withAlpha(Palette.WARNING, 0.6 + 0.4 * Math.sin(time * 14)));
            drawCentred(g, "De muren bewegen!", W / 2, MARGIN + 26);
        } else {
            g.setColor(Palette.TEXT_DIM);
            drawCentred(g, "Muren verschuiven over " + (int) Math.ceil(game.shiftCountdown()) + " s", W / 2, MARGIN + 26);
        }

        double share = Math.clamp(game.shiftCountdown() / game.level().shiftInterval(), 0, 1);
        bar(g, x + 16, MARGIN + 36, width - 32, 8, share, warning ? Palette.WARNING : Palette.WALL_EDGE);
    }

    private void drawStamina(Graphics2D g, GameDTO game) {
        PlayerDTO player = game.player();
        if (game.hunters().isEmpty() && player.stamina() >= 1) {
            return;
        }

        int width = 260;
        int x = (W - width) / 2;
        int y = H - MARGIN - 34;
        panel(g, x, y, width, 34);

        g.setFont(SMALL);
        g.setColor(player.exhausted() ? Palette.DOOR_LOCKED : Palette.TEXT_DIM);
        g.drawString(player.exhausted() ? "Buiten adem" : "Adem", x + 14, y + 22);
        bar(g, x + 104, y + 13, width - 120, 8, player.stamina(),
            player.exhausted() ? Palette.TEXT_DIM : Palette.PLAYER);
    }

    private void drawHint(Graphics2D g, GameDTO game) {
        double age = game.elapsed();
        if (age > HINT_TIME) {
            return;
        }

        double alpha = Math.min(1, (HINT_TIME - age) / 1.5);
        g.setFont(LABEL);
        FontMetrics metrics = g.getFontMetrics();

        String hint = game.level().hint();
        String keys = game.level().number() <= 1
            ? "Pijltjes, WASD of ZQSD om te lopen"
            : "Shift rennen   Spatie lichtflits   M kaart   Esc pauze";

        int width = Math.max(metrics.stringWidth(hint), metrics.stringWidth(keys)) + 40;
        int x = (W - width) / 2;
        int y = H - 170;

        g.setColor(Palette.withAlpha(new Color(12, 10, 24), 0.85 * alpha));
        g.fill(new RoundRectangle2D.Double(x, y, width, 70, 14, 14));
        g.setColor(Palette.withAlpha(Palette.TEXT, alpha));
        drawCentred(g, hint, W / 2, y + 28);
        g.setFont(SMALL);
        g.setColor(Palette.withAlpha(Palette.TEXT_DIM, alpha));
        drawCentred(g, keys, W / 2, y + 54);
    }

    private void drawMap(Graphics2D g, GameDTO game, double time) {
        MazeDTO maze = game.maze();
        double scale = Math.min(230.0 / maze.cols(), 150.0 / maze.rows());
        double width = maze.cols() * scale;
        double height = maze.rows() * scale;
        double x = W - MARGIN - width - 10;
        double y = H - MARGIN - height - 10;

        panel(g, (int) x - 10, (int) y - 10, (int) width + 20, (int) height + 20);

        for (int row = 0; row < maze.rows(); row++) {
            for (int col = 0; col < maze.cols(); col++) {
                if (maze.memoryOf(col, row) == TileMemory.FLOOR) {
                    g.setColor(new Color(0x6a62a8));
                    g.fill(new Rectangle2D.Double(x + col * scale, y + row * scale, scale + 0.5, scale + 0.5));
                }
            }
        }

        // Items show up once their tile has been seen.
        for (CellDTO key : game.keys()) {
            if (maze.isKnown(key)) {
                dot(g, x + key.centerX() * scale, y + key.centerY() * scale, scale * 0.9, Palette.KEY);
            }
        }
        CellDTO door = game.door();
        if (maze.isKnown(door)) {
            dot(g, x + door.centerX() * scale, y + door.centerY() * scale, scale * 1.1,
                game.doorOpen() ? Palette.DOOR_OPEN : Palette.DOOR_LOCKED);
        }

        double blink = 0.75 + 0.25 * Math.sin(time * 8);
        dot(g, x + game.player().x() * scale, y + game.player().y() * scale, scale * 1.3,
            Palette.withAlpha(Palette.PLAYER, blink));
    }

    // --- Small drawing helpers ------------------------------------------------

    public static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        double rest = seconds - minutes * 60;
        return String.format(Locale.ROOT, "%d:%04.1f", minutes, Math.floor(rest * 10) / 10);
    }

    static void panel(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(Palette.PANEL);
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 14, 14));
    }

    static void drawCentred(Graphics2D g, String text, int centreX, int baseline) {
        g.drawString(text, centreX - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    static void drawRight(Graphics2D g, String text, int rightX, int baseline) {
        g.drawString(text, rightX - g.getFontMetrics().stringWidth(text), baseline);
    }

    private static void label(Graphics2D g, String text, int baseline) {
        g.setFont(SMALL);
        g.setColor(Palette.TEXT_DIM);
        g.drawString(text, MARGIN + 16, baseline);
    }

    private static void bar(Graphics2D g, int x, int y, int width, int height, double share, Color color) {
        g.setColor(new Color(255, 255, 255, 30));
        g.fill(new RoundRectangle2D.Double(x, y, width, height, height, height));
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y, Math.max(0, width * share), height, height, height));
    }

    private static void diamond(Graphics2D g, double cx, double cy, double radius, boolean filled) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(cx, cy - radius * 1.2);
        shape.lineTo(cx + radius, cy);
        shape.lineTo(cx, cy + radius * 1.2);
        shape.lineTo(cx - radius, cy);
        shape.closePath();

        if (filled) {
            g.setColor(Palette.KEY);
            g.fill(shape);
        } else {
            g.setColor(Palette.TEXT_DIM);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(shape);
        }
    }

    private static void dot(Graphics2D g, double cx, double cy, double size, Color color) {
        double d = Math.max(4, size);
        g.setColor(color);
        g.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
    }
}
