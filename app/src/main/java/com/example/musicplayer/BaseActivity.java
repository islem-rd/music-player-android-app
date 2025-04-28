package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

/**
 * Base activity that handles common functionality for all activities
 * including mini player management
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected MusicPlayer musicPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        musicPlayer = MusicPlayer.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }

    /**
     * Setup the mini player fragment
     * This should be called after setContentView in the child activity
     */
    protected void setupMiniPlayerFragment() {
        // Find the fragment container
        View miniPlayerContainer = findViewById(R.id.mini_player_container);
        if (miniPlayerContainer == null) {
            Log.e(TAG, "Mini player container not found in " + getClass().getSimpleName());
            return;
        }

        // Check if the fragment is already added
        FragmentManager fragmentManager = getSupportFragmentManager();
        MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) fragmentManager
                .findFragmentById(R.id.mini_player_container);

        if (miniPlayerFragment == null) {
            // Create and add the fragment
            miniPlayerFragment = new MiniPlayerFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.mini_player_container, miniPlayerFragment)
                    .commit();

            // Set listener for mini player clicks
            miniPlayerFragment.setMiniPlayerListener(this::openFullPlayer);
        }

        // Register a listener to update the mini player when the song changes
        MiniPlayerFragment finalMiniPlayerFragment = miniPlayerFragment;
        musicPlayer.setOnSongChangedListener((song, singer, img, path) -> {
            runOnUiThread(() -> {
                if (finalMiniPlayerFragment != null && finalMiniPlayerFragment.isAdded()) {
                    finalMiniPlayerFragment.updateMiniPlayer();
                }
            });
        });
    }

    /**
     * Update the mini player UI
     */
    protected void updateMiniPlayer() {
        MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_player_container);

        if (miniPlayerFragment != null && musicPlayer.isMiniPlayerVisible()) {
            miniPlayerFragment.updateMiniPlayer();
        }
    }

    /**
     * Open the full player activity
     * This can be overridden by child activities if needed
     */
    protected void openFullPlayer() {
        if (musicPlayer.getCurrentPath() == null) {
            Toast.makeText(this, "No song is currently playing", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, new_activity.class);
        intent.putExtra("song", musicPlayer.getCurrentSong());
        intent.putExtra("singer", musicPlayer.getCurrentSinger());
        intent.putExtra("path", musicPlayer.getCurrentPath());
        intent.putExtra("img", musicPlayer.getCurrentImg());
        intent.putExtra("currentPosition", musicPlayer.getMediaPlayer().getCurrentPosition());
        intent.putExtra("sourceActivity", getClass().getSimpleName());
        intent.putExtra("isResuming", true);

        // Handle playlist vs regular playback
        if (musicPlayer.isPlayingFromPlaylist()) {
            intent.putExtra("isFromPlaylist", true);
            intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
            intent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
            intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
        } else if (musicPlayer.getAllSongs() != null) {
            intent.putParcelableArrayListExtra("songList", musicPlayer.getAllSongs());
            intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
        }

        // Start the activity with transition
        MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_player_container);

        if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
            View miniPlayerView = miniPlayerFragment.getView();
            ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                    this,
                    intent,
                    miniPlayerView.findViewById(R.id.mini_player),
                    miniPlayerView.findViewById(R.id.album_art),
                    miniPlayerView.findViewById(R.id.song_title),
                    miniPlayerView.findViewById(R.id.artist_name)
            );
        } else {
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }
}
