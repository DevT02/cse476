package com.example.studylink.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.studylink.R;

import java.io.File;

public class EventDetailsActivity extends AppCompatActivity {

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

        // Initialize UI elements
        TextView eventTitleTextView = findViewById(R.id.eventTitleDetails);
        TextView eventDescriptionTextView = findViewById(R.id.eventDescriptionDetails);
        TextView eventDateTextView = findViewById(R.id.eventDateDetails);
        TextView eventLocationTextView = findViewById(R.id.eventLocationDetails);
        ImageView eventImageView = findViewById(R.id.eventImageDetails);
        Button navigateButton = findViewById(R.id.btnNavigate);

        // Set event details to the UI elements
        eventTitleTextView.setText(eventTitle);
        eventDescriptionTextView.setText(eventDescription);
        eventDateTextView.setText(eventDate);
        eventLocationTextView.setText(eventLocation);

        // Load the event image using Glide if the URL is not empty
        if (eventImageUrl != null && !eventImageUrl.isEmpty()) {
            // Check if the URL is a network URL or a local file path
            if (eventImageUrl.startsWith("http")) {
                // It's a network URL
                Glide.with(this)
                        .load(eventImageUrl) // Load network URL directly
                        .placeholder(R.drawable.baseline_place_24) // Placeholder image
                        .error(R.drawable.baseline_place_24) // Error image
                        .into(eventImageView);
            } else {
                // It's a local file path
                File imageFile = new File(eventImageUrl); // Convert to File object for local path
                Glide.with(this)
                        .load(imageFile) // Load the image from the File object
                        .placeholder(R.drawable.baseline_place_24) // Placeholder image
                        .error(R.drawable.baseline_place_24) // Error image
                        .into(eventImageView);
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
