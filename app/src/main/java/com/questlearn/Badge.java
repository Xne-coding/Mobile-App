package com.questlearn;

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

    public static java.util.List<Badge> getSampleData() {
        java.util.List<Badge> list = new java.util.ArrayList<>();
        list.add(new Badge("First Steps", "🗺️", 0xFFE3F2FD, true));
        list.add(new Badge("Bookworm", "📚", 0xFFC8E6C9, true));
        list.add(new Badge("Scientist", "🔬", 0xFFFCE4EC, true));
        list.add(new Badge("Sprinter", "🏃", 0xFFFFE0B2, true));
        list.add(new Badge("Star Pupil", "🌟", 0xFFEDE7F6, true));
        list.add(new Badge("Beaconeer", "📡", 0xFFE0F7FA, true));
        list.add(new Badge("Champion", "🏆", 0xFFEEEEEE, false));
        list.add(new Badge("Eagle Eye", "🦅", 0xFFEEEEEE, false));
        return list;
    }
}
