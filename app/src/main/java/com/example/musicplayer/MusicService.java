package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MusicService extends Service {

    public static final String CHANNEL_ID = "MusicPlayerChannel";
    private MusicPlayer musicPlayer;
    private MediaPlayer mediaPlayer;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        musicPlayer = MusicPlayer.getInstance();
        mediaPlayer = musicPlayer.getMediaPlayer();
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannel();

        // Listen for player state changes to update notification
        musicPlayer.setOnSongChangedListener((song, singer, img, path) -> {
            showNotification();
        });

        musicPlayer.setOnPlayerStateChangedListener(isPlaying -> {
            showNotification();
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MusicService", "onStartCommand called");

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_START_FOREGROUND":
                    showNotification();
                    break;
                case "ACTION_PLAY":
                    musicPlayer.resume();
                    showNotification();
                    break;
                case "ACTION_PAUSE":
                    musicPlayer.pause();
                    showNotification();
                    break;
                case "ACTION_NEXT":
                    playNextSong();
                    showNotification();
                    break;
                case "ACTION_PREVIOUS":
                    playPreviousSong();
                    showNotification();
                    break;
            }
        }

        return START_STICKY;
    }

    private void showNotification() {
        Log.d("MusicService", "showNotification called");

        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.setAction("ACTION_PLAY");
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, MusicService.class);
        pauseIntent.setAction("ACTION_PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MusicService.class);
        prevIntent.setAction("ACTION_PREVIOUS");
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Bitmap albumArt = getAlbumArtBitmap();
        boolean isPlaying = musicPlayer.isPlaying();

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        PendingIntent playPausePendingIntent = isPlaying ? pausePendingIntent : playPendingIntent;
        String playPauseLabel = isPlaying ? "Pause" : "Play";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.albumcover)
                .setLargeIcon(albumArt)
                .setContentTitle(musicPlayer.getCurrentSong())
                .setContentText(musicPlayer.getCurrentSinger())
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPendingIntent)
                .addAction(playPauseIcon, playPauseLabel, playPausePendingIntent)
                .addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(isPlaying);

        startForeground(1, builder.build());
    }

    private Bitmap getAlbumArtBitmap() {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(musicPlayer.getCurrentPath());
            byte[] art = retriever.getEmbeddedPicture();
            retriever.release();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                return Bitmap.createScaledBitmap(bitmap, 512, 512, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.albumcover);
        return Bitmap.createScaledBitmap(bitmap, 512, 512, false);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void playNextSong() {
        item next = musicPlayer.getNextSong();
        if (next != null) {
            try {
                musicPlayer.playSong(next, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void playPreviousSong() {
        item prev = musicPlayer.getPreviousSong();
        if (prev != null) {
            try {
                musicPlayer.playSong(prev, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}