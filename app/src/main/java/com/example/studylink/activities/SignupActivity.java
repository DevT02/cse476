package com.example.studylink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studylink.HomeActivity;
import com.example.studylink.R;

public class SignupActivity extends AppCompatActivity {

    private EditText emailField;
    private EditText passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to the signup layout
        setContentView(R.layout.activity_sign_up);

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

            // Check if all fields are filled
            if (email.isEmpty() || password.isEmpty()) {
                // Show an error message if any fields are empty
                Toast.makeText(SignupActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            } else {
                // Continue to the homepage if all fields are valid
                Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
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
