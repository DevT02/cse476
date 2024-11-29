package com.fnprrt.studylink.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AppCompatActivity;

import com.fnprrt.studylink.HomeActivity;
import com.fnprrt.studylink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    // UI elements for email and password input
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView signupLink;
    private FirebaseAuth mAuth; // Firebase Auth instance
    private FirebaseFirestore db; // Firestore instance


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);

        // Check if it's the first run
        boolean isFirstRun = preferences.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            // Clear SharedPreferences on the first run only
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear(); // Clear all data
            editor.putBoolean("isFirstRun", false); // Set first run to false
            editor.apply();
        }

        boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        

        // Initialize FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize UI elements
        inputEmail = findViewById(R.id.username);
        inputPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.login_button);
        signupLink = findViewById(R.id.signup_link); // The "Sign Up" link

        // Set up login button click listener
        btnLogin.setOnClickListener(v -> {
            // Get email and password from input fields
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            // Check if fields are not empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Set up signup link click listener
        signupLink.setOnClickListener(v -> {
            // Start the SignupActivity when the user clicks the Sign Up link
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            String hashedPassword = hashPassword(password);
                            saveUserDataToFirestore(user.getUid(), email, hashedPassword);

                            SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putBoolean("isFirstLogin", true);
                            editor.apply();

                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        handleLoginError(task.getException());
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String email, String hashedPassword) {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("passwordHash", hashedPassword);

        db.collection("users").document(userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(LoginActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.encodeToString(hash, Base64.NO_WRAP); // Works on all Android versions
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private void handleLoginError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(LoginActivity.this, "No account found with this email", Toast.LENGTH_SHORT).show();
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(LoginActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
