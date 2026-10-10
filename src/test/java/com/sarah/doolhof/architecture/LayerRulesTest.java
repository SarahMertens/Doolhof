package com.sarah.doolhof.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LayerRulesTest {

    private static final String ROOT = "com.sarah.doolhof.";
    private static final Path SOURCES = Path.of("src", "main", "java", "com", "sarah", "doolhof");
    private static final Pattern IMPORT = Pattern.compile("^import (?:static )?(" + Pattern.quote(ROOT) + "[\\w.]+);");

    @Test
    void thePresentationLayerOnlyTalksToServicesAndDtos() throws IOException {
        assertOnlyUses("presentation",
            "presentation.", "domain.service.GameService", "domain.service.LevelService", "dto.", "common.");
    }

    @Test
    void theDomainLayerKnowsNothingAboutTheScreen() throws IOException {
        assertOnlyUses("domain", "domain.", "persistence.ProgressRepository", "dto.", "common.", "config.", "exception.");
    }

    @Test
    void theModelDoesNotKnowAboutDtosOrStorage() throws IOException {
        assertOnlyUses("domain/model", "domain.model.", "domain.maze.", "common.", "config.", "exception.");
        assertOnlyUses("domain/maze", "domain.maze.");
    }

    @Test
    void thePersistenceLayerOnlyKnowsWhatItStores() throws IOException {
        assertOnlyUses("persistence", "persistence.", "domain.model.Progress", "exception.");
    }

    @Test
    void dtosAreSelfContained() throws IOException {
        assertOnlyUses("dto", "dto.", "common.");
        assertOnlyUses("common", "common.");
    }

    @Test
    void theDomainNeverUsesSwingOrAwt() throws IOException {
        for (String folder : List.of("domain", "dto", "common", "config", "exception", "persistence")) {
            for (Path file : javaFiles(folder)) {
                String source = Files.readString(file);
                assertFalse(source.contains("import java.awt") || source.contains("import javax.swing")
                    || source.contains("import javax.sound"), file + " uses a screen or sound library");
            }
        }
    }

    private static void assertOnlyUses(String folder, String... allowed) throws IOException {
        List<String> violations = new ArrayList<>();
        List<Path> files = javaFiles(folder);
        assertFalse(files.isEmpty(), "no sources found in " + SOURCES.resolve(folder).toAbsolutePath());

        for (Path file : files) {
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = IMPORT.matcher(line);
                if (!matcher.find()) {
                    continue;
                }
                String imported = matcher.group(1).substring(ROOT.length());
                if (Stream.of(allowed).noneMatch(imported::startsWith)) {
                    violations.add(file.getFileName() + " imports " + imported);
                }
            }
        }
        assertTrue(violations.isEmpty(), folder + " breaks the layer rules:\n" + String.join("\n", violations));
    }

    private static List<Path> javaFiles(String folder) throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCES.resolve(folder))) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
