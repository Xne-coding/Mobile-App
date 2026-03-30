package com.questlearn;

public class Challenge {
    public enum AccentColor { BLUE, GREEN, ORANGE }

    private String id;
    private String name;
    private String location;
    private int distanceMeters;
    private int xpPoints;
    private int progressPercent;
    private AccentColor accentColor;
    private String icon;

    public Challenge(String id, String name, String location, int distanceMeters,
                     int xpPoints, int progressPercent, AccentColor accentColor, String icon) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.distanceMeters = distanceMeters;
        this.xpPoints = xpPoints;
        this.progressPercent = progressPercent;
        this.accentColor = accentColor;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public int getDistanceMeters() { return distanceMeters; }
    public int getXpPoints() { return xpPoints; }
    public int getProgressPercent() { return progressPercent; }
    public AccentColor getAccentColor() { return accentColor; }
    public String getIcon() { return icon; }

    // Sample data factory
    public static java.util.List<Challenge> getSampleData() {
        java.util.List<Challenge> list = new java.util.ArrayList<>();
        list.add(new Challenge("ch1", "Clifton Quest: Library Discovery",
                "Clifton Library", 80, 150, 65, AccentColor.BLUE, "📚"));
        list.add(new Challenge("ch2", "Science Trail: Erasmus Labs",
                "Erasmus Darwin Building", 220, 200, 30, AccentColor.GREEN, "⚗️"));
        list.add(new Challenge("ch3", "Sports Route: Clifton Sports Centre",
                "Clifton Sports Village", 340, 100, 90, AccentColor.ORANGE, "🏟️"));
        return list;
    }
}
