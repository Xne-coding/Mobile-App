package com.questlearn;

/*
 * Challenge — everything the home list and detail screen need for one quest:
 * title, building name, XP, progress, accent colour, and a little icon character.
 */

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

    /** @return matching challenge from the catalog, or null */
    public static Challenge findById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Challenge c : getSampleData()) {
            if (id.equals(c.getId())) return c;
        }
        return null;
    }

    public static java.util.List<Challenge> getSampleData() {
        java.util.List<Challenge> list = new java.util.ArrayList<>();
        list.add(new Challenge("ch1",  "Library Discovery",           "Clifton Library",              0, 150, 0, AccentColor.BLUE,   "📚"));
        list.add(new Challenge("ch2",  "Science Trail",               "Erasmus Darwin",               0, 200, 0, AccentColor.GREEN,  "⚗️"));
        list.add(new Challenge("ch3",  "Sports Route",                "Lee Westwood Sports Centre",   0, 100, 0, AccentColor.ORANGE, "🏟️"));
        list.add(new Challenge("ch4",  "Union Trail",                 "Students' Union",              0, 120, 0, AccentColor.BLUE,   "🎉"));
        list.add(new Challenge("ch5",  "Byron's Code",                "Ada Byron King",               0, 150, 0, AccentColor.GREEN,  "💻"));
        list.add(new Challenge("ch6",  "Clare's Corner",              "John Clare Lecture Theatre",    0, 100, 0, AccentColor.BLUE,   "🎓"));
        list.add(new Challenge("ch7",  "Knowledge Hub",               "Teaching & Learning",          0, 130, 0, AccentColor.GREEN,  "📖"));
        list.add(new Challenge("ch8",  "Lab Quest",                   "CELS / NSRC",                  0, 180, 0, AccentColor.ORANGE, "🔬"));
        list.add(new Challenge("ch9",  "Lawrence Walk",               "DH Lawrence",                  0, 110, 0, AccentColor.BLUE,   "✍️"));
        list.add(new Challenge("ch10", "Evans Explorer",              "Mary Ann Evans",               0, 140, 0, AccentColor.GREEN,  "📝"));
        list.add(new Challenge("ch11", "Robbins Route",               "Lionel Robbins",               0, 120, 0, AccentColor.BLUE,   "📊"));
        list.add(new Challenge("ch12", "Nolan Mission",               "Anthony Nolan",                0, 160, 0, AccentColor.ORANGE, "🧬"));
        list.add(new Challenge("ch13", "Research Quest",              "Cancer Research Centre",        0, 200, 0, AccentColor.GREEN,  "🏥"));
        list.add(new Challenge("ch14", "Tech Trail",                  "ISTeC",                        0, 130, 0, AccentColor.BLUE,   "⚙️"));
        list.add(new Challenge("ch15", "Hall Hunt",                   "New Hall Block",               0, 110, 0, AccentColor.ORANGE, "🏠"));
        list.add(new Challenge("ch16", "Club Quest",                  "The Clubhouse",                0, 100, 0, AccentColor.BLUE,   "☕"));
        list.add(new Challenge("ch17", "Cricket Challenge",           "Cricket Pavilion",             0, 90,  0, AccentColor.GREEN,  "🏏"));
        list.add(new Challenge("ch18", "Engineer's Path",             "Engineering Buildings",        0, 170, 0, AccentColor.ORANGE, "🔧"));
        list.add(new Challenge("ch19", "Smart Lab",                   "iSMART",                       0, 190, 0, AccentColor.GREEN,  "🤖"));
        list.add(new Challenge("ch20", "Pavilion Quest",              "Pavilion Building",            0, 100, 0, AccentColor.BLUE,   "🍽️"));
        list.add(new Challenge("ch21", "Franklin's Lab",              "Rosalind Franklin",            0, 250, 0, AccentColor.ORANGE, "🧪"));
        return list;
    }
}
