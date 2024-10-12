package com.example.studylink.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

        // load events from SharedPreferences
        loadEvents();

        // Set up the adapter for the ListView
        eventListAdapter = new EventListAdapter(this, filteredEvents);
        eventsListView.setAdapter(eventListAdapter);

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

    // load events from SharedPreferences
    private void loadEvents() {
        SharedPreferences preferences = getSharedPreferences("user_events", MODE_PRIVATE);
        int eventCount = preferences.getInt("event_count", 0);

        events.clear();
        for (int i = 0; i < eventCount; i++) {
            String title = preferences.getString("event_title_" + i, "Unnamed Event");
            String description = preferences.getString("event_description_" + i, "");
            String date = preferences.getString("event_date_" + i, "");
            String location = preferences.getString("event_location_" + i, "");
            String imageUri = preferences.getString("event_image_uri_" + i, "");
            Event event = new Event(title, description, date, location, imageUri); // Ensure that the image URI is passed
            events.add(event);
        }

        filteredEvents.clear();
        filteredEvents.addAll(events);
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
