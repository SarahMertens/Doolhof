package com.sarah.doolhof.presentation.render;

import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.LevelDTO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public final class Screens {

    private static final int W = WorldRenderer.WIDTH;
    private static final int H = WorldRenderer.HEIGHT;

    private static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 104)
        .deriveFont(Map.of(TextAttribute.TRACKING, 0.12));
    private static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 64);
    private static final Font ROW = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 18);

    private static final int ROW_HEIGHT = 46;

    private Screens() {
    }

    public static void drawMenu(Graphics2D g, List<LevelDTO> levels, int selected, boolean muted, double time) {
        smooth(g);

        // The demo game keeps running behind the menu; dim it so the text is readable.
        g.setColor(new Color(7, 6, 13, 120));
        g.fillRect(0, 0, W, H);

        g.setFont(TITLE);
        g.setColor(Palette.withAlpha(Palette.PLAYER, 0.35));
        g.drawString("DOOLHOF", 74, 204);
        g.setColor(Palette.TEXT);
        g.drawString("DOOLHOF", 70, 200);

        g.setFont(BODY);
        g.setColor(Palette.TEXT_DIM);
        g.drawString("Vind de sleutels. Ontloop de jagers. Vertrouw de muren niet.", 76, 244);

        int listX = 76;
        int listY = 300;
        int listWidth = 620;

        for (LevelDTO level : levels) {
            int y = listY + level.number() * ROW_HEIGHT;
            boolean unlocked = level.unlocked();
            boolean isSelected = level.number() == selected;

            if (isSelected) {
                g.setColor(Palette.withAlpha(Palette.PLAYER, 0.22 + 0.06 * Math.sin(time * 4)));
                g.fill(new RoundRectangle2D.Double(listX - 14, y - 30, listWidth, ROW_HEIGHT - 4, 12, 12));
                g.setColor(Palette.PLAYER);
                g.fill(new RoundRectangle2D.Double(listX - 14, y - 30, 6, ROW_HEIGHT - 4, 6, 6));
            }

            g.setFont(ROW);
            g.setColor(unlocked ? Palette.TEXT : Palette.withAlpha(Palette.TEXT_DIM, 0.55));
            g.drawString(String.valueOf(level.number()), listX + 8, y);
            g.drawString(level.name(), listX + 44, y);

            g.setFont(BODY);
            OptionalDouble best = level.bestTime();
            if (!unlocked) {
                g.setColor(Palette.withAlpha(Palette.TEXT_DIM, 0.55));
                Hud.drawRight(g, "Op slot", listX + listWidth - 34, y);
            } else if (best.isPresent()) {
                g.setColor(Palette.KEY);
                Hud.drawRight(g, Hud.formatTime(best.getAsDouble()), listX + listWidth - 34, y);
            } else {
                g.setColor(Palette.TEXT_DIM);
                Hud.drawRight(g, "Nog niet gehaald", listX + listWidth - 34, y);
            }
        }

        // Details of the selected level.
        LevelDTO level = levels.get(selected);
        int panelX = 770;
        Hud.panel(g, panelX, 300 - 30, 440, 250);

        g.setFont(ROW);
        g.setColor(Palette.TEXT);
        g.drawString(level.name(), panelX + 24, 300 + 8);

        g.setFont(BODY);
        g.setColor(Palette.TEXT_DIM);
        drawWrapped(g, level.unlocked()
            ? level.hint()
            : "Haal eerst level " + (selected - 1) + " om dit level te openen.", panelX + 24, 300 + 40, 392);

        int factY = 300 + 110;
        String[] facts = {
            !level.dark() ? "Het licht is aan" : "Donker, je ziet " + trim(level.lightRadius()) + " tegels ver",
            level.keys() == 0 ? "Geen sleutels nodig" : level.keys() + " sleutels",
            level.hunters() == 0 ? "Geen jagers" : level.hunters() + (level.hunters() == 1 ? " jager" : " jagers"),
            level.shiftingWalls() ? "Muren verschuiven elke " + trim(level.shiftInterval()) + " s" : "Muren staan stil",
        };
        for (String fact : facts) {
            g.setColor(Palette.PLAYER);
            g.fillRect(panelX + 24, factY - 10, 8, 8);
            g.setColor(Palette.TEXT);
            g.drawString(fact, panelX + 44, factY);
            factY += 28;
        }

        g.setFont(Hud.SMALL);
        g.setColor(Palette.TEXT_DIM);
        g.drawString("Pijltjes kiezen    Enter spelen    G geluid " + (muted ? "aan" : "uit")
            + "    F11 volledig scherm    Esc afsluiten", 76, H - 34);
    }

    public static void drawPause(Graphics2D g) {
        overlay(g, "Pauze", Palette.TEXT, new String[] {"Het doolhof wacht."},
            "Enter verder    R opnieuw    Esc naar het menu");
    }

    public static void drawWon(Graphics2D g, GameDTO game) {
        boolean hasNext = !game.level().last();
        String[] lines = {
            "Tijd " + Hud.formatTime(game.elapsed()) + (game.newRecord() ? "   nieuw record" : ""),
            hasNext ? "Level " + (game.level().number() + 1) + " is open." : "Je hebt het hart van het doolhof verlaten.",
        };
        overlay(g, "Ontsnapt", Palette.DOOR_OPEN, lines,
            (hasNext ? "Enter volgend level    " : "") + "R opnieuw    Esc naar het menu");
    }

    public static void drawLost(Graphics2D g, GameDTO game) {
        overlay(g, "Gevangen", Palette.DOOR_LOCKED,
            new String[] {"Je had " + game.keysCollected() + " van de " + game.totalKeys() + " sleutels."},
            "Enter opnieuw proberen    Esc naar het menu");
    }

    private static void overlay(Graphics2D g, String heading, Color headingColor, String[] lines, String keys) {
        smooth(g);
        g.setColor(new Color(7, 6, 13, 190));
        g.fillRect(0, 0, W, H);

        g.setFont(HEADING);
        g.setColor(headingColor);
        Hud.drawCentred(g, heading, W / 2, 300);

        g.setFont(ROW);
        g.setColor(Palette.TEXT);
        int y = 356;
        for (String line : lines) {
            Hud.drawCentred(g, line, W / 2, y);
            y += 32;
        }

        g.setFont(BODY);
        g.setColor(Palette.TEXT_DIM);
        Hud.drawCentred(g, keys, W / 2, y + 36);
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(line.toString(), x, y);
                y += 24;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        g.drawString(line.toString(), x, y);
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value).replace('.', ',');
    }

    private static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
