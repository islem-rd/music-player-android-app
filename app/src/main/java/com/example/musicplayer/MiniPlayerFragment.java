package com.example.musicplayer;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.IOException;

public class MiniPlayerFragment extends Fragment {
    private static final String TAG = "MiniPlayerFragment";

    private CardView miniPlayerView;
    private ImageView albumArtView;
    private TextView songTitleView;
    private TextView artistNameView;
    private ImageView playPauseButton;
    private ImageView nextButton;
    private ImageView prevButton;
    private MusicPlayer musicPlayer;

    public interface MiniPlayerListener {
        void onMiniPlayerClicked();
    }

    private MiniPlayerListener listener;

    public void setMiniPlayerListener(MiniPlayerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        musicPlayer = MusicPlayer.getInstance();

        // Add completion listener to handle song transitions
        MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            try {
                if (musicPlayer.isPlayingFromPlaylist()) {
                    item nextSong = musicPlayer.getNextSong();
                    if (nextSong != null) {
                        playSong(nextSong);
                    }
                } else if (musicPlayer.getAllSongs() != null && !musicPlayer.getAllSongs().isEmpty()) {
                    if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                        int size = musicPlayer.getAllSongs().size();
                        int randomIndex = (int) (Math.random() * size);
                        musicPlayer.setCurrentSongIndex(randomIndex);
                        playSong(musicPlayer.getAllSongs().get(randomIndex));
                    } else {
                        int currentIndex = musicPlayer.getCurrentSongIndex();
                        int nextIndex = (currentIndex + 1) % musicPlayer.getAllSongs().size();
                        musicPlayer.setCurrentSongIndex(nextIndex);
                        playSong(musicPlayer.getAllSongs().get(nextIndex));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onCompletion", e);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        miniPlayerView = view.findViewById(R.id.mini_player);
        albumArtView = view.findViewById(R.id.album_art);
        songTitleView = view.findViewById(R.id.song_title);
        artistNameView = view.findViewById(R.id.artist_name);
        playPauseButton = view.findViewById(R.id.play_pause_button);
        nextButton = view.findViewById(R.id.next);
        prevButton = view.findViewById(R.id.before);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            miniPlayerView.setTransitionName("mini_player_transition");
            albumArtView.setTransitionName("album_art_transition");
            songTitleView.setTransitionName("song_title_transition");
            artistNameView.setTransitionName("artist_name_transition");
        }

        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMiniPlayer();

        // Set up listener for player state changes
        musicPlayer.setOnPlayerStateChangedListener(isPlaying -> {
            if (isPlaying) {
                playPauseButton.setImageResource(R.drawable.pause);
            } else {
                playPauseButton.setImageResource(R.drawable.play);
            }
        });
    }

    private void setupClickListeners() {
        miniPlayerView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMiniPlayerClicked();
            } else {
                openFullPlayer();
            }
        });

        playPauseButton.setOnClickListener(v -> {
            try {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.pause();
                } else {
                    musicPlayer.resume();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling play/pause", e);
                Toast.makeText(getContext(), "Error playing media", Toast.LENGTH_SHORT).show();
            }
        });

        nextButton.setOnClickListener(v -> {
            try {
                if (musicPlayer.isPlayingFromPlaylist()) {
                    item nextSong = musicPlayer.getNextSong();
                    if (nextSong != null) {
                        playSong(nextSong);
                    }
                } else if (musicPlayer.getAllSongs() != null && !musicPlayer.getAllSongs().isEmpty()) {
                    if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                        int size = musicPlayer.getAllSongs().size();
                        int randomIndex = (int) (Math.random() * size);
                        musicPlayer.setCurrentSongIndex(randomIndex);
                        playSong(musicPlayer.getAllSongs().get(randomIndex));
                    } else {
                        int currentIndex = musicPlayer.getCurrentSongIndex();
                        int nextIndex = (currentIndex + 1) % musicPlayer.getAllSongs().size();
                        musicPlayer.setCurrentSongIndex(nextIndex);
                        playSong(musicPlayer.getAllSongs().get(nextIndex));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing next song", e);
                Toast.makeText(getContext(), "Error playing next song", Toast.LENGTH_SHORT).show();
            }
        });

        prevButton.setOnClickListener(v -> {
            try {
                MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();

                if (musicPlayer.isPlayingFromPlaylist()) {
                    item prevSong = musicPlayer.getPreviousSong();
                    if (prevSong != null) {
                        playSong(prevSong);
                    }
                } else if (musicPlayer.getAllSongs() != null && !musicPlayer.getAllSongs().isEmpty()) {
                    if (mediaPlayer.getCurrentPosition() < 3000) {
                        if (musicPlayer.getPlayMode() == MusicPlayer.MODE_SHUFFLE) {
                            int size = musicPlayer.getAllSongs().size();
                            int randomIndex = (int) (Math.random() * size);
                            musicPlayer.setCurrentSongIndex(randomIndex);
                            playSong(musicPlayer.getAllSongs().get(randomIndex));
                        } else {
                            int currentIndex = musicPlayer.getCurrentSongIndex();
                            int prevIndex = (currentIndex - 1 + musicPlayer.getAllSongs().size()) % musicPlayer.getAllSongs().size();
                            musicPlayer.setCurrentSongIndex(prevIndex);
                            playSong(musicPlayer.getAllSongs().get(prevIndex));
                        }
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing previous song", e);
                Toast.makeText(getContext(), "Error playing previous song", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateMiniPlayer() {
        if (!musicPlayer.isMiniPlayerVisible()) {
            miniPlayerView.setVisibility(View.GONE);
            return;
        }

        try {
            miniPlayerView.setVisibility(View.VISIBLE);

            String currentSong = musicPlayer.getCurrentSong();
            String currentSinger = musicPlayer.getCurrentSinger();
            String currentPath = musicPlayer.getCurrentPath();

            if (currentSong != null && currentSinger != null) {
                songTitleView.setText(currentSong);
                artistNameView.setText(currentSinger);
            }

            if (currentPath != null) {
                loadAlbumArt(currentPath);
            }

            // Update play/pause button based on current state
            playPauseButton.setImageResource(musicPlayer.isPlaying() ? R.drawable.pause : R.drawable.play);
        } catch (Exception e) {
            Log.e(TAG, "Error updating mini player", e);
        }
    }

    private void loadAlbumArt(String path) throws IOException {
        if (path == null || getContext() == null) return;

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(path);
            byte[] art = mmr.getEmbeddedPicture();
            if (art != null) {
                Glide.with(getContext())
                        .asBitmap()
                        .load(art)
                        .placeholder(R.drawable.albumcover)
                        .error(R.drawable.albumcover)
                        .into(albumArtView);
            } else {
                albumArtView.setImageResource(R.drawable.albumcover);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading album art", e);
            albumArtView.setImageResource(R.drawable.albumcover);
        } finally {
            mmr.release();
        }
    }

    private void openFullPlayer() {
        if (getActivity() == null) return;

        try {
            Intent intent = new Intent(getActivity(), new_activity.class);
            intent.putExtra("song", musicPlayer.getCurrentSong());
            intent.putExtra("singer", musicPlayer.getCurrentSinger());
            intent.putExtra("path", musicPlayer.getCurrentPath());
            intent.putExtra("img", musicPlayer.getCurrentImg());
            intent.putExtra("currentPosition", musicPlayer.getMediaPlayer().getCurrentPosition());
            intent.putExtra("isResuming", true);
            intent.putExtra("sourceActivity", getActivity().getClass().getSimpleName());

            if (musicPlayer.isPlayingFromPlaylist()) {
                intent.putExtra("isFromPlaylist", true);
                intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
                intent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
                intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
            } else if (musicPlayer.getAllSongs() != null) {
                intent.putParcelableArrayListExtra("songList", musicPlayer.getAllSongs());
                intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                        getActivity(),
                        intent,
                        miniPlayerView,
                        albumArtView,
                        songTitleView,
                        artistNameView
                );
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening full player", e);
            Toast.makeText(getContext(), "Error opening player", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSong(item song) {
        if (song == null || song.getPath() == null || getContext() == null) {
            Toast.makeText(getContext(), "Cannot play this song", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            musicPlayer.playSong(song, getContext());
            updateMiniPlayer();
        } catch (Exception e) {
            Log.e(TAG, "Error playing song", e);
            Toast.makeText(getContext(), "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }

    public void showMiniPlayer() {
        musicPlayer.setMiniPlayerVisible(true);
        updateMiniPlayer();
    }

    public void hideMiniPlayer() {
        musicPlayer.setMiniPlayerVisible(false);
        miniPlayerView.setVisibility(View.GONE);
    }
}