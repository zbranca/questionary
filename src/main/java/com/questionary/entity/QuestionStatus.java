package com.questionary.entity;

public enum QuestionStatus {
    UNANSWERED("—"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String label;

    QuestionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
