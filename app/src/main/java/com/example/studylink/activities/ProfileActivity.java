package com.example.studylink.activities;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.studylink.HomeActivity;
import com.example.studylink.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 100;
    private static final int REQUEST_PERMISSION_CAMERA = 200;

    // ui elements
    private ImageView profilePicture;
    private EditText inputName, inputEmail, inputInterests, inputContactLink, inputClasses, inputBio;
    private SwitchMaterial switchGPA, switchAvailability, switchNotifications;
    private Button btnUploadImage, btnSaveChanges, btnLogOut, btnTakePicture;

    private Uri profileImageUri;
    private static final String PROFILE_IMAGE_URI_KEY = "profile_image_uri";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // activity result launcher for image picking
    private ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        profileImageUri = selectedImageUri;
                        setImageUri(profileImageUri); // Temporarily set the image
                    }
                }
            }
    );

    // activity result launcher for capturing photos
    private ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result && profileImageUri != null) {
                    Log.d("ProfileActivity", "Image captured successfully. URI: " + profileImageUri.toString());
                    profilePicture.setImageURI(profileImageUri);
                } else {
                    Log.e("ProfileActivity", "Failed to capture image or profileImageUri is null.");
                    Toast.makeText(ProfileActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up the toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable the back button
        }

        // initialize ui elements
        profilePicture = findViewById(R.id.profilePicture);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputInterests = findViewById(R.id.inputInterests);
        inputContactLink = findViewById(R.id.inputContactLink);
        inputClasses = findViewById(R.id.inputClasses);
        inputBio = findViewById(R.id.inputBio);
        switchGPA = findViewById(R.id.switchGPA);
        switchAvailability = findViewById(R.id.switchAvailability);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnLogOut = findViewById(R.id.btnLogOut);
        btnTakePicture = findViewById(R.id.btnTakePicture);

        // restore profile image uri if available
        if (savedInstanceState != null) {
            profileImageUri = savedInstanceState.getParcelable(PROFILE_IMAGE_URI_KEY);
            if (profileImageUri != null) {
                setImageUri(profileImageUri);
            }
        }

        // load user data
        loadUserData();

        // set click listeners
        btnTakePicture.setOnClickListener(v -> requestCameraPermission());
        btnUploadImage.setOnClickListener(v -> requestStoragePermission());
        btnSaveChanges.setOnClickListener(v -> saveUserData()); // Update to navigate to HomeActivity
        btnLogOut.setOnClickListener(v -> {
            SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Handle the back button click in the toolbar
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to HomeActivity when the back button is pressed
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            startActivity(intent);
            finish(); // End the current activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // request storage permission
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker(); // Permission already granted
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker(); // Permission already granted
            }
        }
    }


    // request camera permissions
    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // Show explanation before requesting permission
                    new AlertDialog.Builder(this)
                            .setTitle("Camera Permission Needed")
                            .setMessage("This app requires access to your camera to take profile pictures.")
                            .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA))
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    // Request permission directly
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
                }
            } else {
                openCamera(); // Permission already granted, open the camera
            }
        } else {
            openCamera(); // Permission handling is not needed for Android versions below M
        }
    }

    // request permissions and handle any errors as necessary
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
                if (!showRationale) {
                    // User has permanently denied permission, navigate to settings
                    new AlertDialog.Builder(this)
                            .setTitle("Camera Permission Required")
                            .setMessage("Camera permission is needed to take photos. Please enable it in the app settings.")
                            .setPositiveButton("Settings", (dialog, which) -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    Toast.makeText(this, "Permission denied to use camera", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with image loading
                openImagePicker();
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (!showRationale) {
                    // User has permanently denied permission, navigate to settings
                    new AlertDialog.Builder(this)
                            .setTitle("Storage Permission Required")
                            .setMessage("Storage permission is needed to access photos. Please enable it in the app settings.")
                            .setPositiveButton("Settings", (dialog, which) -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (profileImageUri != null) {
            outState.putParcelable(PROFILE_IMAGE_URI_KEY, profileImageUri);
        }
    }

    // open image picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        pickImageLauncher.launch(intent);
    }

    // open camera
    private void openCamera() {
        File destinationFile = new File(getExternalFilesDir(null), "profile_picture.jpg");
        // Use FileProvider to get a secure content URI
        profileImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", destinationFile);
        takePictureLauncher.launch(profileImageUri);
    }


    // set image uri to the imageview safely
    private void setImageUri(Uri uri) {
        try {
            Log.d("ProfileActivity", "Attempting to load image Uri: " + uri.toString());
            grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                throw new Exception("Failed to open input stream");
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            profilePicture.setImageBitmap(bitmap);
        } catch (SecurityException e) {
            Log.e("ProfileActivity", "SecurityException when loading image Uri: " + uri.toString(), e);
            Toast.makeText(this, "Error loading image: Permission Denied", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.e("ProfileActivity", "File not found when loading image Uri: " + uri.toString(), e);
            Toast.makeText(this, "Error loading image: File not found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("ProfileActivity", "IOException when loading image Uri: " + uri.toString(), e);
            Toast.makeText(this, "Error loading image: Input/output issue", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ProfileActivity", "General Exception when loading image Uri: " + uri.toString(), e);
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    // save profile image uri
    private void saveProfileImageUri(Uri uri) {
        SharedPreferences preferences = getSharedPreferences("user_profile", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PROFILE_IMAGE_URI_KEY, uri.toString());
        editor.apply();
        Log.d("ProfileActivity", "Saved profile image URI: " + uri.toString());
    }

    // copy image to internal storage
    private void copyImageToInternalStorage(Uri sourceUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            profilePicture.setImageBitmap(bitmap);
            profileImageUri = sourceUri; // Set URI but do not save yet
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error setting image: " + e.getMessage());
            Toast.makeText(this, "Failed to set image", Toast.LENGTH_SHORT).show();
        }
    }


    // load user data
    private void loadUserData() {
        // Get the currently logged-in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Get the email from Firebase Auth and set it to the email input field if it's not null
            String email = currentUser.getEmail();
            if (email != null && inputEmail.getText().toString().isEmpty()) {
                inputEmail.setText(email);
            }
        }

        // Fetch profile data from Firestore
        String userId = mAuth.getCurrentUser().getUid(); // Unique user ID

        db.collection("profiles").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Map<String, Object> profileData = task.getResult().getData();

                        if (profileData != null) {
                            inputName.setText((String) profileData.get("name"));
                            // Only set the email from Firestore if it's empty (useful for cases where the Auth email is primary)
                            if (inputEmail.getText().toString().isEmpty()) {
                                inputEmail.setText((String) profileData.get("email"));
                            }
                            inputInterests.setText((String) profileData.get("interests"));
                            inputContactLink.setText((String) profileData.get("contactLink"));
                            inputClasses.setText((String) profileData.get("classes"));
                            inputBio.setText((String) profileData.get("bio"));
                            switchGPA.setChecked((Boolean) profileData.get("gpaVisible"));
                            switchAvailability.setChecked((Boolean) profileData.get("availability"));
                            switchNotifications.setChecked((Boolean) profileData.get("notificationsEnabled"));

                            if (profileData.containsKey("profileImageUri")) {
                                String uriString = (String) profileData.get("profileImageUri");
                                if (uriString != null && !uriString.isEmpty()) {
                                    profileImageUri = Uri.parse(uriString);
                                    setImageUri(profileImageUri); // Load and display the image
                                } else {
                                    Log.d("ProfileActivity", "No profile image URI found");
                                }
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                        Log.e("ProfileActivity", "Failed to load profile data", task.getException());
                    }
                });
    }


    // save user data and navigate to HomeActivity
    private void saveUserData() {
        String userId = mAuth.getCurrentUser().getUid(); // Unique user ID
        String name = inputName.getText().toString();
        String email = inputEmail.getText().toString();
        String interests = inputInterests.getText().toString();
        String contactLink = inputContactLink.getText().toString();
        String classes = inputClasses.getText().toString();
        String bio = inputBio.getText().toString();
        boolean gpaVisible = switchGPA.isChecked();
        boolean availability = switchAvailability.isChecked();
        boolean notificationsEnabled = switchNotifications.isChecked();

        // Create a profile data map
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("email", email);
        profileData.put("interests", interests);
        profileData.put("contactLink", contactLink);
        profileData.put("classes", classes);
        profileData.put("bio", bio);
        profileData.put("gpaVisible", gpaVisible);
        profileData.put("availability", availability);
        profileData.put("notificationsEnabled", notificationsEnabled);

        // Check if profile image URI is set, and only add it if it's not null
        if (profileImageUri != null) {
            profileData.put("profileImageUri", profileImageUri.toString());
        }

        // Save data to Firestore under "profiles" collection with userId as the document ID
        db.collection("profiles").document(userId).set(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                    Log.e("ProfileActivity", "Failed to save profile", e);
                });
    }


    private void saveImageToInternalStorage(Uri sourceUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            File destinationFile = new File(getFilesDir(), "profile_picture.jpg");

            if (destinationFile.exists()) {
                destinationFile.delete();
            }

            OutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            profileImageUri = Uri.fromFile(destinationFile); // Update with saved image URI

            Log.d("ProfileActivity", "Image saved successfully. URI: " + profileImageUri.toString());

        } catch (Exception e) {
            Log.e("ProfileActivity", "Error saving image: " + e.getMessage());
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
