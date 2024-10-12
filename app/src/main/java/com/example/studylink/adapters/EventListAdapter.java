package com.example.studylink.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.studylink.R;
import com.example.studylink.activities.EventDetailsActivity;
import com.example.studylink.models.Event;

import java.io.File;
import java.util.List;

public class EventListAdapter extends ArrayAdapter<Event> {

    private List<Event> events;
    private Context context;

    public EventListAdapter(Context context, List<Event> events) {
        super(context, 0, events);
        this.context = context;
        this.events = events;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event, parent, false);
        }

        TextView eventTitleTextView = convertView.findViewById(R.id.eventTitle);
        ImageView eventImageView = convertView.findViewById(R.id.eventImage);
        TextView eventDescription = convertView.findViewById(R.id.eventDescription);

        Event event = events.get(position);
        eventTitleTextView.setText(event.getTitle());
        eventDescription.setText(event.getDescription());

        if (!event.getImageUrl().isEmpty()) {
            Uri imageUri;
            if (event.getImageUrl().startsWith("content://")) {
                imageUri = Uri.parse(event.getImageUrl());
            } else {
                // Assume it's a file path in internal storage
                File imageFile = new File(context.getFilesDir(), event.getImageUrl());
                imageUri = Uri.fromFile(imageFile);
            }

            Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.baseline_place_24)
                    .error(R.drawable.baseline_place_24)
                    .into(eventImageView);
        } else {
            eventImageView.setImageResource(R.drawable.baseline_place_24);
        }        Log.d("EventListAdapter", "Image URI: " + event.getImageUrl());

        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_description", event.getDescription());
            intent.putExtra("event_date", event.getDate());
            intent.putExtra("event_location", event.getLocation());
            intent.putExtra("event_image_url", event.getImageUrl());
            context.startActivity(intent);
        });

        return convertView;
    }

}
