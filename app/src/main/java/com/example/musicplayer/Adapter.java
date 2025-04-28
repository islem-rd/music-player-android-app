package com.example.musicplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private static final String TAG = "Adapter";
    Context c;
    List<item> items;
    private OnItemClickListener listener;
    private PlaylistManager playlistManager;
    private AlertDialog dialog;
    private MusicApiService musicApiService;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public Adapter(Context c, List<item> items) {
        this.c = c;
        this.items = items;
        this.playlistManager = new PlaylistManager(c);
        this.musicApiService = new MusicApiService();
    }

    // Setter for listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(c).inflate(R.layout.item_view, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        item currentItem = items.get(position);

        holder.song.setText(currentItem.getSong());
        holder.singer.setText(currentItem.getSinger());
        holder.duree.setText(currentItem.getDuree());

        // Load embedded image from song metadata
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(currentItem.getPath());

            byte[] art = mmr.getEmbeddedPicture(); // <-- This gets the album art
            if (art != null) {
                Glide.with(holder.itemView.getContext())
                        .asBitmap()
                        .load(art)
                        .placeholder(R.drawable.albumcover)
                        .error(R.drawable.albumcover)
                        .into(holder.imgv);
            } else {
                holder.imgv.setImageResource(R.drawable.albumcover);
            }
        } catch (Exception e) {
            holder.imgv.setImageResource(R.drawable.albumcover);
            Log.e(TAG, "Error loading embedded image: " + e.getMessage());
        } finally {
            try {
                mmr.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Setup heart and more options
        setupHeartAnimation(holder.heart, position);
        holder.moreOptions.setOnClickListener(v -> showSongOptionsMenu(v, position));
    }


    private void showSongOptionsMenu(View view, int position) {
        try {
            PopupMenu popupMenu = new PopupMenu(c, view);
            popupMenu.inflate(R.menu.song_options_menu);

            popupMenu.setOnMenuItemClickListener(item -> {
                try {
                    int id = item.getItemId();
                    if (id == R.id.action_add_to_playlist) {
                        showAddToPlaylistDialog(position);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(c, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return false;
            });

            popupMenu.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(c, "Menu error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddToPlaylistDialog(int position) {
        try {
            item songToAdd = items.get(position);
            List<Liste> playlists = playlistManager.getPlaylists();

            // Get a valid context for the dialog
            Context dialogContext = c;
            if (!(dialogContext instanceof Activity) && c != null) {
                // If we don't have an Activity context, try to get one
                if (c.getApplicationContext() instanceof Activity) {
                    dialogContext = (Activity) c.getApplicationContext();
                } else {
                    // If we can't get an Activity context, show a toast and return
                    Toast.makeText(c, "Cannot add to playlist at this time", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Use the safe context
            AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);

            View dialogView = LayoutInflater.from(dialogContext).inflate(R.layout.dialog_select_playlist, null);

            RecyclerView rvPlaylists = dialogView.findViewById(R.id.rvPlaylists);
            rvPlaylists.setLayoutManager(new LinearLayoutManager(dialogContext));

            PlaylistSelectionAdapter adapter = new PlaylistSelectionAdapter(playlists, playlist -> {
                addSongToPlaylist(songToAdd, playlist);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
            rvPlaylists.setAdapter(adapter);

            dialogView.findViewById(R.id.btnCreateNewPlaylist).setOnClickListener(v -> {
                if (dialog != null) {
                    dialog.dismiss();
                }
                showCreatePlaylistDialog(songToAdd);
            });

            dialog = builder.setView(dialogView).create();

            // Only show if we have a valid context
            if (dialogContext instanceof Activity) {
                Activity activity = (Activity) dialogContext;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    dialog.show();
                }
            } else {
                dialog.show();
            }
        } catch (Exception e) {
            Log.e("Adapter", "Error showing dialog", e);
            Toast.makeText(c, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreatePlaylistDialog(item songToAdd) {
        try {
            // Get a valid context for the dialog
            Context dialogContext = c;
            if (!(dialogContext instanceof Activity) && c != null) {
                // If we don't have an Activity context, try to get one
                if (c.getApplicationContext() instanceof Activity) {
                    dialogContext = (Activity) c.getApplicationContext();
                } else {
                    // If we can't get an Activity context, show a toast and return
                    Toast.makeText(c, "Cannot create playlist at this time", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            LayoutInflater inflater = LayoutInflater.from(dialogContext);
            View dialogView = inflater.inflate(R.layout.dialog_create_playlist, null);
            EditText etPlaylistName = dialogView.findViewById(R.id.etPlaylistName);
            EditText etPlaylistDescription = dialogView.findViewById(R.id.etPlaylistDescription);

            AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
            builder.setView(dialogView)
                    .setPositiveButton("Create", (dialog, which) -> {
                        String name = etPlaylistName.getText().toString().trim();
                        String description = etPlaylistDescription.getText().toString().trim();

                        if (name.isEmpty()) {
                            Toast.makeText(c, "Please enter a playlist name", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create new playlist
                        Liste newPlaylist = new Liste(name, description.isEmpty() ? "Custom Playlist" : description, R.drawable.albumcover);

                        // Add song to the new playlist
                        newPlaylist.addSong(songToAdd);

                        // Save the playlist
                        List<Liste> playlists = playlistManager.getPlaylists();
                        playlists.add(newPlaylist);
                        playlistManager.savePlaylists(playlists);

                        Toast.makeText(c, "Added to new playlist: " + name, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null);

            dialog = builder.create();

            // Only show if we have a valid context
            if (dialogContext instanceof Activity) {
                Activity activity = (Activity) dialogContext;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    dialog.show();
                }
            } else {
                dialog.show();
            }
        } catch (Exception e) {
            Log.e("Adapter", "Error showing create playlist dialog", e);
            Toast.makeText(c, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addSongToPlaylist(item song, Liste playlist) {
        // Check if song is already in the playlist
        boolean songExists = false;
        List<item> playlistSongs = playlist.getSongs();

        if (playlistSongs != null) {
            for (item existingSong : playlistSongs) {
                if (existingSong.getPath() != null && existingSong.getPath().equals(song.getPath())) {
                    songExists = true;
                    break;
                }
            }
        }

        if (songExists) {
            Toast.makeText(c, "Song already exists in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add song to playlist
        playlist.addSong(song);

        // Save updated playlist
        List<Liste> playlists = playlistManager.getPlaylists();
        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getPlaylisttitle().equals(playlist.getPlaylisttitle())) {
                playlists.set(i, playlist);
                break;
            }
        }
        playlistManager.savePlaylists(playlists);

        Toast.makeText(c, "Added to playlist: " + playlist.getPlaylisttitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder class (now inside Adapter)
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgv, heart, moreOptions;
        TextView singer, song, duree;

        public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);

            heart = itemView.findViewById(R.id.heart2);
            duree = itemView.findViewById(R.id.tvSongDuration);
            imgv = itemView.findViewById(R.id.imgSongCover);
            singer = itemView.findViewById(R.id.tvArtistName);
            song = itemView.findViewById(R.id.tvSongTitle);
            moreOptions = itemView.findViewById(R.id.btnMoreOptions);

            // Set click listener on the item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }

    // Heart animation logic with favorites functionality
    private void setupHeartAnimation(ImageView heart, int position) {
        // Check if song is in favorites and set initial state
        item song = items.get(position);
        boolean isInFavorites = isSongInFavorites(song);
        heart.setImageResource(isInFavorites ? R.drawable.redheart : R.drawable.heart);
        heart.setTag(isInFavorites);

        heart.setOnClickListener(v -> {
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(heart, "scaleX", 0.9f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(heart, "scaleY", 0.9f);
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(100);
            animatorSet.start();

            boolean isLiked = (heart.getTag() != null && (boolean) heart.getTag());
            heart.setImageResource(isLiked ? R.drawable.heart : R.drawable.redheart);
            heart.setTag(!isLiked);

            // Handle favorites functionality
            if (!isLiked) {
                // Add to favorites
                addToFavorites(position);
            } else {
                // Remove from favorites
                removeFromFavorites(position);
            }

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    AnimatorSet resetAnimator = new AnimatorSet();
                    ObjectAnimator scaleXReset = ObjectAnimator.ofFloat(heart, "scaleX", 1f);
                    ObjectAnimator scaleYReset = ObjectAnimator.ofFloat(heart, "scaleY", 1f);
                    resetAnimator.playTogether(scaleXReset, scaleYReset);
                    resetAnimator.setDuration(100);
                    resetAnimator.start();
                }
            });
        });
    }

    // Check if a song is in the Favorites playlist
    private boolean isSongInFavorites(item song) {
        List<Liste> playlists = playlistManager.getPlaylists();
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals("Favorites")) {
                List<item> favoriteSongs = playlist.getSongs();
                if (favoriteSongs != null) {
                    for (item favSong : favoriteSongs) {
                        // Compare by path which should be unique
                        if (favSong.getPath() != null && favSong.getPath().equals(song.getPath())) {
                            return true;
                        }
                    }
                }
                break;
            }
        }
        return false;
    }

    // Add a song to the Favorites playlist
    private void addToFavorites(int position) {
        item song = items.get(position);
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
                if (favSong.getPath() != null && favSong.getPath().equals(song.getPath())) {
                    alreadyInFavorites = true;
                    break;
                }
            }
        }

        if (!alreadyInFavorites) {
            favoritesPlaylist.addSong(song);
            playlistManager.savePlaylists(playlists);
            Toast.makeText(c, "Added to Favorites", Toast.LENGTH_SHORT).show();
        }
    }

    // Remove a song from the Favorites playlist
    private void removeFromFavorites(int position) {
        item song = items.get(position);
        List<Liste> playlists = playlistManager.getPlaylists();

        // Find the Favorites playlist
        for (Liste playlist : playlists) {
            if (playlist.getPlaylisttitle().equals("Favorites")) {
                List<item> favoriteSongs = playlist.getSongs();
                if (favoriteSongs != null) {
                    for (int i = 0; i < favoriteSongs.size(); i++) {
                        item favSong = favoriteSongs.get(i);
                        if (favSong.getPath() != null && favSong.getPath().equals(song.getPath())) {
                            favoriteSongs.remove(i);
                            playlistManager.savePlaylists(playlists);
                            Toast.makeText(c, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }
                break;
            }
        }
    }
}