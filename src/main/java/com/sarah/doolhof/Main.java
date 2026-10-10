package com.sarah.doolhof;

import com.sarah.doolhof.domain.service.GameService;
import com.sarah.doolhof.domain.service.LevelService;
import com.sarah.doolhof.persistence.FileProgressRepository;
import com.sarah.doolhof.persistence.ProgressRepository;
import com.sarah.doolhof.presentation.GameWindow;
import javax.swing.SwingUtilities;

/**
 * Starts the game. This is the only class that knows all three layers: it
 * builds them from the bottom up and hands each one the layer below it.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // Persistence
        ProgressRepository progressRepository = FileProgressRepository.inHomeFolder();

        // Domain
        LevelService levelService = new LevelService(progressRepository);
        GameService gameService = new GameService(levelService);

        // Presentation. Swing components must be created on the event dispatch thread.
        SwingUtilities.invokeLater(() -> new GameWindow(gameService, levelService).show());
    }
}
