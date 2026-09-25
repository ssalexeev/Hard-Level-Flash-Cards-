package org.example.flashcards;

public enum Action {
    ADD("add"),
    REMOVE("remove"),
    IMPORT("import"),
    EXPORT("export"),
    ASK("ask"),
    EXIT("exit"),
    LOG("log"),
    HARDEST_CARD("hardest card"),
    RESET_STATS("reset stats");

    private final String value;

    Action(String value) {
        this.value = value;
    }

    public static Action fromValue(String value){
        for(Action action : values()){
            if (action.value.equals(value)) return action;
        }
        throw new IllegalArgumentException("Unknown Action: " + value);
    }
}
