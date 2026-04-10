package com.questlearn;

/*
 * CreateAccountActivity — register with email, password, and display name.
 * Validates inputs locally, calls FirebaseRepository.createUser, then saves basics
 * to SharedPreferences and jumps to MainActivity on success.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questlearn.db.FirebaseRepository;

public class CreateAccountActivity extends QuestLearnBaseActivity {

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        SystemBarInsets.applyToRoot(this, R.id.createAccountRoot);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithAnimation());

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmLayout = findViewById(R.id.confirmLayout);

        Button btnCreate = findViewById(R.id.btnCreateAccount);
        btnCreate.setOnClickListener(v -> {
            try {
                createAccount();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to create account.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Read fields, validate, then create Firebase user and Firestore profile row. */
    private void createAccount() {
        clearErrors();
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Enter your name");
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            emailLayout.setError("Enter a valid email");
            valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            valid = false;
        }
        if (!password.equals(confirm)) {
            confirmLayout.setError("Passwords do not match");
            valid = false;
        }
        if (!valid) return;

        try {
            FirebaseRepository repo = new FirebaseRepository();
            repo.createUser(email, password, name, new FirebaseRepository.Callback<FirebaseRepository.AuthResult>() {
                @Override
                public void onSuccess(FirebaseRepository.AuthResult result) {
                    if (result.success) {
                        SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("user_logged_in", true)
                                .putString("user_name", result.displayName)
                                .putString("user_initials", result.initials)
                                .putString("user_email", email)
                                .putBoolean("is_guest", false)
                                .commit();

                        Toast.makeText(CreateAccountActivity.this,
                                "Welcome, " + result.displayName + "!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CreateAccountActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        UiTransitions.openForward(CreateAccountActivity.this);
                        finish();
                    } else {
                        Toast.makeText(CreateAccountActivity.this,
                                result.message, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(CreateAccountActivity.this,
                            "Error: " + message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            String msg = e.getMessage();
            Toast.makeText(this,
                    "Error: " + (msg != null ? msg : e.getClass().getSimpleName()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmLayout.setError(null);
    }

    private void finishWithAnimation() {
        finish();
        UiTransitions.closeBackward(this);
    }
}
