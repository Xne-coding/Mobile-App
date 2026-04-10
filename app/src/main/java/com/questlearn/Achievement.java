package com.questlearn;

/** Profile achievement tile: title, blurb, icon, bonus XP, colour, locked or unlocked. */
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
}
