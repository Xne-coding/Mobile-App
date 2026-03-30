package com.questlearn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import android.widget.Button;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        MaterialCardView btnSso = findViewById(R.id.btnSso);
        Button btnGuest = findViewById(R.id.btnGuest);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnSso.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, SsoLoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open account creation.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void continueAsGuest() {
        SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("user_logged_in", true)
                .putString("user_name", "Guest User")
                .putString("user_initials", "G")
                .putBoolean("is_guest", true)
                .apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open main screen.", Toast.LENGTH_SHORT).show();
        }
    }
}

