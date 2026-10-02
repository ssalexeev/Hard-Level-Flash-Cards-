package org.example.flashcards;

import org.example.flashcards.entity.FlashCard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class FileCardService {
    private final Map<String, FlashCard> cards;
    private final Logger logger;


    public FileCardService() {
        this.cards = new LinkedHashMap<>();
        this.logger = new Logger();
    }

    public Logger logger() {
        return this.logger;
    }

    public static class Logger {
        public final List<String> logHistory = new ArrayList<>();

        public void println(String message) {
            System.out.println(message);
            logHistory.add(message);
        }

        public void printFormatted(String format, Object... args) {
            String formatted = String.format(format, args);
            System.out.print(formatted);
            for (String line : formatted.split("\r?\n")) {
                if (!line.isEmpty()) {
                    logHistory.add(line);
                }
            }
        }
    }

    public String readLine(Scanner scanner) {
        String input = scanner.nextLine();
        logger.logHistory.add(input);
        return input;
    }

    private Set<String> getDefinitions() {
        return cards.values().stream()
                .map(FlashCard::getDefinition)
                .collect(Collectors.toSet());
    }

    public void addCard(Scanner scanner) {
        logger.println("The card:");
        String card = readLine(scanner);

        if (cards.containsKey(card)) {
            logger.println("The card \"" + card + "\" already exists.");
            return;
        }

        logger.println("The definition of the card:");
        String definition = readLine(scanner);
        if (getDefinitions().contains(definition)) {
            logger.println("The definition \"" + definition + "\" already exists.");
        }

        FlashCard flashCard = new FlashCard(card, definition);
        cards.put(card, flashCard);
        logger.println("The pair (\"" + card + "\":\"" + definition + "\") has been added");
    }

    public void removeCard(Scanner scanner) {
        logger.println("Which card?");
        String term = readLine(scanner);

        if (cards.containsKey(term)) {
            cards.remove(term);
            logger.println("The card has been removed.");
        } else {
            logger.println("Can't remove \"" + term + "\": there is no such card.");
        }
    }

    public void loadCardsFromFile(String fileName) {
        Path filePath = Path.of(fileName);

        if (!Files.exists(filePath)) {
            logger.println("File not found.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            int count = 0;

            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    cards.put(parts[0], new FlashCard(parts[0], parts[1], Integer.parseInt(parts[2])));
                    count++;
                }
            }
            logger.println(count + " cards have been loaded.");
        } catch (IOException | NumberFormatException e) {
            logger.println("File not found.");
        }
    }


    // Interactive file loading prompt via Scanner
    public void loadCardsFromFile(Scanner scanner) {
        logger.println("File name:");
        String fileName = readLine(scanner);
        loadCardsFromFile(fileName);
    }

    // Programmatic saving (CLI -export) -> NO "File name:" prompt!
    public void saveCardsToFile(String fileName) {
        Path filePath = Path.of(fileName);

        try {
            List<String> lines = cards.values()
                    .stream()
                    .map(flashCard -> flashCard.getTerm()
                            + "," + flashCard.getDefinition()
                            + "," + flashCard.getCountOfErrors())
                    .toList();

            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.println(cards.size() + " cards have been saved.\n");
        } catch (IOException e) {
            logger.println("Error saving file: " + e.getMessage() + "\n");
        }
    }

    // Interactive file saving prompt via Scanner
    public void saveCardsToFile(Scanner scanner) {
        logger.println("File name:");
        String fileName = readLine(scanner);
        saveCardsToFile(fileName);
    }

    public void ask(Scanner scanner) {
        logger.println("How many times to ask?");
        int times = Integer.parseInt(readLine(scanner));

        if (cards.isEmpty()) {
            return;
        }

        List<String> terms = List.copyOf(cards.keySet());
        int index = 0;

        for (int i = 0; i < times; i++) {
            String term = terms.get(index % terms.size());
            FlashCard flashCard = cards.get(term);
            String correctDefinition = flashCard.getDefinition();

            logger.println("Print the definition of \"" + term + "\":");
            String userAnswer = readLine(scanner);

            if (userAnswer.equals(correctDefinition)) {
                logger.println("Correct!");
            } else {
                flashCard.setCountOfErrors(flashCard.getCountOfErrors() + 1);

                if (getDefinitions().contains(userAnswer)) {
                    String otherTerm = cards.values().stream()
                            .filter(e -> e.getDefinition().equals(userAnswer))
                            .map(FlashCard::getTerm)
                            .findFirst()
                            .orElse("");

                    logger.println("Wrong. The right answer is \"" + correctDefinition +
                            "\", but your definition is correct for \"" + otherTerm + "\" card.");
                } else {
                    logger.println("Wrong. The right answer is \"" + correctDefinition + "\".");
                }
            }
            index++;
        }
        logger.println("");
    }

    public void resetErrors() {
        cards.values().forEach(c -> c.setCountOfErrors(0));
        logger.println("Card statistics have been reset.");
    }

    public void hardestCard() {
        int maxErrors = cards.values().stream()
                .mapToInt(FlashCard::getCountOfErrors)
                .max()
                .orElse(0);

        if (maxErrors == 0) {
            logger.println("There are no cards with errors.");
            return;
        }

        List<FlashCard> hardestCards = cards.values().stream()
                .filter(card -> card.getCountOfErrors() == maxErrors)
                .toList();

        String cardNames = hardestCards.stream()
                .map(c -> "\"" + c.getTerm() + "\"")
                .collect(Collectors.joining(", "));

        boolean isPlural = hardestCards.size() > 1;
        String isOrAre = isPlural ? "are" : "is";
        String cardOrCards = isPlural ? "cards" : "card";
        String themOrIt = isPlural ? "them" : "it";

        logger.printFormatted("The hardest %s %s %s. You have %d errors answering %s.%n%n",
                cardOrCards, isOrAre, cardNames, maxErrors, themOrIt);
    }

    public void saveLog(final Scanner scanner) {
        logger.println("File name:");
        String fileName = readLine(scanner);

        try {
            Files.write(Path.of(fileName), logger.logHistory,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            logger.println("The log has been saved.");
        } catch (IOException e) {
            logger.println("Error saving log file: " + e.getMessage() + "\n");
        }
    }
}
