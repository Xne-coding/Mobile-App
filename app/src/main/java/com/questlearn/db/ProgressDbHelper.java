package com.questlearn.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProgressDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "questlearn.db";
    private static final int DB_VERSION = 3;

    private static final String TABLE_PROGRESS = "progress";
    private static final String TABLE_CHALLENGE_STATE = "challenge_state";
    private static final String TABLE_SCAN_HISTORY = "scan_history";
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_LEVEL = "level";
    private static final String COL_XP = "xp";
    private static final String COL_XP_TARGET = "xp_target";
    private static final String COL_CHALLENGES = "challenges_done";
    private static final String COL_RANK = "rank_value";
    private static final String COL_STREAK = "streak";
    private static final String COL_BADGES = "badges";
    private static final String COL_CHALLENGE_ID = "challenge_id";
    private static final String COL_COMPLETED = "completed";
    private static final String COL_LAST_CODE = "last_code";
    private static final String COL_COMPLETED_AT = "completed_at";
    private static final String COL_CODE = "code";
    private static final String COL_XP_REWARD = "xp_reward";
    private static final String COL_SCANNED_AT = "scanned_at";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";
    private static final String COL_DISPLAY_NAME = "display_name";
    private static final String COL_INITIALS = "initials";
    private static final String COL_CREATED_AT = "created_at";

    private static final String CH1 = "ch1";
    private static final String CH2 = "ch2";
    private static final String CH3 = "ch3";

    private static final Map<String, CodeConfig> HARDCODED_CODES = new HashMap<>();

    static {
        HARDCODED_CODES.put("CLIFTON-LIB-001", new CodeConfig(CH1, 150));
        HARDCODED_CODES.put("ERASMUS-LABS-220", new CodeConfig(CH2, 200));
        HARDCODED_CODES.put("SPORTS-VILLAGE-340", new CodeConfig(CH3, 100));
    }

    private static class CodeConfig {
        final String challengeId;
        final int xpReward;

        CodeConfig(String challengeId, int xpReward) {
            this.challengeId = challengeId;
            this.xpReward = xpReward;
        }
    }

    public static class ProgressData {
        public int level;
        public int xp;
        public int xpTarget;
        public int challengesDone;
        public int rankValue;
        public int streak;
        public int badges;
    }

    public static class CodeRedemptionResult {
        public boolean success;
        public String message;
        public String challengeId;
        public int xpReward;
        public ProgressData progressData;
    }

    public static class AuthResult {
        public boolean success;
        public String message;
        public String displayName;
        public String initials;
    }

    public ProgressDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_PROGRESS + " ("
                        + COL_ID + " INTEGER PRIMARY KEY, "
                        + COL_LEVEL + " INTEGER NOT NULL, "
                        + COL_XP + " INTEGER NOT NULL, "
                        + COL_XP_TARGET + " INTEGER NOT NULL, "
                        + COL_CHALLENGES + " INTEGER NOT NULL, "
                        + COL_RANK + " INTEGER NOT NULL, "
                        + COL_STREAK + " INTEGER NOT NULL, "
                        + COL_BADGES + " INTEGER NOT NULL"
                        + ")"
        );
        db.execSQL(
                "CREATE TABLE " + TABLE_CHALLENGE_STATE + " ("
                        + COL_CHALLENGE_ID + " TEXT PRIMARY KEY, "
                        + COL_COMPLETED + " INTEGER NOT NULL, "
                        + COL_LAST_CODE + " TEXT, "
                        + COL_COMPLETED_AT + " INTEGER NOT NULL"
                        + ")"
        );
        db.execSQL(
                "CREATE TABLE " + TABLE_SCAN_HISTORY + " ("
                        + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_CODE + " TEXT NOT NULL, "
                        + COL_CHALLENGE_ID + " TEXT NOT NULL, "
                        + COL_XP_REWARD + " INTEGER NOT NULL, "
                        + COL_SCANNED_AT + " INTEGER NOT NULL"
                        + ")"
        );
        db.execSQL(
                "CREATE TABLE " + TABLE_USERS + " ("
                        + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_EMAIL + " TEXT NOT NULL UNIQUE, "
                        + COL_PASSWORD + " TEXT NOT NULL, "
                        + COL_DISPLAY_NAME + " TEXT NOT NULL, "
                        + COL_INITIALS + " TEXT NOT NULL, "
                        + COL_CREATED_AT + " INTEGER NOT NULL"
                        + ")"
        );
        insertDefaultProgress(db);
        seedChallengeState(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_CHALLENGE_STATE + " ("
                            + COL_CHALLENGE_ID + " TEXT PRIMARY KEY, "
                            + COL_COMPLETED + " INTEGER NOT NULL, "
                            + COL_LAST_CODE + " TEXT, "
                            + COL_COMPLETED_AT + " INTEGER NOT NULL"
                            + ")"
            );
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_SCAN_HISTORY + " ("
                            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + COL_CODE + " TEXT NOT NULL, "
                            + COL_CHALLENGE_ID + " TEXT NOT NULL, "
                            + COL_XP_REWARD + " INTEGER NOT NULL, "
                            + COL_SCANNED_AT + " INTEGER NOT NULL"
                            + ")"
            );
            seedChallengeState(db);
        }
        if (oldVersion < 3) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + COL_EMAIL + " TEXT NOT NULL UNIQUE, "
                            + COL_PASSWORD + " TEXT NOT NULL, "
                            + COL_DISPLAY_NAME + " TEXT NOT NULL, "
                            + COL_INITIALS + " TEXT NOT NULL, "
                            + COL_CREATED_AT + " INTEGER NOT NULL"
                            + ")"
            );
        }
    }

    public ProgressData getProgress() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_PROGRESS,
                null,
                COL_ID + "=?",
                new String[]{"1"},
                null,
                null,
                null
        );

        try {
            if (c.moveToFirst()) {
                ProgressData data = mapCursor(c);
                return data;
            }
        } finally {
            c.close();
        }

        SQLiteDatabase writable = getWritableDatabase();
        insertDefaultProgress(writable);
        return getProgress();
    }

    public ProgressData addXp(int xpToAdd) {
        ProgressData data = getProgress();
        if (xpToAdd < 0) xpToAdd = 0;
        data.xp += xpToAdd;
        while (data.xp >= data.xpTarget) {
            data.level++;
            data.xp -= data.xpTarget;
        }
        writeProgress(data);
        return data;
    }

    public ProgressData completeChallenge(int xpReward) {
        ProgressData data = getProgress();
        data.challengesDone += 1;
        data.streak += 1;
        if (xpReward < 0) xpReward = 0;
        data.xp += xpReward;
        while (data.xp >= data.xpTarget) {
            data.level++;
            data.xp -= data.xpTarget;
        }
        writeProgress(data);
        return data;
    }

    public ProgressData addBadge() {
        ProgressData data = getProgress();
        data.badges += 1;
        writeProgress(data);
        return data;
    }

    public ProgressData incrementStreak() {
        ProgressData data = getProgress();
        data.streak += 1;
        writeProgress(data);
        return data;
    }

    public ProgressData resetStreak() {
        ProgressData data = getProgress();
        data.streak = 0;
        writeProgress(data);
        return data;
    }

    public void resetProgress() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getDefaultValues();
        db.update(TABLE_PROGRESS, values, COL_ID + "=?", new String[]{"1"});
        db.delete(TABLE_SCAN_HISTORY, null, null);
        ContentValues resetState = new ContentValues();
        resetState.put(COL_COMPLETED, 0);
        resetState.putNull(COL_LAST_CODE);
        resetState.put(COL_COMPLETED_AT, 0);
        db.update(TABLE_CHALLENGE_STATE, resetState, null, null);
    }

    public CodeRedemptionResult redeemHardcodedCode(String rawCode, String expectedChallengeId) {
        CodeRedemptionResult result = new CodeRedemptionResult();
        String code = normalizeCode(rawCode);
        if (code.isEmpty()) {
            result.success = false;
            result.message = "Code is empty.";
            return result;
        }

        CodeConfig cfg = HARDCODED_CODES.get(code);
        if (cfg == null) {
            result.success = false;
            result.message = "Invalid code.";
            return result;
        }

        if (expectedChallengeId != null && !expectedChallengeId.isEmpty()
                && !expectedChallengeId.equals(cfg.challengeId)) {
            result.success = false;
            result.message = "This code does not match the current challenge.";
            return result;
        }

        if (isChallengeCompleted(cfg.challengeId)) {
            result.success = false;
            result.message = "Challenge already completed.";
            return result;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ProgressData progress = getProgress();
            progress.challengesDone += 1;
            progress.streak += 1;
            progress.xp += cfg.xpReward;
            while (progress.xp >= progress.xpTarget) {
                progress.level += 1;
                progress.xp -= progress.xpTarget;
            }
            writeProgress(progress);
            markChallengeCompleted(db, cfg.challengeId, code);
            insertScanHistory(db, code, cfg.challengeId, cfg.xpReward);
            db.setTransactionSuccessful();

            result.success = true;
            result.challengeId = cfg.challengeId;
            result.xpReward = cfg.xpReward;
            result.progressData = progress;
            result.message = "Code accepted.";
        } finally {
            db.endTransaction();
        }

        return result;
    }

    public String exportDatabaseForSqliteViewer(Context context) throws IOException {
        File dbFile = context.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            getReadableDatabase();
        }

        File targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (targetDir == null) {
            targetDir = context.getExternalFilesDir(null);
        }
        if (targetDir == null) {
            throw new IOException("No external files directory available.");
        }
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Could not create export directory.");
        }

        File outFile = new File(targetDir, "questlearn_export.db");
        copyFile(context.getDatabasePath(DB_NAME), outFile);
        return outFile.getAbsolutePath();
    }

    public AuthResult createUser(String rawEmail, String rawPassword, String rawDisplayName) {
        AuthResult result = new AuthResult();
        String email = normalizeCode(rawEmail).toLowerCase(Locale.ROOT);
        String password = rawPassword == null ? "" : rawPassword.trim();
        String displayName = rawDisplayName == null ? "" : rawDisplayName.trim();
        if (email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
            result.success = false;
            result.message = "All fields are required.";
            return result;
        }
        if (password.length() < 6) {
            result.success = false;
            result.message = "Password must be at least 6 characters.";
            return result;
        }

        if (userExists(email)) {
            result.success = false;
            result.message = "Account already exists.";
            return result;
        }

        String initials = getInitials(displayName);
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_DISPLAY_NAME, displayName);
        values.put(COL_INITIALS, initials);
        values.put(COL_CREATED_AT, System.currentTimeMillis());

        long row = getWritableDatabase().insert(TABLE_USERS, null, values);
        if (row == -1) {
            result.success = false;
            result.message = "Could not create account.";
            return result;
        }
        result.success = true;
        result.message = "Account created.";
        result.displayName = displayName;
        result.initials = initials;
        return result;
    }

    public AuthResult authenticateUser(String rawEmail, String rawPassword) {
        AuthResult result = new AuthResult();
        String email = normalizeCode(rawEmail).toLowerCase(Locale.ROOT);
        String password = rawPassword == null ? "" : rawPassword.trim();
        Cursor c = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{COL_PASSWORD, COL_DISPLAY_NAME, COL_INITIALS},
                COL_EMAIL + "=?",
                new String[]{email},
                null,
                null,
                null
        );
        try {
            if (!c.moveToFirst()) {
                result.success = false;
                result.message = "No account found for this email.";
                return result;
            }
            String storedPassword = c.getString(0);
            if (!storedPassword.equals(password)) {
                result.success = false;
                result.message = "Incorrect password.";
                return result;
            }
            result.success = true;
            result.message = "Sign-in successful.";
            result.displayName = c.getString(1);
            result.initials = c.getString(2);
            return result;
        } finally {
            c.close();
        }
    }

    public boolean updatePassword(String rawEmail, String newPassword) {
        String email = normalizeCode(rawEmail).toLowerCase(Locale.ROOT);
        ContentValues values = new ContentValues();
        values.put(COL_PASSWORD, newPassword == null ? "" : newPassword.trim());
        int updated = getWritableDatabase().update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{email});
        return updated > 0;
    }

    private boolean userExists(String email) {
        Cursor c = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{COL_ID},
                COL_EMAIL + "=?",
                new String[]{email},
                null,
                null,
                null
        );
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
        if (parts.length == 1 && !parts[0].isEmpty()) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return "U";
    }

    private ProgressData mapCursor(Cursor c) {
        ProgressData data = new ProgressData();
        data.level = c.getInt(c.getColumnIndexOrThrow(COL_LEVEL));
        data.xp = c.getInt(c.getColumnIndexOrThrow(COL_XP));
        data.xpTarget = c.getInt(c.getColumnIndexOrThrow(COL_XP_TARGET));
        data.challengesDone = c.getInt(c.getColumnIndexOrThrow(COL_CHALLENGES));
        data.rankValue = c.getInt(c.getColumnIndexOrThrow(COL_RANK));
        data.streak = c.getInt(c.getColumnIndexOrThrow(COL_STREAK));
        data.badges = c.getInt(c.getColumnIndexOrThrow(COL_BADGES));
        return data;
    }

    private void writeProgress(ProgressData data) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LEVEL, data.level);
        values.put(COL_XP, data.xp);
        values.put(COL_XP_TARGET, data.xpTarget);
        values.put(COL_CHALLENGES, data.challengesDone);
        values.put(COL_RANK, data.rankValue);
        values.put(COL_STREAK, data.streak);
        values.put(COL_BADGES, data.badges);
        db.update(TABLE_PROGRESS, values, COL_ID + "=?", new String[]{"1"});
    }

    private void seedChallengeState(SQLiteDatabase db) {
        upsertChallengeState(db, CH1);
        upsertChallengeState(db, CH2);
        upsertChallengeState(db, CH3);
    }

    private void upsertChallengeState(SQLiteDatabase db, String challengeId) {
        ContentValues values = new ContentValues();
        values.put(COL_CHALLENGE_ID, challengeId);
        values.put(COL_COMPLETED, 0);
        values.put(COL_COMPLETED_AT, 0);
        db.insertWithOnConflict(TABLE_CHALLENGE_STATE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private boolean isChallengeCompleted(String challengeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_CHALLENGE_STATE,
                new String[]{COL_COMPLETED},
                COL_CHALLENGE_ID + "=?",
                new String[]{challengeId},
                null,
                null,
                null
        );
        try {
            if (c.moveToFirst()) {
                return c.getInt(0) == 1;
            }
            return false;
        } finally {
            c.close();
        }
    }

    private void markChallengeCompleted(SQLiteDatabase db, String challengeId, String code) {
        ContentValues values = new ContentValues();
        values.put(COL_COMPLETED, 1);
        values.put(COL_LAST_CODE, code);
        values.put(COL_COMPLETED_AT, System.currentTimeMillis());
        db.update(TABLE_CHALLENGE_STATE, values, COL_CHALLENGE_ID + "=?", new String[]{challengeId});
    }

    private void insertScanHistory(SQLiteDatabase db, String code, String challengeId, int xpReward) {
        ContentValues values = new ContentValues();
        values.put(COL_CODE, code);
        values.put(COL_CHALLENGE_ID, challengeId);
        values.put(COL_XP_REWARD, xpReward);
        values.put(COL_SCANNED_AT, System.currentTimeMillis());
        db.insert(TABLE_SCAN_HISTORY, null, values);
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null) return "";
        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

    private void copyFile(File source, File target) throws IOException {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    private void insertDefaultProgress(SQLiteDatabase db) {
        ContentValues values = getDefaultValues();
        db.insertWithOnConflict(TABLE_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private ContentValues getDefaultValues() {
        ContentValues values = new ContentValues();
        values.put(COL_ID, 1);
        values.put(COL_LEVEL, 0);
        values.put(COL_XP, 0);
        values.put(COL_XP_TARGET, 1500);
        values.put(COL_CHALLENGES, 0);
        values.put(COL_RANK, 0);
        values.put(COL_STREAK, 0);
        values.put(COL_BADGES, 0);
        return values;
    }
}
