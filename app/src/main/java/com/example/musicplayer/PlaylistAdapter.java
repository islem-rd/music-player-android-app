package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.SongViewHolder> {

    private List<item> songs;
    private Context context;
    private OnSongActionListener listener;
    private PlaylistManager playlistManager;
    private AlertDialog dialog;

    public interface OnSongActionListener {
        void onSongClick(int position);
        void onSongRemove(int position);
    }

    public PlaylistAdapter(List<item> songs, Context context, OnSongActionListener listener) {
        this.songs = songs;
        this.context = context;
        this.listener = listener;
        this.playlistManager = new PlaylistManager(context);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_view2, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        item song = songs.get(position);

        holder.tvSongTitle.setText(song.getSong());
        holder.tvArtistName.setText(song.getSinger());
        holder.tvSongDuration.setText(song.getDuree());

        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(song.getPath());
            byte[] art = mmr.getEmbeddedPicture();

            if (art != null) {
                Glide.with(context)
                        .asBitmap()
                        .load(art)
                        .placeholder(R.drawable.albumcover)
                        .error(R.drawable.albumcover)
                        .into(holder.imgSongCover);
            } else {
                holder.imgSongCover.setImageResource(R.drawable.albumcover);
            }
            mmr.release();
        } catch (Exception e) {
            holder.imgSongCover.setImageResource(R.drawable.albumcover);
        }

        holder.btnMoreOptions.setOnClickListener(v -> showPopupMenu(v, position));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(position);
            }
        });
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.playlist_song_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_remove_song) {
                if (listener != null) {
                    listener.onSongRemove(position);
                }
                return true;
            }
            return false;
        });

        popupMenu.show();
    }


    private void showCreatePlaylistDialog(item songToAdd) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_create_playlist, null);
        EditText etPlaylistName = dialogView.findViewById(R.id.etPlaylistName);
        EditText etPlaylistDescription = dialogView.findViewById(R.id.etPlaylistDescription);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etPlaylistName.getText().toString().trim();
                    String description = etPlaylistDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(context, "Please enter a playlist name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Liste newPlaylist = new Liste(name, description.isEmpty() ? "Custom Playlist" : description, R.drawable.albumcover);
                    newPlaylist.addSong(songToAdd);

                    List<Liste> playlists = playlistManager.getPlaylists();
                    playlists.add(newPlaylist);
                    playlistManager.savePlaylists(playlists);

                    Toast.makeText(context, "Added to new playlist: " + name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null);

        dialog = builder.create();
        dialog.show();
    }

    private void addSongToPlaylist(item song, Liste playlist) {
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
            Toast.makeText(context, "Song already exists in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        playlist.addSong(song);

        List<Liste> playlists = playlistManager.getPlaylists();
        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getPlaylisttitle().equals(playlist.getPlaylisttitle())) {
                playlists.set(i, playlist);
                break;
            }
        }
        playlistManager.savePlaylists(playlists);

        Toast.makeText(context, "Added to playlist: " + playlist.getPlaylisttitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSongCover;
        TextView tvSongTitle;
        TextView tvArtistName;
        TextView tvSongDuration;
        ImageButton btnMoreOptions;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSongCover = itemView.findViewById(R.id.imgSongCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            tvSongDuration = itemView.findViewById(R.id.tvSongDuration);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}
