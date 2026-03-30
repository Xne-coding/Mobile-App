package com.questlearn;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questlearn.db.ProgressDbHelper;

public class CreateAccountActivity extends AppCompatActivity {

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

        ProgressDbHelper db = new ProgressDbHelper(this);
        ProgressDbHelper.AuthResult result = db.createUser(email, password, name);
        if (result.success) {
            Toast.makeText(this, "Account created. Please sign in.", Toast.LENGTH_SHORT).show();
            finishWithAnimation();
        } else {
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
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
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
