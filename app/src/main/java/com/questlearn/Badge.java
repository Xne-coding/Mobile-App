package com.questlearn;

/** Small badge earned by finishing a specific challenge (emoji + name + earned flag). */
public class Badge {
    private String name;
    private String emoji;
    private int backgroundColor;
    private boolean earned;

    public Badge(String name, String emoji, int backgroundColor, boolean earned) {
        this.name = name;
        this.emoji = emoji;
        this.backgroundColor = backgroundColor;
        this.earned = earned;
    }

    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public int getBackgroundColor() { return backgroundColor; }
    public boolean isEarned() { return earned; }
}
