package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends BaseActivity implements PlaylistAdapter.OnSongActionListener {

    private static final String TAG = "PlaylistActivity";

    private RecyclerView songsRecyclerView;
    private PlaylistAdapter songAdapter;
    private List<item> playlistSongs;
    private FloatingActionButton btnPlay;
    private ImageButton btnBack, btnOptions;
    private TextView tvPlaylistName, tvPlaylistDescription, tvSongCount, tvEmptyPlaylist;
    private ImageView imgPlaylistCover;
    private PlaylistManager playlistManager;
    private Liste currentPlaylist;
    private String playlistName;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_layout);

        playlistManager = new PlaylistManager(this);
        initializeViews();
        setupImagePicker();

        playlistName = getIntent().getStringExtra("PLAYLIST_NAME");
        if (playlistName == null) {
            Toast.makeText(this, "Error loading playlist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPlaylistData();
        setupClickListeners();

        // Setup mini player fragment
        setupMiniPlayerFragment();
    }

    private void initializeViews() {
        songsRecyclerView = findViewById(R.id.recyclerViewSongs);
        btnPlay = findViewById(R.id.btnPlay);
        btnBack = findViewById(R.id.btnBack);
        btnOptions = findViewById(R.id.btnOptions);
        tvPlaylistName = findViewById(R.id.tvPlaylistTitle);
        tvPlaylistDescription = findViewById(R.id.tvPlaylistDescription);
        tvSongCount = findViewById(R.id.tvSongCount);
        tvEmptyPlaylist = findViewById(R.id.tvEmptyPlaylist);
        imgPlaylistCover = findViewById(R.id.imgPlaylistCover);

        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        getContentResolver().takePersistableUriPermission(
                                                selectedImageUri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        );
                                    } catch (SecurityException e) {
                                        Log.e(TAG, "Failed to take persistent permission: " + e.getMessage());
                                    }
                                }
                                imgPlaylistCover.setImageURI(selectedImageUri);
                                currentPlaylist.setCoverImageUri(selectedImageUri.toString());
                                playlistManager.updatePlaylist(playlistName, currentPlaylist);
                                Toast.makeText(this, "Cover image updated", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e(TAG, "Error setting cover image: " + e.getMessage());
                                Toast.makeText(this, "Error setting cover image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        imagePickerLauncher.launch(intent);
    }

    private void loadPlaylistData() {
        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals(playlistName)) {
                currentPlaylist = playlist;
                break;
            }
        }

        if (currentPlaylist == null) {
            currentPlaylist = new Liste(playlistName, "No description", R.drawable.albumcover);
            playlistManager.addPlaylist(currentPlaylist);
        }

        tvPlaylistName.setText(currentPlaylist.getPlaylisttitle());
        tvPlaylistDescription.setText(currentPlaylist.getDescriptplaylist());

        if (currentPlaylist.getCoverImageUri() != null && !currentPlaylist.getCoverImageUri().isEmpty()) {
            imgPlaylistCover.setImageURI(Uri.parse(currentPlaylist.getCoverImageUri()));
            if (imgPlaylistCover.getDrawable() == null) {
                imgPlaylistCover.setImageResource(currentPlaylist.getCoverResourceId());
            }
        } else {
            imgPlaylistCover.setImageResource(currentPlaylist.getCoverResourceId());
        }

        playlistSongs = currentPlaylist.getSongs();
        if (playlistSongs == null) {
            playlistSongs = new ArrayList<>();
        }

        for (item track : playlistSongs) {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(track.getPath());
                byte[] art = mmr.getEmbeddedPicture();
                track.setImg(null);
                mmr.release();
            } catch (Exception e) {
                Log.e(TAG, "Metadata extraction failed for song: " + track.getSong(), e);
            }
        }

        updateSongCount();
        songAdapter = new PlaylistAdapter(playlistSongs, this, this);
        songsRecyclerView.setAdapter(songAdapter);
        updateEmptyState();
    }

    private void updateSongCount() {
        int count = playlistSongs.size();
        tvSongCount.setText(count + (count == 1 ? " song" : " songs"));
    }

    private void updateEmptyState() {
        if (playlistSongs.isEmpty()) {
            tvEmptyPlaylist.setVisibility(View.VISIBLE);
            songsRecyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyPlaylist.setVisibility(View.GONE);
            songsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnOptions.setOnClickListener(v -> showPlaylistOptionsMenu());

        btnPlay.setOnClickListener(v -> {
            if (!playlistSongs.isEmpty()) {
                int firstIndex = 0;
                item firstSong = playlistSongs.get(firstIndex);

                // Play the first song in the playlist
                playPlaylistSong(firstIndex);
            } else {
                Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSongClick(int position) {
        // Play the selected song from the playlist
        playPlaylistSong(position);
    }

    /**
     * Play a song from the playlist and open the full player
     */
    private void playPlaylistSong(int position) {
        if (position < 0 || position >= playlistSongs.size()) {
            Toast.makeText(this, "Invalid song position", Toast.LENGTH_SHORT).show();
            return;
        }

        item songToPlay = playlistSongs.get(position);

        // Set up the MusicPlayer with playlist information
        musicPlayer.setPlayingFromPlaylist(true);
        musicPlayer.setCurrentPlaylistName(currentPlaylist.getPlaylisttitle());
        musicPlayer.setCurrentPlaylistSongs(new ArrayList<>(playlistSongs));
        musicPlayer.setCurrentSongIndex(position);

        // Play the song using the MusicPlayer's playSong method
        musicPlayer.playSong(songToPlay, this);

        // Open the full player activity
        Intent intent = new Intent(this, new_activity.class);
        intent.putExtra("song", songToPlay.getSong());
        intent.putExtra("singer", songToPlay.getSinger());
        intent.putExtra("path", songToPlay.getPath());
        intent.putExtra("img", songToPlay.getImg());
        intent.putExtra("isFromPlaylist", true);
        intent.putExtra("playlistName", currentPlaylist.getPlaylisttitle());
        intent.putExtra("currentPosition", musicPlayer.getMediaPlayer().getCurrentPosition());
        intent.putParcelableArrayListExtra("songList", new ArrayList<>(playlistSongs));
        intent.putExtra("currentSongIndex", position);
        intent.putExtra("sourceActivity", "PlaylistActivity");

        // Use shared element transition for mini player to full player if mini player is visible
        MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_player_container);

        if (miniPlayerFragment != null && miniPlayerFragment.getView() != null && musicPlayer.isMiniPlayerVisible()) {
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

    private void showPlaylistOptionsMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnOptions);
        popupMenu.inflate(R.menu.playlist_options_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_rename_playlist) {
                showRenamePlaylistDialog();
                return true;
            } else if (itemId == R.id.action_change_description) {
                showChangeDescriptionDialog();
                return true;
            } else if (itemId == R.id.action_change_cover) {
                openImagePicker();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void showRenamePlaylistDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        EditText etInput = dialogView.findViewById(R.id.etInput);
        etInput.setText(currentPlaylist.getPlaylisttitle());
        etInput.setHint("Playlist name");

        new AlertDialog.Builder(this)
                .setTitle("Rename Playlist")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        String oldName = currentPlaylist.getPlaylisttitle();
                        currentPlaylist.setPlaylisttitle(newName);
                        tvPlaylistName.setText(newName);
                        playlistManager.updatePlaylist(oldName, currentPlaylist);
                        playlistName = newName;
                        Toast.makeText(this, "Playlist renamed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeDescriptionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        EditText etInput = dialogView.findViewById(R.id.etInput);
        etInput.setText(currentPlaylist.getDescriptplaylist());
        etInput.setHint("Playlist description");

        new AlertDialog.Builder(this)
                .setTitle("Change Description")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newDescription = etInput.getText().toString().trim();
                    currentPlaylist.setDescriptplaylist(newDescription);
                    tvPlaylistDescription.setText(newDescription);
                    playlistManager.updatePlaylist(currentPlaylist.getPlaylisttitle(), currentPlaylist);
                    Toast.makeText(this, "Description updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSongRemove(int position) {
        if (position >= 0 && position < playlistSongs.size()) {
            playlistSongs.remove(position);
            songAdapter.notifyItemRemoved(position);
            updateSongCount();
            updateEmptyState();
            currentPlaylist.setSongs(playlistSongs);
            playlistManager.updatePlaylist(currentPlaylist.getPlaylisttitle(), currentPlaylist);
            Toast.makeText(this, "Song removed from playlist", Toast.LENGTH_SHORT).show();
        }
    }
}
