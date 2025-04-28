package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryManager {
    private static final String TAG = "HistoryManager";
    private static final int MAX_HISTORY_SIZE = 20;
    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public HistoryManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    // Update the addToHistory method to better organize history by user
    public void addToHistory(item song) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "User not logged in, skipping history update");
            return;
        }

        if (song == null || song.getPath() == null) {
            Log.d(TAG, "Invalid song data, skipping history update");
            return;
        }

        // Client-side validation
        if (!isUserAuthenticated()) {
            Log.d(TAG, "User not authenticated, skipping history update");
            return;
        }

        // Create a unique document ID to prevent duplicates in history
        String documentId = user.getUid() + "_" + song.getPath().hashCode();

        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("song", song.getSong());
        historyItem.put("singer", song.getSinger());
        historyItem.put("path", song.getPath());
        historyItem.put("img", song.getImg());
        historyItem.put("userId", user.getUid());
        historyItem.put("timestamp", FieldValue.serverTimestamp());

        // Store history in a user-specific subcollection for better organization
        db.collection("users")
                .document(user.getUid())
                .collection("history")
                .document(documentId)
                .set(historyItem, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "History item added"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding history item", e));
    }

    // Update the getHistory method to retrieve from the new structure
    public void getHistory(OnHistoryLoadedListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            listener.onHistoryLoaded(new ArrayList<>());
            return;
        }

        // Query the user-specific history subcollection
        db.collection("users")
                .document(user.getUid())
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_HISTORY_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<item> history = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Additional client-side validation
                        if (document.getString("userId") != null &&
                                document.getString("userId").equals(user.getUid())) {
                            item song = new item(
                                    document.getString("singer"),
                                    document.getString("song"),
                                    document.getString("path"),
                                    "0:00",
                                    document.getString("img")
                            );
                            history.add(song);
                        }
                    }
                    listener.onHistoryLoaded(history);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting history", e);
                    listener.onHistoryLoaded(new ArrayList<>());
                });
    }

    // Update the clearHistory method to work with the new structure
    public void clearHistory(OnHistoryClearedListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            listener.onHistoryCleared(false);
            return;
        }

        // Delete the entire history subcollection for the user
        db.collection("users")
                .document(user.getUid())
                .collection("history")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> listener.onHistoryCleared(true))
                            .addOnFailureListener(e -> listener.onHistoryCleared(false));
                })
                .addOnFailureListener(e -> listener.onHistoryCleared(false));
    }

    private boolean isUserAuthenticated() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && !user.isAnonymous();
    }

    // Update the removeFromHistory method to work with the new structure
    public boolean removeFromHistory(String path) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || path == null) {
            return false;
        }

        // Create the same document ID used when adding to history
        String documentId = user.getUid() + "_" + path.hashCode();

        db.collection("users")
                .document(user.getUid())
                .collection("history")
                .document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "History item removed"))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing history item", e));

        return true; // Consider using a callback instead of returning a boolean
    }


    public interface OnHistoryLoadedListener {
        void onHistoryLoaded(List<item> history);
    }

    public interface OnHistoryClearedListener {
        void onHistoryCleared(boolean success);
    }
}
