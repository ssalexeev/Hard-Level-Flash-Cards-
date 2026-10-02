package org.example.flashcards.enums;

public enum Action {
    ADD("add"),
    REMOVE("remove"),
    IMPORT("import"),
    EXPORT("export"),
    ASK("ask"),
    EXIT("exit"),
    LOG("log"),
    HARDEST_CARD("hardest_card"),
    RESET_STATS("reset_stats");

    private final String value;

    Action(String value) {
        this.value = value;
    }

    public static Action fromValue(String value) {
        for (Action action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown Action: " + value);
    }
}
