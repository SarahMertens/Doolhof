package com.sarah.doolhof.presentation.render;

import java.awt.Color;

final class Palette {

    static final Color VOID = new Color(0x07060d);

    static final Color FLOOR = new Color(0x4a4270);
    static final Color FLOOR_ALT = new Color(0x524a7b);
    static final Color WALL_TOP = new Color(0x8a7fd6);
    static final Color WALL_FRONT = new Color(0x5a509f);
    static final Color WALL_EDGE = new Color(0xb7adf5);

    static final Color MEMORY_FLOOR = new Color(0x131124);
    static final Color MEMORY_WALL = new Color(0x27233f);

    static final Color PLAYER = new Color(0xff2bd6);
    static final Color PLAYER_CORE = new Color(0xffc4f3);

    static final Color KEY = new Color(0xffd23f);
    static final Color DOOR_OPEN = new Color(0x5dfc9b);
    static final Color DOOR_LOCKED = new Color(0xff5c5c);

    static final Color HUNTER = new Color(0x0d0b14);
    static final Color HUNTER_EYE = new Color(0xff3030);
    static final Color HUNTER_SEARCH = new Color(0xffb020);
    static final Color HUNTER_STUNNED = new Color(0x8f8aa8);

    static final Color WARNING = new Color(0xffa424);
    static final Color FLARE = new Color(0xfff3c4);

    static final Color TEXT = new Color(0xf2efff);
    static final Color TEXT_DIM = new Color(0x9c96c0);
    static final Color PANEL = new Color(12, 10, 24, 215);

    private Palette() {
    }

    static Color withAlpha(Color color, double alpha) {
        int a = (int) Math.round(255 * Math.clamp(alpha, 0, 1));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    static Color mix(Color from, Color to, double share) {
        double t = Math.clamp(share, 0, 1);
        return new Color(
            (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
            (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
            (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }
}
