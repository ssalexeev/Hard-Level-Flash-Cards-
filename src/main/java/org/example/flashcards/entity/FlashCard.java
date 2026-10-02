package org.example.flashcards.entity;

public class FlashCard {
    private String term;
    private String definition;
    private int countOfErrors = 0;


    public FlashCard(String term, String definition) {
        this.term = term;
        this.definition = definition;
    }
    public FlashCard(String term, String definition, int countOfErrors) {
        this.term = term;
        this.definition = definition;
        this.countOfErrors = countOfErrors;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public int getCountOfErrors() {
        return countOfErrors;
    }

    public void setCountOfErrors(int countOfErrors) {
        this.countOfErrors = countOfErrors;
    }
}
