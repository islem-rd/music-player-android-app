package com.example.musicplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MusicApiService {
    private static final String TAG = "MusicApiService";
    private static final String SPOTIFY_CLIENT_ID = "4e765c79902f49a6baebe6aff81b1e9a";
    private static final String SPOTIFY_CLIENT_SECRET = "2b411fac1209445997ab7d9f0df1b3c6";
    private static final String SPOTIFY_BASE_URL = "https://api.spotify.com/";
    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/";

    private SpotifyApi spotifyApi;
    private String accessToken;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isAuthenticating = false;
    private final Object authLock = new Object();
    private ImageCacheManager imageCacheManager;
    private Context context;

    public MusicApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SPOTIFY_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        spotifyApi = retrofit.create(SpotifyApi.class);
        authenticateSpotify();
    }

    public MusicApiService(Context context) {
        this();
        this.context = context.getApplicationContext();
        this.imageCacheManager = new ImageCacheManager(context);
    }

    private void authenticateSpotify() {
        synchronized (authLock) {
            if (isAuthenticating) {
                return;
            }
            isAuthenticating = true;
        }

        Retrofit authRetrofit = new Retrofit.Builder()
                .baseUrl(SPOTIFY_AUTH_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        String credentials = SPOTIFY_CLIENT_ID + ":" + SPOTIFY_CLIENT_SECRET;
        String base64Auth = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        SpotifyAuthApi authApi = authRetrofit.create(SpotifyAuthApi.class);
        Call<AuthResponse> call = authApi.getToken(
                "Basic " + base64Auth,
                "client_credentials"
        );

        // Execute the request asynchronously
        executor.execute(() -> {
            try {
                Response<AuthResponse> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    accessToken = response.body().accessToken;
                    Log.d(TAG, "Authentication successful, token received");
                } else {
                    Log.e(TAG, "Authentication failed: " +
                            (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Authentication error", e);
            } finally {
                synchronized (authLock) {
                    isAuthenticating = false;
                }
            }
        });
    }

    public interface SpotifyApi {
        @GET("v1/search")
        Call<SearchResponse> searchTrack(
                @Header("Authorization") String authorization,
                @Query("q") String query,
                @Query("type") String type,
                @Query("limit") int limit
        );
        @GET("v1/playlists/{playlist_id}/tracks")
        Call<PlaylistTracksResponse> getPlaylistTracks(
                @Header("Authorization") String authorization,
                @retrofit2.http.Path("playlist_id") String playlistId,
                @Query("limit") int limit
        );

    }


    public interface SpotifyAuthApi {
        @POST("api/token")
        @FormUrlEncoded
        Call<AuthResponse> getToken(
                @Header("Authorization") String authorization,
                @Field("grant_type") String grantType
        );
    }

    public static class AuthResponse {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("expires_in")
        public int expiresIn;

        @SerializedName("token_type")
        public String tokenType;
    }

    public static class SearchResponse {
        @SerializedName("tracks")
        public Tracks tracks;
    }

    public static class Tracks {
        @SerializedName("items")
        public List<Track> items;
    }

    public static class Track {
        @SerializedName("name")
        public String name;
        @SerializedName("id")
        public String id;

        @SerializedName("artists")
        public List<Artist> artists;

        @SerializedName("album")
        public Album album;
    }

    public static class Artist {
        @SerializedName("name")
        public String name;
    }

    public static class Album {
        @SerializedName("name")
        public String name;

        @SerializedName("images")
        public List<Image> images;
    }

    public static class Image {
        @SerializedName("url")
        public String url;

        @SerializedName("height")
        public int height;

        @SerializedName("width")
        public int width;
    }

    public void getAlbumArt(String artist, String track, final AlbumArtCallback callback) {
        if (accessToken == null) {
            // If we don't have a token yet, authenticate and then retry
            authenticateSpotify();
            mainHandler.postDelayed(() -> getAlbumArt(artist, track, callback), 2000);
            return;
        }

        String query = artist + " " + track;
        Call<SearchResponse> call = spotifyApi.searchTrack(
                "Bearer " + accessToken,
                query,
                "track",
                1
        );

        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.code() == 401) {
                    // Token expired, refresh and retry
                    Log.d(TAG, "Token expired, refreshing...");
                    authenticateSpotify();
                    mainHandler.postDelayed(() -> getAlbumArt(artist, track, callback), 2000);
                    return;
                }

                if (response.isSuccessful() && response.body() != null
                        && response.body().tracks != null
                        && response.body().tracks.items != null
                        && !response.body().tracks.items.isEmpty()) {

                    Track trackResult = response.body().tracks.items.get(0);
                    if (trackResult.album != null && trackResult.album.images != null && !trackResult.album.images.isEmpty()) {
                        // Get the highest quality image (usually the first one)
                        Image bestImage = trackResult.album.images.get(0);
                        final String imageUrl = bestImage.url;

                        // Log successful image retrieval
                        Log.d(TAG, "Album art found for " + artist + " - " + track + ": " + imageUrl);

                        // Check if we have the image cache manager
                        if (imageCacheManager != null) {
                            // Check if the image is already cached
                            String cachedPath = imageCacheManager.getCachedImagePath(imageUrl);
                            if (cachedPath != null) {
                                // Image is already cached, return the local path
                                Log.d(TAG, "Using cached image: " + cachedPath);
                                mainHandler.post(() -> callback.onSuccess(cachedPath));
                                return;
                            }

                            // Image is not cached, download and cache it
                            imageCacheManager.cacheImageFromUrl(imageUrl, new ImageCacheManager.ImageCacheCallback() {
                                @Override
                                public void onSuccess(String localPath) {
                                    Log.d(TAG, "Image cached successfully: " + localPath);
                                    mainHandler.post(() -> callback.onSuccess(localPath));
                                }

                                @Override
                                public void onError(String message) {
                                    Log.e(TAG, "Failed to cache image: " + message);
                                    // Fall back to the original URL
                                    mainHandler.post(() -> callback.onSuccess(imageUrl));
                                }
                            });
                        } else {
                            // No cache manager, just return the URL
                            mainHandler.post(() -> callback.onSuccess(imageUrl));
                        }
                        return;
                    }
                }

                Log.d(TAG, "No album art found for " + artist + " - " + track);
                mainHandler.post(() -> callback.onError("No album art found"));
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        });
    }
    public static class PlaylistTracksResponse {
        @SerializedName("items")
        public List<TrackItem> items;
    }

    public static class TrackItem {
        @SerializedName("track")
        public Track track;
    }
    public interface TracksCallback {
        void onTracksFetched(List<MusicTrack> tracks);
        void onError(String message);
    }

    public void fetchTopTracksFromPlaylist(String playlistId, int limit, TracksCallback callback) {
        if (accessToken == null || accessToken.isEmpty()) {
            Log.w(TAG, "Access token not ready, retrying in 2s...");
            authenticateSpotify(); // start auth
            mainHandler.postDelayed(() ->
                    fetchTopTracksFromPlaylist(playlistId, limit, callback), 2000);
            return;
        }

        Call<PlaylistTracksResponse> call = spotifyApi.getPlaylistTracks(
                "Bearer " + accessToken,
                playlistId,
                limit
        );

        call.enqueue(new Callback<PlaylistTracksResponse>() {
            @Override
            public void onResponse(Call<PlaylistTracksResponse> call, Response<PlaylistTracksResponse> response) {
                if (response.code() == 401) {
                    Log.w(TAG, "Token expired, refreshing...");
                    accessToken = null;
                    authenticateSpotify();
                    mainHandler.postDelayed(() ->
                            fetchTopTracksFromPlaylist(playlistId, limit, callback), 2000);
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<TrackItem> items = response.body().items;
                    List<MusicTrack> musicTracks = new ArrayList<>();

                    for (TrackItem item : items) {
                        if (item.track != null && item.track.album != null) {
                            MusicTrack track = new MusicTrack();
                            track.setTitle(item.track.name);
                            track.setArtist(item.track.artists != null && !item.track.artists.isEmpty()
                                    ? item.track.artists.get(0).name : "Unknown");
                            track.setAlbum(item.track.album.name);
                            track.setSpotifyUrl("https://open.spotify.com/track/" + item.track.id);

                            if (item.track.album.images != null && !item.track.album.images.isEmpty()) {
                                track.setAlbumArt(item.track.album.images.get(0).url);
                            }

                            track.setDeezerLink(""); // Not needed here
                            musicTracks.add(track);
                        }
                    }

                    mainHandler.post(() -> callback.onTracksFetched(musicTracks));
                } else {
                    try {
                        Log.e(TAG, "Failed to fetch playlist: " + response.code() + " - " +
                                (response.errorBody() != null ? response.errorBody().string() : "no body"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mainHandler.post(() -> callback.onError("Failed to fetch playlist"));
                    Log.e(TAG, "Response failed! Code: " + response.code());

                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to read error body", e);
                        }
                    }
                    Log.d(TAG, "Access token: " + accessToken);

                }
            }

            @Override
            public void onFailure(Call<PlaylistTracksResponse> call, Throwable t) {
                Log.e(TAG, "Spotify API call failed", t);
                mainHandler.post(() -> callback.onError("API call failed: " + t.getMessage()));
            }
        });
    }



    public interface AlbumArtCallback {
        void onSuccess(String imageUrl);
        void onError(String message);
    }
}