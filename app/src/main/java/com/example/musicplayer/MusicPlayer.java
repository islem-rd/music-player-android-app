package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * Singleton class to manage music playback across the app
 */
public class MusicPlayer {
    private static final String TAG = "MusicPlayer";
    public static final int MODE_NORMAL = 0;
    public static final int MODE_SHUFFLE = 1;
    public static final int MODE_REPEAT_ONE = 2;

    private static MusicPlayer instance;

    private MediaPlayer mediaPlayer;
    private String currentPath;
    private String currentSong;
    private String currentSinger;
    private String currentImg;
    private boolean isPrepared = false;
    private boolean isMiniPlayerVisible = false;
    private boolean isPlayingFromPlaylist = false;
    private String currentPlaylistName = null;
    private ArrayList<item> currentPlaylistSongs = null;
    private ArrayList<item> allSongs = null;
    private int currentSongIndex = 0;
    private int playMode = MODE_NORMAL;
    private boolean repeatOne = false;
    private Random random = new Random();
    private item currentSongItem;



    private MusicPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    public static synchronized MusicPlayer getInstance() {
        if (instance == null) {
            instance = new MusicPlayer();
        }
        return instance;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        instance = null;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public boolean isMiniPlayerVisible() {
        return isMiniPlayerVisible;
    }

    public void setMiniPlayerVisible(boolean miniPlayerVisible) {
        this.isMiniPlayerVisible = miniPlayerVisible;
    }

    public boolean isPlayingFromPlaylist() {
        return isPlayingFromPlaylist;
    }

    public void setPlayingFromPlaylist(boolean playingFromPlaylist) {
        this.isPlayingFromPlaylist = playingFromPlaylist;
    }

    public String getCurrentPlaylistName() {
        return currentPlaylistName;
    }

    public void setCurrentPlaylistName(String currentPlaylistName) {
        this.currentPlaylistName = currentPlaylistName;
    }

    public ArrayList<item> getCurrentPlaylistSongs() {
        return currentPlaylistSongs;
    }

    public void setCurrentPlaylistSongs(ArrayList<item> currentPlaylistSongs) {
        this.currentPlaylistSongs = currentPlaylistSongs;
    }
    private OnSongChangedListener onSongChangedListener;

    public interface OnSongChangedListener {
        void onSongChanged(String song, String singer, String img, String path);
    }

    public void setOnSongChangedListener(OnSongChangedListener listener) {
        this.onSongChangedListener = listener;
    }

    public void setCurrentSongItem(item song) {
        if (song != null) {
            this.currentSongItem = song;
            this.currentSong = song.getSong();
            this.currentSinger = song.getSinger();
            this.currentImg = song.getImg();
            this.currentPath = song.getPath();

            // Notify any listeners that the current song has changed
            if (onSongChangedListener != null) {
                onSongChangedListener.onSongChanged(currentSong, currentSinger, currentImg, currentPath);
            }
        }
    }
    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public void setCurrentSongIndex(int index) {
        int size = isPlayingFromPlaylist ? currentPlaylistSongs.size() : allSongs.size();
        if (index >= 0 && index < size) {
            currentSongIndex = index;
        } else {
            Log.w(TAG, "Invalid song index: " + index);
        }
    }


    public int getPlayMode() {
        return playMode;
    }

    public void setPlayMode(int playMode) {
        this.playMode = playMode;
    }

    public boolean isRepeatOne() {
        return repeatOne;
    }

    public void setRepeatOne(boolean repeatOne) {
        this.repeatOne = repeatOne;
    }

    public void toggleRepeatOne() {
        this.repeatOne = !this.repeatOne;
    }

    public String getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(String currentSong) {
        this.currentSong = currentSong;
    }

    public String getCurrentSinger() {
        return currentSinger;
    }

    public void setCurrentSinger(String currentSinger) {
        this.currentSinger = currentSinger;
    }

    public String getCurrentImg() {
        return currentImg;
    }

    public void setCurrentImg(String currentImg) {
        this.currentImg = currentImg;
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    public void setPrepared(boolean prepared) {
        this.isPrepared = prepared;
    }

    public ArrayList<item> getAllSongs() {
        return allSongs;
    }

    public void setAllSongs(ArrayList<item> allSongs) {
        this.allSongs = allSongs;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * Update current song information
     * @param song The song title
     * @param singer The artist name
     * @param img The image path
     * @param path The file path
     */
    public void setCurrentSongInfo(String song, String singer, String img, String path) {
        this.currentSong = song;
        this.currentSinger = singer;
        this.currentImg = img;
        this.currentPath = path;
    }

    /**
     * Update current song information from an item
     * @param songItem The song item
     */
    public void updateFromItem(item songItem) {
        if (songItem != null) {
            setCurrentSongInfo(
                    songItem.getSong(),
                    songItem.getSinger(),
                    songItem.getImg(),
                    songItem.getPath()
            );
        }
    }

    /**
     * Cycle through play modes (Normal -> Shuffle -> Repeat One -> Normal)
     */
    public void cyclePlayMode() {
        playMode = (playMode + 1) % 3;

        // If switching to repeat one mode, ensure it's set
        if (playMode == MODE_REPEAT_ONE) {
            repeatOne = true;
        } else {
            repeatOne = false;
        }

        Log.d(TAG, "Play mode changed to: " + getPlayModeName());
    }

    /**
     * Get the name of the current play mode
     * @return The play mode name
     */
    public String getPlayModeName() {
        switch (playMode) {
            case MODE_SHUFFLE:
                return "Shuffle";
            case MODE_REPEAT_ONE:
                return "Repeat One";
            case MODE_NORMAL:
            default:
                return "Normal";
        }
    }

    /**
     * Find the index of a song in the current playlist
     * @param path The path of the song to find
     * @return The index of the song, or -1 if not found
     */
    public int findSongIndexInPlaylist(String path) {
        if (currentPlaylistSongs == null || path == null) {
            return -1;
        }

        for (int i = 0; i < currentPlaylistSongs.size(); i++) {
            if (path.equals(currentPlaylistSongs.get(i).getPath())) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Get the next song based on current play mode
     * @return The next song to play
     */
    public item getNextSong() {
        if (repeatOne) {
            // If repeat one is enabled, return the current song
            if (isPlayingFromPlaylist && currentPlaylistSongs != null && !currentPlaylistSongs.isEmpty()) {
                return currentPlaylistSongs.get(currentSongIndex);
            } else if (allSongs != null && !allSongs.isEmpty()) {
                return allSongs.get(currentSongIndex);
            }
            return null;
        }

        if (isPlayingFromPlaylist && currentPlaylistSongs != null && !currentPlaylistSongs.isEmpty()) {
            int nextIndex;

            if (playMode == MODE_SHUFFLE) {
                // Play random song from playlist
                nextIndex = random.nextInt(currentPlaylistSongs.size());
            } else {
                // Play next song in order
                nextIndex = (currentSongIndex + 1) % currentPlaylistSongs.size();
            }

            currentSongIndex = nextIndex;
            return currentPlaylistSongs.get(nextIndex);
        } else if (allSongs != null && !allSongs.isEmpty()) {
            int nextIndex;

            if (playMode == MODE_SHUFFLE) {
                // Play random song from all songs
                nextIndex = random.nextInt(allSongs.size());
            } else {
                // Play next song in order
                nextIndex = (currentSongIndex + 1) % allSongs.size();
            }

            currentSongIndex = nextIndex;
            return allSongs.get(nextIndex);
        }

        return null;
    }

    /**
     * Get the previous song based on current play mode
     * @return The previous song to play
     */
    public item getPreviousSong() {
        if (repeatOne) {
            // If repeat one is enabled, return the current song
            if (isPlayingFromPlaylist && currentPlaylistSongs != null && !currentPlaylistSongs.isEmpty()) {
                return currentPlaylistSongs.get(currentSongIndex);
            } else if (allSongs != null && !allSongs.isEmpty()) {
                return allSongs.get(currentSongIndex);
            }
            return null;
        }

        if (isPlayingFromPlaylist && currentPlaylistSongs != null && !currentPlaylistSongs.isEmpty()) {
            int prevIndex;

            if (playMode == MODE_SHUFFLE) {
                // Play random song from playlist
                prevIndex = random.nextInt(currentPlaylistSongs.size());
            } else {
                // Play previous song in order
                prevIndex = (currentSongIndex - 1 + currentPlaylistSongs.size()) % currentPlaylistSongs.size();
            }

            currentSongIndex = prevIndex;
            return currentPlaylistSongs.get(prevIndex);
        } else if (allSongs != null && !allSongs.isEmpty()) {
            int prevIndex;

            if (playMode == MODE_SHUFFLE) {
                // Play random song from all songs
                prevIndex = random.nextInt(allSongs.size());
            } else {
                // Play previous song in order
                prevIndex = (currentSongIndex - 1 + allSongs.size()) % allSongs.size();
            }

            currentSongIndex = prevIndex;
            return allSongs.get(prevIndex);
        }

        return null;
    }

    /**
     * Get the current song item
     * @return The current song item
     */
    public item getCurrentSongItem() {
        if (isPlayingFromPlaylist && currentPlaylistSongs != null && !currentPlaylistSongs.isEmpty()) {
            if (currentSongIndex >= 0 && currentSongIndex < currentPlaylistSongs.size()) {
                return currentPlaylistSongs.get(currentSongIndex);
            }
        } else if (allSongs != null && !allSongs.isEmpty()) {
            if (currentSongIndex >= 0 && currentSongIndex < allSongs.size()) {
                return allSongs.get(currentSongIndex);
            }
        }
        return null;
    }
    public void setCurrentSongFromIndex() {
        if (isPlayingFromPlaylist && currentPlaylistSongs != null) {
            if (currentSongIndex >= 0 && currentSongIndex < currentPlaylistSongs.size()) {
                updateFromItem(currentPlaylistSongs.get(currentSongIndex));
            }
        } else if (allSongs != null) {
            if (currentSongIndex >= 0 && currentSongIndex < allSongs.size()) {
                updateFromItem(allSongs.get(currentSongIndex));
            }
        }
    }

    public void syncIndexWithCurrentSong() {
        if (currentPath == null) return;

        if (isPlayingFromPlaylist && currentPlaylistSongs != null) {
            for (int i = 0; i < currentPlaylistSongs.size(); i++) {
                if (currentPath.equals(currentPlaylistSongs.get(i).getPath())) {
                    currentSongIndex = i;
                    return;
                }
            }
        } else if (allSongs != null) {
            for (int i = 0; i < allSongs.size(); i++) {
                if (currentPath.equals(allSongs.get(i).getPath())) {
                    currentSongIndex = i;
                    return;
                }
            }
        }
    }
    private OnPlayerStateChangedListener onPlayerStateChangedListener;

    public interface OnPlayerStateChangedListener {
        void onPlayerStateChanged(boolean isPlaying);
    }

    public void setOnPlayerStateChangedListener(OnPlayerStateChangedListener listener) {
        this.onPlayerStateChangedListener = listener;
    }

    public void notifyPlayerStateChanged() {
        if (onPlayerStateChangedListener != null) {
            onPlayerStateChangedListener.onPlayerStateChanged(isPlaying());
        }
    }



    public void playSong(item song, Context context) {
        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction("ACTION_START_FOREGROUND");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d("MusicPlayer", "Service started successfully");
        } catch (Exception e) {
            Log.e("MusicPlayer", "Erreur en démarrant MusicService", e);
        }


        if (song == null || song.getPath() == null) {
            Toast.makeText(context, "Cannot play this song", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            setCurrentSongItem(song);
            // Reset the media player
            mediaPlayer.reset();

            // Set data source
            mediaPlayer.setDataSource(context, Uri.parse(song.getPath()));

            // Update current song info
            updateFromItem(song);

            // Set prepared state
            setPrepared(false);

            // Prepare async
            mediaPlayer.prepareAsync();

            // Set listeners
            mediaPlayer.setOnPreparedListener(mp -> {
                setPrepared(true);
                mp.start();
                setMiniPlayerVisible(true);
                notifyPlayerStateChanged();
                // Add to history
                new HistoryManager(context).addToHistory(song);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                // Handle song completion
                item nextSong = getNextSong();
                if (nextSong != null) {
                    playSong(nextSong, context);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error playing song", e);
            Toast.makeText(context, "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPlayerStateChanged();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            notifyPlayerStateChanged();
        }
    }
}
