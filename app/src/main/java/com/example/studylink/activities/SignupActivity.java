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
        signUpButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Check if all fields are filled, if so register the user
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(SignupActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(email, password);
            }
        });
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        String hashedPassword = hashPassword(password);  // Hash the password
                        saveUserDataToFirestore(userId, email, hashedPassword);

                        Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
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
                });
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

    private void saveUserDataToFirestore(String userId, String email, String hashedPassword) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("passwordHash", hashedPassword);

        db.collection("users").document(userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(SignupActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
