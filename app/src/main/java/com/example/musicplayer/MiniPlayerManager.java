package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Centralized manager for the mini player that appears across activities
 * Handles state management, UI updates, and transitions
 */
public class MiniPlayerManager {
    private static final String TAG = "MiniPlayerManager";
    private static MiniPlayerManager instance;

    private Context context;
    private CardView miniPlayerView;
    private ImageView albumArtView;
    private TextView songTitleView;
    private TextView artistNameView;
    private ImageView playPauseButton;
    private ImageManager imageManager;
    private MusicPlayer musicPlayer;

    private MiniPlayerManager(Context context) {
        this.context = context.getApplicationContext();
        this.musicPlayer = MusicPlayer.getInstance();

        // Initialize ImageManager lazily to avoid circular dependencies
        try {
            this.imageManager = ImageManager.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ImageManager", e);
            // We'll initialize it later if needed
        }
    }

    public static synchronized MiniPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MiniPlayerManager(context);
        } else if (instance.context == null) {
            // Reinitialize if context was lost
            instance.context = context.getApplicationContext();
        }
        return instance;
    }

    /**
     * Initialize the mini player views
     * @param miniPlayerView The CardView containing the mini player
     * @param albumArtView The ImageView for album art
     * @param songTitleView The TextView for song title
     * @param artistNameView The TextView for artist name
     * @param playPauseButton The ImageView for play/pause button
     */
    public void initializeViews(CardView miniPlayerView, ImageView albumArtView,
                                TextView songTitleView, TextView artistNameView,
                                ImageView playPauseButton) {
        if (miniPlayerView == null) {
            Log.e(TAG, "Cannot initialize with null miniPlayerView");
            return;
        }

        this.miniPlayerView = miniPlayerView;
        this.albumArtView = albumArtView;
        this.songTitleView = songTitleView;
        this.artistNameView = artistNameView;
        this.playPauseButton = playPauseButton;

        // Make sure ImageManager is initialized
        if (imageManager == null) {
            try {
                this.imageManager = ImageManager.getInstance(context);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing ImageManager", e);
            }
        }

        setupClickListeners(miniPlayerView.getContext());
    }

    /**
     * Update the mini player UI with current song information
     */
    public void updateMiniPlayer() {
        if (miniPlayerView == null) {
            Log.e(TAG, "Cannot update mini player: views not initialized");
            return;
        }

        if (!musicPlayer.isMiniPlayerVisible()) {
            miniPlayerView.setVisibility(View.GONE);
            return;
        }

        try {
            // Show the mini player
            miniPlayerView.setVisibility(View.VISIBLE);

            item currentItem = musicPlayer.getCurrentSongItem();
            if (currentItem != null) {
                if (songTitleView != null) songTitleView.setText(currentItem.getSong());
                if (artistNameView != null) artistNameView.setText(currentItem.getSinger());

                if (albumArtView != null) {
                    // Prefer image from file metadata
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    try {
                        mmr.setDataSource(currentItem.getPath());
                        byte[] art = mmr.getEmbeddedPicture();
                        if (art != null) {
                            Glide.with(context)
                                    .asBitmap()
                                    .load(art)
                                    .placeholder(R.drawable.albumcover)
                                    .error(R.drawable.albumcover)
                                    .into(albumArtView);
                        } else {
                            albumArtView.setImageResource(R.drawable.albumcover);
                        }
                    } catch (Exception e) {
                        albumArtView.setImageResource(R.drawable.albumcover);
                    } finally {
                        mmr.release();
                    }
                }
            }


            // Update play/pause button
            updatePlayPauseButton();
        } catch (Exception e) {
            Log.e(TAG, "Error updating mini player", e);
        }
    }

    /**
     * Update the play/pause button based on current playback state
     */
    public void updatePlayPauseButton() {
        if (playPauseButton != null) {
            try {
                MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    playPauseButton.setImageResource(R.drawable.pause);
                } else {
                    playPauseButton.setImageResource(R.drawable.play);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating play/pause button", e);
                // Default to play button on error
                playPauseButton.setImageResource(R.drawable.play);
            }
        }
    }

    /**
     * Setup click listeners for the mini player
     * @param context The context for the listeners
     */
    private void setupClickListeners(final Context context) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity, cannot set up click listeners");
            return;
        }

        final Activity activity = (Activity) context;

        // Mini player click listener to open full player
        if (miniPlayerView != null) {
            miniPlayerView.setOnClickListener(v -> openFullPlayer(activity));
        }

        // Play/pause button click listener
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(v -> {
                try {
                    MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            playPauseButton.setImageResource(R.drawable.play);
                        } else {
                            mediaPlayer.start();
                            playPauseButton.setImageResource(R.drawable.pause);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error toggling play/pause", e);
                    Toast.makeText(context, "Error playing media", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Open the full player activity
     * @param activity The current activity
     */
    public void openFullPlayer(Activity activity) {
        try {
            // Don't pause or stop the MediaPlayer, just let it continue playing
            Intent intent = new Intent(activity, new_activity.class);

            // Add current song info to intent
            intent.putExtra("song", musicPlayer.getCurrentSong());
            intent.putExtra("singer", musicPlayer.getCurrentSinger());
            intent.putExtra("path", musicPlayer.getCurrentPath());
            intent.putExtra("img", musicPlayer.getCurrentImg());

            MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();
            int currentPosition = 0;
            if (mediaPlayer != null) {
                try {
                    currentPosition = mediaPlayer.getCurrentPosition();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error getting current position", e);
                }
            }
            intent.putExtra("currentPosition", currentPosition);
            intent.putExtra("sourceActivity", activity.getClass().getSimpleName());
            intent.putExtra("isResuming", true);  // We're continuing with the current song

            // If playing from playlist, pass the playlist info
            if (musicPlayer.isPlayingFromPlaylist()) {
                intent.putExtra("isFromPlaylist", true);
                intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
                intent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
                intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
            } else {
                // If not playing from playlist, pass all songs
                ArrayList<item> allSongs = musicPlayer.getAllSongs();
                if (allSongs != null && !allSongs.isEmpty()) {
                    intent.putParcelableArrayListExtra("songList", allSongs);
                    intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                }
            }

            // Use shared element transition for mini player to full player
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    miniPlayerView != null && albumArtView != null &&
                    songTitleView != null && artistNameView != null) {

                ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                        activity,
                        intent,
                        miniPlayerView,
                        albumArtView,
                        songTitleView,
                        artistNameView
                );
            } else {
                // Fallback for older devices
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening full player", e);
            Toast.makeText(activity, "Error opening player", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show the mini player with the current song
     */
    public void showMiniPlayer() {
        if (miniPlayerView != null) {
            musicPlayer.setMiniPlayerVisible(true);
            updateMiniPlayer();
        } else {
            Log.e(TAG, "Cannot show mini player: views not initialized");
        }
    }

    /**
     * Hide the mini player
     */
    public void hideMiniPlayer() {
        if (miniPlayerView != null) {
            musicPlayer.setMiniPlayerVisible(false);
            miniPlayerView.setVisibility(View.GONE);
        }
    }

    /**
     * Check if the mini player is visible
     * @return true if visible, false otherwise
     */
    public boolean isMiniPlayerVisible() {
        return miniPlayerView != null && miniPlayerView.getVisibility() == View.VISIBLE;
    }

    /**
     * Play a song and update the mini player
     * @param song The song to play
     * @param activity The current activity
     */


    /**
     * Play the next song
     * @param activity The current activity
     */
    public void playNextSong(Activity activity) {
        try {
            item nextSong = musicPlayer.getNextSong();
            if (nextSong != null) {
                // Actually play the song, not just update UI
                playSong(nextSong, activity);

                // Log for debugging
                Log.d(TAG, "Playing next song: " + nextSong.getSong() + " - " + nextSong.getSinger());
            } else {
                Toast.makeText(activity, "No next song available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing next song", e);
            Toast.makeText(activity, "Error playing next song", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Play the previous song
     * @param activity The current activity
     */
    public void playPreviousSong(Activity activity) {
        try {
            item prevSong = musicPlayer.getPreviousSong();
            if (prevSong != null) {
                // Actually play the song, not just update UI
                playSong(prevSong, activity);

                // Log for debugging
                Log.d(TAG, "Playing previous song: " + prevSong.getSong() + " - " + prevSong.getSinger());
            } else {
                Toast.makeText(activity, "No previous song available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing previous song", e);
            Toast.makeText(activity, "Error playing previous song", Toast.LENGTH_SHORT).show();
        }
    }
    public void playSong(item song, Activity activity) {
        if (song == null || song.getPath() == null) {
            Toast.makeText(activity, "Cannot play this song", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the centralized playback method from MusicPlayer
        musicPlayer.playSong(song, activity);

        // Update the UI
        updateMiniPlayer();
    }



}
