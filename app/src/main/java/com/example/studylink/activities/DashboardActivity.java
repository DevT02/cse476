package com.example.studylink.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;


import com.example.studylink.HomeActivity;
import com.example.studylink.R;
import com.example.studylink.adapters.EventListAdapter;
import com.example.studylink.models.Event;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // UI elements
    private SearchView searchBar;  // Change from EditText to SearchView
    private ListView eventsListView;

    // hold events and filtered events
    private List<Event> events = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();
    private EventListAdapter eventListAdapter;

    // Firebase Firestore instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Set up the toolbar and enable the back button
        Toolbar toolbar = findViewById(R.id.toolbar_find_groups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Enable the back button
        }

        // init UI elements
        searchBar = findViewById(R.id.searchBar);  // No cast needed
        eventsListView = findViewById(R.id.eventsListView);

        // Set up the adapter for the ListView
        eventListAdapter = new EventListAdapter(this, filteredEvents);
        eventsListView.setAdapter(eventListAdapter);

        // Load events from Firestore
        loadEventsFromDatabase();

        // set up search bar query listener for SearchView
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Trigger event filtering when search is submitted
                filterEvents(query, "", "");
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Trigger event filtering on text change
                filterEvents(newText, "", "");
                return false;
            }
        });
    }

    // Handle back button in the toolbar to go to HomeActivity
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate to HomeActivity
            Intent intent = new Intent(DashboardActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Load events from Firebase Firestore
    private void loadEventsFromDatabase() {
        db.collection("events")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        events.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String description = document.getString("description");
                            Timestamp dateTimestamp = document.getTimestamp("date");
                            String date = dateTimestamp != null ? dateTimestamp.toDate().toString() : "No date available";
                            String location = document.getString("location");
                            String imageUrl = document.getString("image");
                            if (imageUrl != null && !imageUrl.startsWith("file://")) {
                                imageUrl = "file://" + imageUrl;
                            }

                            Event event = new Event(title, description, date, location, imageUrl);
                            events.add(event);
                        }
                    }
                    filteredEvents.clear();
                    filteredEvents.addAll(events);
                    eventListAdapter.notifyDataSetChanged();
                });
    }

    // filter events based on query, date, and location
    private void filterEvents(String query, String dateFilter, String locationFilter) {
        filteredEvents.clear();
        for (Event event : events) {
            boolean matchesQuery = event.getTitle().toLowerCase().contains(query.toLowerCase());
            boolean matchesDate = dateFilter.isEmpty() || event.getDate().contains(dateFilter);
            boolean matchesLocation = locationFilter.isEmpty() || event.getLocation().contains(locationFilter);

            if (matchesQuery && matchesDate && matchesLocation) {
                filteredEvents.add(event);
            }
        }
        eventListAdapter.notifyDataSetChanged();
    }
}
