package com.questlearn;

/*
 * AvatarOption — a preset profile picture: stable id, emoji, and circle background colour.
 * The list is fixed in code; the chosen id is stored in prefs / Firestore.
 */

import java.util.ArrayList;
import java.util.List;

public class AvatarOption {
    private final String id;
    private final String emoji;
    private final int backgroundColor;

    public AvatarOption(String id, String emoji, int backgroundColor) {
        this.id = id;
        this.emoji = emoji;
        this.backgroundColor = backgroundColor;
    }

    public String getId() { return id; }
    public String getEmoji() { return emoji; }
    public int getBackgroundColor() { return backgroundColor; }

    public static List<AvatarOption> getAll() {
        List<AvatarOption> list = new ArrayList<>();
        list.add(new AvatarOption("av1",  "\uD83D\uDE0A", 0xFF2196F3)); // smiling face, blue
        list.add(new AvatarOption("av2",  "\uD83D\uDE0E", 0xFF9C27B0)); // sunglasses, purple
        list.add(new AvatarOption("av3",  "\uD83E\uDDD1\u200D\uD83C\uDF93", 0xFF4CAF50)); // student, green
        list.add(new AvatarOption("av4",  "\uD83D\uDE80", 0xFFFF9800)); // rocket, orange
        list.add(new AvatarOption("av5",  "\uD83E\uDD16", 0xFF607D8B)); // robot, blue-grey
        list.add(new AvatarOption("av6",  "\uD83E\uDDD9", 0xFF673AB7)); // mage, deep purple
        list.add(new AvatarOption("av7",  "\uD83C\uDFC6", 0xFFFFC107)); // trophy, amber
        list.add(new AvatarOption("av8",  "\uD83C\uDF1F", 0xFFE91E63)); // star, pink
        list.add(new AvatarOption("av9",  "\uD83D\uDC3B", 0xFF795548)); // bear, brown
        list.add(new AvatarOption("av10", "\uD83D\uDC31", 0xFFFF5722)); // cat, deep orange
        list.add(new AvatarOption("av11", "\uD83D\uDC36", 0xFF8BC34A)); // dog, light green
        list.add(new AvatarOption("av12", "\uD83E\uDD8A", 0xFFFF7043)); // fox, orange
        list.add(new AvatarOption("av13", "\uD83D\uDC27", 0xFF37474F)); // penguin, dark grey
        list.add(new AvatarOption("av14", "\uD83E\uDD89", 0xFF5C6BC0)); // owl, indigo
        list.add(new AvatarOption("av15", "\uD83C\uDF32", 0xFF388E3C)); // tree, dark green
        list.add(new AvatarOption("av16", "\u26A1",       0xFF1565C0)); // lightning, dark blue
        return list;
    }

    public static AvatarOption findById(String id) {
        if (id == null) return null;
        for (AvatarOption opt : getAll()) {
            if (opt.getId().equals(id)) return opt;
        }
        return null;
    }
}
