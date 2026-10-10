package com.sarah.doolhof.presentation;

import com.sarah.doolhof.domain.service.GameService;
import com.sarah.doolhof.domain.service.LevelService;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class GameWindow {

    private final JFrame frame = new JFrame("Doolhof");
    private final GamePanel panel;
    private boolean fullScreen;

    public GameWindow(GameService gameService, LevelService levelService) {
        Image door = loadImage("/images/deur.png");

        panel = new GamePanel(gameService, levelService, door, this::toggleFullScreen);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setIconImage(door);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
        panel.start();
    }

    private void toggleFullScreen() {
        GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        fullScreen = !fullScreen;

        // The window decorations can only change while the window is not displayable.
        frame.dispose();
        frame.setUndecorated(fullScreen);
        if (fullScreen && screen.isFullScreenSupported()) {
            screen.setFullScreenWindow(frame);
        } else {
            screen.setFullScreenWindow(null);
            frame.pack();
            frame.setLocationRelativeTo(null);
            fullScreen = false;
        }
        frame.setVisible(true);
        panel.requestFocusInWindow();
    }

    private static Image loadImage(String path) {
        try (InputStream in = GameWindow.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
