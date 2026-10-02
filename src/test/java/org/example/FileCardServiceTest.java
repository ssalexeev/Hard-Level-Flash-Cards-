package org.example;

import org.example.flashcards.FileCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCardServiceTest {

    private FileCardService service;

    @BeforeEach
    void setUp() {
        service = new FileCardService();
    }

    private Scanner createScannerInput(String... lines) {
        String inputData = String.join("\n", lines) + "\n";
        return new Scanner(inputData);
    }

    @Test
    void testAddCardSuccess() {
        Scanner scanner = createScannerInput("France", "Paris");

        service.addCard(scanner);

        List<String> logs = service.logger().logHistory;
        assertTrue(
                logs.contains("The pair (\"France\":\"Paris\") has been added"),
                "Card pair should be added successfully to log history"
        );
    }

    @Test
    void testAddDuplicateCardTerm() {
        Scanner scanner1 = createScannerInput("France", "Paris");
        Scanner scanner2 = createScannerInput("France", "Nice");

        service.addCard(scanner1);
        service.addCard(scanner2);

        List<String> logs = service.logger().logHistory;
        // Removed trailing '\n' as logger stores the message without newlines
        assertTrue(
                logs.contains("The card \"France\" already exists."),
                "Should log duplicate card error message when card term already exists"
        );
    }

    @Test
    void testRemoveCardSuccess() {
        service.addCard(createScannerInput("Japan", "Tokyo"));

        service.removeCard(createScannerInput("Japan"));
        assertTrue(
                service.logger().logHistory.contains("The card has been removed."),
                "Card should be successfully removed"
        );
    }

    @Test
    void testRemoveCardFailure() {
        // Remove non-existing card
        service.removeCard(createScannerInput("Japan"));
        assertTrue(
                service.logger().logHistory.contains("Can't remove \"Japan\": there is no such card."),
                "Should log error message when trying to remove a non-existent card"
        );
    }

    @Test
    void testAskCorrectAnswer() {
        service.addCard(createScannerInput("Spain", "Madrid"));
        service.ask(createScannerInput("1", "Madrid"));

        assertTrue(
                service.logger().logHistory.contains("Correct!"),
                "Should log 'Correct!' when the user inputs the right answer"
        );
    }

    @Test
    void testAskWrongAnswerIncrementsErrorsAndUpdatesHardestCard() {
        service.addCard(createScannerInput("Germany", "Berlin"));

        // Ask 1 time with wrong answer
        service.ask(createScannerInput("1", "Munich"));

        assertTrue(
                service.logger().logHistory.contains("Wrong. The right answer is \"Berlin\"."),
                "Should log wrong answer message with correct definition"
        );

        // Verify hardest card reflects error
        service.hardestCard();
        String logsCombined = String.join("\n", service.logger().logHistory);
        assertTrue(
                logsCombined.contains("The hardest card is \"Germany\". You have 1 errors answering it."),
                "Hardest card summary should display the card with the highest error count"
        );
    }

    @Test
    void testResetErrors() {
        service.addCard(createScannerInput("Italy", "Rome"));
        service.ask(createScannerInput("1", "Milan"));

        service.resetErrors();
        service.hardestCard();

        assertTrue(
                service.logger().logHistory.contains("There are no cards with errors."),
                "Error count should be reset to 0 for all cards"
        );
    }

    @Test
    void testSaveAndLoadCardsFromFile(@TempDir Path tempDir) throws IOException {
        Path filePath = tempDir.resolve("cards.txt");

        service.addCard(createScannerInput("Cat", "gatto"));
        service.addCard(createScannerInput("Dog", "cane"));
        service.saveCardsToFile(filePath.toString());

        assertTrue(Files.exists(filePath), "Export file should exist at target path");

        FileCardService newService = new FileCardService();
        newService.loadCardsFromFile(filePath.toString());

        assertTrue(
                newService.logger().logHistory.contains("2 cards have been loaded."),
                "Should log exact count of cards loaded from file"
        );
    }

    @Test
    void testLoadFromFileNotFound() {
        service.loadCardsFromFile("non_existent_file.txt");

        assertTrue(
                service.logger().logHistory.contains("File not found."),
                "Should log 'File not found.' when attempting to load from a missing file path"
        );
    }

    @Test
    void testSaveLog(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("session.log");

        service.addCard(createScannerInput("Sun", "Star"));
        service.saveLog(createScannerInput(logFile.toString()));

        assertTrue(Files.exists(logFile), "Log file should be created on disk");
        List<String> fileLines = Files.readAllLines(logFile);

        assertFalse(fileLines.isEmpty(), "Saved log file should not be empty");
        assertTrue(fileLines.contains("The card:"), "Saved log file should contain prompt history");
        assertTrue(fileLines.contains("Sun"), "Saved log file should contain user input history");
    }

    @Test
    void testAskWrongAnswerMatchingAnotherCardDefinition() {
        // Add two distinct cards
        service.addCard(createScannerInput("France", "Paris"));
        service.addCard(createScannerInput("Japan", "Tokyo"));

        // Ask for France, but answer "Tokyo" (which belongs to Japan)
        service.ask(createScannerInput("1", "Tokyo"));

        List<String> logs = service.logger().logHistory;
        assertTrue(
                logs.stream().anyMatch(line -> line.contains("Wrong. The right answer is \"Paris\", but your definition is correct for \"Japan\" card.")),
                "Should log specific message when user provides a definition that belongs to another existing card"
        );
    }

    @Test
    void testSaveCardsToFileIOExceptionHandling() {
        // Pass an invalid file path (non-existent directory path) to trigger IOException
        service.addCard(createScannerInput("Spain", "Madrid"));
        service.saveCardsToFile("/invalid_directory_path_12345/cards.txt");

        List<String> logs = service.logger().logHistory;
        assertTrue(
                logs.stream().anyMatch(line -> line.startsWith("Error saving file:")),
                "Should catch IOException and log error message when file path is invalid"
        );
    }

}