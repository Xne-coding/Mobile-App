package com.questlearn;

public class Achievement {
    private String name;
    private String description;
    private String icon;
    private int xpEarned;
    private int iconBackgroundColor;
    private boolean unlocked;

    public Achievement(String name, String description, String icon,
                       int xpEarned, int iconBackgroundColor, boolean unlocked) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.xpEarned = xpEarned;
        this.iconBackgroundColor = iconBackgroundColor;
        this.unlocked = unlocked;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getXpEarned() { return xpEarned; }
    public int getIconBackgroundColor() { return iconBackgroundColor; }
    public boolean isUnlocked() { return unlocked; }

    public static java.util.List<Achievement> getSampleData() {
        java.util.List<Achievement> list = new java.util.ArrayList<>();
        list.add(new Achievement(
                "Campus Navigator",
                "Visited 10 unique locations on campus",
                "🗺️", 200, 0xFFE3F2FD, true));
        list.add(new Achievement(
                "Library Scholar",
                "Completed all 5 Library challenges",
                "📚", 300, 0xFFC8E6C9, true));
        list.add(new Achievement(
                "7-Day Streak",
                "Check in every day for a week",
                "🔥", 500, 0xFFFFE0B2, true));
        list.add(new Achievement(
                "Campus Champion",
                "Reach #1 on the leaderboard",
                "🏆", 1000, 0xFFEEEEEE, false));
        return list;
    }
}
