package org.example;

import org.example.flashcards.FlashCardsEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashCardsEngineTest {

    private final InputStream originalIn = System.in;
    private FlashCardsEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FlashCardsEngine();
    }

    @AfterEach
    void restoreSystemIn() {
        System.setIn(originalIn);
    }

    private void provideInput(String... lines) {
        String inputData = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(inputData.getBytes()));
    }

    @Test
    void testProcessArgsImportAndExport() {
        String[] args = {"-import", "import.txt", "-export", "export.txt"};

        engine.processArgs(args);

        // Verify state through execution without throwing exceptions
        assertDoesNotThrow(
                () -> engine.processArgs(args),
                "Processing valid -import and -export CLI flags should execute cleanly"
        );
    }

    @Test
    void testStartAndExitImmediately() {
        provideInput("exit");

        assertDoesNotThrow(
                () -> engine.start(new String[]{}),
                "Engine should process 'exit' command and exit gracefully without throwing exceptions"
        );
    }

    @Test
    void testAddCardAndExitWorkflow() {
        // Inputs:
        // 1. "add" -> action
        // 2. "Japan" -> card term
        // 3. "Tokyo" -> definition
        // 4. "exit" -> finish engine loop
        provideInput("add", "Japan", "Tokyo", "exit");

        assertDoesNotThrow(
                () -> engine.start(new String[]{}),
                "Engine loop should process card creation and exit successfully"
        );
    }

    @Test
    void testImportAndExportInteractiveActions(@TempDir Path tempDir) throws IOException {
        Path importFile = tempDir.resolve("import_cards.txt");
        Path exportFile = tempDir.resolve("export_cards.txt");

        // Seed import file with 1 flashcard entry (term,definition,errors)
        Files.write(importFile, List.of("France,Paris,0"));

        // Inputs:
        // 1. "import" -> load file
        // 2. importFile path
        // 3. "export" -> save file
        // 4. exportFile path
        // 5. "exit"
        provideInput("import", importFile.toString(), "export", exportFile.toString(), "exit");

        engine.start(new String[]{});

        assertTrue(
                Files.exists(exportFile),
                "Export action should write flashcards to the user-specified destination file"
        );

        List<String> exportedLines = Files.readAllLines(exportFile);
        assertFalse(
                exportedLines.isEmpty(),
                "Exported file should contain the card imported during the interactive session"
        );
        assertTrue(
                exportedLines.contains("France,Paris,0"),
                "Exported file should preserve card term, definition, and error count"
        );
    }

    @Test
    void testAutoImportAndAutoExportFlags(@TempDir Path tempDir) throws IOException {
        Path importFile = tempDir.resolve("auto_import.txt");
        Path exportFile = tempDir.resolve("auto_export.txt");

        // Seed auto-import file
        Files.write(importFile, List.of("Germany,Berlin,0"));

        String[] args = {"-import", importFile.toString(), "-export", exportFile.toString()};

        // Command loop input: immediately exit
        provideInput("exit");

        engine.start(args);

        assertTrue(
                Files.exists(exportFile),
                "Engine should automatically export cards on exit when -export flag is provided"
        );

        List<String> exportedLines = Files.readAllLines(exportFile);
        assertTrue(
                exportedLines.contains("Germany,Berlin,0"),
                "Auto-exported file should contain cards loaded via the -import CLI argument"
        );
    }

    @Test
    void testInteractiveExportWhenNoExportFlagProvided(@TempDir Path tempDir) throws IOException {
        Path interactiveExportFile = tempDir.resolve("manual_export.txt");

        // Inputs:
        // 1. "add" -> term: "Spain", def: "Madrid"
        // 2. "export" -> path: manual_export.txt
        // 3. "exit"
        provideInput("add", "Spain", "Madrid", "export", interactiveExportFile.toString(), "exit");

        engine.start(new String[]{});

        assertTrue(
                Files.exists(interactiveExportFile),
                "Interactive 'export' action should save cards to file when -export CLI flag is absent"
        );

        List<String> exportedLines = Files.readAllLines(interactiveExportFile);
        assertTrue(
                exportedLines.contains("Spain,Madrid,0"),
                "Interactive exported file should accurately save added cards"
        );
    }

    @Test
    void testProcessArgsSingleFlagsAndInvalidArgs() {
        // Test -import only and -export only, plus trailing/dangling flag cases
        String[] args1 = {"-import", "import.txt"};
        engine.processArgs(args1);

        String[] args2 = {"-export", "export.txt"};
        engine.processArgs(args2);

        // Dangling flags without value and unknown flag branches
        String[] args3 = {"-import", "-export", "-unknown"};
        assertDoesNotThrow(
                () -> engine.processArgs(args3),
                "Processing incomplete or unknown CLI flags should not throw exceptions"
        );
    }

    @Test
    void testAllSwitchBranchesWorkflow(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("test.log");

        // Inputs triggering remaining switch cases:
        // 1. "remove" -> card: "Unknown"
        // 2. "ask" -> count: "1" -> answer: "wrong"
        // 3. "hardest_card"
        // 4. "reset_stats"
        // 5. "log" -> path: logPath
        // 6. "exit"
        provideInput(
                "remove", "Unknown",
                "ask", "1",
                "hardest_card",
                "reset_stats",
                "log", logPath.toString(),
                "exit"
        );

        assertDoesNotThrow(
                () -> engine.start(new String[]{}),
                "Engine should process all menu actions (remove, ask, hardest_card, reset_stats, log) without throwing exceptions"
        );

        assertTrue(
                Files.exists(logPath),
                "Log action branch should execute and create the log file on disk"
        );
    }

    @Test
    void testStartWithOnlyExportFlag(@TempDir Path tempDir) throws IOException {
        Path exportFile = tempDir.resolve("auto_export_only.txt");
        String[] args = {"-export", exportFile.toString()};

        // Inputs: "add" -> "Cat" -> "Gatto" -> "exit"
        provideInput("add", "Cat", "Gatto", "exit");

        engine.start(args);

        assertTrue(
                Files.exists(exportFile),
                "Exit branch should trigger auto-export when -export flag is set"
        );

        List<String> exportedLines = Files.readAllLines(exportFile);
        assertTrue(
                exportedLines.contains("Cat,Gatto,0"),
                "Auto-exported file on exit should contain added card"
        );
    }

    @Test
    void testStartWithOnlyImportFlag(@TempDir Path tempDir) throws IOException {
        Path importFile = tempDir.resolve("auto_import_only.txt");
        Files.write(importFile, List.of("Dog,Cane,0"));

        String[] args = {"-import", importFile.toString()};

        // Inputs: exit immediately
        provideInput("exit");

        assertDoesNotThrow(
                () -> engine.start(args),
                "Engine startup should successfully load cards when only -import flag is provided"
        );
    }
}