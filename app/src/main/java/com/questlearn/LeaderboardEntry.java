package com.questlearn;

/**
 * One row on the leaderboard: rank, display name, avatar (emoji preset or initials),
 * XP total, and whether this row is the current user (for blue highlight).
 */
public class LeaderboardEntry {
    private int rank;
    private String name;
    private String initials;
    private String avatarId;
    private int xpPoints;
    private int avatarColor;
    private boolean isCurrentUser;

    public LeaderboardEntry(int rank, String name, String initials, String avatarId,
                            int xpPoints, int avatarColor, boolean isCurrentUser) {
        this.rank = rank;
        this.name = name;
        this.initials = initials;
        this.avatarId = avatarId;
        this.xpPoints = xpPoints;
        this.avatarColor = avatarColor;
        this.isCurrentUser = isCurrentUser;
    }

    public int getRank() { return rank; }
    public String getName() { return name; }
    public String getInitials() { return initials; }
    public String getAvatarId() { return avatarId; }
    public int getXpPoints() { return xpPoints; }
    public int getAvatarColor() { return avatarColor; }
    public boolean isCurrentUser() { return isCurrentUser; }

    /** Top three get medal emojis; everyone else sees the numeric rank. */
    public String getRankDisplay() {
        switch (rank) {
            case 1: return "\uD83E\uDD47";
            case 2: return "\uD83E\uDD48";
            case 3: return "\uD83E\uDD49";
            default: return String.valueOf(rank);
        }
    }
}
