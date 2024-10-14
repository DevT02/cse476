package com.example.studylink.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studylink.HomeActivity;
import com.example.studylink.R;

public class LoginActivity extends AppCompatActivity {

    // UI elements for email and password input
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView signupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements
        inputEmail = findViewById(R.id.username);
        inputPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.login_button);
        signupLink = findViewById(R.id.signup_link); // The "Sign Up" link

        // Set up login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get email and password from input fields
                String email = inputEmail.getText().toString();
                String password = inputPassword.getText().toString();

                // Check if the credentials are correct
                if (email.equals("test@example.com") && password.equals("password")) {
                    // Save login state in shared preferences
                    SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();

                    // Redirect to dashboard activity
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish(); // Close login activity
                } else {
                    // Show error message for invalid credentials
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up signup link click listener
        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the SignUpActivity when the user clicks the Sign Up link
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }
}
