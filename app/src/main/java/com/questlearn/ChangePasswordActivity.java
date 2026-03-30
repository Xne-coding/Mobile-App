package com.questlearn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questlearn.db.ProgressDbHelper;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout oldLayout;
    private TextInputLayout newLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithAnimation());

        oldLayout = findViewById(R.id.oldPasswordLayout);
        newLayout = findViewById(R.id.newPasswordLayout);
        confirmLayout = findViewById(R.id.confirmPasswordLayout);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        Button btnSave = findViewById(R.id.btnSavePassword);
        btnSave.setOnClickListener(v -> {
            try {
                updatePassword();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to change password.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword() {
        clearErrors();
        SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
        String email = prefs.getString("user_email", "");
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "No signed-in account found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldPass = etOldPassword.getText() != null ? etOldPassword.getText().toString() : "";
        String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        boolean valid = true;
        if (TextUtils.isEmpty(oldPass)) {
            oldLayout.setError("Enter current password");
            valid = false;
        }
        if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
            newLayout.setError("New password must be at least 6 characters");
            valid = false;
        }
        if (!newPass.equals(confirm)) {
            confirmLayout.setError("Passwords do not match");
            valid = false;
        }
        if (!valid) return;

        ProgressDbHelper db = new ProgressDbHelper(this);
        ProgressDbHelper.AuthResult auth = db.authenticateUser(email, oldPass);
        if (!auth.success) {
            oldLayout.setError("Current password is incorrect");
            return;
        }

        boolean updated = db.updatePassword(email, newPass);
        if (updated) {
            Toast.makeText(this, "Password updated.", Toast.LENGTH_SHORT).show();
            finishWithAnimation();
        } else {
            Toast.makeText(this, "Password update failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearErrors() {
        oldLayout.setError(null);
        newLayout.setError(null);
        confirmLayout.setError(null);
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
