package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
public class PlaylistSelectionAdapter extends RecyclerView.Adapter<PlaylistSelectionAdapter.PlaylistViewHolder> {

    private List<Liste> playlists;
    private OnPlaylistSelectedListener listener;

    public interface OnPlaylistSelectedListener {
        void onPlaylistSelected(Liste playlist);
    }

    public PlaylistSelectionAdapter(List<Liste> playlists, OnPlaylistSelectedListener listener) {
        this.playlists = playlists != null ? playlists : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_selection, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Liste playlist = playlists.get(position);
        holder.tvPlaylistName.setText(playlist.getPlaylisttitle());

        try {
            holder.imgPlaylistCover.setImageResource(playlist.getCoverResourceId());
        } catch (Exception e) {
            holder.imgPlaylistCover.setImageResource(R.drawable.albumcover);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onPlaylistSelected(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlaylistCover;
        TextView tvPlaylistName;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlaylistCover = itemView.findViewById(R.id.imgPlaylistCover);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
        }
    }
}