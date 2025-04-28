package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages playlist storage and retrieval using SharedPreferences
 */
public class PlaylistManager {
    private static final String TAG = "PlaylistManager";
    private static final String PREF_NAME = "PlaylistPreferences";
    private static final String KEY_PLAYLISTS = "playlists";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public PlaylistManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * Save a list of playlists to SharedPreferences
     */
    public void savePlaylists(List<Liste> playlists) {
        try {
            String json = gson.toJson(playlists);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_PLAYLISTS, json);
            editor.apply();
            Log.d(TAG, "Playlists saved successfully: " + playlists.size());
        } catch (Exception e) {
            Log.e(TAG, "Error saving playlists", e);
        }
    }

    /**
     * Retrieve the list of playlists from SharedPreferences
     */
    public List<Liste> getPlaylists() {
        try {
            String json = sharedPreferences.getString(KEY_PLAYLISTS, null);
            if (json == null) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<Liste>>() {}.getType();
            List<Liste> playlists = gson.fromJson(json, type);
            Log.d(TAG, "Playlists loaded successfully: " + playlists.size());
            return playlists;
        } catch (Exception e) {
            Log.e(TAG, "Error loading playlists", e);
            return new ArrayList<>();
        }
    }

    /**
     * Add a new playlist to the existing list
     */
    public void addPlaylist(Liste playlist) {
        List<Liste> playlists = getPlaylists();
        playlists.add(playlist);
        savePlaylists(playlists);
        Log.d(TAG, "Added playlist: " + playlist.getPlaylisttitle());
    }

    /**
     * Remove a playlist by name
     */
    public void removePlaylist(String playlistName) {
        List<Liste> playlists = getPlaylists();
        boolean removed = false;

        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getPlaylisttitle().equals(playlistName)) {
                playlists.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            savePlaylists(playlists);
            Log.d(TAG, "Removed playlist: " + playlistName);
        } else {
            Log.w(TAG, "Playlist not found for removal: " + playlistName);
        }
    }

    /**
     * Update an existing playlist
     */
    public void updatePlaylist(String oldName, Liste updatedPlaylist) {
        List<Liste> playlists = getPlaylists();
        boolean updated = false;

        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getPlaylisttitle().equals(oldName)) {
                playlists.set(i, updatedPlaylist);
                updated = true;
                break;
            }
        }

        if (updated) {
            savePlaylists(playlists);
            Log.d(TAG, "Updated playlist: " + oldName + " to " + updatedPlaylist.getPlaylisttitle());
        } else {
            Log.w(TAG, "Playlist not found for update: " + oldName);
        }
    }
}
