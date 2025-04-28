package com.example.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class DiscoverTrackAdapter extends RecyclerView.Adapter<DiscoverTrackAdapter.ViewHolder> {

    private final Context context;
    private List<MusicTrack> tracks;
    private OnItemClickListener listener;
    private HistoryManager historyManager;
    private PlaylistManager playlistManager;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public DiscoverTrackAdapter(Context context, List<MusicTrack> tracks) {
        this.context = context;
        this.tracks = tracks;
        this.historyManager = new HistoryManager(context);
        this.playlistManager = new PlaylistManager(context);
    }

    // Setter for click listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Method to update the tracks list
    public void updateTracks(List<MusicTrack> newTracks) {
        this.tracks = newTracks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicTrack track = tracks.get(position);

        holder.songTitle.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());

        // Load album art with Glide
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.default_album_art)
                .error(R.drawable.default_album_art);

        if (track.getAlbumArt() != null && !track.getAlbumArt().isEmpty()) {
            if (track.getAlbumArt().startsWith("http")) {
                Glide.with(context)
                        .load(track.getAlbumArt())
                        .apply(requestOptions)
                        .into(holder.albumArt);
            } else {
                Glide.with(context)
                        .load(Uri.parse(track.getAlbumArt()))
                        .apply(requestOptions)
                        .into(holder.albumArt);
            }
        } else {
            holder.albumArt.setImageResource(R.drawable.default_album_art);
        }

        // Set item click listener to play the song
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });

        // Set more options click listener
        holder.moreOptions.setOnClickListener(v -> {
            showPopupMenu(v, position);
        });
    }

    private void showPopupMenu(View view, int position) {
        MusicTrack track = tracks.get(position);

        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.song_option_menu);



        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

           if (id == R.id.action_share) {
                shareSong(track);
                return true;
            } else if (id == R.id.action_add_to_playlist) {
                // Add option to add to playlist
                showAddToPlaylistDialog(track);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void removeFromHistory(int position) {
        MusicTrack track = tracks.get(position);

        // Remove from HistoryManager
        boolean removed = historyManager.removeFromHistory(track.getPath());

        if (removed) {
            // Remove from adapter's data
            tracks.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, tracks.size());

            Toast.makeText(context, "Removed from history", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSong(MusicTrack track) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this song!");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm listening to " + track.getTitle() + " by " + track.getArtist());
        context.startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showAddToPlaylistDialog(MusicTrack track) {
        // Get all playlists
        List<Liste> playlists = playlistManager.getPlaylists();

        if (playlists.isEmpty()) {
            Toast.makeText(context, "No playlists available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create playlist names array for dialog
        String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            playlistNames[i] = playlists.get(i).getPlaylisttitle();
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add to Playlist");
        builder.setItems(playlistNames, (dialog, which) -> {
            // Convert track to item
            item songItem = new item(
                    track.getArtist(),
                    track.getTitle(),
                    track.getPath(),
                    "0:00",
                    track.getAlbumArt()
            );

            // Add to selected playlist
            Liste selectedPlaylist = playlists.get(which);
            selectedPlaylist.addSong(songItem);
            playlistManager.savePlaylists(playlists);

            Toast.makeText(context, "Added to " + selectedPlaylist.getPlaylisttitle(), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Update the getItemCount method to limit displayed items to 5
    @Override
    public int getItemCount() {
        return tracks.size();
    }

    // Add a method to remove an item at a specific position
    public void removeItem(int position) {
        if (position >= 0 && position < tracks.size()) {
            MusicTrack track = tracks.get(position);

            // Remove from HistoryManager if it's a history item
            if (historyManager != null && track.getPath() != null) {
                historyManager.removeFromHistory(track.getPath());
            }

            // Remove from adapter's data
            tracks.remove(position);

            // Use more efficient notification methods
            notifyItemRemoved(position);

            // Only notify about range change if there are items after the removed one
            if (position < tracks.size()) {
                notifyItemRangeChanged(position, tracks.size() - position);
            }

            Toast.makeText(context, "Removed from history", Toast.LENGTH_SHORT).show();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        TextView songTitle;
        TextView artistName;
        ImageView moreOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            albumArt = itemView.findViewById(R.id.image_album);
            songTitle = itemView.findViewById(R.id.text_song_title);
            artistName = itemView.findViewById(R.id.text_artist);
            moreOptions = itemView.findViewById(R.id.image_more);
        }
    }
}
