package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Adapterpl extends RecyclerView.Adapter<Adapterpl.PlaylistViewHolder> {
    private List<Liste> playlists;
    private Context context;

    public Adapterpl(Context context, List<Liste> playlists) {
        this.context = context;
        this.playlists = playlists;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Liste playlist = playlists.get(position);
        holder.playlistName.setText(playlist.getPlaylisttitle());
        holder.playlistDescription.setText(playlist.getDescriptplaylist());

        // Check if there's a custom cover image URI
        if (playlist.getCoverImageUri() != null && !playlist.getCoverImageUri().isEmpty()) {
            // Try to load the image from the URI
            holder.playlistCover.setImageURI(Uri.parse(playlist.getCoverImageUri()));

            // If the image couldn't be loaded (URI might be invalid), fall back to the default
            if (holder.playlistCover.getDrawable() == null) {
                holder.playlistCover.setImageResource(playlist.getCoverResourceId());
            }
        } else {
            // No custom image, use the default resource
            holder.playlistCover.setImageResource(playlist.getCoverResourceId());
        }
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView playlistCover;
        TextView playlistName;
        TextView playlistDescription;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistCover = itemView.findViewById(R.id.imgSongCover);
            playlistName = itemView.findViewById(R.id.songname);
            playlistDescription = itemView.findViewById(R.id.artist);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Liste selectedPlaylist = playlists.get(position);
                    Intent intent = new Intent(context, PlaylistActivity.class);
                    intent.putExtra("PLAYLIST_NAME", selectedPlaylist.getPlaylisttitle());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required when starting activity from adapter
                    context.startActivity(intent);
                }
            });
        }
    }
}
