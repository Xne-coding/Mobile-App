package com.questlearn;

/*
 * ChangePasswordActivity — change password for a signed-in, non-guest user.
 * Re-authenticates with the old password through Firebase, then sets the new one.
 */

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questlearn.db.FirebaseRepository;

public class ChangePasswordActivity extends QuestLearnBaseActivity {

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
        SystemBarInsets.applyToRoot(this, R.id.changePasswordRoot);

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

    /** Validates fields, then asks Firebase to re-auth with old password and set the new one. */
    private void updatePassword() {
        clearErrors();

        FirebaseRepository repo = new FirebaseRepository();
        if (!repo.isSignedIn() || repo.isGuest()) {
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

        repo.updatePassword(oldPass, newPass, new FirebaseRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean updated) {
                if (updated) {
                    Toast.makeText(ChangePasswordActivity.this,
                            "Password updated.", Toast.LENGTH_SHORT).show();
                    finishWithAnimation();
                } else {
                    Toast.makeText(ChangePasswordActivity.this,
                            "Password update failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                oldLayout.setError(message);
            }
        });
    }

    private void clearErrors() {
        oldLayout.setError(null);
        newLayout.setError(null);
        confirmLayout.setError(null);
    }

    private void finishWithAnimation() {
        finish();
        UiTransitions.closeBackward(this);
    }
}
