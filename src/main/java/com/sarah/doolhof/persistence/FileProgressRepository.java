package com.sarah.doolhof.persistence;

import com.sarah.doolhof.domain.model.Progress;
import com.sarah.doolhof.exception.ProgressStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class FileProgressRepository implements ProgressRepository {

    private static final String UNLOCKED = "unlocked";
    private static final String BEST_PREFIX = "best.";

    private final Path file;

    public FileProgressRepository(Path file) {
        this.file = file;
    }

    public static FileProgressRepository inHomeFolder() {
        return new FileProgressRepository(
            Path.of(System.getProperty("user.home"), ".doolhof", "progress.properties"));
    }

    @Override
    public Progress load() {
        Properties values = read();

        int highestUnlocked = 0;
        Map<Integer, Double> bestTimes = new HashMap<>();

        for (String key : values.stringPropertyNames()) {
            try {
                if (key.equals(UNLOCKED)) {
                    highestUnlocked = Integer.parseInt(values.getProperty(key).trim());
                } else if (key.startsWith(BEST_PREFIX)) {
                    int level = Integer.parseInt(key.substring(BEST_PREFIX.length()));
                    bestTimes.put(level, Double.parseDouble(values.getProperty(key).trim()));
                }
            } catch (NumberFormatException damaged) {
                // Skip this line and keep the rest.
            }
        }
        return new Progress(highestUnlocked, bestTimes);
    }

    @Override
    public void save(Progress progress) {
        Properties values = new Properties();
        values.setProperty(UNLOCKED, Integer.toString(progress.highestUnlocked()));
        progress.bestTimes().forEach(
            (level, seconds) -> values.setProperty(BEST_PREFIX + level, Double.toString(seconds)));

        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                values.store(out, "Doolhof progress");
            }
        } catch (IOException e) {
            throw new ProgressStorageException("Could not save progress to " + file, e);
        }
    }

    private Properties read() {
        Properties values = new Properties();
        if (!Files.isRegularFile(file)) {
            return values;
        }
        try (InputStream in = Files.newInputStream(file)) {
            values.load(in);
        } catch (IOException | IllegalArgumentException unreadable) {
            values.clear();
        }
        return values;
    }
}
