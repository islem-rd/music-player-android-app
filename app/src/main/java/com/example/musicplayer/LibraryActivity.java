package com.example.musicplayer;

import static android.content.ContentValues.TAG;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends BaseActivity {
    private CardView cardSearch;
    private EditText etSearch;
    private ImageView imgSearch;
    private ImageButton btnClear;
    private CardView cardSuggestions;
    private RecyclerView rvSuggestions, rvplaylists;
    private SuggestionAdapter adapter;
    private ListAdapter adapterplaylist;
    private List<String> playlistsl = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;
    private ArrayList<item> items;
    private PlaylistManager playlistManager;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        // Enable hardware acceleration for smoother transitions
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        setContentView(R.layout.library_layout);

        // Setup transition names for shared elements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityTransitionHelper.setupBackNavigation(this);
        }

        playlistManager = new PlaylistManager(this);
        musicPlayer = MusicPlayer.getInstance();

        cardSearch = findViewById(R.id.cardSearch);
        etSearch = findViewById(R.id.etSearch);
        imgSearch = findViewById(R.id.imgSearch);
        btnClear = findViewById(R.id.btnClear);
        rvplaylists = findViewById(R.id.playlists);
        cardSuggestions = findViewById(R.id.cardSuggestions);
        rvSuggestions = findViewById(R.id.rvSuggestions);
        bottomNavigationView = findViewById(R.id.bottom_navigation1);
        bottomNavigationView.setSelectedItemId(R.id.nav_library);

        // Setup mini player fragment
        setupMiniPlayerFragment();

        // Get intent data
        items = getIntent().getParcelableArrayListExtra("songList");

        // Check if we're coming from a playlist
        boolean isFromPlaylist = getIntent().getBooleanExtra("isFromPlaylist", false);
        if (isFromPlaylist) {
            String playlistName = getIntent().getStringExtra("playlistName");
            ArrayList<item> playlistSongs = getIntent().getParcelableArrayListExtra("songList");

            // Update the MusicPlayer singleton
            if (playlistName != null && playlistSongs != null) {
                musicPlayer.setPlayingFromPlaylist(true);
                musicPlayer.setCurrentPlaylistName(playlistName);
                musicPlayer.setCurrentPlaylistSongs(playlistSongs);
            }
        }

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                // Check which item is clicked
                if (item.getItemId() == R.id.nav_home) {
                    // Handle home item click
                    Intent intent = new Intent(LibraryActivity.this, MainActivity.class);
                    if (musicPlayer.isMiniPlayerVisible()) {
                        intent.putExtra("isMiniPlayerVisible", true);
                        intent.putExtra("song", musicPlayer.getCurrentSong());
                        intent.putExtra("singer", musicPlayer.getCurrentSinger());
                        intent.putExtra("img", musicPlayer.getCurrentImg());
                        intent.putExtra("path", musicPlayer.getCurrentPath());

                        // Pass playlist info if playing from playlist
                        if (musicPlayer.isPlayingFromPlaylist()) {
                            intent.putExtra("isFromPlaylist", true);
                            intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
                            intent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
                            intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                        } else if (items != null) {
                            intent.putParcelableArrayListExtra("songList", items);
                        }

                        // Use shared element transition for mini player
                        MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.mini_player_container);

                        if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                            View miniPlayerView = miniPlayerFragment.getView();
                            ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                                    LibraryActivity.this,
                                    intent,
                                    miniPlayerView.findViewById(R.id.mini_player),
                                    miniPlayerView.findViewById(R.id.album_art),
                                    miniPlayerView.findViewById(R.id.song_title),
                                    miniPlayerView.findViewById(R.id.artist_name)
                            );
                        } else {
                            // Use slide animation for regular navigation
                            ActivityTransitionHelper.startActivityWithTransition(
                                    LibraryActivity.this,
                                    intent,
                                    ActivityTransitionHelper.TRANSITION_SLIDE_LEFT,
                                    false
                            );
                        }
                    } else {
                        // Use slide animation for regular navigation
                        ActivityTransitionHelper.startActivityWithTransition(
                                LibraryActivity.this,
                                intent,
                                ActivityTransitionHelper.TRANSITION_SLIDE_LEFT,
                                false
                        );
                    }
                    return true;

                } else if (item.getItemId() == R.id.nav_decouvrir) {
                    Intent searchIntent = new Intent(LibraryActivity.this, DecouvrirActivity.class);
                    if (items != null) {
                        searchIntent.putParcelableArrayListExtra("songList", items);
                    }

                    if (musicPlayer.isMiniPlayerVisible()) {
                        searchIntent.putExtra("isMiniPlayerVisible", true);
                        searchIntent.putExtra("song", musicPlayer.getCurrentSong());
                        searchIntent.putExtra("singer", musicPlayer.getCurrentSinger());
                        searchIntent.putExtra("img", musicPlayer.getCurrentImg());
                        searchIntent.putExtra("path", musicPlayer.getCurrentPath());

                        // Pass playlist info if playing from playlist
                        if (musicPlayer.isPlayingFromPlaylist()) {
                            searchIntent.putExtra("isFromPlaylist", true);
                            searchIntent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
                            searchIntent.putParcelableArrayListExtra("songList", musicPlayer.getCurrentPlaylistSongs());
                            searchIntent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                        }

                        // Use the new consistent transition method
                        MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.mini_player_container);

                        if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                            View miniPlayerView = miniPlayerFragment.getView();
                            ActivityTransitionHelper.startActivityWithDiscoverTransition(
                                    LibraryActivity.this,
                                    searchIntent,
                                    miniPlayerView.findViewById(R.id.mini_player),
                                    miniPlayerView.findViewById(R.id.album_art),
                                    miniPlayerView.findViewById(R.id.song_title),
                                    miniPlayerView.findViewById(R.id.artist_name)
                            );
                        } else {
                            // Use slide animation for regular navigation
                            ActivityTransitionHelper.startActivityWithTransition(
                                    LibraryActivity.this,
                                    searchIntent,
                                    ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                                    false
                            );
                        }
                    } else {
                        // Use slide animation for regular navigation
                        ActivityTransitionHelper.startActivityWithTransition(
                                LibraryActivity.this,
                                searchIntent,
                                ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                                false
                        );
                    }
                    return true;
                }
                return false; // Return false if no item was handled
            }
        });

        // Setup RecyclerView
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuggestionAdapter(this, this::onSuggestionClick);
        rvSuggestions.setAdapter(adapter);
        rvplaylists.setLayoutManager(new LinearLayoutManager(this));
        List<Liste> pl = playlistManager.getPlaylists();
        if (pl.isEmpty()) {
            pl.add(new Liste("Favorites", "Your favorite songs", R.drawable.albumcover));
            playlistManager.savePlaylists(pl);
        }
        adapterplaylist = new ListAdapter(pl, this::onplonClick, this);
        rvplaylists.setAdapter(adapterplaylist);

        // Initialize mock data
        initializeMockData();

        // Setup search functionality
        setupSearchBar();

        //This ensures that the keyboard only appears when the user clicks the search bar
        etSearch.setOnClickListener(v -> {
            etSearch.setFocusableInTouchMode(true);
            etSearch.setFocusable(true);
            etSearch.requestFocus();

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void createNewPlaylist(String name, String description) {
        Liste newPlaylist = new Liste(name, description, R.drawable.albumcover);
        playlistManager.addPlaylist(newPlaylist);

        // Refresh the playlist adapter
        List<Liste> updatedPlaylists = playlistManager.getPlaylists();
        adapterplaylist = new ListAdapter(updatedPlaylists, this::onplonClick, this);
        rvplaylists.setAdapter(adapterplaylist);
    }

    private void onplonClick(int position) {
        List<Liste> playlists = playlistManager.getPlaylists();
        if (position >= 0 && position < playlists.size()) {
            Liste selectedPlaylist = playlists.get(position);
            Intent intent = new Intent(this, PlaylistActivity.class);
            intent.putExtra("PLAYLIST_NAME", selectedPlaylist.getPlaylisttitle());

            // Use slide animation for playlist navigation
            ActivityTransitionHelper.startActivityWithTransition(
                    this,
                    intent,
                    ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                    false
            );
        }
    }

    private void initializeMockData() {
        // Clear existing data
        playlistsl.clear();

        // Get real playlists from PlaylistManager
        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            // Add playlist name
            playlistsl.add(playlist.getPlaylisttitle());
        }
    }

    private void setupSearchBar() {
        // Text change listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();

                // Show/hide clear button
                btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                // Change search icon color
                updateSearchIconColor(!query.isEmpty());

                // Update search bar background
                updateSearchBarBackground(!query.isEmpty());

                // Filter and show suggestions
                if (!query.isEmpty()) {
                    List<suggestion> filteredSuggestions = filterSuggestions(query);
                    showSuggestions(filteredSuggestions);
                } else {
                    hideSuggestions();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Focus change listener
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (!etSearch.getText().toString().trim().isEmpty()) {
                    showSuggestions(filterSuggestions(etSearch.getText().toString().trim().toLowerCase()));
                }
            } else {
                hideSuggestions();
            }
        });

        // Clear button click listener
        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.requestFocus();
        });
    }

    private List<suggestion> filterSuggestions(String query) {
        List<suggestion> filteredSuggestions = new ArrayList<>();

        // Filter playlists (max 3)
        int count = 0;
        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            String playlistTitle = playlist.getPlaylisttitle().toLowerCase();
            if (playlistTitle.contains(query) && count < 3) {
                filteredSuggestions.add(new suggestion(playlist.getPlaylisttitle(), suggestion.TYPE_PLAYLIST));
                count++;
            }
        }

        // Also filter songs if items list is available (max 3)
        if (items != null) {
            count = 0;
            for (item song : items) {
                if (song.getSong().toLowerCase().contains(query) && count < 3) {
                    // Store the full song object in the suggestion
                    filteredSuggestions.add(new suggestion(song, suggestion.TYPE_SONG));
                    count++;
                }
            }
        }

        return filteredSuggestions;
    }

    private void showSuggestions(List<suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            hideSuggestions();
            return;
        }

        adapter.setSuggestions(suggestions);

        if (cardSuggestions.getVisibility() != View.VISIBLE) {
            // Animate suggestions appearance
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            cardSuggestions.startAnimation(slideDown);
            cardSuggestions.setVisibility(View.VISIBLE);
        }
        cardSuggestions.clearAnimation(); // Stop any ongoing animation
    }

    private void hideSuggestions() {
        if (cardSuggestions.getVisibility() == View.VISIBLE) {
            cardSuggestions.clearAnimation(); // Stop any ongoing animation
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    cardSuggestions.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            cardSuggestions.startAnimation(slideUp);
        }
    }

    private void updateSearchIconColor(boolean active) {
        int colorFrom = active ? Color.parseColor("#AAAAAA") : Color.parseColor("#6200EE");
        int colorTo = active ? Color.parseColor("#6200EE") : Color.parseColor("#AAAAAA");

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(animator ->
                imgSearch.setColorFilter((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    private void updateSearchBarBackground(boolean active) {
        int colorFrom = Color.parseColor(active ? "#33FFFFFF" : "#66FFFFFF");
        int colorTo = Color.parseColor(active ? "#66FFFFFF" : "#33FFFFFF");

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(animator ->
                cardSearch.setCardBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        colorAnimation.start();
    }

    public void onSuggestionClick(suggestion suggestion) {
        int suggestionType = suggestion.getType();
        hideSuggestions();
        etSearch.setText(suggestion.getText());
        etSearch.setSelection(etSearch.getText().length());
        etSearch.clearFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }

        if (suggestionType == suggestion.TYPE_PLAYLIST) {
            navigateToPlaylist(suggestion.getText());
        } else if (suggestionType == suggestion.TYPE_SONG) {
            // Check if we have the full song object
            item song = suggestion.getSong();
            if (song != null) {
                try {
                    // Reset playlist mode when playing from search
                    musicPlayer.setPlayingFromPlaylist(false);
                    musicPlayer.setCurrentPlaylistName(null);
                    musicPlayer.setCurrentPlaylistSongs(null);

                    // Set the current song in MusicPlayer
                    musicPlayer.setCurrentSongItem(song);

                    // Find the index
                    int index = -1;
                    if (items != null) {
                        for (int i = 0; i < items.size(); i++) {
                            if (items.get(i).getPath().equals(song.getPath())) {
                                index = i;
                                break;
                            }
                        }
                    }

                    if (index == -1) index = 0;
                    musicPlayer.setCurrentSongIndex(index);

                    // Play the song first
                    musicPlayer.playSong(song, this);

                    // Then open the full player activity
                    Intent intent = new Intent(this, new_activity.class);
                    intent.putExtra("song", song.getSong());
                    intent.putExtra("singer", song.getSinger());
                    intent.putExtra("path", song.getPath());
                    intent.putExtra("img", song.getImg());
                    intent.putExtra("currentPosition", 0); // Start from beginning
                    intent.putParcelableArrayListExtra("songList", (ArrayList<item>) items);
                    intent.putExtra("currentSongIndex", index);
                    intent.putExtra("sourceActivity", "LibraryActivity");

                    // Use fade transition for song selection
                    ActivityTransitionHelper.startActivityWithTransition(
                            this,
                            intent,
                            ActivityTransitionHelper.TRANSITION_FADE,
                            false
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Error playing song from suggestion", e);
                    Toast.makeText(this, "Error playing song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Fallback to text-based search
                    navigateToSong(suggestion.getText());
                }
            } else {
                // Fallback to text-based search
                navigateToSong(suggestion.getText());
            }
        }
    }

    private void navigateToPlaylist(String playlistName) {
        // Find the playlist in the PlaylistManager
        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals(playlistName)) {
                // Found the playlist, navigate to PlaylistActivity
                Intent intent = new Intent(this, PlaylistActivity.class);
                intent.putExtra("PLAYLIST_NAME", playlistName);

                // Use slide animation for playlist navigation
                ActivityTransitionHelper.startActivityWithTransition(
                        this,
                        intent,
                        ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                        false
                );
                return;
            }
        }

        // If playlist not found, show error message
        Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
    }

    private void navigateToSong(String songName) {
        // Find the song in the items list
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                item song = items.get(i);
                if (song.getSong().equals(songName)) {
                    try {
                        // Reset playlist mode when playing from search
                        musicPlayer.setPlayingFromPlaylist(false);
                        musicPlayer.setCurrentPlaylistName(null);
                        musicPlayer.setCurrentPlaylistSongs(null);

                        // Set the current song in MusicPlayer
                        musicPlayer.setCurrentSongItem(song);
                        musicPlayer.setCurrentSongIndex(i);

                        // Play the song first
                        musicPlayer.playSong(song, this);

                        // Then open the full player activity
                        Intent intent = new Intent(this, new_activity.class);
                        intent.putExtra("song", song.getSong());
                        intent.putExtra("singer", song.getSinger());
                        intent.putExtra("path", song.getPath());
                        intent.putExtra("img", song.getImg());
                        intent.putExtra("currentPosition", 0); // Start from beginning
                        intent.putParcelableArrayListExtra("songList", (ArrayList<item>) items);
                        intent.putExtra("currentSongIndex", i);
                        intent.putExtra("sourceActivity", "LibraryActivity");

                        // Use fade transition for song selection
                        ActivityTransitionHelper.startActivityWithTransition(
                                this,
                                intent,
                                ActivityTransitionHelper.TRANSITION_FADE,
                                false
                        );
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Error playing song from search", e);
                        Toast.makeText(this, "Error playing song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        // If song not found, show error message
        Toast.makeText(this, "Song not found", Toast.LENGTH_SHORT).show();
    }

    private void playSong(item song) {
        if (song == null || song.getPath() == null) {
            Toast.makeText(this, "Cannot play this song", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update MusicPlayer with ALL song info
        musicPlayer.setCurrentSongInfo(
                song.getSong(),
                song.getSinger(),
                song.getImg(),
                song.getPath()
        );

        // Update current index
        updateCurrentSongIndex(song);

        // Add to history
        HistoryManager historyManager = new HistoryManager(this);
        historyManager.addToHistory(song);

        try {
            MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Update UI
            updateMiniPlayer();

        } catch (IOException e) {
            Log.e("LibraryActivity", "Error playing song", e);
            Toast.makeText(this, "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCurrentSongIndex(item song) {
        if (musicPlayer.isPlayingFromPlaylist()) {
            // Find index in playlist
            ArrayList<item> playlistSongs = musicPlayer.getCurrentPlaylistSongs();
            if (playlistSongs != null) {
                for (int i = 0; i < playlistSongs.size(); i++) {
                    if (playlistSongs.get(i).getPath().equals(song.getPath())) {
                        musicPlayer.setCurrentSongIndex(i);
                        break;
                    }
                }
            }
        } else if (items != null) {
            // Find index in all songs
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getPath().equals(song.getPath())) {
                    musicPlayer.setCurrentSongIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Apply reverse animation when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear image caches when activity is paused
        Glide.get(this).clearMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            MusicPlayer.getInstance().release();
        }
        // Clear heavy resources
        Glide.get(this).clearMemory();
    }
}
