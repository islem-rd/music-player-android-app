package com.example.musicplayer;

import static android.content.ContentValues.TAG;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION = 99;

    private Adapter sAdapter;
    private List<itempl> itemp;
    private List<item> items = new ArrayList<>(); // Initialize to avoid NPE
    private CardView cardSearch;
    private BottomNavigationView bottomNavigationView;
    private EditText etSearch;
    private ImageView imgSearch;
    private ImageButton btnClear;
    private CardView cardSuggestions;
    private RecyclerView rvSuggestions;
    private SuggestionAdapter sugadapter;
    private int x = 0;
    private int currentSongIndex = 0;
    private int aleaClickCount = 0;
    private RecyclerView rv, rvpl;
    private PlaylistManager playlistManager;
    private Adapterpl playlistAdapter;
    private boolean isPlayingFromPlaylist = false;
    private String currentPlaylistName = null;
    private ArrayList<item> currentPlaylistSongs = null;
    private MusicApiService musicApiService;
    private ImageManager imageManager;



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

        try {
            // Enable hardware acceleration for smoother transitions
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );

            setContentView(R.layout.activity_main);

            // Initialize managers first
            initializeManagers();

            // Setup UI components
            initializeViews();

            // Setup mini player fragment
            setupMiniPlayerFragment();

            // Setup listeners and adapters
            setupListeners();

            // Request permissions and load songs
            checkPermissionsAndLoadSongs();

            // Initialize search functionality
            setupSearchBar();

            // Process intent data (if any)
            processIntentData();


        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeManagers() {
        try {
            // Initialize managers in the correct order
            playlistManager = new PlaylistManager(this);
            musicPlayer = MusicPlayer.getInstance();
            imageManager = ImageManager.getInstance(this);
            musicApiService = new MusicApiService(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing managers", e);
            throw e; // Re-throw to be caught by the main try-catch
        }
    }

    private void initializeViews() {
        try {
            // Find all views
            cardSearch = findViewById(R.id.cardSearch);
            etSearch = findViewById(R.id.etSearch);
            imgSearch = findViewById(R.id.imgSearch);
            btnClear = findViewById(R.id.btnClear);
            cardSuggestions = findViewById(R.id.cardSuggestions);
            rvSuggestions = findViewById(R.id.rvSuggestions);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            rv = findViewById(R.id.rv);
            rvpl = findViewById(R.id.rvpl);

            // Set initial state
            bottomNavigationView.setSelectedItemId(R.id.nav_home);

            // Set transition names for shared elements
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityTransitionHelper.setupBackNavigation(this);
            }

            // Initialize RecyclerViews
            setupRecyclerViews();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupRecyclerViews() {
        // Setup suggestions RecyclerView
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        sugadapter = new SuggestionAdapter(this, this::onSuggestionClick);
        rvSuggestions.setAdapter(sugadapter);

        // Setup playlists RecyclerView
        rvpl.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<Liste> playlists = playlistManager.getPlaylists();
        if (playlists.isEmpty()) {
            playlists.add(new Liste("Favorites", "Your favorite songs", R.drawable.albumcover));
            playlistManager.savePlaylists(playlists);
        }
        playlistAdapter = new Adapterpl(this, playlists);
        rvpl.setAdapter(playlistAdapter);

        // Setup songs RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(this));
        sAdapter = new Adapter(this, items);
        rv.setAdapter(sAdapter);
    }

    private void setupListeners() {
        try {
            // Setup bottom navigation
            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_library) {
                    navigateToLibrary();
                    return true;
                } else if (item.getItemId() == R.id.nav_decouvrir) {
                    navigateToDiscover();
                    return true;
                }
                return false;
            });

            // Setup song adapter click listener
            sAdapter.setOnItemClickListener(position -> {
                if (position >= 0 && position < items.size()) {
                    item selectedItem = items.get(position);

                    // Update MusicPlayer singleton with current song info
                    musicPlayer.setCurrentSongIndex(position);
                    musicPlayer.setAllSongs(new ArrayList<>(items));

                    // Play the song first
                    musicPlayer.playSong(selectedItem, MainActivity.this);

                    // Then open the full player activity
                    Intent intent = new Intent(MainActivity.this, new_activity.class);
                    intent.putExtra("song", selectedItem.getSong());
                    intent.putExtra("singer", selectedItem.getSinger());
                    intent.putExtra("path", selectedItem.getPath());
                    intent.putExtra("img", selectedItem.getImg());
                    intent.putExtra("currentSongIndex", position);
                    intent.putParcelableArrayListExtra("songList", (ArrayList<item>) items);
                    intent.putExtra("sourceActivity", "MainActivity");

                    // Use shared element transition for mini player to full player
                    MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.mini_player_container);

                    if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                        View miniPlayerView = miniPlayerFragment.getView();
                        ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                                MainActivity.this,
                                intent,
                                miniPlayerView.findViewById(R.id.mini_player),
                                miniPlayerView.findViewById(R.id.album_art),
                                miniPlayerView.findViewById(R.id.song_title),
                                miniPlayerView.findViewById(R.id.artist_name)
                        );
                    } else {
                        // Fallback for older devices
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                }
            });

            // Setup search text click listener
            etSearch.setOnClickListener(v -> {
                etSearch.setFocusableInTouchMode(true);
                etSearch.setFocusable(true);
                etSearch.requestFocus();

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up listeners", e);
            throw e;
        }
    }

    private void checkPermissionsAndLoadSongs() {
        boolean hasPermission = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34)
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            if (!hasPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            if (!hasPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION);
            }
        } else { // Older versions
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            if (!hasPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }

        if (hasPermission) {
            getSongs();
        }
    }

    private void processIntentData() {
        try {
            // Get intent data
            boolean isVisible = getIntent().getBooleanExtra("isMiniPlayerVisible", false);
            String song = getIntent().getStringExtra("song");
            String singer = getIntent().getStringExtra("singer");
            String img = getIntent().getStringExtra("img");
            String path = getIntent().getStringExtra("path");

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

                    // Find the current song index in the playlist
                    if (path != null) {
                        for (int i = 0; i < playlistSongs.size(); i++) {
                            if (path.equals(playlistSongs.get(i).getPath())) {
                                musicPlayer.setCurrentSongIndex(i);
                                break;
                            }
                        }
                    }
                }
            } else {
                // Not from a playlist, reset the playlist flags
                musicPlayer.setPlayingFromPlaylist(false);
                musicPlayer.setCurrentPlaylistName(null);
                musicPlayer.setCurrentPlaylistSongs(null);
            }

            // If we have songs loaded, set them in the MusicPlayer
            if (!items.isEmpty()) {
                musicPlayer.setAllSongs(new ArrayList<>(items));
            }

            // If the flag is true, show the mini player
            if (isVisible && song != null && singer != null) {
                musicPlayer.setCurrentSongInfo(song, singer, img, path);
                musicPlayer.setMiniPlayerVisible(true);
                updateMiniPlayer();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing intent data", e);
            // Don't throw here, just log the error
        }
    }

    private void navigateToLibrary() {
        try {
            Intent intent = new Intent(MainActivity.this, LibraryActivity.class);

            // Add mini player info if visible
            if (musicPlayer.isMiniPlayerVisible()) {
                intent.putExtra("isMiniPlayerVisible", true);
                intent.putExtra("song", musicPlayer.getCurrentSong());
                intent.putExtra("singer", musicPlayer.getCurrentSinger());
                intent.putExtra("img", musicPlayer.getCurrentImg());
                intent.putExtra("path", musicPlayer.getCurrentPath());
                intent.putParcelableArrayListExtra("songList", (ArrayList<item>) items);

                // Pass playlist info if playing from playlist
                if (musicPlayer.isPlayingFromPlaylist()) {
                    intent.putExtra("isFromPlaylist", true);
                    intent.putExtra("playlistName", musicPlayer.getCurrentPlaylistName());
                    intent.putParcelableArrayListExtra("playlistSongs", musicPlayer.getCurrentPlaylistSongs());
                    intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                }

                // Use shared element transition for mini player
                MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mini_player_container);

                if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                    View miniPlayerView = miniPlayerFragment.getView();
                    ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                            MainActivity.this,
                            intent,
                            miniPlayerView.findViewById(R.id.mini_player),
                            miniPlayerView.findViewById(R.id.album_art),
                            miniPlayerView.findViewById(R.id.song_title),
                            miniPlayerView.findViewById(R.id.artist_name)
                    );
                } else {
                    // Use slide animation for regular navigation
                    ActivityTransitionHelper.startActivityWithTransition(
                            MainActivity.this,
                            intent,
                            ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                            false
                    );
                }
            } else {
                // Use slide animation for regular navigation
                ActivityTransitionHelper.startActivityWithTransition(
                        MainActivity.this,
                        intent,
                        ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                        false
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to library", e);
            Toast.makeText(this, "Error navigating to library", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDiscover() {
        try {
            Intent intent = new Intent(MainActivity.this, DecouvrirActivity.class);
            intent.putParcelableArrayListExtra("songList", (ArrayList<item>) items);

            // Add mini player info if visible
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
                }

                // Use the new consistent transition method
                MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mini_player_container);

                if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                    View miniPlayerView = miniPlayerFragment.getView();
                    ActivityTransitionHelper.startActivityWithDiscoverTransition(
                            MainActivity.this,
                            intent,
                            miniPlayerView.findViewById(R.id.mini_player),
                            miniPlayerView.findViewById(R.id.album_art),
                            miniPlayerView.findViewById(R.id.song_title),
                            miniPlayerView.findViewById(R.id.artist_name)
                    );
                } else {
                    // Use slide animation for regular navigation
                    ActivityTransitionHelper.startActivityWithTransition(
                            MainActivity.this,
                            intent,
                            ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                            false
                    );
                }
            } else {
                // Use slide animation for regular navigation
                ActivityTransitionHelper.startActivityWithTransition(
                        MainActivity.this,
                        intent,
                        ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                        false
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to discover", e);
            Toast.makeText(this, "Error navigating to discover", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSongs();
            } else {
                Toast.makeText(this, "Permission Denied! Cannot access songs.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getSongs() {
        try {
            ContentResolver contentResolver = getContentResolver();
            Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = contentResolver.query(songUri, null, null, null, null);

            // Clear existing items
            items.clear();

            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                long durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                // Format duration from milliseconds to mm:ss
                String durationFormatted = formatDuration(durationMs);

                // Get album art URI
                Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
                String albumArtPath = getAlbumArtPath(this, albumId);

                item track = new item(
                        artist,
                        title,
                        data,
                        durationFormatted,
                        albumArtPath  // This might be null — handle in adapter
                );

                items.add(track);        // Add to your list of songs
            }
            cursor.close();

            if (sAdapter != null) {
                sAdapter.notifyDataSetChanged();
            }

            if (items.isEmpty()) {
                Toast.makeText(this, "No songs found", Toast.LENGTH_SHORT).show();
            } else {
                // Update MusicPlayer with all songs
                musicPlayer.setAllSongs(new ArrayList<>(items));
            }

            // Notify adapter
            if (sAdapter != null) {
                sAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting songs", e);
            Toast.makeText(this, "Error loading songs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getAlbumArtPath(Context context, long albumId) {
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(albumArtUri, albumId);

        try {
            // Try opening the file to check if it exists
            context.getContentResolver().openInputStream(uri).close();
            return uri.toString(); // Valid URI
        } catch (Exception e) {
            return null; // File not found or inaccessible
        }
    }

    private String formatDuration(long durationMs) {
        int seconds = (int) (durationMs / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
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
                // Search bar has focus, show suggestions if there's text
                if (!etSearch.getText().toString().trim().isEmpty()) {
                    String query = etSearch.getText().toString().trim().toLowerCase();
                    List<suggestion> filteredSuggestions = filterSuggestions(query);
                    showSuggestions(filteredSuggestions);
                }
                // Update search bar appearance
                updateSearchBarBackground(true);
                updateSearchIconColor(true);
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
        int count = 0;

        // Filter playlists (max 3)
        count = 0;
        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().toLowerCase().contains(query) && count < 3) {
                filteredSuggestions.add(new suggestion(playlist.getPlaylisttitle(), suggestion.TYPE_PLAYLIST));
                count++;
            }
        }
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

        sugadapter.setSuggestions(suggestions);

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
        try {
            super.onDestroy();
            if (isFinishing()) {
                // Only release the player if the app is actually finishing
                // Not if just rotating or changing configuration
                MusicPlayer.getInstance().release();
            }
            // Clear heavy resources
            Glide.get(this).clearMemory();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
}
