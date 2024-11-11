package com.example.studylink.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studylink.HomeActivity;
import com.example.studylink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

import com.google.android.material.snackbar.Snackbar;


public class SignupActivity extends AppCompatActivity {

    private EditText emailField;
    private EditText passwordField;
    private FirebaseAuth mAuth; // Firestrore Auth
    private FirebaseFirestore db;  // Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to the signup layout
        setContentView(R.layout.activity_sign_up);

        // Initialize FirebaseAuth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize the EditText fields and the Sign Up button
        emailField = findViewById(R.id.inputEmail);
        passwordField = findViewById(R.id.inputPassword);
        Button signUpButton = findViewById(R.id.btnLogin);

        // Check if there's a saved instance to restore values
        if (savedInstanceState != null) {
            emailField.setText(savedInstanceState.getString("SIGNUP_EMAIL"));
            passwordField.setText(savedInstanceState.getString("SIGNUP_PASSWORD"));
        }

        // Set an onClickListener for the Sign Up button
//        signUpButton.setOnClickListener(view -> {
//            String email = emailField.getText().toString().trim();
//            String password = passwordField.getText().toString().trim();
//
//            if (email.isEmpty() || password.isEmpty()) {
//                Toast.makeText(SignupActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
//            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//                Toast.makeText(SignupActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
//            } else if (!isPasswordStrong(password)) {
//                Toast.makeText(SignupActivity.this, "Password must be at least 8 characters, include uppercase, lowercase, number, and special character.", Toast.LENGTH_LONG).show();
//            } else {
//                registerUser(email, password);
//            }
//        });

        signUpButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, "Please fill all the fields", Snackbar.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(view, "Invalid email format", Snackbar.LENGTH_SHORT).show();
            } else if (!isPasswordStrong(password)) {
                Snackbar.make(view, "Password must be at least 8 characters, have uppercase, lowercase, number, special character.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", v -> {}).show();
            } else {
                registerUser(email, password);
            }
        });


    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        mAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {
                                    if (emailTask.isSuccessful()) {
                                        Toast.makeText(SignupActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(SignupActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        handleSignupError(task.getException());
                    }
                });
    }


    private void handleSignupError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            Toast.makeText(SignupActivity.this, "Account already exists", Toast.LENGTH_SHORT).show();
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            Toast.makeText(SignupActivity.this, "Password is too weak", Toast.LENGTH_SHORT).show();
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(SignupActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(SignupActivity.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPasswordStrong(String password) {
        // Define a regex for a strong password pattern
        String passwordPattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(passwordPattern);
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

    // Save the state when the activity is paused or the orientation changes
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current input in the fields
        outState.putString("SIGNUP_EMAIL", emailField.getText().toString());
        outState.putString("SIGNUP_PASSWORD", passwordField.getText().toString());
    }
}
