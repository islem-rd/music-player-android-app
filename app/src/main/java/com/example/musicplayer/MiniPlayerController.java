package com.example.musicplayer;


import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.io.IOException;

/**
 * A reusable controller that handles MiniPlayer logic across all activities
 */
public class MiniPlayerController {

    private static final String TAG = "MiniPlayerController";
    private final MusicPlayer musicPlayer;
    private CardView miniPlayer;
    private ImageView albumArt;
    private TextView songTitle;
    private TextView artistName;
    private ImageView playPauseButton;
    private Context context;

    public MiniPlayerController(Context context) {
        this.context = context;
        this.musicPlayer = MusicPlayer.getInstance();
    }

    public void bind(CardView miniPlayer, ImageView albumArt, TextView songTitle,
                     TextView artistName, ImageView playPauseButton) {
        this.miniPlayer = miniPlayer;
        this.albumArt = albumArt;
        this.songTitle = songTitle;
        this.artistName = artistName;
        this.playPauseButton = playPauseButton;

        setupListeners();
    }

    private void setupListeners() {
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
                    Log.e(TAG, "Play/Pause toggle failed", e);
                    Toast.makeText(context, "Error toggling playback", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (miniPlayer != null) {
            miniPlayer.setOnClickListener(v -> {
                Activity activity = (Activity) context;
                MiniPlayerManager.getInstance(context).openFullPlayer(activity);
            });
        }
    }

    public void update() throws IOException {
        if (!musicPlayer.isMiniPlayerVisible()) {
            if (miniPlayer != null) miniPlayer.setVisibility(View.GONE);
            return;
        }

        item currentItem = musicPlayer.getCurrentSongItem();
        if (currentItem == null) return;

        if (miniPlayer != null) miniPlayer.setVisibility(View.VISIBLE);
        if (songTitle != null) songTitle.setText(currentItem.getSong());
        if (artistName != null) artistName.setText(currentItem.getSinger());

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(currentItem.getPath());
            byte[] art = mmr.getEmbeddedPicture();
            if (art != null && albumArt != null) {
                Glide.with(context)
                        .asBitmap()
                        .load(art)
                        .placeholder(R.drawable.albumcover)
                        .error(R.drawable.albumcover)
                        .into(albumArt);
            } else if (albumArt != null) {
                albumArt.setImageResource(R.drawable.albumcover);
            }
        } catch (Exception e) {
            if (albumArt != null) albumArt.setImageResource(R.drawable.albumcover);
            Log.e(TAG, "Album art load failed", e);
        } finally {
            mmr.release();
        }


        if (playPauseButton != null) {
            MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                playPauseButton.setImageResource(R.drawable.pause);
            } else {
                playPauseButton.setImageResource(R.drawable.play);
            }
        }
    }
}
