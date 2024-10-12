package com.example.studylink.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.studylink.HomeActivity;
import com.example.studylink.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class EventActivity extends AppCompatActivity {

    // elements for event details
    private EditText inputEventTitle, inputEventDescription, inputEventDate, inputEventTime, inputEventLocation;
    private Button btnSubmitEvent, btnPickDate, btnPickTime, btnUploadImage;
    private ImageView eventImageView;

    // URI for the event image
    private Uri eventImageUri;

    // for picking an image from the gallery (ensure correct perms.)
    private ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    eventImageUri = result.getData().getData();
                    eventImageView.setImageURI(eventImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        // Set up the toolbar and enable the back button
        Toolbar toolbar = findViewById(R.id.toolbar_create_event);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable the back button
        }

        // init UI elements
        initializeViews();
        // set up listeners for buttons
        setupListeners();
    }

    // Handle the back button click in the toolbar
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate to HomeActivity when the back button is pressed
            Intent intent = new Intent(EventActivity.this, HomeActivity.class);
            startActivity(intent);
            finish(); // End the current activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // initialize UI elements
    private void initializeViews() {
        inputEventTitle = findViewById(R.id.inputEventTitle);
        inputEventDescription = findViewById(R.id.inputEventDescription);
        inputEventLocation = findViewById(R.id.inputEventLocation);
        inputEventDate = findViewById(R.id.inputEventDate);
        inputEventTime = findViewById(R.id.inputEventTime);
        eventImageView = findViewById(R.id.eventImageView);
        btnSubmitEvent = findViewById(R.id.btnSubmitEvent);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnUploadImage = findViewById(R.id.btnUploadImage);
    }

    // set up listeners for buttons
    private void setupListeners() {
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnUploadImage.setOnClickListener(v -> openImagePicker());
        btnSubmitEvent.setOnClickListener(v -> submitEvent());
    }

    // show date picker dialog
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            inputEventDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month1 + 1, year1));
        }, year, month, day);
        datePickerDialog.show();
    }

    // show time picker dialog
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            inputEventTime.setText(String.format("%02d:%02d", hourOfDay, minute1));
        }, hour, minute, true);
        timePickerDialog.show();
    }

    // open image picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    // submit event details
    private void submitEvent() {
        String eventTitle = inputEventTitle.getText().toString();
        String eventDescription = inputEventDescription.getText().toString();
        String eventDate = inputEventDate.getText().toString();
        String eventTime = inputEventTime.getText().toString();
        String eventLocation = inputEventLocation.getText().toString();

        // Check if all fields are filled and image is uploaded
        if (!eventTitle.isEmpty() && !eventDescription.isEmpty() && !eventDate.isEmpty() &&
                !eventTime.isEmpty() && !eventLocation.isEmpty() && eventImageUri != null) {

            // Save image to internal storage
            String savedImagePath = saveImageToInternalStorage(eventImageUri);
            if (savedImagePath != null) {
                // Save event details to SharedPreferences
                saveEventToSharedPreferences(eventTitle, eventDescription, eventDate, eventTime, eventLocation, savedImagePath);
                Toast.makeText(EventActivity.this, "Event Created Successfully!", Toast.LENGTH_SHORT).show();
                // Navigate to Dashboard
                navigateToDashboard();
            } else {
                Toast.makeText(EventActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(EventActivity.this, "Please fill in all fields and upload an image", Toast.LENGTH_SHORT).show();
        }
    }

    // save image to internal storage
    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File outputDir = getApplicationContext().getFilesDir();
            File outputFile = new File(outputDir, "event_image_" + System.currentTimeMillis() + ".jpg");

            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4 * 1024]; // 4KB buffer
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // save event details to SharedPreferences
    private void saveEventToSharedPreferences(String title, String description, String date, String time, String location, String imagePath) {
        SharedPreferences preferences = getSharedPreferences("user_events", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        int eventCount = preferences.getInt("event_count", 0);

        editor.putString("event_title_" + eventCount, title);
        editor.putString("event_description_" + eventCount, description);
        editor.putString("event_date_" + eventCount, date);
        editor.putString("event_time_" + eventCount, time);
        editor.putString("event_location_" + eventCount, location);
        editor.putString("event_image_path_" + eventCount, imagePath);
        editor.putInt("event_count", eventCount + 1);
        editor.apply();
    }

    // Navigate to DashboardActivity (or HomeActivity in this case)
    private void navigateToDashboard() {
        Intent intent = new Intent(EventActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
