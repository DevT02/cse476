package com.example.studylink;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.Menu;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studylink.activities.DashboardActivity;
import com.example.studylink.activities.EventActivity;
import com.example.studylink.activities.LoginActivity;
import com.example.studylink.activities.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private ImageButton btnLogout;
    private ImageButton btnSettings;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize UI elements
        btnLogout = findViewById(R.id.btn_logout);
        btnSettings = findViewById(R.id.btn_settings);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Call the method to show release notes if it's the user's first login
        showReleaseNotesIfFirstLogin();

        // Handle Logout button click
        btnLogout.setOnClickListener(v -> logout());

        btnSettings.setOnClickListener(v -> openSettings());

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create_event) {
                openCreateEvent();
                return true;
            } else if (id == R.id.nav_find_groups) {
                openFindGroups();
                return true;
            } else {
                return false;
            }
        });
    }

    private void showReleaseNotesIfFirstLogin() {
        // Check if it's the user's first login by using SharedPreferences
        SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isFirstLogin = preferences.getBoolean("isFirstLogin", true);

        if (isFirstLogin) {
            // Inflate the custom layout for the release notes dialog
            View dialogView = getLayoutInflater().inflate(R.layout.release_notes_dialog, null);

            // Create the dialog and set the custom view
            AlertDialog releaseNotesDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            // Set up the close button inside the dialog
            Button closeButton = dialogView.findViewById(R.id.close_button);
            closeButton.setOnClickListener(v -> releaseNotesDialog.dismiss());

            // Show the dialog
            releaseNotesDialog.show();

            // Update SharedPreferences to indicate that the release notes have been shown
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstLogin", false);
            editor.apply();
        }
    }

    private void logout() {
        // Show a toast message for feedback
        Toast.makeText(HomeActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();

        // Clear the saved login state in SharedPreferences
        SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", false); // Set logged-in state to false
        editor.apply();

        // Sign out from Firebase Authentication (if applicable)
        FirebaseAuth.getInstance().signOut();

        // Redirect to LoginActivity
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close the current activity
    }

    private void openSettings() {
        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void openCreateEvent() {
        Intent intent = new Intent(HomeActivity.this, EventActivity.class);
        startActivity(intent);
    }

    private void openFindGroups() {
        Intent intent = new Intent(HomeActivity.this, DashboardActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Uncheck all items in the BottomNavigationView
        Menu menu = bottomNavigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }
}
