package com.example.musicplayer;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_PLAYLIST = 1;
    private static final int TYPE_FOOTER = 2;
    private List<Liste> playlist;
    private OnplClickListener listener;
    private Context context;
    private PlaylistManager playlistManager;

    public interface OnplClickListener {
        void onplClick(int position);
    }

    public ListAdapter(List<Liste> pl, OnplClickListener listener, Context context) {
        this.playlist = pl;
        this.listener = listener;
        this.context = context;
        this.playlistManager = new PlaylistManager(context);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == playlist.size()) {
            return TYPE_FOOTER; // Last item is the footer
        }
        return TYPE_PLAYLIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plfooter_layout, parent, false);
            return new FooterViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_view, parent, false);
            return new ListViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ListViewHolder) {
            Liste pl = playlist.get(position);
            ((ListViewHolder) holder).plTitle.setText(pl.getPlaylisttitle());
            ((ListViewHolder) holder).plDesc.setText(pl.getDescriptplaylist());

            // Set the cover image - first try the custom URI, then fall back to resource ID
            if (pl.getCoverImageUri() != null) {
                ((ListViewHolder) holder).plCover.setImageURI(Uri.parse(pl.getCoverImageUri()));
                // If the URI is invalid or image can't be loaded, fall back to default
                if (((ListViewHolder) holder).plCover.getDrawable() == null) {
                    ((ListViewHolder) holder).plCover.setImageResource(pl.getCoverResourceId());
                }
            } else {
                ((ListViewHolder) holder).plCover.setImageResource(pl.getCoverResourceId());
            }

            // Set up the 3-dot menu click listener
            ((ListViewHolder) holder).menuIcon.setOnClickListener(v -> {
                showPopupMenu(v, position);
            });
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind();
        }
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.playlist_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                deletePlaylist(position);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void deletePlaylist(int position) {
        if (position >= 0 && position < playlist.size()) {
            // Get the playlist name before removing it
            String playlistName = playlist.get(position).getPlaylisttitle();

            // Remove from the adapter list
            playlist.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, playlist.size() - position);

            // Remove from local storage
            playlistManager.removePlaylist(playlistName);

            // Show confirmation
            Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return playlist.size() + 1; // +1 to include footer
    }

    // Playlist ViewHolder
    public class ListViewHolder extends RecyclerView.ViewHolder {
        ImageView plCover;
        TextView plTitle;
        TextView plDesc;
        ImageView menuIcon;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            plTitle = itemView.findViewById(R.id.tvSongTitle);
            plDesc = itemView.findViewById(R.id.tvArtistName);
            plCover = itemView.findViewById(R.id.imgSongCover);
            menuIcon = itemView.findViewById(R.id.btnMoreOptions);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < playlist.size()) {
                    listener.onplClick(position);
                }
            });
        }
    }

    // Footer ViewHolder
    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind() {
            itemView.setOnClickListener(v -> {
                // Create a new playlist with a unique name
                String newPlaylistName = "New Playlist " + (playlist.size() + 1);
                Liste newPlaylist = new Liste(newPlaylistName, "Custom Playlist", R.drawable.albumcover);

                // Add to the list and notify adapter
                playlist.add(newPlaylist);
                notifyItemInserted(playlist.size() - 1);

                // Save to local storage
                playlistManager.savePlaylists(playlist);
            });
        }
    }
}
