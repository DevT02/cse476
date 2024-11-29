// HomeActivity.java
package com.fnprrt.studylink;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;

import com.fnprrt.studylink.activities.DashboardActivity;
import com.fnprrt.studylink.activities.EventActivity;
import com.fnprrt.studylink.activities.EventDetailsActivity; // Import EventDetailsActivity
import com.fnprrt.studylink.activities.LoginActivity;
import com.fnprrt.studylink.activities.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ImageButton btnLogout;
    private ImageButton btnSettings;
    private BottomNavigationView bottomNavigationView;
    private TextView greetingTextView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListView joinedEventsListView;

    private ArrayAdapter<String> adapter;
    private List<String> joinedEventsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        btnLogout = findViewById(R.id.btn_logout);
        btnSettings = findViewById(R.id.btn_settings);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        greetingTextView = findViewById(R.id.greetingTextView);
        joinedEventsListView = findViewById(R.id.joinedEventsListView);

        loadUserName();
        loadJoinedEvents();

        // Handle Release Notes if First Login
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

        // Handle item clicks on joined events
        joinedEventsListView.setOnItemClickListener((parent, view, position, id) -> {
            String eventTitle = joinedEventsList.get(position);
            openEventDetails(eventTitle);
        });
    }

    private void loadUserName() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("profiles").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String name = document.getString("name");
                        if (name != null && !name.isEmpty()) {
                            greetingTextView.setText(getString(R.string.greetingsss) + name + ".");
                        } else {
                            greetingTextView.setText(R.string.greetingss);
                        }
                    } else {
                        greetingTextView.setText(R.string.greetings);
                        Toast.makeText(HomeActivity.this, "Failed to load name.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showReleaseNotesIfFirstLogin() {
        // Existing code ...
    }

    private void logout() {
        // Existing code ...
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
        loadJoinedEvents();

        // Uncheck all items in the BottomNavigationView
        Menu menu = bottomNavigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }

    private void loadJoinedEvents() {
        SharedPreferences prefs = getSharedPreferences("joined_events", MODE_PRIVATE);
        Set<String> joinedEvents = prefs.getStringSet("joined_event_titles", new HashSet<>());

        joinedEventsList = new ArrayList<>(joinedEvents);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, joinedEventsList);

        joinedEventsListView.setAdapter(adapter);
    }

    private void openEventDetails(String eventTitle) {
        // Show a loading toast
        //Toast.makeText(this, "Loading event details...", Toast.LENGTH_SHORT).show();

        // Query Firestore for the event with the given title
        db.collection("events")
                .whereEqualTo("title", eventTitle)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot eventDoc = task.getResult().getDocuments().get(0);
                        String title = eventDoc.getString("title");
                        String description = eventDoc.getString("description");
                        String date = eventDoc.getTimestamp("date") != null ? eventDoc.getTimestamp("date").toDate().toString() : "No date available";
                        String location = eventDoc.getString("location");
                        String imageUrl = eventDoc.getString("image");

                        // Create an Intent to start EventDetailsActivity
                        Intent intent = new Intent(HomeActivity.this, EventDetailsActivity.class);
                        intent.putExtra("event_title", title);
                        intent.putExtra("event_description", description);
                        intent.putExtra("event_date", date);
                        intent.putExtra("event_location", location);
                        intent.putExtra("event_image_url", imageUrl);

                        startActivity(intent);
                    } else {
                        Toast.makeText(HomeActivity.this, "Event details not found.", Toast.LENGTH_SHORT).show();
                        Log.e("HomeActivity", "Event not found for title: " + eventTitle);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this, "Failed to load event details.", Toast.LENGTH_SHORT).show();
                    Log.e("HomeActivity", "Error fetching event details: ", e);
                });
    }
}
