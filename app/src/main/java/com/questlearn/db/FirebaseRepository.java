package com.questlearn.db;

/*
 * FirebaseRepository — the app's bridge to Firebase.
 *
 * This class talks to:
 *   • Firebase Auth (who is logged in, sign up, sign in, guest mode, password changes)
 *   • Cloud Firestore (user profile, XP, challenges done, leaderboard, rewards, vouchers)
 *
 * The rest of the app asks for data or actions here; this class handles the messy
 * async callbacks and Firestore paths. Campus QR / manual codes are matched against
 * HARDCODED_CODES and then the matching challenge is marked complete and XP updated.
 */

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    /** Codes users can type or scan; each one unlocks a challenge and awards XP. */
    private static final Map<String, CodeConfig> HARDCODED_CODES = new HashMap<>();

    static {
        HARDCODED_CODES.put("CLIFTON-LIB-001",     new CodeConfig("ch1", 150));
        HARDCODED_CODES.put("ERASMUS-LABS-220",     new CodeConfig("ch2", 200));
        HARDCODED_CODES.put("SPORTS-VILLAGE-340",   new CodeConfig("ch3", 100));
        HARDCODED_CODES.put("STUDENTS-UNION-004",   new CodeConfig("ch4", 120));
        HARDCODED_CODES.put("ADA-BYRON-005",        new CodeConfig("ch5", 150));
        HARDCODED_CODES.put("JOHN-CLARE-006",       new CodeConfig("ch6", 100));
        HARDCODED_CODES.put("TEACH-LEARN-007",      new CodeConfig("ch7", 130));
        HARDCODED_CODES.put("CELS-NSRC-008",        new CodeConfig("ch8", 180));
        HARDCODED_CODES.put("DH-LAWRENCE-009",      new CodeConfig("ch9", 110));
        HARDCODED_CODES.put("MARY-EVANS-010",       new CodeConfig("ch10", 140));
        HARDCODED_CODES.put("LIONEL-ROB-011",       new CodeConfig("ch11", 120));
        HARDCODED_CODES.put("ANTHONY-NOL-012",      new CodeConfig("ch12", 160));
        HARDCODED_CODES.put("CANCER-RES-013",       new CodeConfig("ch13", 200));
        HARDCODED_CODES.put("ISTEC-LAB-014",        new CodeConfig("ch14", 130));
        HARDCODED_CODES.put("NEW-HALL-015",         new CodeConfig("ch15", 110));
        HARDCODED_CODES.put("CLUBHOUSE-016",        new CodeConfig("ch16", 100));
        HARDCODED_CODES.put("CRICKET-PAV-017",      new CodeConfig("ch17", 90));
        HARDCODED_CODES.put("ENGINEER-018",         new CodeConfig("ch18", 170));
        HARDCODED_CODES.put("ISMART-LAB-019",       new CodeConfig("ch19", 190));
        HARDCODED_CODES.put("PAVILION-020",         new CodeConfig("ch20", 100));
        HARDCODED_CODES.put("ROSALIND-021",         new CodeConfig("ch21", 250));
    }

    /** Small helper: which challenge a code belongs to, and how much XP it gives. */
    private static class CodeConfig {
        final String challengeId;
        final int xpReward;
        CodeConfig(String challengeId, int xpReward) {
            this.challengeId = challengeId;
            this.xpReward = xpReward;
        }
    }

    /*
     * Simple data bags we pass back to the UI.
     * (Field names line up with what ProgressDbHelper used when the app was SQLite-only.)
     */

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

    public static class LeaderboardRow {
        public String displayName;
        public String initials;
        public String avatarId;
        public int totalXp;
        public boolean isCurrentUser;
    }

    public static class RewardData {
        public int rewardPoints;
        public int dailyXp;
        public int weeklyXp;
        public int availableToConvert;
    }

    public static class VoucherRecord {
        public String voucherType;
        public int amountPence;
        public long redeemedAt;
        public String voucherCode;
    }

    /** One row from users/{uid}/scanHistory (written when a campus code is redeemed). */
    public static class RecentScanEntry {
        public final String challengeId;
        public final String code;
        public final int xpReward;
        public final long scannedAt;

        public RecentScanEntry(String challengeId, String code, int xpReward, long scannedAt) {
            this.challengeId = challengeId != null ? challengeId : "";
            this.code = code != null ? code : "";
            this.xpReward = xpReward;
            this.scannedAt = scannedAt;
        }
    }

    /** Fire-and-forget style callback: success with a value, or an error message. */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public FirebaseRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // --- Quick auth checks (used all over the UI) ---

    private FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public boolean isGuest() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null && u.isAnonymous();
    }

    private String getUid() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    /** Firestore document for the signed-in user: users/{uid} */
    private DocumentReference userDoc() {
        return db.collection("users").document(getUid());
    }

    // --- Sign up, sign in, guest, sign out, password ---

    public void createUser(String email, String password, String name, Callback<AuthResult> callback) {
        String trimEmail = email == null ? "" : email.trim();
        String trimPass = password == null ? "" : password.trim();
        String trimName = name == null ? "" : name.trim();

        if (trimEmail.isEmpty() || trimPass.isEmpty() || trimName.isEmpty()) {
            AuthResult r = new AuthResult();
            r.success = false;
            r.message = "All fields are required.";
            callback.onSuccess(r);
            return;
        }
        if (trimPass.length() < 6) {
            AuthResult r = new AuthResult();
            r.success = false;
            r.message = "Password must be at least 6 characters.";
            callback.onSuccess(r);
            return;
        }

        String initials = getInitials(trimName);

        auth.createUserWithEmailAndPassword(trimEmail, trimPass)
                .addOnSuccessListener(authRes -> {
                    FirebaseUser user = authRes.getUser();
                    if (user == null) {
                        AuthResult r = new AuthResult();
                        r.success = false;
                        r.message = "Account creation failed.";
                        callback.onSuccess(r);
                        return;
                    }
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(trimName).build();
                    user.updateProfile(profile);

                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("email", trimEmail.toLowerCase(Locale.ROOT));
                    userDoc.put("displayName", trimName);
                    userDoc.put("initials", initials);
                    userDoc.put("createdAt", System.currentTimeMillis());
                    userDoc.put("totalXp", 0);
                    userDoc.put("weeklyXp", 0);
                    userDoc.put("weekStart", "");

                    db.collection("users").document(user.getUid())
                            .set(userDoc)
                            .addOnSuccessListener(v -> initProgressDoc(user.getUid(), () -> {
                                AuthResult r = new AuthResult();
                                r.success = true;
                                r.message = "Account created.";
                                r.displayName = trimName;
                                r.initials = initials;
                                callback.onSuccess(r);
                            }))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    AuthResult r = new AuthResult();
                    r.success = false;
                    r.message = e.getMessage() != null ? e.getMessage() : "Account creation failed.";
                    callback.onSuccess(r);
                });
    }

    public void signIn(String email, String password, Callback<AuthResult> callback) {
        String trimEmail = email == null ? "" : email.trim();
        String trimPass = password == null ? "" : password.trim();

        auth.signInWithEmailAndPassword(trimEmail, trimPass)
                .addOnSuccessListener(authRes -> {
                    FirebaseUser user = authRes.getUser();
                    if (user == null) {
                        AuthResult r = new AuthResult();
                        r.success = false;
                        r.message = "Sign-in failed.";
                        callback.onSuccess(r);
                        return;
                    }
                    db.collection("users").document(user.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                AuthResult r = new AuthResult();
                                r.success = true;
                                r.message = "Sign-in successful.";
                                r.displayName = doc.getString("displayName");
                                r.initials = doc.getString("initials");
                                if (r.displayName == null) r.displayName = "NTU Student";
                                if (r.initials == null) r.initials = "U";
                                callback.onSuccess(r);
                            })
                            .addOnFailureListener(e -> {
                                AuthResult r = new AuthResult();
                                r.success = true;
                                r.message = "Sign-in successful.";
                                r.displayName = user.getDisplayName() != null ? user.getDisplayName() : "NTU Student";
                                r.initials = getInitials(r.displayName);
                                callback.onSuccess(r);
                            });
                })
                .addOnFailureListener(e -> {
                    AuthResult r = new AuthResult();
                    r.success = false;
                    r.message = e.getMessage() != null ? e.getMessage() : "Sign-in failed.";
                    callback.onSuccess(r);
                });
    }

    public void signInAnonymously(Callback<Void> callback) {
        auth.signInAnonymously()
                .addOnSuccessListener(authRes -> {
                    FirebaseUser user = authRes.getUser();
                    if (user == null) { callback.onError("Anonymous sign-in failed."); return; }
                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("email", "");
                    userDoc.put("displayName", "Guest User");
                    userDoc.put("initials", "G");
                    userDoc.put("createdAt", System.currentTimeMillis());
                    userDoc.put("totalXp", 0);
                    userDoc.put("weeklyXp", 0);
                    userDoc.put("weekStart", "");
                    db.collection("users").document(user.getUid())
                            .set(userDoc, SetOptions.merge())
                            .addOnSuccessListener(v -> initProgressDoc(user.getUid(), () -> callback.onSuccess(null)))
                            .addOnFailureListener(e -> callback.onSuccess(null));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signOut() {
        auth.signOut();
    }

    /**
     * Sends a Firebase Auth password-reset email to {@code email}.  Completes successfully even if no user
     * exists for that address (Firebase behaviour, to limit email enumeration).
     */
    public void sendPasswordResetEmail(String email, Callback<Void> callback) {
        String trimEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (trimEmail.isEmpty()) {
            callback.onError("Enter your email address.");
            return;
        }
        if (!trimEmail.contains("@")) {
            callback.onError("Enter a valid email address.");
            return;
        }
        auth.sendPasswordResetEmail(trimEmail)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Could not send reset email."));
    }

    public void updatePassword(String oldPassword, String newPassword, Callback<Boolean> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onSuccess(false);
            return;
        }
        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(cred)
                .addOnSuccessListener(v -> user.updatePassword(newPassword)
                        .addOnSuccessListener(v2 -> callback.onSuccess(true))
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError("Current password is incorrect."));
    }

    // --- XP, level, streak, rank (home screen stats) ---

    private void initProgressDoc(String uid, Runnable onDone) {
        DocumentReference ref = db.collection("users").document(uid)
                .collection("progress").document("current");
        ref.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Map<String, Object> defaults = new HashMap<>();
                defaults.put("level", 0);
                defaults.put("xp", 0);
                defaults.put("xpTarget", 1500);
                defaults.put("challengesDone", 0);
                defaults.put("rankValue", 0);
                defaults.put("streak", 0);
                defaults.put("badges", 0);
                ref.set(defaults).addOnCompleteListener(t -> onDone.run());
            } else {
                onDone.run();
            }
        }).addOnFailureListener(e -> onDone.run());
    }

    public void getProgress(Callback<ProgressData> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(defaultProgress()); return; }
        db.collection("users").document(uid)
                .collection("progress").document("current")
                .get()
                .addOnSuccessListener(doc -> callback.onSuccess(docToProgress(doc)))
                .addOnFailureListener(e -> callback.onSuccess(defaultProgress()));
    }

    public void writeProgress(ProgressData data, Callback<Void> callback) {
        String uid = getUid();
        if (uid == null) { if (callback != null) callback.onError("Not signed in."); return; }
        Map<String, Object> map = new HashMap<>();
        map.put("level", data.level);
        map.put("xp", data.xp);
        map.put("xpTarget", data.xpTarget);
        map.put("challengesDone", data.challengesDone);
        map.put("rankValue", data.rankValue);
        map.put("streak", data.streak);
        map.put("badges", data.badges);
        db.collection("users").document(uid)
                .collection("progress").document("current")
                .set(map, SetOptions.merge())
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    // --- Which challenges are finished? ---

    public void isChallengeCompleted(String challengeId, Callback<Boolean> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(false); return; }
        db.collection("users").document(uid)
                .collection("challengeStates").document(challengeId)
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean completed = doc.getBoolean("completed");
                    callback.onSuccess(completed != null && completed);
                })
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    public void getCompletedChallengeIds(Callback<List<String>> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(new ArrayList<>()); return; }
        db.collection("users").document(uid)
                .collection("challengeStates")
                .whereEqualTo("completed", true)
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) ids.add(d.getId());
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>()));
    }

    public void countCompletedChallenges(Callback<Integer> callback) {
        getCompletedChallengeIds(new Callback<List<String>>() {
            @Override public void onSuccess(List<String> ids) { callback.onSuccess(ids.size()); }
            @Override public void onError(String msg) { callback.onSuccess(0); }
        });
    }

    /**
     * Latest successful code redemptions for the profile “recent achievements” list.
     * Ordered by {@code scannedAt} descending (newest first).
     */
    public void getRecentScanHistory(int limit, Callback<List<RecentScanEntry>> callback) {
        String uid = getUid();
        if (uid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        int lim = Math.max(1, Math.min(limit, 50));
        db.collection("users").document(uid)
                .collection("scanHistory")
                .orderBy("scannedAt", Query.Direction.DESCENDING)
                .limit(lim)
                .get()
                .addOnSuccessListener(qs -> {
                    List<RecentScanEntry> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String chId = d.getString("challengeId");
                        String code = d.getString("code");
                        Long xp = d.getLong("xpReward");
                        Long at = d.getLong("scannedAt");
                        int xpv = xp != null ? xp.intValue() : 0;
                        long atv = at != null ? at : 0L;
                        out.add(new RecentScanEntry(chId, code, xpv, atv));
                    }
                    callback.onSuccess(out);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseRepo", "getRecentScanHistory: " + e.getMessage(), e);
                    callback.onSuccess(new ArrayList<>());
                });
    }

    // --- Scan or type a campus code; grant XP if valid ---

    public void redeemHardcodedCode(String rawCode, String expectedChallengeId, Callback<CodeRedemptionResult> callback) {
        CodeRedemptionResult result = new CodeRedemptionResult();
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            result.success = false; result.message = "Code is empty.";
            callback.onSuccess(result);
            return;
        }
        CodeConfig cfg = HARDCODED_CODES.get(code);
        if (cfg == null) {
            result.success = false; result.message = "Invalid code.";
            callback.onSuccess(result);
            return;
        }
        if (expectedChallengeId != null && !expectedChallengeId.isEmpty()
                && !expectedChallengeId.equals(cfg.challengeId)) {
            result.success = false; result.message = "This code does not match the current challenge.";
            callback.onSuccess(result);
            return;
        }

        String uid = getUid();
        if (uid == null) {
            result.success = false; result.message = "Not signed in.";
            callback.onSuccess(result);
            return;
        }

        String finalCode = code;
        isChallengeCompleted(cfg.challengeId, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean completed) {
                if (completed) {
                    CodeRedemptionResult r = new CodeRedemptionResult();
                    r.success = false; r.message = "Challenge already completed.";
                    callback.onSuccess(r);
                    return;
                }
                getProgress(new Callback<ProgressData>() {
                    @Override
                    public void onSuccess(ProgressData progress) {
                        progress.challengesDone += 1;
                        progress.streak += 1;
                        progress.badges += 1;
                        progress.xp += cfg.xpReward;
                        while (progress.xp >= progress.xpTarget) {
                            progress.level += 1;
                            progress.xp -= progress.xpTarget;
                        }
                        WriteBatch batch = db.batch();
                        DocumentReference progRef = db.collection("users").document(uid)
                                .collection("progress").document("current");
                        Map<String, Object> pm = new HashMap<>();
                        pm.put("level", progress.level);
                        pm.put("xp", progress.xp);
                        pm.put("xpTarget", progress.xpTarget);
                        pm.put("challengesDone", progress.challengesDone);
                        pm.put("rankValue", progress.rankValue);
                        pm.put("streak", progress.streak);
                        pm.put("badges", progress.badges);
                        batch.set(progRef, pm, SetOptions.merge());

                        DocumentReference csRef = db.collection("users").document(uid)
                                .collection("challengeStates").document(cfg.challengeId);
                        Map<String, Object> cs = new HashMap<>();
                        cs.put("completed", true);
                        cs.put("lastCode", finalCode);
                        cs.put("completedAt", System.currentTimeMillis());
                        batch.set(csRef, cs, SetOptions.merge());

                        DocumentReference shRef = db.collection("users").document(uid)
                                .collection("scanHistory").document();
                        Map<String, Object> sh = new HashMap<>();
                        sh.put("code", finalCode);
                        sh.put("challengeId", cfg.challengeId);
                        sh.put("xpReward", cfg.xpReward);
                        sh.put("scannedAt", System.currentTimeMillis());
                        batch.set(shRef, sh);

                        batch.commit()
                                .addOnSuccessListener(v -> {
                                    int totalXp = (progress.level * progress.xpTarget) + progress.xp;
                                    syncUserXp(totalXp);

                                    CodeRedemptionResult r = new CodeRedemptionResult();
                                    r.success = true;
                                    r.challengeId = cfg.challengeId;
                                    r.xpReward = cfg.xpReward;
                                    r.progressData = progress;
                                    r.message = "Code accepted.";
                                    callback.onSuccess(r);
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    }
                    @Override public void onError(String msg) { callback.onError(msg); }
                });
            }
            @Override public void onError(String msg) { callback.onError(msg); }
        });
    }

    // --- Leaderboard list + syncing the current user's XP to Firestore ---

    public void syncUserXp(int totalXp) {
        String uid = getUid();
        if (uid == null) return;
        FirebaseUser user = getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("totalXp", totalXp);
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                map.put("displayName", name);
                map.put("initials", getInitials(name));
            }
            if (user.getEmail() != null) {
                map.put("email", user.getEmail().toLowerCase(Locale.ROOT));
            }
        }
        userDoc().set(map, SetOptions.merge());
    }

    public void getLeaderboard(Callback<List<LeaderboardRow>> callback) {
        String uid = getUid();
        db.collection("users")
                .whereGreaterThan("totalXp", 0)
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(qs -> callback.onSuccess(snapshotToLeaderboard(qs, uid)))
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseRepo", "Leaderboard query failed: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public void syncWeeklyXp() {
        String uid = getUid();
        if (uid == null) return;
        getDailyXpFromHistory(0, getMondayMidnightMs(), new Callback<Integer>() {
            @Override
            public void onSuccess(Integer weeklyXp) {
                Map<String, Object> m = new HashMap<>();
                m.put("weeklyXp", weeklyXp);
                m.put("weekStart", mondayDateString());
                userDoc().set(m, SetOptions.merge());
            }
            @Override public void onError(String msg) {}
        });
    }

    public void getWeeklyLeaderboard(Callback<List<LeaderboardRow>> callback) {
        String uid = getUid();
        db.collection("users")
                .whereGreaterThan("weeklyXp", 0)
                .orderBy("weeklyXp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(qs -> {
                    List<LeaderboardRow> rows = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        LeaderboardRow row = new LeaderboardRow();
                        row.displayName = d.getString("displayName");
                        row.initials = d.getString("initials");
                        row.avatarId = d.getString("avatarId");
                        Long wxp = d.getLong("weeklyXp");
                        row.totalXp = wxp != null ? wxp.intValue() : 0;
                        row.isCurrentUser = d.getId().equals(uid);
                        if (row.displayName == null) row.displayName = "Unknown";
                        if (row.initials == null) row.initials = "?";
                        rows.add(row);
                    }
                    callback.onSuccess(rows);
                })
                .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>()));
    }

    // --- Reward points, weekly XP, vouchers ---

    public void getRewardData(Callback<RewardData> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(emptyReward()); return; }

        db.collection("users").document(uid)
                .collection("rewards").document("current")
                .get()
                .addOnSuccessListener(doc -> {
                    int rewardPoints = intVal(doc, "rewardPoints");
                    int convertedToday = intVal(doc, "convertedToday");
                    String convertDate = doc.getString("convertDate");
                    String today = todayDateString();

                    getDailyXpFromHistory(0, getTodayMidnightMs(), new Callback<Integer>() {
                        @Override
                        public void onSuccess(Integer dailyXp) {
                            getDailyXpFromHistory(0, getMondayMidnightMs(), new Callback<Integer>() {
                                @Override
                                public void onSuccess(Integer weeklyXp) {
                                    RewardData data = new RewardData();
                                    data.rewardPoints = rewardPoints;
                                    data.dailyXp = dailyXp;
                                    data.weeklyXp = weeklyXp;
                                    if (today.equals(convertDate)) {
                                        data.availableToConvert = Math.max(0, dailyXp - convertedToday);
                                    } else {
                                        data.availableToConvert = dailyXp;
                                    }
                                    callback.onSuccess(data);
                                }
                                @Override public void onError(String msg) { callback.onSuccess(emptyReward()); }
                            });
                        }
                        @Override public void onError(String msg) { callback.onSuccess(emptyReward()); }
                    });
                })
                .addOnFailureListener(e -> callback.onSuccess(emptyReward()));
    }

    public void convertDailyXpToPoints(Callback<Integer> callback) {
        getRewardData(new Callback<RewardData>() {
            @Override
            public void onSuccess(RewardData data) {
                int availableXp = data.availableToConvert;
                int pointsEarned = availableXp / 10;
                int xpConsumed = pointsEarned * 10;
                if (pointsEarned <= 0) { callback.onSuccess(0); return; }

                String uid = getUid();
                if (uid == null) { callback.onSuccess(0); return; }

                String today = todayDateString();
                int newConverted = (today.equals(todayDateString())) ?
                        (data.availableToConvert == data.dailyXp ?
                                xpConsumed : (data.dailyXp - data.availableToConvert) + xpConsumed) :
                        xpConsumed;

                Map<String, Object> map = new HashMap<>();
                map.put("rewardPoints", data.rewardPoints + pointsEarned);
                map.put("convertedToday", newConverted);
                map.put("convertDate", today);
                db.collection("users").document(uid)
                        .collection("rewards").document("current")
                        .set(map, SetOptions.merge())
                        .addOnSuccessListener(v -> callback.onSuccess(pointsEarned))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }
            @Override public void onError(String msg) { callback.onError(msg); }
        });
    }

    public void redeemVoucher(int pointsCost, String voucherType, int amountPence, Callback<String> callback) {
        getRewardData(new Callback<RewardData>() {
            @Override
            public void onSuccess(RewardData data) {
                if (data.rewardPoints < pointsCost) { callback.onSuccess(null); return; }
                String uid = getUid();
                if (uid == null) { callback.onSuccess(null); return; }

                String voucherCode = generateVoucherCode(voucherType);
                WriteBatch batch = db.batch();

                DocumentReference rewardsRef = db.collection("users").document(uid)
                        .collection("rewards").document("current");
                Map<String, Object> rm = new HashMap<>();
                rm.put("rewardPoints", data.rewardPoints - pointsCost);
                batch.set(rewardsRef, rm, SetOptions.merge());

                DocumentReference vhRef = db.collection("users").document(uid)
                        .collection("voucherHistory").document();
                Map<String, Object> vh = new HashMap<>();
                vh.put("voucherType", voucherType);
                vh.put("amountPence", amountPence);
                vh.put("voucherCode", voucherCode);
                vh.put("redeemedAt", System.currentTimeMillis());
                batch.set(vhRef, vh);

                batch.commit()
                        .addOnSuccessListener(v -> callback.onSuccess(voucherCode))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }
            @Override public void onError(String msg) { callback.onError(msg); }
        });
    }

    public void getVoucherHistory(Callback<List<VoucherRecord>> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(new ArrayList<>()); return; }
        db.collection("users").document(uid)
                .collection("voucherHistory")
                .orderBy("redeemedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    List<VoucherRecord> records = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        VoucherRecord r = new VoucherRecord();
                        r.voucherType = d.getString("voucherType");
                        Long ap = d.getLong("amountPence");
                        r.amountPence = ap != null ? ap.intValue() : 0;
                        Long ra = d.getLong("redeemedAt");
                        r.redeemedAt = ra != null ? ra : 0;
                        r.voucherCode = d.getString("voucherCode");
                        records.add(r);
                    }
                    callback.onSuccess(records);
                })
                .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>()));
    }

    // --- Preset emoji avatar id saved on the user doc ---

    public void saveAvatarId(String avatarId, Callback<Void> callback) {
        String uid = getUid();
        if (uid == null) { if (callback != null) callback.onError("Not signed in."); return; }
        Map<String, Object> map = new HashMap<>();
        map.put("avatarId", avatarId);
        userDoc().set(map, SetOptions.merge())
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    public void getAvatarId(Callback<String> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(null); return; }
        userDoc().get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.getString("avatarId")))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    // --- Small internal utilities (formatting, parsing, initials, etc.) ---

    private void getDailyXpFromHistory(int fallback, long sinceMs, Callback<Integer> callback) {
        String uid = getUid();
        if (uid == null) { callback.onSuccess(fallback); return; }
        db.collection("users").document(uid)
                .collection("scanHistory")
                .whereGreaterThanOrEqualTo("scannedAt", sinceMs)
                .get()
                .addOnSuccessListener(qs -> {
                    int total = 0;
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Long xp = d.getLong("xpReward");
                        if (xp != null) total += xp.intValue();
                    }
                    callback.onSuccess(total);
                })
                .addOnFailureListener(e -> callback.onSuccess(fallback));
    }

    private ProgressData docToProgress(DocumentSnapshot doc) {
        ProgressData p = new ProgressData();
        if (doc == null || !doc.exists()) return defaultProgress();
        p.level = intVal(doc, "level");
        p.xp = intVal(doc, "xp");
        p.xpTarget = intVal(doc, "xpTarget");
        if (p.xpTarget == 0) p.xpTarget = 1500;
        p.challengesDone = intVal(doc, "challengesDone");
        p.rankValue = intVal(doc, "rankValue");
        p.streak = intVal(doc, "streak");
        p.badges = intVal(doc, "badges");
        return p;
    }

    private ProgressData defaultProgress() {
        ProgressData p = new ProgressData();
        p.xpTarget = 1500;
        return p;
    }

    private RewardData emptyReward() {
        return new RewardData();
    }

    private List<LeaderboardRow> snapshotToLeaderboard(QuerySnapshot qs, String currentUid) {
        List<LeaderboardRow> rows = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) {
            LeaderboardRow row = new LeaderboardRow();
            row.displayName = d.getString("displayName");
            row.initials = d.getString("initials");
            row.avatarId = d.getString("avatarId");
            Long txp = d.getLong("totalXp");
            row.totalXp = txp != null ? txp.intValue() : 0;
            row.isCurrentUser = d.getId().equals(currentUid);
            if (row.displayName == null) row.displayName = "Unknown";
            if (row.initials == null) row.initials = "?";
            rows.add(row);
        }
        return rows;
    }

    private int intVal(DocumentSnapshot doc, String key) {
        Long v = doc.getLong(key);
        return v != null ? v.intValue() : 0;
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
        if (parts.length == 1 && !parts[0].isEmpty())
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return "U";
    }

    private String generateVoucherCode(String type) {
        String prefix = "twenty_pound".equals(type) ? "QL20" : "QL5";
        long ts = System.currentTimeMillis() / 1000;
        String rand = Long.toHexString(Double.doubleToLongBits(Math.random()))
                .substring(0, 4).toUpperCase(Locale.ROOT);
        return prefix + "-" + ts + "-" + rand;
    }

    private long getTodayMidnightMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getMondayMidnightMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        return cal.getTimeInMillis();
    }

    private String todayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }

    private String mondayDateString() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(cal.getTime());
    }
}
