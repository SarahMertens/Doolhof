package com.sarah.doolhof.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sarah.doolhof.domain.model.Progress;
import com.sarah.doolhof.exception.ProgressStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileProgressRepositoryTest {

    @TempDir
    Path folder;

    private Path file() {
        return folder.resolve("sub").resolve("progress.properties");
    }

    @Test
    void withoutAFileThePlayerStartsFromTheBeginning() {
        Progress progress = new FileProgressRepository(file()).load();

        assertEquals(1, progress.highestUnlocked());
        assertTrue(progress.bestTimes().isEmpty());
    }

    @Test
    void whatIsSavedComesBackAfterARestart() {
        Progress progress = new Progress();
        progress.recordWin(1, 48.75);
        progress.recordWin(2, 61.5);
        new FileProgressRepository(file()).save(progress);

        Progress reopened = new FileProgressRepository(file()).load();

        assertEquals(3, reopened.highestUnlocked());
        assertEquals(Map.of(1, 48.75, 2, 61.5), reopened.bestTimes());
    }

    @Test
    void savingReplacesWhatWasThere() {
        FileProgressRepository repository = new FileProgressRepository(file());
        repository.save(new Progress(5, Map.of(1, 10.0, 4, 20.0)));

        repository.save(new Progress(5, Map.of(1, 9.0)));

        assertEquals(Map.of(1, 9.0), repository.load().bestTimes());
    }

    @Test
    void theFileOfTheFirstVersionCanStillBeRead() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "#Doolhof progress\nunlocked=2\nbest.0=21.3\nbest.1=40.0\n");

        Progress progress = new FileProgressRepository(file()).load();

        assertEquals(2, progress.highestUnlocked());
        assertEquals(21.3, progress.bestTime(0).getAsDouble());
        assertEquals(40.0, progress.bestTime(1).getAsDouble());
    }

    @Test
    void damagedLinesAreSkippedAndTheRestIsKept() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "unlocked=veel\nbest.1=snel\nbest.x=3\nbest.2=33.5\nsomething=else\n");

        Progress progress = new FileProgressRepository(file()).load();

        assertEquals(1, progress.highestUnlocked());
        assertEquals(Map.of(2, 33.5), progress.bestTimes());
    }

    @Test
    void aFileThatCannotBeWrittenIsReported() throws IOException {
        // A file where the folder should be makes saving impossible.
        Path blocker = folder.resolve("blocker");
        Files.writeString(blocker, "x");
        FileProgressRepository repository = new FileProgressRepository(blocker.resolve("progress.properties"));

        assertThrows(ProgressStorageException.class, () -> repository.save(new Progress()));
    }
}
