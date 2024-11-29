package com.fnprrt.studylink.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fnprrt.studylink.R;
import com.fnprrt.studylink.models.Event;

import java.util.List;
import com.fnprrt.studylink.activities.EventDetailsActivity;


public class EventListAdapter extends ArrayAdapter<Event> {

    private final Context context;
    private final List<Event> events;

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

        // Get references to the views
        TextView groupTitle = convertView.findViewById(R.id.group_title);
        TextView groupDescription = convertView.findViewById(R.id.group_description);
        TextView groupTime = convertView.findViewById(R.id.group_time);
        TextView groupLocation = convertView.findViewById(R.id.group_location);
        ImageView groupImageUrl = convertView.findViewById(R.id.eventImageDetails);

        // Get the event at the current position
        Event event = events.get(position);

        // Set the event details in the views
        groupTitle.setText(event.getTitle());
        groupDescription.setText(event.getDescription());
        groupTime.setText(event.getDate());
        groupLocation.setText(event.getLocation());

        String imageUrl = event.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl) // Load the remote image URL
                    .placeholder(R.drawable.baseline_place_24) // Placeholder while loading
                    .error(R.drawable.baseline_place_24) // Error fallback
                    .into(groupImageUrl);
        } else {
            groupImageUrl.setImageResource(R.drawable.baseline_place_24); // Default image for empty or null URLs
        }


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
