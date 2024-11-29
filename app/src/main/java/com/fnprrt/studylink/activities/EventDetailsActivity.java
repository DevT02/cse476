// EventDetailsActivity.java
package com.fnprrt.studylink.activities;

import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.fnprrt.studylink.R;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class EventDetailsActivity extends AppCompatActivity {

    private Button joinLeaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Retrieve event details from the intent
        String eventTitle = getIntent().getStringExtra("event_title");
        String eventDescription = getIntent().getStringExtra("event_description");
        String eventDate = getIntent().getStringExtra("event_date");
        String eventLocation = getIntent().getStringExtra("event_location");
        String eventImageUrl = getIntent().getStringExtra("event_image_url");

        Log.d("EventDetailsActivity", "Received event details - Title: " + eventTitle
                + ", Description: " + eventDescription
                + ", Date: " + eventDate
                + ", Location: " + eventLocation
                + ", Image URL: " + eventImageUrl);

        // Initialize UI elements
        TextView eventTitleTextView = findViewById(R.id.eventTitleDetails);
        TextView eventDescriptionTextView = findViewById(R.id.eventDescriptionDetails);
        TextView eventDateTextView = findViewById(R.id.eventDateDetails);
        TextView eventLocationTextView = findViewById(R.id.eventLocationDetails);
        ImageView eventImageView = findViewById(R.id.eventImageDetails);
        Button navigateButton = findViewById(R.id.btnNavigate);
        joinLeaveButton = findViewById(R.id.btnJoinLeave);

        // Set event details to the UI elements
        eventTitleTextView.setText(eventTitle);
        eventDescriptionTextView.setText(eventDescription);
        eventDateTextView.setText(eventDate);
        eventLocationTextView.setText(eventLocation);

        // Load the event image using Glide if the URL is not empty
        if (eventImageUrl != null && !eventImageUrl.isEmpty()) {
            Log.d("EventDetailsActivity", "Attempting to load image with URL: " + eventImageUrl);
            // Check if the URL is a network URL or a local file path
            if (eventImageUrl.startsWith("http")) { // Network URL
                Glide.with(this)
                        .load(eventImageUrl)
                        .placeholder(R.drawable.baseline_place_24)
                        .error(R.drawable.baseline_place_24)
                        .into(eventImageView);
            } else if (eventImageUrl.startsWith("content://")) { // Content URI
                Glide.with(this)
                        .load(Uri.parse(eventImageUrl)) // Parse the URI
                        .placeholder(R.drawable.baseline_place_24)
                        .error(R.drawable.baseline_place_24)
                        .into(eventImageView);
            } else { // Local file path
                File imageFile = new File(eventImageUrl);
                Log.d("EventDetailsActivity", "Loading image from file: " + imageFile.getAbsolutePath());
                String filePath = eventImageUrl.replace("file://", ""); // Remove the `file://` prefix
                File imageFileFormatted = new File(filePath); // Create the File object using the corrected path
                Log.d("EventDetailsActivity", "Formatted file path: " + imageFile.getAbsolutePath());
                if (imageFileFormatted.exists()) {
                    Glide.with(this)
                            .load(imageFileFormatted)
                            .placeholder(R.drawable.baseline_place_24)
                            .error(R.drawable.baseline_place_24)
                            .into(eventImageView);
                } else {
                    Log.e("EventDetailsActivity", "Image file does not exist: " + imageFileFormatted.getAbsolutePath());
                    Toast.makeText(this, "Image file not found, displaying default image.", Toast.LENGTH_SHORT).show();
                    eventImageView.setImageResource(R.drawable.baseline_place_24);
                }
            }
        } else {
            // Set a default image if the URL is empty
            eventImageView.setImageResource(R.drawable.baseline_place_24);
        }

        // Handle navigation button click
        navigateButton.setOnClickListener(v -> {
            if (eventLocation != null && !eventLocation.isEmpty()) {
                openLocationInMaps(eventLocation);
            } else {
                Toast.makeText(EventDetailsActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Join/Leave button
        SharedPreferences prefs = getSharedPreferences("joined_events", MODE_PRIVATE);
        Set<String> joinedEvents = prefs.getStringSet("joined_event_titles", new HashSet<>());

        if (joinedEvents.contains(eventTitle)) {
            joinLeaveButton.setText("Leave");
        } else {
            joinLeaveButton.setText("Join");
        }

        joinLeaveButton.setOnClickListener(v -> {
            SharedPreferences prefs1 = getSharedPreferences("joined_events", MODE_PRIVATE);
            Set<String> joinedEvents1 = new HashSet<>(prefs1.getStringSet("joined_event_titles", new HashSet<>()));

            if (joinedEvents1.contains(eventTitle)) {
                // User wants to leave
                joinedEvents1.remove(eventTitle);
                prefs1.edit().putStringSet("joined_event_titles", joinedEvents1).apply();
                joinLeaveButton.setText("Join");
                Toast.makeText(EventDetailsActivity.this, "You have left the event.", Toast.LENGTH_SHORT).show();
            } else {
                // User wants to join
                joinedEvents1.add(eventTitle);
                prefs1.edit().putStringSet("joined_event_titles", joinedEvents1).apply();
                joinLeaveButton.setText("Leave");
                Toast.makeText(EventDetailsActivity.this, "You have joined the event.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to open the location in Google Maps and start navigation
    private void openLocationInMaps(String location) {
        // Create a URI for the navigation intent
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(location));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Verify that there is an app available to handle this intent
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps not available", Toast.LENGTH_SHORT).show();
        }
    }
}
