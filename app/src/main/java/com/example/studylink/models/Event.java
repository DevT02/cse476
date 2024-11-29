package com.fnprrt.studylink.models;

public class Event {
    private String title;
    private String description;
    private String date;
    private String location;
    private String imageUrl;

    public Event() {}

    public Event(String title, String description, String date, String location, String imageUrl) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
}
