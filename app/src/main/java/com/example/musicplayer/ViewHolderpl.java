package com.example.musicplayer;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ViewHolderpl extends RecyclerView.ViewHolder {
    ImageView imgpl,footer;
    TextView singerpl, songpl;

    public ViewHolderpl(@NonNull View itemView) {
        super(itemView);
        // Initializing the views from the item_playlist layout
        footer = itemView.findViewById(R.id.imgPlaylistCover2);
        imgpl = itemView.findViewById(R.id.imgSongCover);
        singerpl = itemView.findViewById(R.id.artist);
        songpl = itemView.findViewById(R.id.songname);
    }
}
