package com.example.musicplayer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;
import android.view.View;

public class ActivityTransitionHelper {
    public static final int TRANSITION_SLIDE_RIGHT = 1;
    public static final int TRANSITION_SLIDE_LEFT = 2;
    public static final int TRANSITION_FADE = 3;

    public static void startActivityWithMiniPlayerTransition(
            Activity activity,
            Intent intent,
            View miniPlayer,
            View albumArt,
            View songTitle,
            View artistName) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create the transition pairs for shared elements
            Pair<View, String> miniPlayerPair = Pair.create(miniPlayer, "mini_player_transition");
            Pair<View, String> albumArtPair = Pair.create(albumArt, "album_art_transition");
            Pair<View, String> songTitlePair = Pair.create(songTitle, "song_title_transition");
            Pair<View, String> artistNamePair = Pair.create(artistName, "artist_name_transition");

            // Create the activity options with the transitions
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    miniPlayerPair,
                    albumArtPair,
                    songTitlePair,
                    artistNamePair
            );

            // Start the activity with the options
            activity.startActivity(intent, options.toBundle());
        } else {
            // Fallback for older devices
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    public static void startActivityWithTransition(
            Activity activity,
            Intent intent,
            int transitionType,
            boolean isReversed) {

        activity.startActivity(intent);

        switch (transitionType) {
            case TRANSITION_SLIDE_RIGHT:
                if (isReversed) {
                    activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                } else {
                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
                break;
            case TRANSITION_SLIDE_LEFT:
                if (isReversed) {
                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
                break;
            case TRANSITION_FADE:
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
        }
    }

    public static void startActivityWithDiscoverTransition(
            Activity activity,
            Intent intent,
            View miniPlayer,
            View albumArt,
            View songTitle,
            View artistName) {

        // Similar to mini player transition but with different animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Pair<View, String> miniPlayerPair = Pair.create(miniPlayer, "mini_player_transition");
            Pair<View, String> albumArtPair = Pair.create(albumArt, "album_art_transition");
            Pair<View, String> songTitlePair = Pair.create(songTitle, "song_title_transition");
            Pair<View, String> artistNamePair = Pair.create(artistName, "artist_name_transition");

            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    miniPlayerPair,
                    albumArtPair,
                    songTitlePair,
                    artistNamePair
            );

            activity.startActivity(intent, options.toBundle());
        } else {
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    public static void setupBackNavigation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.postponeEnterTransition();
            activity.getWindow().getDecorView().post(() -> activity.startPostponedEnterTransition());
        }
    }
}