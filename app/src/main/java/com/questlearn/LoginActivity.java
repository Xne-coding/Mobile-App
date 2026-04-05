package com.questlearn;

/*
 * LoginActivity — entry choices: NTU SSO (email/password flow), guest sign-in
 * via Firebase anonymous auth, or create account. Guest mode stores a simple
 * profile in SharedPreferences and skips full registration.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.card.MaterialCardView;
import com.questlearn.db.FirebaseRepository;

public class LoginActivity extends QuestLearnBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SystemBarInsets.applyToRoot(this, R.id.loginRoot);

        MaterialCardView btnSso = findViewById(R.id.btnSso);
        Button btnGuest = findViewById(R.id.btnGuest);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnSso.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, SsoLoginActivity.class);
                startActivity(intent);
                UiTransitions.openForward(this);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open SSO login.", Toast.LENGTH_SHORT).show();
            }
        });

        btnGuest.setOnClickListener(v -> {
            try {
                continueAsGuest();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to continue as guest.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateAccount.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, CreateAccountActivity.class);
                startActivity(intent);
                UiTransitions.openForward(this);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open account creation.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Anonymous Firebase user + local prefs so the app treats them as "Guest User". */
    private void continueAsGuest() {
        FirebaseRepository repo = new FirebaseRepository();
        repo.signInAnonymously(new FirebaseRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("user_logged_in", true)
                        .putString("user_name", "Guest User")
                        .putString("user_initials", "G")
                        .putBoolean("is_guest", true)
                        .apply();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                UiTransitions.openForward(LoginActivity.this);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this,
                        "Guest sign-in failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
