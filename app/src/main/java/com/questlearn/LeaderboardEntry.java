package com.questlearn;

public class LeaderboardEntry {
    private int rank;
    private String name;
    private String initials;
    private int xpPoints;
    private int avatarColor;
    private boolean isCurrentUser;

    public LeaderboardEntry(int rank, String name, String initials,
                            int xpPoints, int avatarColor, boolean isCurrentUser) {
        this.rank = rank;
        this.name = name;
        this.initials = initials;
        this.xpPoints = xpPoints;
        this.avatarColor = avatarColor;
        this.isCurrentUser = isCurrentUser;
    }

    public int getRank() { return rank; }
    public String getName() { return name; }
    public String getInitials() { return initials; }
    public int getXpPoints() { return xpPoints; }
    public int getAvatarColor() { return avatarColor; }
    public boolean isCurrentUser() { return isCurrentUser; }

    public String getRankDisplay() {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return String.valueOf(rank);
        }
    }

    public static java.util.List<LeaderboardEntry> getSampleData() {
        java.util.List<LeaderboardEntry> list = new java.util.ArrayList<>();
        list.add(new LeaderboardEntry(1, "Sophie R.", "SR", 2340, 0xFFFF9800, false));
        list.add(new LeaderboardEntry(2, "James O.", "JO", 1980, 0xFF9C27B0, false));
        list.add(new LeaderboardEntry(3, "Ava L.", "AL", 1560, 0xFF009688, false));
        list.add(new LeaderboardEntry(4, "You", "DM", 1240, 0xFF2196F3, true));
        return list;
    }
}
