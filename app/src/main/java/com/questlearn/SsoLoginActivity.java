package com.questlearn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.checkbox.MaterialCheckBox;

public class SsoLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialCheckBox cbRemember;
    private Button btnSignIn;
    private ImageButton btnBack;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sso_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        cbRemember = findViewById(R.id.cbRemember);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnBack = findViewById(R.id.btnBack);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnBack.setOnClickListener(v -> {
            onBackPressed();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Password reset link sent to your university email.",
                    Toast.LENGTH_SHORT).show();
        });

        btnSignIn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Reset errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Validate
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Please enter your university email");
            valid = false;
        } else if (!email.contains("@")) {
            emailLayout.setError("Please enter a valid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Please enter your password");
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            valid = false;
        }

        if (!valid) return;

        // Show loading state
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in…");

        // Simulate SSO authentication delay
        btnSignIn.postDelayed(() -> {
            saveSession(email, cbRemember.isChecked());
            navigateToHome();
        }, 1200);
    }

    private void saveSession(String email, boolean rememberMe) {
        // Extract name from email prefix
        String emailUser = email.contains("@") ? email.split("@")[0] : email;
        String displayName = formatName(emailUser);
        String initials = getInitials(displayName);

        SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("user_logged_in", rememberMe);
        editor.putString("user_name", "Don Jacques Maseengo");
        editor.putString("user_initials", "DM");
        editor.putString("user_email", email);
        editor.putBoolean("is_guest", false);
        editor.apply();
    }

    private String formatName(String emailUser) {
        // Convert "don.maseengo" -> "Don Maseengo"
        String[] parts = emailUser.split("[._]");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                name.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        return name.toString().trim();
    }

    private String getInitials(String name) {
        String[] words = name.split(" ");
        if (words.length >= 2) {
            return String.valueOf(words[0].charAt(0)).toUpperCase()
                    + String.valueOf(words[words.length - 1].charAt(0)).toUpperCase();
        } else if (words.length == 1 && !words[0].isEmpty()) {
            return String.valueOf(words[0].charAt(0)).toUpperCase();
        }
        return "U";
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
