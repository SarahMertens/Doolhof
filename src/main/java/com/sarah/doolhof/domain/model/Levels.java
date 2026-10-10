package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.domain.maze.ClassicMaze;
import com.sarah.doolhof.exception.LevelNotFoundException;
import java.util.List;

public final class Levels {

    private static final List<Level> ALL = List.of(
        new Level(0, "Het origineel", "Het eerste doolhof, zoals het begon. Zoek de deur.",
            ClassicMaze.COLS, ClassicMaze.ROWS, 0, 0, 0, 0, 0, 0, 0, true),
        new Level(1, "Schemer", "Het licht gaat uit. Vind 2 sleutels en dan de deur.",
            21, 15, 1001, 2, 0, 1, 5.5, 0, 0, false),
        new Level(2, "De jager", "Je bent niet alleen. Rennen maakt lawaai.",
            25, 17, 2002, 3, 1, 2, 5.5, 0, 0, false),
        new Level(3, "Schuivende muren", "Het doolhof verandert terwijl je loopt.",
            29, 19, 3003, 3, 1, 2, 5.5, 26, 5, false),
        new Level(4, "Twee paar ogen", "Twee jagers, en ze zoeken samen.",
            33, 21, 4004, 4, 2, 2, 5.0, 23, 6, false),
        new Level(5, "Kaarslicht", "Je ziet nog maar een paar stappen ver.",
            37, 23, 5005, 4, 2, 3, 3.8, 21, 7, false),
        new Level(6, "De roedel", "Drie jagers. Blijf in beweging.",
            41, 25, 6006, 5, 3, 3, 4.6, 18, 8, false),
        new Level(7, "Het hart", "Alles tegelijk. Veel succes.",
            47, 29, 7007, 6, 4, 3, 4.0, 15, 10, false));

    private Levels() {
    }

    public static List<Level> all() {
        return ALL;
    }

    public static Level get(int number) {
        if (number < 0 || number >= ALL.size()) {
            throw new LevelNotFoundException(number);
        }
        return ALL.get(number);
    }

    public static boolean isLast(int number) {
        return number == ALL.size() - 1;
    }

    public static int count() {
        return ALL.size();
    }
}
