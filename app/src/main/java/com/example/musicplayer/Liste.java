package com.example.musicplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Liste implements Serializable {
    private String playlisttitle;
    private String descriptplaylist;
    private int coverResourceId;
    private String coverImageUri; // Add this field to store custom image URI
    private List<item> songs;

    public Liste(String playlisttitle, String descriptplaylist, int coverResourceId) {
        this.playlisttitle = playlisttitle;
        this.descriptplaylist = descriptplaylist;
        this.coverResourceId = coverResourceId;
        this.coverImageUri = null;
        this.songs = new ArrayList<>();
    }

    public String getPlaylisttitle() {
        return playlisttitle;
    }

    public String getDescriptplaylist() {
        return descriptplaylist;
    }

    public int getCoverResourceId() {
        return coverResourceId;
    }

    public String getCoverImageUri() {
        return coverImageUri;
    }

    public void setPlaylisttitle(String playlisttitle) {
        this.playlisttitle = playlisttitle;
    }

    public void setDescriptplaylist(String descriptplaylist) {
        this.descriptplaylist = descriptplaylist;
    }

    public void setCoverResourceId(int coverResourceId) {
        this.coverResourceId = coverResourceId;
    }

    public void setCoverImageUri(String coverImageUri) {
        this.coverImageUri = coverImageUri;
    }

    // Methods for managing songs in the playlist
    public List<item> getSongs() {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        return songs;
    }

    public void setSongs(List<item> songs) {
        this.songs = songs;
    }

    public void addSong(item song) {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        songs.add(song);
    }

    public void removeSong(item song) {
        if (songs != null) {
            songs.remove(song);
        }
    }

    // Compatibility methods for the PlaylistManager
    public String getName() {
        return playlisttitle;
    }

    public String getDescription() {
        return descriptplaylist;
    }

    public int getCoverImage() {
        return coverResourceId;
    }
}
