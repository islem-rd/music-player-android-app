package com.example.musicplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class new_activity extends AppCompatActivity {
    private static final String TAG = "new_activity";
    private ArrayList<item> items;
    private ImageView down, play, heart, back, next, alea, order;
    private ShapeableImageView musicimg;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView duree, sin, son;
    private Handler handler = new Handler();
    private ConstraintLayout consL;
    private int cheart = 0;
    private Runnable updateSeekBarRunnable;
    private int currentSongIndex;
    private PlaylistManager playlistManager;
    private int aleaClickCount = 0;
    private int currentPosition;
    private String song, singer, path, img;
    private MusicPlayer musicPlayer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the navigation bar (Android 4.4+)
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_new);

        try {
            playlistManager = new PlaylistManager(this);
            musicPlayer = MusicPlayer.getInstance();
            mediaPlayer = musicPlayer.getMediaPlayer();
            ImageManager imageManager = ImageManager.getInstance(this);

            // Get intent data
            currentSongIndex = getIntent().getIntExtra("currentSongIndex", 0);
            currentPosition = getIntent().getIntExtra("currentPosition", 0);

            item currentItem = MusicPlayer.getInstance().getCurrentSongItem();

            if (currentItem == null || currentItem.getPath() == null) {
                Toast.makeText(this, "Song data is missing!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

// Optional: Store for local use
            song = currentItem.getSong();
            singer = currentItem.getSinger();
            path = currentItem.getPath();

            if (song == null || path == null) {
                Toast.makeText(this, "Error: Song data is missing!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // Check if we're coming from a playlist
            boolean isFromPlaylist = getIntent().getBooleanExtra("isFromPlaylist", false);
            String playlistName = getIntent().getStringExtra("playlistName");
            ArrayList<item> songList = getIntent().getParcelableArrayListExtra("songList");

            if (isFromPlaylist && playlistName != null) {
                // Update the UI to show playlist name
                setTitle("Playing from: " + playlistName);

                // Update the MusicPlayer singleton
                musicPlayer.setPlayingFromPlaylist(true);
                musicPlayer.setCurrentPlaylistName(playlistName);
                musicPlayer.setCurrentPlaylistSongs(songList);
                musicPlayer.setCurrentSongIndex(currentSongIndex);
            } else {
                // Not from a playlist, reset the playlist flags
                musicPlayer.setPlayingFromPlaylist(false);
                musicPlayer.setCurrentPlaylistName(null);
                musicPlayer.setCurrentPlaylistSongs(null);
            }

            if (songList != null) {
                items = songList;
            }

            // Initialize UI elements
            play = findViewById(R.id.play);
            back = findViewById(R.id.before);
            next = findViewById(R.id.next);
            alea = findViewById(R.id.alea);
            order = findViewById(R.id.order);
            duree = findViewById(R.id.duree);
            seekBar = findViewById(R.id.seekBar);
            heart = findViewById(R.id.heart);
            sin = findViewById(R.id.singerName);
            son = findViewById(R.id.musicName);
            musicimg = findViewById(R.id.musicImg);
            down = findViewById(R.id.down);
            consL = findViewById(R.id.second_layout);

            // Update MusicPlayer with current song info
            musicPlayer.setCurrentSongInfo(song, singer, img, path);

            if (song == null || path == null) {
                Toast.makeText(this, "Error: Song data is missing!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set song info in UI
            sin.setText(singer);
            son.setText(song);

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(path); // from intent

                byte[] art = mmr.getEmbeddedPicture();
                if (art != null) {
                    Glide.with(this)
                            .asBitmap()
                            .load(art)
                            .placeholder(R.drawable.albumcover)
                            .error(R.drawable.albumcover)
                            .into(musicimg);
                } else {
                    musicimg.setImageResource(R.drawable.albumcover);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art", e);
                musicimg.setImageResource(R.drawable.albumcover);
            } finally {
                mmr.release();
            }


            // Check if we need to play a new song or continue with the current one
            boolean shouldPlayNewSong = !getIntent().getBooleanExtra("isResuming", false);

            // Setup MediaPlayer
            try {
                // If we're coming from clicking a song in the list, the song should already be playing
                // Just update the UI to reflect the current state
                if (mediaPlayer.isPlaying()) {
                    play.setImageResource(R.drawable.pause);
                } else {
                    play.setImageResource(R.drawable.play);
                }

                // Make sure the seekbar is updated immediately with the current position
                if (mediaPlayer != null) {
                    seekBar.setMax(mediaPlayer.getDuration());
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    duree.setText(formatTime(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up media player", e);
                Toast.makeText(this, "Error setting up player", Toast.LENGTH_SHORT).show();
            }

            // Setup seekbar
            seekBar.setMax(mediaPlayer.getDuration());
            duree.setText(formatTime(mediaPlayer.getDuration() - currentPosition));

            updateSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            int currentPos = mediaPlayer.getCurrentPosition();
                            seekBar.setProgress(currentPos);
                            duree.setText(formatTime(mediaPlayer.getDuration() - currentPos));
                        }
                        // Continue updating even if not playing to keep UI responsive
                        handler.postDelayed(this, 500); // Update every 500ms instead of 1000ms for smoother UI
                    } catch (IllegalStateException e) {
                        // Handle potential IllegalStateException that can occur if MediaPlayer is in an invalid state
                        Log.e(TAG, "Error updating seekbar", e);
                    }
                }
            };
            handler.post(updateSeekBarRunnable);

            mediaPlayer.setOnCompletionListener(mp -> {
                play.setImageResource(R.drawable.play);
                boolean repeatOne = musicPlayer.isRepeatOne();
                int mode = musicPlayer.getPlayMode();

                if (repeatOne) {
                    // Repeat current song
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                    play.setImageResource(R.drawable.pause);
                } else if (musicPlayer.isPlayingFromPlaylist()) {
                    // Playing from playlist, get next song
                    item nextSong = musicPlayer.getNextSong();
                    if (nextSong != null) {
                        playSong(nextSong);
                    }
                } else if (items != null && !items.isEmpty()) {
                    // Not playing from playlist, use normal logic
                    if (mode == MusicPlayer.MODE_SHUFFLE) {
                        // Play random song
                        int randomIndex = new Random().nextInt(items.size());
                        currentSongIndex = randomIndex;
                        playSong(items.get(randomIndex));
                    } else {
                        // Play next song in order
                        if (currentSongIndex < items.size() - 1) {
                            currentSongIndex++;
                            playSong(items.get(currentSongIndex));
                        } else {
                            // Loop back to first song
                            currentSongIndex = 0;
                            playSong(items.get(currentSongIndex));
                        }
                    }
                }
            });

            // Down button click listener
            // Replace your existing down.setOnClickListener with this implementation
            down.setOnClickListener(v -> {
                animate(down, () -> {
                    musicPlayer.setMiniPlayerVisible(true);

                    // Always get the most recent song info from MusicPlayer
                    String currentSong = musicPlayer.getCurrentSong();
                    String currentSinger = musicPlayer.getCurrentSinger();
                    String currentPath = musicPlayer.getCurrentPath();
                    String currentImg = musicPlayer.getCurrentImg();
                    int currentPosition = mediaPlayer.getCurrentPosition();

                    String sourceActivity = getIntent().getStringExtra("sourceActivity");
                    Intent intent;

                    if ("DecouvrirActivity".equals(sourceActivity)) {
                        intent = new Intent(new_activity.this, DecouvrirActivity.class);
                    } else if ("LibraryActivity".equals(sourceActivity)) {
                        intent = new Intent(new_activity.this, LibraryActivity.class);
                    } else {
                        intent = new Intent(new_activity.this, MainActivity.class);
                    }

                    // Updated: Always use latest info
                    intent.putExtra("isMiniPlayerVisible", true);
                    intent.putExtra("song", currentSong);
                    intent.putExtra("singer", currentSinger);
                    intent.putExtra("path", currentPath);
                    intent.putExtra("img", currentImg);
                    intent.putExtra("currentPosition", currentPosition);

                    // Pass playlist info if needed
                    if (musicPlayer.isPlayingFromPlaylist()) {
                        intent.putExtra("isFromPlaylist", true);
                        intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
                        intent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
                        intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                    } else if (items != null) {
                        intent.putParcelableArrayListExtra("songList", items);
                        intent.putExtra("currentSongIndex", currentSongIndex);
                    }

                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                });
            });


            // Play/pause button click listener
            play.setOnClickListener(v -> {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    play.setImageResource(R.drawable.play);
                } else {
                    mediaPlayer.start();
                    play.setImageResource(R.drawable.pause);
                }
            });

            // Next button click listener
            back.setOnClickListener(v -> {
                musicPlayer.setMiniPlayerVisible(true);

                try {
                    if (musicPlayer.isPlayingFromPlaylist()) {
                        // Get the previous song from the playlist
                        item prevSong = musicPlayer.getPreviousSong();
                        if (prevSong != null) {
                            playSong(prevSong);
                            Log.d(TAG, "Playing previous song from playlist: " + prevSong.getSong());
                        }
                    } else if (items != null && !items.isEmpty()) {
                        if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                            // Shuffle is enabled, play a random song
                            int randomIndex = (int) (Math.random() * items.size());
                            currentSongIndex = randomIndex;
                            playSong(items.get(randomIndex));
                            Log.d(TAG, "Playing random song: " + items.get(randomIndex).getSong());
                        } else {
                            // If we're at the beginning of the song (less than 3 seconds in), go to previous song
                            if (mediaPlayer.getCurrentPosition() < 3000) {
                                if (currentSongIndex <= 0) {
                                    currentSongIndex = items.size() - 1; // Loop to last song
                                } else {
                                    currentSongIndex--;
                                }
                                playSong(items.get(currentSongIndex));
                                Log.d(TAG, "Playing previous song: " + items.get(currentSongIndex).getSong());
                            } else {
                                // Otherwise just restart the current song
                                mediaPlayer.seekTo(0);
                                Log.d(TAG, "Restarting current song");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error playing previous song", e);
                    Toast.makeText(new_activity.this, "Error playing previous song", Toast.LENGTH_SHORT).show();
                }
            });
            MusicPlayer.getInstance().setOnPlayerStateChangedListener(isPlaying -> {
                if (isPlaying) {
                    play.setImageResource(R.drawable.pause); // show Pause when playing
                } else {
                    play.setImageResource(R.drawable.play); // show Play when stopped
                }
            });


            // Previous button click listener
            // Next button example
            next.setOnClickListener(v -> {
                musicPlayer.setMiniPlayerVisible(true);

                try {
                    if (musicPlayer.isPlayingFromPlaylist()) {
                        item nextSong = musicPlayer.getNextSong();
                        if (nextSong != null) {
                            currentSongIndex = musicPlayer.getCurrentSongIndex(); // Sync index
                            playSong(nextSong);
                        }
                    } else if (items != null && !items.isEmpty()) {
                        if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                            int randomIndex = new Random().nextInt(items.size());
                            currentSongIndex = randomIndex;
                            playSong(items.get(randomIndex));
                        } else {
                            if (currentSongIndex >= items.size() - 1) {
                                currentSongIndex = 0;
                            } else {
                                currentSongIndex++;
                            }
                            playSong(items.get(currentSongIndex));
                        }
                        musicPlayer.setCurrentSongIndex(currentSongIndex); // Update singleton
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error playing next song", e);
                    Toast.makeText(this, "Error playing next song", Toast.LENGTH_SHORT).show();
                }
            });

            // SeekBar listener
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                        duree.setText(formatTime(mediaPlayer.getDuration() - progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // Shuffle button click listener
            alea.setOnClickListener(v -> {
                animate(alea, () -> {
                    // Ensure repeat is turned off when shuffle is on
                    if (musicPlayer.isRepeatOne()) {
                        musicPlayer.toggleRepeatOne();
                        order.setImageResource(R.drawable.repeat);
                    }

                    if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                        // If already in shuffle mode, turn it off
                        musicPlayer.setPlayMode(MusicPlayer.MODE_NORMAL);
                        alea.setImageResource(R.drawable.alea);
                    } else {
                        // Enable shuffle mode
                        musicPlayer.setPlayMode(MusicPlayer.MODE_SHUFFLE);
                        alea.setImageResource(R.drawable.aleaclicked);
                    }
                });
            });

            // Repeat button click listener
            order.setOnClickListener(v -> {
                animate(order, () -> {
                    musicPlayer.toggleRepeatOne();

                    if (musicPlayer.isRepeatOne()) {
                        // Enable repeat one
                        order.setImageResource(R.drawable.repeatclicked);
                        // Ensure shuffle is turned off when repeat is on
                        if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                            musicPlayer.setPlayMode(MusicPlayer.MODE_NORMAL);
                            alea.setImageResource(R.drawable.alea);
                        }
                    } else {
                        // Disable repeat
                        order.setImageResource(R.drawable.repeat);
                    }
                });
            });

            heart.setImageResource(isSongInFavorites() ? R.drawable.redheart : R.drawable.heart);

            heart.setOnClickListener(v -> animate(heart, () -> {
                boolean isInFavorites = isSongInFavorites();

                if (isInFavorites) {
                    // Remove from favorites
                    removeFromFavorites();
                    heart.setImageResource(R.drawable.heart);
                    Toast.makeText(new_activity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                } else {
                    // Add to favorites
                    addToFavorites();
                    heart.setImageResource(R.drawable.redheart);
                    Toast.makeText(new_activity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                }
            }));

            // Update UI based on current play mode
            if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                alea.setImageResource(R.drawable.aleaclicked);
            } else {
                alea.setImageResource(R.drawable.alea);
            }

            if (musicPlayer.isRepeatOne()) {
                order.setImageResource(R.drawable.repeatclicked);
            } else {
                order.setImageResource(R.drawable.repeat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing player: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void animate(View view, Runnable onAnimationEnd) {
        AnimatorSet animatorSet = new AnimatorSet();

        // Scale down the image
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f);

        // Play the animations together
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(60); // Duration of the scaling effect

        animatorSet.start(); // Start the animation

        // Once the animation ends, change the image
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Animate back to original size
                view.animate().scaleX(1f).scaleY(1f).setDuration(60).start();

                // Run the provided callback to change the image
                onAnimationEnd.run();
            }
        });
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    private void playSong(item song) {
        if (song == null) {
            Toast.makeText(new_activity.this, "Error: Song data is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MusicPlayer.getInstance().playSong(song, new_activity.this);

            sin.setText(song.getSinger());
            son.setText(song.getSong());

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(song.getPath());
                byte[] art = mmr.getEmbeddedPicture();
                if (art != null) {
                    Glide.with(this)
                            .asBitmap()
                            .load(art)
                            .placeholder(R.drawable.albumcover)
                            .into(musicimg);
                } else {
                    musicimg.setImageResource(R.drawable.albumcover);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art", e);
                musicimg.setImageResource(R.drawable.albumcover);
            } finally {
                mmr.release();
            }

            seekBar.setMax(MusicPlayer.getInstance().getMediaPlayer().getDuration());
            duree.setText(formatTime(MusicPlayer.getInstance().getMediaPlayer().getDuration()));

        } catch (Exception e) {
            Log.e(TAG, "Error playing song", e);
            Toast.makeText(this, "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop the handler when pausing, so music can continue playing
    }
    private void loadCurrentSongUI() throws IOException {
        item currentSong = MusicPlayer.getInstance().getCurrentSongItem();
        if (currentSong != null && currentSong.getPath() != null) {
            son.setText(currentSong.getSong());
            sin.setText(currentSong.getSinger());

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(currentSong.getPath());
                byte[] art = mmr.getEmbeddedPicture();
                if (art != null) {
                    Glide.with(this)
                            .asBitmap()
                            .load(art)
                            .placeholder(R.drawable.albumcover)
                            .into(musicimg);
                } else {
                    musicimg.setImageResource(R.drawable.albumcover);
                }
            } catch (Exception e) {
                musicimg.setImageResource(R.drawable.albumcover);
            } finally {
                mmr.release();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Make sure the seekbar is updating
        handler.post(updateSeekBarRunnable);
        try {
            loadCurrentSongUI();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        musicPlayer.setMiniPlayerVisible(true);

        String currentSong = musicPlayer.getCurrentSong();
        String currentSinger = musicPlayer.getCurrentSinger();
        String currentPath = musicPlayer.getCurrentPath();
        String currentImg = musicPlayer.getCurrentImg();
        int currentPosition = mediaPlayer.getCurrentPosition();

        String sourceActivity = getIntent().getStringExtra("sourceActivity");
        Intent intent;

        if ("DecouvrirActivity".equals(sourceActivity)) {
            intent = new Intent(new_activity.this, DecouvrirActivity.class);
        } else if ("LibraryActivity".equals(sourceActivity)) {
            intent = new Intent(new_activity.this, LibraryActivity.class);
        } else {
            intent = new Intent(new_activity.this, MainActivity.class);
        }

        intent.putExtra("isMiniPlayerVisible", true);
        intent.putExtra("song", currentSong);
        intent.putExtra("singer", currentSinger);
        intent.putExtra("path", currentPath);
        intent.putExtra("img", currentImg);
        intent.putExtra("currentPosition", currentPosition);

        if (musicPlayer.isPlayingFromPlaylist()) {
            intent.putExtra("isFromPlaylist", true);
            intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
            intent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
            intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
        } else if (items != null) {
            intent.putParcelableArrayListExtra("songList", items);
            intent.putExtra("currentSongIndex", currentSongIndex);
        }

        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }


    private boolean isSongInFavorites() {
        if (path == null) return false;

        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals("Favorites")) {
                List<item> favoriteSongs = playlist.getSongs();
                if (favoriteSongs != null) {
                    for (item favSong : favoriteSongs) {
                        // Compare by path which should be unique
                        if (favSong.getPath() != null && favSong.getPath().equals(path)) {
                            return true;
                        }
                    }
                }
                break;
            }
        }
        return false;
    }

    private void addToFavorites() {
        if (path == null) return;

        // Create a song item from the current playing song
        item currentSong = new item(singer, song, path, formatTime(mediaPlayer.getDuration()), img);

        List<Liste> playlists = playlistManager.getPlaylists();

        // Find the Favorites playlist
        Liste favoritesPlaylist = null;
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals("Favorites")) {
                favoritesPlaylist = playlist;
                break;
            }
        }

        // If Favorites playlist doesn't exist, create it
        if (favoritesPlaylist == null) {
            favoritesPlaylist = new Liste("Favorites", "Your favorite songs", R.drawable.albumcover);
            playlists.add(favoritesPlaylist);
        }

        // Add the song to favorites if it's not already there
        boolean alreadyInFavorites = false;
        List<item> favoriteSongs = favoritesPlaylist.getSongs();
        if (favoriteSongs != null) {
            for (item favSong : favoriteSongs) {
                if (favSong.getPath() != null && favSong.getPath().equals(path)) {
                    alreadyInFavorites = true;
                    break;
                }
            }
        }

        if (!alreadyInFavorites) {
            favoritesPlaylist.addSong(currentSong);
            playlistManager.savePlaylists(playlists);
        }
    }

    private void removeFromFavorites() {
        if (path == null) return;

        List<Liste> playlists = playlistManager.getPlaylists();

        // Find the Favorites playlist
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals("Favorites")) {
                List<item> favoriteSongs = playlist.getSongs();
                if (favoriteSongs != null) {
                    for (int i = 0; i < favoriteSongs.size(); i++) {
                        item favSong = favoriteSongs.get(i);
                        if (favSong.getPath() != null && favSong.getPath().equals(path)) {
                            favoriteSongs.remove(i);
                            playlistManager.savePlaylists(playlists);
                            return;
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Don't release or stop the MediaPlayer here
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ensure mini-player is marked as visible when this activity starts
        musicPlayer.setMiniPlayerVisible(true);
    }
}
