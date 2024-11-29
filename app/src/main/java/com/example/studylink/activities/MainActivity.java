package com.fnprrt.studylink.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.fnprrt.studylink.HomeActivity;
import com.fnprrt.studylink.R;

public class MainActivity extends AppCompatActivity {

    // list to hold events
    // private List<Event> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // check login state from shared preferences
        SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);
        Log.d("MainActivity", "isLoggedIn: " + isLoggedIn);

        // redirect based on login state
        if (isLoggedIn) {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        // load events
        loadEvents();
    }

    // method to load events
    private void loadEvents() {
        // implementation to load events
    }
}