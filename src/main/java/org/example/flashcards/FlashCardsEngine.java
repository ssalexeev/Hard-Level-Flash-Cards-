package org.example.flashcards;

import java.util.Scanner;

public class FlashCardsEngine {

    private final FileCardService fileCardService;

    private String exportFile = null;
    private String importFile = null;


    public FlashCardsEngine() {
        this.fileCardService = new FileCardService();
    }


    public void start(String[] args) {
        Scanner scanner = new Scanner(System.in);
        processArgs(args);

        if (importFile != null) {
            fileCardService.loadCardsFromFile(importFile);
        }

        boolean workFlag = true;
        while (workFlag) {
            fileCardService.logger().println("Input the action (add, remove, import, export, ask, exit):");

            Action action = Action.fromValue(fileCardService.readLine(scanner));

            switch (action) {
                case ADD -> fileCardService.addCard(scanner);
                case REMOVE -> fileCardService.removeCard(scanner);
                case IMPORT -> fileCardService.loadCardsFromFile(scanner);

                case EXPORT -> {
                    if (exportFile != null) {
                        fileCardService.saveCardsToFile(exportFile);
                    } else {
                        fileCardService.saveCardsToFile(scanner);
                    }
                }
                case ASK -> fileCardService.ask(scanner);
                case EXIT -> {
                    fileCardService.logger().println("Bye bye!");
                    if (exportFile != null) {
                        fileCardService.saveCardsToFile(exportFile);
                    }
                    workFlag = false;
                    scanner.close();
                }
                case LOG -> fileCardService.saveLog(scanner);
                case HARDEST_CARD -> fileCardService.hardestCard();
                case RESET_STATS -> fileCardService.resetErrors();
                default -> throw new RuntimeException("Unknown Exception");
            }
        }
    }

    public void processArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-import".equals(args[i]) && i + 1 < args.length) {
                this.importFile = args[i + 1];
                i++;
            } else if ("-export".equals(args[i]) && i + 1 < args.length) {
                this.exportFile = args[i + 1];
                i++;
            }
        }
    }


}
