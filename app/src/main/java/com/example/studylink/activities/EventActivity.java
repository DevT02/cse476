package com.example.studylink.activities;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.studylink.HomeActivity;
import com.example.studylink.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {

    // UI elements for event details
    private EditText inputEventTitle, inputEventDescription, inputEventDate, inputEventTime, inputEventLocation;
    private Button btnSubmitEvent, btnPickDate, btnPickTime, btnUploadImage, btnUseCurrentLocation;
    private ImageView eventImageView;

    // URI for the event image
    private Uri eventImageUri;

    // For location services
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // For picking an image from the gallery
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

        // Initialize UI elements
        initializeViews();

        // Set up listeners for buttons
        setupListeners();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

    // Initialize UI elements
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
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
    }

    // Set up listeners for buttons
    private void setupListeners() {
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnUploadImage.setOnClickListener(v -> openImagePicker());
        btnSubmitEvent.setOnClickListener(v -> submitEvent());
        btnUseCurrentLocation.setOnClickListener(v -> getCurrentLocation());
    }

    // Show date picker dialog
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

    // Show time picker dialog
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            inputEventTime.setText(String.format("%02d:%02d", hourOfDay, minute1));
        }, hour, minute, true);
        timePickerDialog.show();
    }

    // Open image picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    // Submit event details
    private void submitEvent() {
        String eventTitle = inputEventTitle.getText().toString();
        String eventDescription = inputEventDescription.getText().toString();
        String eventDate = inputEventDate.getText().toString();
        String eventTime = inputEventTime.getText().toString();
        String eventLocation = inputEventLocation.getText().toString();

        // Check if all required fields are filled
        if (!eventTitle.isEmpty() && !eventDescription.isEmpty() && !eventDate.isEmpty() &&
                !eventTime.isEmpty() && !eventLocation.isEmpty()) {

            // If an image is selected, save it to internal storage
            String savedImagePath = null;
            if (eventImageUri != null) {
                savedImagePath = saveImageToInternalStorage(eventImageUri);
                if (savedImagePath == null) {
                    Toast.makeText(EventActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
                    return; // Exit if image saving fails
                }
            }

            // Save event details to SharedPreferences or database with the image path if available
            saveEventToSharedPreferences(eventTitle, eventDescription, eventDate, eventTime, eventLocation, savedImagePath);
            Toast.makeText(EventActivity.this, "Event Created Successfully!", Toast.LENGTH_SHORT).show();

            // Navigate to Dashboard or HomeActivity
            navigateToDashboard();

        } else {
            Toast.makeText(EventActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }
    }

    // Save image to internal storage
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

    // Save event details to SharedPreferences
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

    // Handle location permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to get location
                getCurrentLocation();
            } else {
                // Permission denied, show an alert dialog
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // If the user denied but didn't check "Don't ask again", explain the importance of the permission
                    showPermissionRationaleDialog();
                } else {
                    // User denied the permission and checked "Don't ask again", direct them to settings
                    showGoToSettingsDialog();
                }
            }
        }
    }
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission to provide location-based services. Please grant the permission.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Try requesting the permission again
                    ActivityCompat.requestPermissions(EventActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
    private void showGoToSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Location permission is permanently denied. You can enable it in the app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Open the app's settings so the user can manually grant the permission
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }




    // Get current location
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission is granted, proceed to get the location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // Use the location object
                            // Reverse geocode to get address
                            getAddressFromLocation(location);
                        } else {
                            // Handle null location
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure in getting location
                        Toast.makeText(this, "Failed to get current location", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Get address from location
    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Build address string
                StringBuilder addressStr = new StringBuilder();

                // Add the street number (sub-thoroughfare) if available
                if (address.getSubThoroughfare() != null) {
                    addressStr.append(address.getSubThoroughfare()).append(" ");
                }

                // Add the street name (thoroughfare) if available
                if (address.getThoroughfare() != null) {
                    addressStr.append(address.getThoroughfare()).append(", ");
                }

                // Add the locality (city) if available
                if (address.getLocality() != null) {
                    addressStr.append(address.getLocality()).append(", ");
                }

                // Add the admin area (state/province) if available
                if (address.getAdminArea() != null) {
                    addressStr.append(address.getAdminArea()).append(", ");
                }

                // Add the country name if available
                if (address.getCountryName() != null) {
                    addressStr.append(address.getCountryName());
                }

                // Set the address in the inputEventLocation EditText
                inputEventLocation.setText(addressStr.toString());
            } else {
                // No address found
                Toast.makeText(this, "Unable to find address for location", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to get address from location", Toast.LENGTH_SHORT).show();
        }
    }

}
