package com.questlearn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class ChallengeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CHALLENGE_ID = "challenge_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_detail);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            try {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to go back.", Toast.LENGTH_SHORT).show();
            }
        });

        // Load challenge data (uses first item for demo; extend for full ID lookup)
        String challengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);
        Challenge challenge = getChallengeById(challengeId);

        // Bind views
        if (challenge != null) {
            bindChallenge(challenge);
        }

        // CTA → Scan (pass challenge XP so ScanActivity can update progress on success)
        Button btnBegin = findViewById(R.id.btnBeginChallenge);
        btnBegin.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, ScanActivity.class);
                if (challenge != null) {
                    intent.putExtra(ScanActivity.EXTRA_CHALLENGE_XP, challenge.getXpPoints());
                    intent.putExtra(ScanActivity.EXTRA_CHALLENGE_ID, challenge.getId());
                }
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open scanner.", Toast.LENGTH_SHORT).show();
            }
        });

        // Open map
        TextView tvOpenMap = findViewById(R.id.tvOpenMap);
        if (tvOpenMap != null) {
            tvOpenMap.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("open_tab", R.id.mapFragment);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open map.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Steps RecyclerView
        RecyclerView rvSteps = findViewById(R.id.rvSteps);
        if (rvSteps != null) {
            rvSteps.setLayoutManager(new LinearLayoutManager(this));
            rvSteps.setAdapter(new StepAdapter(getSampleSteps()));
            rvSteps.setNestedScrollingEnabled(false);
        }

        // Animate mini map
        MiniMapView miniMap = findViewById(R.id.miniMap);
        if (miniMap != null) {
            miniMap.startPinAnimation();
        }
    }

    private void bindChallenge(Challenge challenge) {
        TextView tvIcon = findViewById(R.id.tvChallengeIcon);
        TextView tvName = findViewById(R.id.tvChallengeName);
        TextView tvOrg  = findViewById(R.id.tvChallengeOrg);
        TextView tvXp   = findViewById(R.id.tvXpChip);

        if (tvIcon != null) tvIcon.setText(challenge.getIcon());
        if (tvName != null) tvName.setText(challenge.getName());
        if (tvOrg  != null) tvOrg.setText("🏛️ " + challenge.getLocation() + " · NTU Clifton");
        if (tvXp   != null) tvXp.setText("⚡ " + challenge.getXpPoints() + " XP");
    }

    private Challenge getChallengeById(String id) {
        List<Challenge> all = Challenge.getSampleData();
        if (id == null) return all.get(0);
        for (Challenge c : all) {
            if (c.getId().equals(id)) return c;
        }
        return all.get(0);
    }

    private List<ChallengeStep> getSampleSteps() {
        return Arrays.asList(
                new ChallengeStep(1, "Find the main entrance information board", true),
                new ChallengeStep(2, "Head to the Clifton Library welcome desk", true),
                new ChallengeStep(3, "Scan the QR code near the study hub", false),
                new ChallengeStep(4, "Answer the NTU Clifton trivia question", false)
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
