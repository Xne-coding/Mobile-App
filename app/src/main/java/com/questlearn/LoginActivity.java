package com.questlearn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import android.widget.Button;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        MaterialCardView btnSso = findViewById(R.id.btnSso);
        Button btnGuest = findViewById(R.id.btnGuest);

        btnSso.setOnClickListener(v -> {
            Intent intent = new Intent(this, SsoLoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnGuest.setOnClickListener(v -> {
            continueAsGuest();
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
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
