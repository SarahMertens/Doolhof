package com.sarah.doolhof.presentation;

import com.sarah.doolhof.common.GameStatus;
import com.sarah.doolhof.domain.service.GameService;
import com.sarah.doolhof.domain.service.LevelService;
import com.sarah.doolhof.dto.request.PlayerInputDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.LevelDTO;
import com.sarah.doolhof.presentation.audio.Sound;
import com.sarah.doolhof.presentation.render.Hud;
import com.sarah.doolhof.presentation.render.Screens;
import com.sarah.doolhof.presentation.render.WorldRenderer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private enum Screen { MENU, PLAYING, PAUSED, WON, LOST }

    private final transient GameService gameService;
    private final transient LevelService levelService;
    private final transient Sound sound = new Sound();
    private final transient KeyboardState keyboard = new KeyboardState();
    private final transient Hud hud = new Hud();
    private final transient WorldRenderer renderer;
    private final transient Runnable toggleFullScreen;
    private final Timer timer;

    private Screen screen = Screen.MENU;
    private transient GameDTO game;
    private transient List<LevelDTO> levels;
    private int selectedLevel;
    private boolean showMap = true;
    private long lastTick;
    private double clock;
    private double heartbeatTimer;

    public GamePanel(GameService gameService, LevelService levelService, Image doorImage, Runnable toggleFullScreen) {
        this.gameService = gameService;
        this.levelService = levelService;
        this.renderer = new WorldRenderer(doorImage);
        this.toggleFullScreen = toggleFullScreen;
        this.selectedLevel = levelService.getFurthestUnlockedNumber();

        setPreferredSize(new Dimension(WorldRenderer.WIDTH, WorldRenderer.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        // Tab and Shift must reach the game instead of moving the keyboard focus.
        setFocusTraversalKeysEnabled(false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                keyboard.press(event.getKeyCode());
                handleKey(event.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent event) {
                keyboard.release(event.getKeyCode());
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                // Otherwise a key held while switching windows would stay "down".
                keyboard.clear();
                if (screen == Screen.PLAYING) {
                    screen = Screen.PAUSED;
                }
            }
        });

        showMenu();
        timer = new Timer(8, event -> tick());
    }

    public void start() {
        lastTick = System.nanoTime();
        timer.start();
        requestFocusInWindow();
    }

    // --- Time ---------------------------------------------------------------

    private void tick() {
        long now = System.nanoTime();
        double seconds = (now - lastTick) / 1e9;
        lastTick = now;

        update(seconds);
        repaint();
    }

    void update(double seconds) {
        clock += seconds;

        switch (screen) {
            case MENU -> updateDemo(seconds);
            case PLAYING -> updateGame(keyboard.poll(), seconds);
            // After winning or losing the level stands still, but its last effects play out.
            case WON, LOST -> updateGame(PlayerInputDTO.NONE, seconds);
            case PAUSED -> { }
        }
    }

    private void updateGame(PlayerInputDTO input, double seconds) {
        game = gameService.advance(input, seconds);
        renderer.update(game, seconds);
        sound.play(game.events());

        if (screen != Screen.PLAYING) {
            return;
        }

        playHeartbeat(seconds);

        if (game.status() == GameStatus.WON) {
            screen = Screen.WON;
        } else if (game.status() == GameStatus.LOST) {
            screen = Screen.LOST;
        }
    }

    private void playHeartbeat(double seconds) {
        double distance = game.nearestHunterDistance();

        heartbeatTimer -= seconds;
        if (game.chased() && distance < 9 && heartbeatTimer <= 0) {
            sound.play(Sound.Effect.HEARTBEAT);
            heartbeatTimer = 0.35 + 0.6 * distance / 9;
        }
    }

    private void updateDemo(double seconds) {
        game = gameService.advance(PlayerInputDTO.NONE, seconds);
        renderer.update(game, seconds);

        if (game.status().isOver()) {
            startDemo();
        }
    }

    // --- Switching screens ----------------------------------------------------

    private void showMenu() {
        screen = Screen.MENU;
        // Asked again on every visit: winning a level changes what is unlocked and the best times.
        levels = levelService.getAll();
        startDemo();
    }

    private void startDemo() {
        game = gameService.startDemo();
        renderer.reset(game);
    }

    private void startLevel(int number) {
        game = gameService.startLevel(number);
        renderer.reset(game);
        keyboard.clear();
        heartbeatTimer = 0;
        selectedLevel = number;
        screen = Screen.PLAYING;
    }

    private void restartLevel() {
        startLevel(game.level().number());
    }

    private void handleKey(int key) {
        if (key == KeyEvent.VK_F11) {
            toggleFullScreen.run();
            return;
        }
        if (key == KeyEvent.VK_G) {
            sound.toggleMute();
            return;
        }

        switch (screen) {
            case MENU -> handleMenuKey(key);
            case PLAYING -> {
                switch (key) {
                    case KeyEvent.VK_ESCAPE, KeyEvent.VK_P -> screen = Screen.PAUSED;
                    case KeyEvent.VK_M, KeyEvent.VK_TAB -> showMap = !showMap;
                    default -> { }
                }
            }
            case PAUSED -> {
                switch (key) {
                    case KeyEvent.VK_ENTER, KeyEvent.VK_P -> {
                        keyboard.clear();
                        screen = Screen.PLAYING;
                    }
                    case KeyEvent.VK_R -> restartLevel();
                    case KeyEvent.VK_ESCAPE -> showMenu();
                    default -> { }
                }
            }
            case WON -> {
                switch (key) {
                    case KeyEvent.VK_ENTER -> {
                        if (game.level().last()) {
                            showMenu();
                        } else {
                            startLevel(game.level().number() + 1);
                        }
                    }
                    case KeyEvent.VK_R -> restartLevel();
                    case KeyEvent.VK_ESCAPE -> showMenu();
                    default -> { }
                }
            }
            case LOST -> {
                switch (key) {
                    case KeyEvent.VK_ENTER, KeyEvent.VK_R -> restartLevel();
                    case KeyEvent.VK_ESCAPE -> showMenu();
                    default -> { }
                }
            }
        }
    }

    private void handleMenuKey(int key) {
        switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_Z ->
                selectedLevel = Math.floorMod(selectedLevel - 1, levels.size());
            case KeyEvent.VK_DOWN, KeyEvent.VK_S ->
                selectedLevel = Math.floorMod(selectedLevel + 1, levels.size());
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                if (levels.get(selectedLevel).unlocked()) {
                    startLevel(selectedLevel);
                }
            }
            case KeyEvent.VK_ESCAPE -> System.exit(0);
            default -> { }
        }
    }

    // --- Drawing ------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();

        // Scale the fixed 1280 x 720 picture to the window, keeping its shape.
        double scale = Math.min((double) getWidth() / WorldRenderer.WIDTH, (double) getHeight() / WorldRenderer.HEIGHT);
        g.translate((getWidth() - WorldRenderer.WIDTH * scale) / 2, (getHeight() - WorldRenderer.HEIGHT * scale) / 2);
        g.scale(scale, scale);
        g.setClip(0, 0, WorldRenderer.WIDTH, WorldRenderer.HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawFrame(g);
        g.dispose();
    }

    void drawFrame(Graphics2D g) {
        renderer.draw(g, game);

        switch (screen) {
            case MENU -> Screens.drawMenu(g, levels, selectedLevel, sound.isMuted(), clock);
            case PLAYING -> hud.draw(g, game, showMap, clock);
            case PAUSED -> {
                hud.draw(g, game, showMap, clock);
                Screens.drawPause(g);
            }
            case WON -> Screens.drawWon(g, game);
            case LOST -> Screens.drawLost(g, game);
        }
    }
}
