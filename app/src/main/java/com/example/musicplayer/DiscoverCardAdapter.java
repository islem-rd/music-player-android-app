package com.example.musicplayer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class DiscoverCardAdapter extends RecyclerView.Adapter<DiscoverCardAdapter.ViewHolder> {

    private final Context context;
    private final List<MusicTrack> tracks;
    private OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public DiscoverCardAdapter(Context context, List<MusicTrack> tracks) {
        this.context = context;
        this.tracks = tracks;
    }

    // Setter for click listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_card, parent, false);
        return new ViewHolder(view);
    }

    // Add this to the onBindViewHolder method in DiscoverCardAdapter.java
    // Update the onBindViewHolder method in DiscoverCardAdapter.java
    // Update the onBindViewHolder method in DiscoverCardAdapter.java
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            MusicTrack track = tracks.get(position);

            if (track != null) {
                // Set title and artist with null checks
                holder.songTitle.setText(track.getTitle() != null ? track.getTitle() : "Unknown Title");
                holder.artistName.setText(track.getArtist() != null ? track.getArtist() : "Unknown Artist");

                // Load album art with Glide
                RequestOptions requestOptions = new RequestOptions()
                        .transform(new RoundedCorners(16))
                        .placeholder(R.drawable.default_album_art)
                        .error(R.drawable.default_album_art);

                if (track.getAlbumArt() != null && !track.getAlbumArt().isEmpty()) {
                    if (track.getAlbumArt().startsWith("http")) {
                        // Load from URL (API)
                        Glide.with(holder.itemView.getContext())
                                .load(track.getAlbumArt())
                                .apply(requestOptions)
                                .into(holder.albumArt);
                    } else {
                        // Load from local URI
                        Glide.with(holder.itemView.getContext())
                                .load(Uri.parse(track.getAlbumArt()))
                                .apply(requestOptions)
                                .into(holder.albumArt);
                    }
                } else {
                    holder.albumArt.setImageResource(R.drawable.default_album_art);
                }

                

                // Set click listener
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(position);
                    }
                });
            }
        } catch (Exception e) {
            Log.e("DiscoverCardAdapter", "Error binding view holder: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return tracks != null ? tracks.size() : 0;
    }

    // Add this field to the ViewHolder class in DiscoverCardAdapter
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        TextView songTitle;
        TextView artistName;
        ImageView deezerIcon; // Add this field

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            albumArt = itemView.findViewById(R.id.image_album);
            songTitle = itemView.findViewById(R.id.text_song_title);
            artistName = itemView.findViewById(R.id.text_artist);
            deezerIcon = itemView.findViewById(R.id.deezer_icon); // Initialize the Deezer icon
        }
    }
}