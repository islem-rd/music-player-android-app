package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<suggestionViewHolder> {

    private List<suggestion> suggestions;
    private Context context;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(suggestion suggestion);
    }

    public SuggestionAdapter(Context context, OnSuggestionClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.suggestions = new ArrayList<>();
    }

    public void setSuggestions(List<suggestion> suggestions) {
        this.suggestions = suggestions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public suggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_suggestion, parent, false);
        return new suggestionViewHolder(view, context, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull suggestionViewHolder holder, int position) {
        suggestion suggestion = suggestions.get(position);
        holder.bind(suggestion);
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }
}
