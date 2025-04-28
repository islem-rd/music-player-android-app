package com.example.musicplayer;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class suggestionViewHolder extends RecyclerView.ViewHolder {
    private ImageView imgType;
    private TextView tvSuggestionText;
    private TextView tvType;
    private Context context;
    private SuggestionAdapter.OnSuggestionClickListener listener;

    public suggestionViewHolder(@NonNull View itemView, Context context, SuggestionAdapter.OnSuggestionClickListener listener) {
        super(itemView);
        this.context = context;
        this.listener = listener;

        imgType = itemView.findViewById(R.id.imgType);
        tvSuggestionText = itemView.findViewById(R.id.tvSuggestionText);
        tvType = itemView.findViewById(R.id.tvType);

        itemView.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onSuggestionClick((suggestion) itemView.getTag());
            }
        });
    }

    public void bind(suggestion s) {
        // Store the suggestion in the view's tag for easy retrieval in click listener
        itemView.setTag(s);

        tvSuggestionText.setText(s.getText());
        tvType.setText(s.getTypeString());

        // Set icon based on type
        switch (s.getType()) {
            case suggestion.TYPE_SONG:
                imgType.setImageResource(R.drawable.icmusic);
                imgType.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                break;
            case suggestion.TYPE_ARTIST:
                imgType.setImageResource(R.drawable.artist);
                imgType.setColorFilter(ContextCompat.getColor(context, R.color.colorPurple));
                break;
            case suggestion.TYPE_PLAYLIST:
                imgType.setImageResource(R.drawable.playlist);
                imgType.setColorFilter(ContextCompat.getColor(context, R.color.colorAmber));
                break;
        }
    }
}


