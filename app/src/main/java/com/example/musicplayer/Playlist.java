package com.example.musicplayer;

public class Playlist {
    private String title;
    private String artist;
    private String duration;
    private int coverResourceId;
    public Playlist(String title, String artist, String duration, int coverResourceId) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.coverResourceId = coverResourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDuration() {
        return duration;
    }

    public int getCoverResourceId() {
        return coverResourceId;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setCoverResourceId(int coverResourceId) {
        this.coverResourceId = coverResourceId;
    }
}
