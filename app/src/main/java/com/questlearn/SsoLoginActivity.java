package com.questlearn;

/*
 * SsoLoginActivity — branded "NTU" style email + password sign-in (Firebase email auth).
 * Disables the button while the network call runs, saves session prefs, and clears
 * the back stack when opening the main app.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questlearn.db.FirebaseRepository;

public class SsoLoginActivity extends QuestLearnBaseActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialCheckBox cbRemember;
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sso_login);
        SystemBarInsets.applyToRoot(this, R.id.ssoLoginRoot);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        cbRemember = findViewById(R.id.cbRemember);
        btnSignIn = findViewById(R.id.btnSignIn);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnBack.setOnClickListener(v -> {
            try {
                onBackPressed();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to go back.", Toast.LENGTH_SHORT).show();
            }
        });

        tvForgotPassword.setOnClickListener(v -> attemptPasswordReset(tvForgotPassword));

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

    /** Validates email, then asks Firebase to send a password-reset message to that address. */
    private void attemptPasswordReset(TextView tvForgotPassword) {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        emailLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.forgot_password_need_email));
            return;
        }
        if (!email.contains("@")) {
            emailLayout.setError(getString(R.string.email_invalid));
            return;
        }

        tvForgotPassword.setEnabled(false);
        CharSequence savedLabel = tvForgotPassword.getText();
        tvForgotPassword.setText(R.string.forgot_password_sending);

        FirebaseRepository repo = new FirebaseRepository();
        repo.sendPasswordResetEmail(email, new FirebaseRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                tvForgotPassword.setEnabled(true);
                tvForgotPassword.setText(savedLabel);
                Toast.makeText(SsoLoginActivity.this,
                        R.string.forgot_password_email_sent, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                tvForgotPassword.setEnabled(true);
                tvForgotPassword.setText(savedLabel);
                emailLayout.setError(message);
            }
        });
    }

    /** Basic field checks, then Firebase sign-in with email/password. */
    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        emailLayout.setError(null);
        passwordLayout.setError(null);

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

        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in…");

        FirebaseRepository repo = new FirebaseRepository();
        repo.signIn(email, password, new FirebaseRepository.Callback<FirebaseRepository.AuthResult>() {
            @Override
            public void onSuccess(FirebaseRepository.AuthResult auth) {
                if (!auth.success) {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Sign In Securely");
                    passwordLayout.setError(auth.message);
                    return;
                }
                saveSession(email, cbRemember.isChecked(), auth.displayName, auth.initials);
                navigateToHome();
            }

            @Override
            public void onError(String message) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText("Sign In Securely");
                passwordLayout.setError(message);
            }
        });
    }

    /** Remember who they are locally so fragments can show name/initials without waiting on network. */
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
        UiTransitions.openForward(this);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        UiTransitions.closeBackward(this);
    }
}
