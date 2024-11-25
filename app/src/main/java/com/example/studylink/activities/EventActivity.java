package com.example.studylink.activities;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EventActivity extends AppCompatActivity {

    // UI elements for event details
    private EditText inputEventTitle, inputEventDescription, inputEventDate, inputEventTime, inputEventLocation;
    private Button btnSubmitEvent, btnPickDate, btnPickTime, btnUploadImage, btnUseCurrentLocation;
    private ImageView eventImageView;
    private Timestamp eventTimestamp;

    private FirebaseAuth mAuth; // Firebase Auth instance
    private FirebaseFirestore db; // Firestore instance

    // URI for the event image
    private Uri eventImageUri;

    // For location services
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // For picking an image from the gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    eventImageUri = result.getData().getData();
                    Log.d("EventActivity", "Selected Image URI: " + eventImageUri);
                    eventImageView.setImageURI(eventImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        // Initialize FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

    private void saveEventDataToFirestore(String title, String desc, String image, String location, Timestamp timestamp) {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        // Create an profile data map
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("date", timestamp);
        eventData.put("description", desc);
        eventData.put("location", location);
        eventData.put("title", title);

        if (image != null && !image.isEmpty()) {
            eventData.put("image", image);
        }

        // Create a new document with auto-generated ID in the "events" collection
        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                    // Show success toast
                    Toast.makeText(EventActivity.this, "Event saved successfully", Toast.LENGTH_SHORT).show();
                    // Navigate to Dashboard
                    navigateToDashboard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EventActivity.this, "Error saving event data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            inputEventDate.setText(String.format(Locale.getDefault(),"%02d/%02d/%04d", dayOfMonth, month1 + 1, year1));
        }, year, month, day);
        datePickerDialog.show();
    }

    // Show time picker dialog
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            inputEventTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void combineDateAndTime(String eventDate, String eventTime) {
        // Define the date and time format
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            // Parse the date and time strings into Date objects
            Date date = dateFormat.parse(eventDate);
            Date time = timeFormat.parse(eventTime);

            // Create a Calendar object and set the year, month, and day from the parsed date
            Calendar calendar = Calendar.getInstance();
            if (date != null) {
                calendar.setTime(date);  // Set the full date (year, month, day)
            }

            // Extract the hour and minute from the time string and set them in the calendar
            if (time != null) {
                Calendar timeCalendar = Calendar.getInstance();
                timeCalendar.setTime(time);
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            }

            // Convert the Calendar object to a Date object
            Date combinedDate = calendar.getTime();

            // Convert the Date object to Firebase Timestamp

            // Set the event timestamp (or use it as needed)
            eventTimestamp = new Timestamp(combinedDate);

        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid date or time format", Toast.LENGTH_SHORT).show();
        }
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
        combineDateAndTime(eventDate, eventTime);

        // Check if all required fields are filled
        if (!eventTitle.isEmpty() && !eventDescription.isEmpty() && !eventDate.isEmpty() &&
                !eventTime.isEmpty() && !eventLocation.isEmpty()) {

            // Show a progress indicator
            Toast.makeText(EventActivity.this, "Uploading event...", Toast.LENGTH_SHORT).show();

            if (eventImageUri != null) {
                // Upload image to ImgBB
                uploadImageToImgBB(eventImageUri, uploadedImageUrl -> {
                    // Save event data with the uploaded image URL
                    saveEventDataToFirestore(eventTitle, eventDescription, uploadedImageUrl, eventLocation, eventTimestamp);
                    saveEventToSharedPreferences(eventTitle, eventDescription, eventDate, eventTime, eventLocation, uploadedImageUrl);
                });
            } else {
                // No image selected, proceed with saving event data
                saveEventDataToFirestore(eventTitle, eventDescription, null, eventLocation, eventTimestamp);
                saveEventToSharedPreferences(eventTitle, eventDescription, eventDate, eventTime, eventLocation, null);
            }

        } else {
            Toast.makeText(EventActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }

    }

    // interface for callback
    public interface ImgBBUploadCallback {
        void onSuccess(String uploadedImageUrl);
    }

    // upload to imgBB for firebase
    private void uploadImageToImgBB(Uri imageUri, ImgBBUploadCallback callback) {
        String imgbbApiKey;
        try {
            imgbbApiKey = getApplicationContext()
                    .getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                    .metaData
                    .getString("com.example.studylink.IMGBB_API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            imgbbApiKey = "";
        }
        String uploadUrl = "https://api.imgbb.com/1/upload?key=" + imgbbApiKey;

        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            byte[] imageBytes = new byte[Objects.requireNonNull(inputStream).available()];
            inputStream.read(imageBytes);
            if (inputStream.available() > 32 * 1024 * 1024) { // 32MB limit
                Toast.makeText(EventActivity.this, "Image size exceeds limit", Toast.LENGTH_SHORT).show();
                return;
            }
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", encodedImage)
                    .build();

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    Log.e("ImgBB Upload", "Image upload failed: " + e.getMessage());
                    runOnUiThread(() ->
                            Toast.makeText(EventActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show());
                    retryUpload(imageUri, callback); // Retry on failure
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String uploadedImageUrl = jsonResponse.getJSONObject("data").getString("url");
                            runOnUiThread(() -> callback.onSuccess(uploadedImageUrl));
                        } catch (JSONException e) {
                            Log.e("ImgBB Upload", "Failed to parse response: " + e.getMessage());
                            runOnUiThread(() ->
                                    Toast.makeText(EventActivity.this, "Failed to parse response", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Log.e("ImgBB Upload", "Upload failed: " + response.code());
                        runOnUiThread(() ->
                                Toast.makeText(EventActivity.this, "Upload failed", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (IOException e) {
            Log.e("ImgBB Upload", "Failed to read image: " + e.getMessage());
            Toast.makeText(EventActivity.this, "Failed to read image", Toast.LENGTH_SHORT).show();
        }
    }

    /** retry logic for image upload
    currently retries 3 times before showing a failure message
    * */
    private int retryCount = 0;
    private final int maxRetries = 3;

    private void retryUpload(Uri imageUri, ImgBBUploadCallback callback) {
        if (retryCount < maxRetries) {
            retryCount++;
            Log.d("ImgBB Upload", "Retrying upload... Attempt " + retryCount);
            uploadImageToImgBB(imageUri, callback);
        } else {
            runOnUiThread(() -> {
                Toast.makeText(EventActivity.this, "Image upload failed after " + maxRetries + " attempts", Toast.LENGTH_SHORT).show();
                retryCount = 0; // Reset retry count
            });
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

            if (outputFile.exists()) {
                Log.d("EventActivity", "Image saved to: " + outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            } else {
                Log.e("EventActivity", "Image file does not exist after saving.");
                return null;
            }
        } catch (IOException e) {
            Log.e("EventActivity", "Error saving image: " + e.getMessage());
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Call the superclass implementation first

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to get location
                getCurrentLocation();
            } else {
                // Permission denied, show an alert dialog
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Explain the importance of the permission
                    showPermissionRationaleDialog();
                } else {
                    // User checked "Don't ask again", direct them to settings
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
