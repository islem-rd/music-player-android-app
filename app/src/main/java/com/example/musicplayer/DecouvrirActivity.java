package com.example.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DecouvrirActivity extends BaseActivity {
    private static final String TAG = "DecouvrirActivity";
    private static final int REQUEST_PERMISSION_CODE = 123;
    private static final int MAX_INITIAL_HISTORY_ITEMS = 5; // Number of history items to show initially

    private BottomNavigationView bottomNavigationView;
    private CircleImageView profileImage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerRecommendations;
    private RecyclerView recyclerTrending;
    private RecyclerView recyclerHistory;
    private ProgressBar progressBar;
    private ImageView btnSearch;
    private Button btnClearHistory;
    private TextView emptyHistoryText;
    private EditText editSearch;
    private ImageView btnClearSearch;
    private RecyclerView recyclerSearchResults;
    private TextView textSearchResults;
    private TextView textNoResults;
    private TextView textRecommendationsTitle, textTrendingTitle, textNoConnection;

    private List<MusicTrack> localTracks = new ArrayList<>();
    private List<MusicTrack> recommendedTracks = new ArrayList<>();
    private List<MusicTrack> trendingTracks = new ArrayList<>();
    private List<MusicTrack> allHistoryTracks = new ArrayList<>(); // Full history list
    private List<MusicTrack> displayedHistoryTracks = new ArrayList<>(); // Limited display list
    private List<MusicTrack> searchResults = new ArrayList<>();

    private DiscoverCardAdapter recommendationsAdapter;
    private DiscoverCardAdapter trendingAdapter;
    private DiscoverTrackAdapter historyAdapter;
    private DiscoverTrackAdapter searchAdapter;

    private HistoryManager historyManager;
    private boolean isHistoryExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.decouvrir_layout);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        textRecommendationsTitle = findViewById(R.id.text_recommendations_title);
        textTrendingTitle = findViewById(R.id.text_trending_title);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );


        // Setup transition names for shared elements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityTransitionHelper.setupBackNavigation(this);
        }

        // Initialize managers
        historyManager = new HistoryManager(this);
        musicPlayer = MusicPlayer.getInstance();

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottom_navigation2);
        profileImage = findViewById(R.id.profile_image);
        recyclerRecommendations = findViewById(R.id.recycler_recommendations);
        recyclerTrending = findViewById(R.id.recycler_trending);
        recyclerHistory = findViewById(R.id.recycler_history);
        progressBar = findViewById(R.id.progress_bar);

        // Setup mini player fragment
        setupMiniPlayerFragment();

        // Find the history section views
        ViewGroup historySection = (ViewGroup) recyclerHistory.getParent();
        btnClearHistory = historySection.findViewById(R.id.btn_clear_history);
        emptyHistoryText = findViewById(R.id.empty_history_text);

        // Find search related views
        editSearch = findViewById(R.id.edit_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        recyclerSearchResults = findViewById(R.id.recycler_search_results);
        textSearchResults = findViewById(R.id.text_search_results);
        textNoResults = findViewById(R.id.text_no_results);

        // Set up bottom navigation
        bottomNavigationView.setVisibility(View.VISIBLE);
        bottomNavigationView.setSelectedItemId(R.id.nav_decouvrir);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                Intent intent = new Intent(DecouvrirActivity.this, MainActivity.class);
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
                        intent.putParcelableArrayListExtra("playlistSongs", musicPlayer.getCurrentPlaylistSongs());
                        intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                    }

                    // Use shared element transition for mini player
                    MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.mini_player_container);

                    if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                        View miniPlayerView = miniPlayerFragment.getView();
                        ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                                DecouvrirActivity.this,
                                intent,
                                miniPlayerView.findViewById(R.id.mini_player),
                                miniPlayerView.findViewById(R.id.album_art),
                                miniPlayerView.findViewById(R.id.song_title),
                                miniPlayerView.findViewById(R.id.artist_name)
                        );
                    } else {
                        // Use slide animation for regular navigation
                        ActivityTransitionHelper.startActivityWithTransition(
                                DecouvrirActivity.this,
                                intent,
                                ActivityTransitionHelper.TRANSITION_SLIDE_LEFT,
                                false
                        );
                    }
                } else {
                    // Use slide animation for regular navigation
                    ActivityTransitionHelper.startActivityWithTransition(
                            DecouvrirActivity.this,
                            intent,
                            ActivityTransitionHelper.TRANSITION_SLIDE_LEFT,
                            false
                    );
                }
                return true;
            } else if (item.getItemId() == R.id.nav_library) {
                Intent intent = new Intent(DecouvrirActivity.this, LibraryActivity.class);
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
                        intent.putParcelableArrayListExtra("playlistSongs", musicPlayer.getCurrentPlaylistSongs());
                        intent.putExtra("currentSongIndex", musicPlayer.getCurrentSongIndex());
                    }

                    // Use shared element transition for mini player
                    MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.mini_player_container);

                    if (miniPlayerFragment != null && miniPlayerFragment.getView() != null) {
                        View miniPlayerView = miniPlayerFragment.getView();
                        ActivityTransitionHelper.startActivityWithMiniPlayerTransition(
                                DecouvrirActivity.this,
                                intent,
                                miniPlayerView.findViewById(R.id.mini_player),
                                miniPlayerView.findViewById(R.id.album_art),
                                miniPlayerView.findViewById(R.id.song_title),
                                miniPlayerView.findViewById(R.id.artist_name)
                        );
                    } else {
                        // Use slide animation for regular navigation
                        ActivityTransitionHelper.startActivityWithTransition(
                                DecouvrirActivity.this,
                                intent,
                                ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                                false
                        );
                    }
                } else {
                    // Use slide animation for regular navigation
                    ActivityTransitionHelper.startActivityWithTransition(
                            DecouvrirActivity.this,
                            intent,
                            ActivityTransitionHelper.TRANSITION_SLIDE_RIGHT,
                            false
                    );
                }
                return true;
            }
            return false;
        });

        // Set up profile image click listener
        profileImage.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                bottomNavigationView.setVisibility(View.GONE);
                if (currentUser != null) {
                    showProfileFragment();
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                    redirectToLogin();
                }
            } else {
                showOfflineNotification();
            }
        });

        // Set up clear history button
        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> {
                showClearHistoryConfirmation();
            });
        }

        // Set up search functionality
        setupSearch();

        // Initialize RecyclerViews
        setupRecyclerViews();

        // Check for permissions and load data
        checkPermissionsAndLoadData();
    }

    // Toggle between showing limited history and full history
    private void toggleHistoryDisplay() {
        isHistoryExpanded = !isHistoryExpanded;
        updateHistoryDisplay();
    }

    // Update the history display based on expanded state
    private void updateHistoryDisplay() {
        if (allHistoryTracks.isEmpty()) {
            return;
        }

        displayedHistoryTracks.clear();

        if (isHistoryExpanded) {
            // Show all history items
            displayedHistoryTracks.addAll(allHistoryTracks);

        } else {
            // Show only the first MAX_INITIAL_HISTORY_ITEMS
            int itemsToShow = Math.min(MAX_INITIAL_HISTORY_ITEMS, allHistoryTracks.size());
            for (int i = 0; i < itemsToShow; i++) {
                displayedHistoryTracks.add(allHistoryTracks.get(i));
            }

        }

        // Update the adapter with the new list
        historyAdapter.updateTracks(displayedHistoryTracks);
        historyAdapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        // Initialize search adapter
        searchAdapter = new DiscoverTrackAdapter(this, searchResults);
        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerSearchResults.setAdapter(searchAdapter);

        // Set up search text change listener
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                    recyclerSearchResults.setVisibility(View.GONE);
                    textSearchResults.setVisibility(View.GONE);
                    textNoResults.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up clear search button
        btnClearSearch.setOnClickListener(v -> {
            editSearch.setText("");
            recyclerSearchResults.setVisibility(View.GONE);
            textSearchResults.setVisibility(View.GONE);
            textNoResults.setVisibility(View.GONE);
        });

        // Set up search results click listener
        searchAdapter.setOnItemClickListener(position -> {
            MusicTrack track = searchResults.get(position);
            playSong(convertToItem(track));
        });
    }

    private void performSearch(String query) {
        searchResults.clear();

        // Search in local tracks
        for (MusicTrack track : localTracks) {
            if (track.getTitle().toLowerCase().contains(query) ||
                    track.getArtist().toLowerCase().contains(query) ||
                    (track.getAlbum() != null && track.getAlbum().toLowerCase().contains(query))) {
                searchResults.add(track);
            }
        }

        // Update UI based on results
        if (searchResults.isEmpty()) {
            textNoResults.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
            textSearchResults.setVisibility(View.GONE);
        } else {
            textNoResults.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.VISIBLE);
            textSearchResults.setVisibility(View.VISIBLE);
            searchAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerViews() {
        try {
            // Set up recommendations RecyclerView
            if (recyclerRecommendations != null) {
                recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                recommendationsAdapter = new DiscoverCardAdapter(this, recommendedTracks);
                recyclerRecommendations.setAdapter(recommendationsAdapter);

                // Set click listener for recommendations
                recommendationsAdapter.setOnItemClickListener(position -> {
                    try {
                        if (position >= 0 && position < recommendedTracks.size()) {
                            MusicTrack track = recommendedTracks.get(position);
                            if (track != null) {
                                if (track.getDeezerLink() != null && !track.getDeezerLink().isEmpty()) {
                                    try {
                                        // Open Deezer link in browser
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(track.getDeezerLink()));
                                        startActivity(browserIntent);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error opening Deezer link", e);
                                        Toast.makeText(DecouvrirActivity.this, "Error opening link", Toast.LENGTH_SHORT).show();
                                        // Fallback to local playback
                                        playSong(convertToItem(track));
                                    }
                                } else {
                                    // Fallback to local playback if no Deezer link
                                    playSong(convertToItem(track));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in recommendation click", e);
                        Toast.makeText(DecouvrirActivity.this, "Error playing track", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Set up trending RecyclerView
            if (recyclerTrending != null) {
                recyclerTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                trendingAdapter = new DiscoverCardAdapter(this, trendingTracks);
                recyclerTrending.setAdapter(trendingAdapter);

                // Set click listener for trending
                trendingAdapter.setOnItemClickListener(position -> {
                    try {
                        if (position >= 0 && position < trendingTracks.size()) {
                            MusicTrack track = trendingTracks.get(position);
                            if (track != null) {
                                if (track.getSpotifyUrl() != null && !track.getSpotifyUrl().isEmpty()) {
                                    try {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(track.getSpotifyUrl()));
                                        intent.putExtra(Intent.EXTRA_REFERRER,
                                                Uri.parse("android-app://" + getPackageName()));
                                        intent.setPackage("com.spotify.music"); // Force open in Spotify app
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Spotify app not found, opening in browser", e);
                                        // Fallback to browser if Spotify app is not installed
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(track.getSpotifyUrl()));
                                        startActivity(browserIntent);
                                    }
                                } else {
                                    Toast.makeText(this, "Spotify link not available", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in trending click", e);
                        Toast.makeText(DecouvrirActivity.this, "Error playing track", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Set up history RecyclerView with the displayed history tracks
            if (recyclerHistory != null) {
                recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
                historyAdapter = new DiscoverTrackAdapter(this, displayedHistoryTracks);
                recyclerHistory.setAdapter(historyAdapter);

                // Add swipe-to-delete functionality for history items
                setupHistorySwipeToDelete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerViews", e);
            Toast.makeText(this, "Error setting up UI components", Toast.LENGTH_SHORT).show();
        }
    }

    // Add a new method to set up swipe-to-delete functionality
    private void setupHistorySwipeToDelete() {
        // Create an ItemTouchHelper for swipe-to-delete
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    historyAdapter.removeItem(position);

                    // If all items are removed, show empty state
                    if (displayedHistoryTracks.isEmpty()) {
                        emptyHistoryText.setVisibility(View.VISIBLE);
                        emptyHistoryText.setText("No play history yet");
                        recyclerHistory.setVisibility(View.GONE);
                    }
                }
            }

            // Improved visual feedback for swipe with better performance
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Get item view
                    View itemView = viewHolder.itemView;

                    // Only draw when swiping left (dX < 0)
                    if (dX < 0) {
                        // Save canvas state
                        c.save();

                        // Clip to the item boundaries
                        c.clipRect(itemView.getRight() + dX, itemView.getTop(),
                                itemView.getRight(), itemView.getBottom());

                        // Draw red background for delete - only on the swiped area
                        Paint paint = new Paint();
                        paint.setColor(Color.parseColor("#FF5252"));

                        c.drawRect(
                                itemView.getRight() + dX,
                                itemView.getTop(),
                                itemView.getRight(),
                                itemView.getBottom(),
                                paint
                        );

                        // Draw delete icon
                        Drawable deleteIcon = ContextCompat.getDrawable(DecouvrirActivity.this, android.R.drawable.ic_menu_delete);
                        if (deleteIcon != null) {
                            int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                            // Calculate icon position based on swipe progress
                            // Only show icon when swiped at least halfway
                            float swipeThreshold = 0.5f;
                            float swipeProgress = Math.min(Math.abs(dX) / (float)itemView.getWidth(), 1f);

                            if (swipeProgress >= swipeThreshold) {
                                int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                                int iconRight = itemView.getRight() - iconMargin;

                                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                                deleteIcon.draw(c);
                            }
                        }

                        // Restore canvas state
                        c.restore();
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        // Attach the ItemTouchHelper to the RecyclerView
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHistory);
    }

    private void checkPermissionsAndLoadData() {
        // For Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        REQUEST_PERMISSION_CODE);
                return;
            }
        }
        // For older Android versions
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
            return;
        }

        // If we have permissions, load data
        loadData();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadData();
            } else {
                Toast.makeText(this, "Permission denied. Limited functionality available.", Toast.LENGTH_SHORT).show();
                // Still load trending data from API even without local storage permission
                fetchTrendingFromSpotify();
            }
        }
    }

    private void loadData() {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Load local tracks for offline fallback

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();

                // Cacher titres + recyclers
                textRecommendationsTitle.setVisibility(View.GONE);
                recyclerRecommendations.setVisibility(View.GONE);
                textTrendingTitle.setVisibility(View.GONE);
                recyclerTrending.setVisibility(View.GONE);

                // Optionnel : afficher un message global
                // textNoConnection.setVisibility(View.VISIBLE);

                return;
            }


            // Load trending tracks from API
            if (isNetworkAvailable()) {
                fetchTrendingFromSpotify();

                // Use personalized recommendations if user has history
                if (allHistoryTracks != null && !allHistoryTracks.isEmpty()) {
                    fetchPersonalizedRecommendations();
                } else {
                    fetchRecommendationsFromDeezer();
                }
            } else {
                // If offline, show a message and use some local tracks as trending
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();

            }

            // Load history from HistoryManager
            loadHistory();
        } catch (Exception e) {
            // Log the error and show a toast
            Log.e(TAG, "Error loading data", e);
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Hide progress bar
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Fallback to offline mode
            generateOfflineTrending();
            generateOfflineRecommendations();
        }
    }

    private void loadLocalTracks() {
        new Thread(() -> {
            localTracks.clear();

            ContentResolver contentResolver = getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

                    // Get album art URI
                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
                    Uri albumArt = ContentUris.withAppendedId(albumArtUri, albumId);

                    MusicTrack track = new MusicTrack();
                    track.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                    track.setTitle(title);
                    track.setArtist(artist);
                    track.setAlbum(album);
                    track.setPath(data);
                    track.setAlbumArt(albumArt.toString());

                    localTracks.add(track);
                }
                cursor.close();
            }

            // Generate recommendations based on local tracks
            runOnUiThread(this::generateRecommendations);
        }).start();
    }

    // Replace the generateRecommendations method with this implementation
    private void generateRecommendations() {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);

        // Check if network is available
        if (!isNetworkAvailable()) {
            // Fallback to local recommendations if offline
            generateOfflineRecommendations();
            return;
        }

        // Fetch recommendations from Deezer API
        fetchRecommendationsFromDeezer();
    }

    // Add this new method to fetch recommendations from Deezer API
    private void fetchRecommendationsFromDeezer() {
        try {
            // Use a general chart or playlist from Deezer
            String url = "https://api.deezer.com/chart/0/tracks?limit=10";

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Deezer API call failed", e);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            generateOfflineRecommendations();
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body() != null ? response.body().string() : "";
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray tracks = jsonObject.getJSONArray("data");

                            List<MusicTrack> newTracks = new ArrayList<>();

                            for (int i = 0; i < tracks.length(); i++) {
                                try {
                                    JSONObject track = tracks.getJSONObject(i);
                                    JSONObject artist = track.getJSONObject("artist");
                                    JSONObject album = track.getJSONObject("album");

                                    MusicTrack musicTrack = new MusicTrack();
                                    musicTrack.setId(track.getLong("id"));
                                    musicTrack.setTitle(track.getString("title"));
                                    musicTrack.setArtist(artist.getString("name"));
                                    musicTrack.setAlbum(album.getString("title"));
                                    musicTrack.setAlbumArt(album.getString("cover_medium"));
                                    musicTrack.setDeezerLink(track.getString("link"));

                                    newTracks.add(musicTrack);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing track: " + e.getMessage());
                                }
                            }

                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    recommendedTracks.clear();
                                    recommendedTracks.addAll(newTracks);
                                    if (recommendationsAdapter != null) {
                                        recommendationsAdapter.notifyDataSetChanged();
                                    }
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }

                                    // Update the recommendations title
                                    try {
                                        ViewGroup parent = (ViewGroup) recyclerRecommendations.getParent();
                                        if (parent != null && parent.getChildCount() > 0) {
                                            View titleView = parent.getChildAt(0);
                                            if (titleView instanceof TextView) {
                                                ((TextView) titleView).setText("Recommended for you");
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating recommendations title", e);
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error", e);
                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    generateOfflineRecommendations();
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                generateOfflineRecommendations();
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchRecommendationsFromDeezer", e);
            generateOfflineRecommendations();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    // Add this method for offline fallback
    private void generateOfflineRecommendations() {
        recommendedTracks.clear();

        if (localTracks.size() > 0) {
            // Create a shuffled copy of local tracks
            List<MusicTrack> shuffled = new ArrayList<>(localTracks);
            Collections.shuffle(shuffled);

            // Take up to 10 tracks for recommendations
            int count = Math.min(shuffled.size(), 10);
            for (int i = 0; i < count; i++) {
                recommendedTracks.add(shuffled.get(i));
            }

            // Update the adapter
            recommendationsAdapter.notifyDataSetChanged();
        }

        // Hide loading indicator
        progressBar.setVisibility(View.GONE);

        // Update the recommendations title
        TextView recommendationsTitle = (TextView) ((ViewGroup) recyclerRecommendations.getParent()).getChildAt(0);
        if (recommendationsTitle != null && recommendationsTitle instanceof TextView) {
            recommendationsTitle.setText("Recommended for you (Offline)");
        }
    }

    private void fetchTrendingFromSpotify() {
        MusicApiService apiService = new MusicApiService(this);
        String playlistId = "31lBPDWB4mjicAZt3ERBqI";

        apiService.fetchTopTracksFromPlaylist(playlistId, 10, new MusicApiService.TracksCallback() {
            @Override
            public void onTracksFetched(List<MusicTrack> tracks) {
                trendingTracks.clear();
                trendingTracks.addAll(tracks);
                trendingAdapter.notifyDataSetChanged();
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DecouvrirActivity.this, "Spotify trending failed: " + message, Toast.LENGTH_SHORT).show();
                generateOfflineTrending();
            }
        });
    }

    // Add this helper method to check if a track is from Deezer
    private boolean isFromDeezer(MusicTrack track) {
        return track.getDeezerLink() != null && !track.getDeezerLink().isEmpty();
    }

    // Add this method to fetch personalized recommendations
    private void fetchPersonalizedRecommendations() {
        try {
            // Check if history is loaded and not empty
            if (allHistoryTracks == null || allHistoryTracks.isEmpty()) {
                // No history available, use general recommendations
                fetchRecommendationsFromDeezer();
                return;
            }

            MusicTrack recentTrack = allHistoryTracks.get(0);
            // Check if the track has artist information
            if (recentTrack == null || recentTrack.getArtist() == null || recentTrack.getArtist().isEmpty()) {
                fetchRecommendationsFromDeezer();
                return;
            }

            String artistName = recentTrack.getArtist();

            // Search for similar artists on Deezer
            String url = "https://api.deezer.com/search/artist?q=" + Uri.encode(artistName) + "&limit=1";

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Deezer API call failed for artist search", e);
                    runOnUiThread(() -> {
                        fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body() != null ? response.body().string() : "";
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray artists = jsonObject.getJSONArray("data");

                            if (artists.length() > 0) {
                                JSONObject artist = artists.getJSONObject(0);
                                long artistId = artist.getLong("id");

                                // Get top tracks from this artist
                                fetchArtistTopTracks(artistId);
                            } else {
                                runOnUiThread(() -> {
                                    fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                                });
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error for artist search", e);
                            runOnUiThread(() -> {
                                fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchPersonalizedRecommendations", e);
            fetchRecommendationsFromDeezer(); // Fallback to general recommendations
        }
    }

    // Add this method to fetch top tracks from an artist
    private void fetchArtistTopTracks(long artistId) {
        try {
            String url = "https://api.deezer.com/artist/" + artistId + "/top?limit=10";

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Deezer API call failed for artist top tracks", e);
                    runOnUiThread(() -> {
                        fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body() != null ? response.body().string() : "";
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray tracks = jsonObject.getJSONArray("data");

                            List<MusicTrack> newTracks = new ArrayList<>();

                            for (int i = 0; i < tracks.length(); i++) {
                                try {
                                    JSONObject track = tracks.getJSONObject(i);
                                    JSONObject artist = track.getJSONObject("artist");
                                    JSONObject album = track.getJSONObject("album");

                                    MusicTrack musicTrack = new MusicTrack();
                                    musicTrack.setId(track.getLong("id"));
                                    musicTrack.setTitle(track.getString("title"));
                                    musicTrack.setArtist(artist.getString("name"));
                                    musicTrack.setAlbum(album.getString("title"));
                                    musicTrack.setAlbumArt(album.getString("cover_medium"));
                                    musicTrack.setDeezerLink(track.getString("link"));

                                    newTracks.add(musicTrack);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing track: " + e.getMessage());
                                }
                            }

                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    recommendedTracks.clear();
                                    recommendedTracks.addAll(newTracks);
                                    if (recommendationsAdapter != null) {
                                        recommendationsAdapter.notifyDataSetChanged();
                                    }
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }

                                    // Update the recommendations title
                                    try {
                                        ViewGroup parent = (ViewGroup) recyclerRecommendations.getParent();
                                        if (parent != null && parent.getChildCount() > 0) {
                                            View titleView = parent.getChildAt(0);
                                            if (titleView instanceof TextView) {
                                                ((TextView) titleView).setText("Based on your listening history");
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating recommendations title", e);
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error for artist top tracks", e);
                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                                }
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                fetchRecommendationsFromDeezer(); // Fallback to general recommendations
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchArtistTopTracks", e);
            fetchRecommendationsFromDeezer(); // Fallback to general recommendations
        }
    }

    private void generateOfflineTrending() {
        trendingTracks.clear();

        if (localTracks.size() > 0) {
            // Create a different shuffled copy of local tracks for trending
            List<MusicTrack> shuffled = new ArrayList<>(localTracks);
            Collections.shuffle(shuffled);

            // Take up to 10 tracks for trending
            int count = Math.min(shuffled.size(), 10);
            for (int i = 0; i < count; i++) {
                trendingTracks.add(shuffled.get(i));
            }

            // Update the adapter
            trendingAdapter.notifyDataSetChanged();
        }
    }

    // Update the loadHistory method to handle limited display
    private void loadHistory() {
        allHistoryTracks.clear();
        displayedHistoryTracks.clear();

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            emptyHistoryText.setVisibility(View.VISIBLE);
            emptyHistoryText.setText("Please log in to view your history");
            recyclerHistory.setVisibility(View.GONE);
            btnClearHistory.setVisibility(View.GONE);

            progressBar.setVisibility(View.GONE);
            return;
        }

        // Check if network is available
        if (!isNetworkAvailable()) {
            emptyHistoryText.setVisibility(View.VISIBLE);
            emptyHistoryText.setText("No internet connection. Connect to view your history.");
            recyclerHistory.setVisibility(View.GONE);
            btnClearHistory.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);

        // Get history from HistoryManager
        historyManager.getHistory(new HistoryManager.OnHistoryLoadedListener() {
            @Override
            public void onHistoryLoaded(List<item> history) {
                runOnUiThread(() -> {
                    allHistoryTracks.clear();
                    displayedHistoryTracks.clear();

                    // Convert history items to MusicTracks
                    for (item historyItem : history) {
                        MusicTrack track = new MusicTrack();
                        track.setTitle(historyItem.getSong());
                        track.setArtist(historyItem.getSinger());
                        track.setPath(historyItem.getPath());
                        track.setAlbumArt(historyItem.getImg());
                        allHistoryTracks.add(track);
                    }

                    // Update UI based on results
                    if (allHistoryTracks.isEmpty()) {
                        emptyHistoryText.setVisibility(View.VISIBLE);
                        emptyHistoryText.setText("No play history yet");
                        recyclerHistory.setVisibility(View.GONE);

                    } else {
                        emptyHistoryText.setVisibility(View.GONE);
                        recyclerHistory.setVisibility(View.VISIBLE);

                        // Only show the first 5 items
                        int itemsToShow = Math.min(5, allHistoryTracks.size());
                        for (int i = 0; i < itemsToShow; i++) {
                            displayedHistoryTracks.add(allHistoryTracks.get(i));
                        }

                    }

                    // Always show clear button if user is logged in and has network
                    btnClearHistory.setVisibility(View.VISIBLE);

                    // Update adapter and hide loading indicator
                    historyAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    // Add this method to check if user is logged in
    private boolean isUserLoggedIn() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null;
    }

    // Improve the showClearHistoryConfirmation method
    private void showClearHistoryConfirmation() {
        if (!isUserLoggedIn() || !isNetworkAvailable()) {
            Toast.makeText(this, "Cannot clear history. Please check login status and network connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear History");
        builder.setMessage("Are you sure you want to clear your play history?");
        builder.setPositiveButton("Clear", (dialog, which) -> {
            // Show loading indicator
            progressBar.setVisibility(View.VISIBLE);

            historyManager.clearHistory(success -> {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (success) {
                        allHistoryTracks.clear();
                        displayedHistoryTracks.clear();
                        historyAdapter.notifyDataSetChanged();
                        emptyHistoryText.setVisibility(View.VISIBLE);
                        emptyHistoryText.setText("No play history yet");
                        recyclerHistory.setVisibility(View.GONE);

                        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to clear history", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Update the playSong method to ensure history is added
    private void playSong(item song) {
        if (song == null || song.getPath() == null) {
            Toast.makeText(this, "Cannot play this song", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to history - ensure this happens for all song plays
        historyManager.addToHistory(song);

        // Get the MediaPlayer from MusicPlayer singleton
        MediaPlayer mediaPlayer = musicPlayer.getMediaPlayer();

        try {
            // Check if we're already playing this song
            if (song.getPath().equals(musicPlayer.getCurrentPath()) && mediaPlayer.isPlaying()) {
                // Already playing this song, just update UI
                return;
            }

            // Reset the MediaPlayer and set the new song
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            musicPlayer.setCurrentPath(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Update MusicPlayer state
            musicPlayer.setMiniPlayerVisible(true);
            musicPlayer.setCurrentSongInfo(song.getSong(), song.getSinger(), song.getImg(), song.getPath());

            // Update mini player UI
            updateMiniPlayer();

        } catch (IOException e) {
            Log.e(TAG, "Error playing song", e);
            Toast.makeText(this, "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPlayerActivity(item song) {
        Intent intent = new Intent(this, new_activity.class);
        intent.putExtra("song", song.getSong());
        intent.putExtra("singer", song.getSinger());
        intent.putExtra("path", song.getPath());
        intent.putExtra("img", song.getImg());
        intent.putExtra("sourceActivity", "DecouvrirActivity");

        // Reset playlist mode when playing from discover
        musicPlayer.setPlayingFromPlaylist(false);
        musicPlayer.setCurrentPlaylistName(null);
        musicPlayer.setCurrentPlaylistSongs(null);

        // Pass all songs as the playlist
        ArrayList<item> allSongs = new ArrayList<>();
        for (MusicTrack track : localTracks) {
            allSongs.add(convertToItem(track));
        }
        intent.putParcelableArrayListExtra("songList", allSongs);

        // Find the current song index
        int currentIndex = 0;
        for (int i = 0; i < allSongs.size(); i++) {
            if (allSongs.get(i).getPath() != null && allSongs.get(i).getPath().equals(song.getPath())) {
                currentIndex = i;
                break;
            }
        }
        intent.putExtra("currentSongIndex", currentIndex);

        // Use shared element transition for mini player to full player
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
            // Fallback for older devices
            ActivityTransitionHelper.startActivityWithTransition(
                    this,
                    intent,
                    ActivityTransitionHelper.TRANSITION_FADE,
                    false
            );
        }
    }

    private item convertToItem(MusicTrack track) {
        return new item(
                track.getArtist(),
                track.getTitle(),
                track.getPath(),
                "0:00", // Duration placeholder
                track.getAlbumArt()
        );
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }

    private void showProfileFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        ProfileFragment profileFragment = new ProfileFragment();
        profileFragment.setOnBackPressedListener(() -> bottomNavigationView.setVisibility(View.VISIBLE));

        transaction.replace(R.id.fragment_container, profileFragment);
        transaction.addToBackStack("profile");
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        View contentSearch = findViewById(R.id.search_scroll_container);
        View contentDiscover = findViewById(R.id.content_discover);

        if (contentSearch.getVisibility() == View.VISIBLE) {
            contentSearch.setVisibility(View.GONE);
            contentDiscover.setVisibility(View.VISIBLE);
            return;
        }

        super.onBackPressed();
        // Apply reverse animation when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        ActivityTransitionHelper.startActivityWithTransition(
                this,
                intent,
                ActivityTransitionHelper.TRANSITION_FADE,
                false
        );
    }

    private void showOfflineNotification() {
        Toast.makeText(this, "You are offline. Offline mode activated.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateProfileImage(mAuth.getCurrentUser());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHistoryVisibility();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear image caches when activity is paused
        Glide.get(this).clearMemory();
    }

    public void updateProfileImage(FirebaseUser user) {
        if (user == null) {
            // User is not logged in, set default profile image
            profileImage.setImageResource(R.drawable.default_profile);
            return;
        }

        // First try to load from Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageBase64 = documentSnapshot.getString("profileImage");
                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                profileImage.setImageBitmap(bitmap);
                                return;
                            } catch (IllegalArgumentException e) {
                                // Invalid Base64 string, fall through to next option
                            }
                        }
                    }
                    loadAuthProfileImage(user);
                })
                .addOnFailureListener(e -> loadAuthProfileImage(user));
    }

    // Add a method to explicitly reset the profile image to default
    public void resetProfileImage() {
        profileImage.setImageResource(R.drawable.default_profile);
    }

    private void loadAuthProfileImage(FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.default_profile)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.default_profile);
        }
    }

    // Add this method to update history visibility based on login status and network
    void updateHistoryVisibility() {
        boolean loggedIn = isUserLoggedIn();
        boolean online = isNetworkAvailable();

        if (!loggedIn) {
            recyclerHistory.setVisibility(View.GONE);
            emptyHistoryText.setVisibility(View.VISIBLE);
            emptyHistoryText.setText("Please log in to view your history");
            btnClearHistory.setVisibility(View.GONE);

        } else if (!online) {
            recyclerHistory.setVisibility(View.GONE);
            emptyHistoryText.setVisibility(View.VISIBLE);
            emptyHistoryText.setText("No internet connection. Connect to view your history.");
            btnClearHistory.setVisibility(View.GONE);

        } else {
            // Only load history if both conditions are met
            loadHistory();
        }
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
