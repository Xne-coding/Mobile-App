package com.questlearn;

public class ChallengeStep {
    private int number;
    private String text;
    private boolean completed;

    public ChallengeStep(int number, String text, boolean completed) {
        this.number = number;
        this.text = text;
        this.completed = completed;
    }

    public int getNumber() { return number; }
    public String getText() { return text; }
    public boolean isCompleted() { return completed; }
}
