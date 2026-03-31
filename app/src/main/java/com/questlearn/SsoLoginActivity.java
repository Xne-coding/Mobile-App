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
import com.questlearn.db.ProgressDbHelper;

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
            try {
                onBackPressed();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to go back.", Toast.LENGTH_SHORT).show();
            }
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Password reset link sent to your NTU email.",
                    Toast.LENGTH_SHORT).show();
        });

        btnSignIn.setOnClickListener(v -> {
            try {
                attemptLogin();
            } catch (Exception e) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText("Sign In Securely");
                Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
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
            emailLayout.setError("Please enter your NTU email");
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

        ProgressDbHelper db = new ProgressDbHelper(this);
        ProgressDbHelper.AuthResult auth = db.authenticateUser(email, password);
        if (!auth.success) {
            btnSignIn.setEnabled(true);
            btnSignIn.setText("Sign In Securely");
            passwordLayout.setError(auth.message);
            return;
        }

        saveSession(email, cbRemember.isChecked(), auth.displayName, auth.initials);
        navigateToHome();
    }

    private void saveSession(String email, boolean rememberMe, String displayName, String initials) {
        SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("user_logged_in", true);
        editor.putBoolean("remember_me", rememberMe);
        editor.putString("user_name", displayName.isEmpty() ? "NTU Student" : displayName);
        editor.putString("user_initials", initials);
        editor.putString("user_email", email);
        editor.putBoolean("is_guest", false);
        editor.apply();
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
